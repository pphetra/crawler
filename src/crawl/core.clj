(ns crawl.core
  (:use crawl.plan somnium.congomongo)
  (:import (org.openqa.selenium By)
     (java.io File)
     (org.openqa.selenium WebDriver NoSuchElementException WebDriverException)
     (org.openqa.selenium.htmlunit HtmlUnitDriver)
     (org.openqa.selenium.firefox FirefoxDriver)
     (java.util.concurrent LinkedBlockingQueue)
     (org.openqa.selenium.chrome ChromeDriver ChromeProfile ChromeExtension)))


(defn new-driver-no-image []
  (def *driver* (new ChromeDriver
		     (ChromeProfile. (File. "/tmp"))
		     (ChromeExtension.)
		     )))

(defn new-driver []
  (def *driver* (ChromeDriver.)))

(defn init-queue []
  (def *queue* (LinkedBlockingQueue.)))

(defn start-thread [driver func]
  (.start
   (Thread.
    (fn []
      (loop []
	(let [code (.take *queue*)]
	  (binding [*driver* driver]
	    (func code)))
	(recur))))))

(defn new-html-driver []
  (def *driver* (HtmlUnitDriver. true)))

(defn go [url]
  (.get *driver* url))

(defn by-css-selector [css]
  (By/cssSelector css))

(defn find-element [by]
  (.findElement *driver* by))

(defn find-elements-in [by elm]
  (.findElements elm by))

(defn send-key [by txt]
  (let [elm (.findElement *driver* by)]
    (.sendKeys elm (into-array [txt]))))

(defn click [by]
  (let [elm (.findElement *driver* by)]
    (.click elm)))

(defn click-all-in [by elm]
  (let [elms (find-elements-in by elm)]
    (map (fn [elm] (.click elm)) elms)))

(defn find-labels [txt elm]
  (let [by (by-css-selector "label")
        elms (find-elements-in by elm)]
    (filter (fn [e] (= (.getText e) txt)) elms)))

(defn click-labels [txt elm]
  (map (fn [e] (.click e)) (find-labels txt elm)))

(defn wait-for [fn by]
  (let [elm (try (find-element by) (catch NoSuchElementException e nil))]
    (if (nil? elm)
      (do
        (Thread/sleep 2000)
        (recur fn by))
      (fn elm)
      )))

(defn wait-for-display [elm]
  (if-not (re-find #"display" (.getAttribute elm "style"))
    elm
    (do
      (Thread/sleep 2000)
      (recur elm))))

(defn js [script]
  (.executeScript *driver* script (into-array [])))

(defn wait-for-loading []
  (let [ret (try (js "return document.title") (catch WebDriverException e nil))]
    (if (nil? ret)
      (do
	(Thread/sleep 2000)
	(recur))
      true)))

(defn wait-for-element [by millisec]
  (let [start (System/currentTimeMillis)
	end (+ start millisec)]
    (loop []
      (Thread/sleep 1000)
      (if (> (System/currentTimeMillis) end)
	false
	(let [elm (try (find-element by)
		       (catch NoSuchElementException e nil))]
	  (if (nil? elm)
	    (recur)
	    elm))))))

(defn start []
  (.get *driver* "https://www.medicare.gov/find-a-plan/questions/home.aspx"))

(defn enter-zip-code [zipCode]
  (send-key (by-css-selector "div#zip-code-field > input") zipCode)
  (click (by-css-selector "input[type=submit][alternatetext=\"Find Plans\"]")))

(defn click-i-dont-know []
  (click-labels "I don't know" *driver*))

(defn click-continue1 []
  (click (by-css-selector "input[type=button][alternatetext=\"Continue to Plan Results\"]")))
    
(defn answer-i-dont-know []
  (do
    (.click (.findElement *driver* (by-css-selector "label[title=\"I don't know what medicare coverage i have\"]")))
    (.click (.findElement *driver* (by-css-selector "label[title=\"I don't know what help i am getting\"]")))
    (click (by-css-selector "input[type=button][alternatetext=\"Continue to Plan Results\"]"))))

(defn answer-no-drug []
  (click (by-css-selector "a[title=\"I don't want to add drugs now\"]")))

(defn answer-no-phamacies []
  (click (by-css-selector "a#lnkDontAddDrugs")))

(defn click-continue []
  (click (by-css-selector "input[type=button][value=\"Continue To Plan Results\"]")))
      
(defn extract-plan [div type]
  (let [root (.findElement *driver* (by-css-selector div))
	elms (.findElements root (by-css-selector ".planName > a"))]
    (map (fn [elm]
	   (list type (.getText elm))) elms)))

;; Prescription Drug Plans
(defn extract-pdp []
  (extract-plan "div#pdpContentWrapper" "pdp"))

;; Medicare Health Plans with drug coverage
(defn extract-mapd []
  (extract-plan "div#mapdContentWrapper" "mapd"))

;; Medicare Health Plans without drug coverage
(defn extract-ma []
  (extract-plan "div#maContentWrapper" "ma"))

(defn extract-all-plan []
  (let [pdp (extract-pdp)
	mapd (extract-mapd)
	ma (extract-ma)]
    (concat pdp mapd ma)))
	
(defn dump-plans []
  (doseq [plan (extract-all-plan)]
    (let [type (first plan)
	  text (second plan)]
      (println (format "%s --> %s\n" type text)))))

(defn save-plans [zipCode]
  (map (fn [plan]
	     (let [type (first plan)
		   text (second plan)
		   matches (re-matches #"(.*)\(((\w{5})-(\d{3})-(\d))\).*" text)
		   name (nth matches 2)
		   desc (.trim (nth matches 1))]
	       (println type text)
	       (println matches)
	       (if (insert-plan name text desc type (if (= type "ma") true false) zipCode)
		 name
		 nil)))
       (extract-all-plan)))

(def *test-mdp* '("FreedomBlue PPO HD Rx (PPO) (H3916-025-0)" "Advantra Elite (PPO) (H5522-008-0)" "Geisinger Gold Classic 3 $0 Deductible Rx (HMO) (H3954-100-0)" "SeniorBlue - Option 2 (PPO) (H3923-013-0)" "Bravo Classic (HMO) (H3949-002-0)" "UnitedHealthcare MedicareComplete (HMO) (H3920-001-0)" "Bravo Achieve (HMO SNP) (H3949-024-0) (SNP)" "SecureHorizons MedicareComplete Choice (PPO) (H3921-001-0)" "Advantra Silver (PPO) (H5522-004-0)" "Evercare Plan IP (PPO SNP) (H3912-001-0) (SNP)"))

(def *test-ma* '("Geisinger Gold Classic 3 (HMO) (H3954-098-0)" "Geisinger Gold Preferred 1 (PPO) (H3924-021-0)" "HumanaChoice R5826-062 (Regional PPO) (R5826-062-0)" "Geisinger Gold Preferred 2 (PPO) (H3924-045-0)" "Geisinger Gold Reserve (MSA) (H8468-001-0)" "UnitedHealthcare MedicareComplete Essential (HMO) (H3920-007-0)" "FreedomBlue PPO Value (PPO) (H3916-012-0)" "Humana Gold Choice H8145-055 (PFFS) (H8145-055-0)" "Today's Options Premier 800 (PFFS) (H2816-008-0)" "Today's Options Advantage 900 (PPO) (H2775-095-0)"))

(def *test-pdp* '("Humana Walmart-Preferred Rx Plan (PDP) (S5884-104-0)" "AARP MedicareRx Preferred (PDP) (S5820-005-0)" "Humana Enhanced (PDP) (S5884-005-0)" "MedicareRx Rewards Plus (PDP) (S5960-144-0)" "First Health Part D Premier (PDP) (S5768-009-0)" "BlueRx Plus (PDP) (S5593-002-0)" "Health Net Orange Option 1 (PDP) (S5678-018-0)" "AmeriHealth Advantage (PDP) (S2770-001-0)" "CIGNA Medicare Rx Plan One (PDP) (S5617-215-0)" "Community CCRx Basic (PDP) (S5803-075-0)"))

(defn go-more-plan-detail [contract plan segment]
  (let [url (format "http://www.medicare.gov/find-a-plan/staticpages/plan-details-benefits-popup.aspx?cntrctid=%s&plnid=%s&sgmntid=%s&ctgry=" contract plan segment)]
    (.get *driver* url)))

(defn go-plan [code]
  (let [ary (.split code "-")
	contract (nth ary 0)
	plan (nth ary 1)
	segment (nth ary 2)]
    (go-more-plan-detail contract plan segment)))

(defn go-plan-detail [contract plan segment]
  (let [url (format "http://www.medicare.gov/find-a-plan/results/planresults/plan-details.aspx?cntrctid=%s&plnid=%s&sgmntid=%s#plan_benefits" contract plan segment)]
    (.get *driver* url)))

(defn click-tab-plan-benefit []
  (let [css (by-css-selector "a[href=\"#plan_benefits\"]")
	elm (.findElement *driver* css)]
    (.click elm)))

(defn extract-benefits []
  (let [elms (.findElements *driver* (by-css-selector "div.benefitsCategory"))]
    (map (fn [elm]
	   (let [header (.findElement elm (by-css-selector "div.benefitsCategoryHeader"))
		 text (.findElement elm (by-css-selector "div.benefitsCategoryText"))]
	     (list (.getText header) (.getText text))))
	 elms)))

(defn dump-benefits []
  (doseq [pair (extract-benefits)]
    (println (format "%s" (first pair)))
    (println "======================================")
    (println (second pair))
    (println "--------------------------------------")))

(defn save-benefits [plan]
  (doseq [pair (extract-benefits)]
    (insert-plan-detail plan (first pair) (second pair))))

(defn wait-and-do [by timeout msg fn]
  (if-not (wait-for-element by timeout)
    (throw (Exception. msg))
    (fn)))

(defn extract-and-save-plan-detail [zipCode]
  (let [plans (remove nil? (save-plans zipCode))]
    (println plans)
    (doseq [plan plans]
      (println (format "process plan %s" plan))
      (go-plan plan)
      (save-benefits plan)
      (mark-completed plan))))

(defn run [zipCode]
  (let [conn (make-connection :medicare)]
    (with-mongo conn
      (start)
      (enter-zip-code zipCode)
      (Thread/sleep 1000)
      (wait-and-do (by-css-selector "label[title=\"I don't know what medicare coverage i have\"]")
		   20000
		   "timeout when wait for step 1 of 4"
		   answer-i-dont-know)
      
      (wait-and-do (by-css-selector "a[title=\"I don't want to add drugs now\"]")
		   20000
		   "timeout when wait for step 2 of 4"
		   answer-no-drug)
      
      (wait-and-do (by-css-selector "a#lnkDontAddDrugs")
		   20000
		   "timeout when wait for step 3 of 4"
		   answer-no-phamacies)
      
      (wait-and-do (by-css-selector "input[type=button][value=\"Continue To Plan Results\"]")
		   20000
		   "timeout when wait for step 4 of 4"
		   click-continue)
      
      (wait-and-do (by-css-selector "div.planGroupResultsPanel")
		   60000
		   "timeout when wait for plan result"
		   dump-plans)
      
      (extract-and-save-plan-detail zipCode))
    (close-connection conn)))
