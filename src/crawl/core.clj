(ns crawl.core
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

(defn fetch [code]
  (go "https://www.medicare.gov/find-a-plan/questions/home.aspx")
  (send-key (by-css-selector "div#zip-code-field > input") code)
  (click (by-css-selector "input[type=submit][alternatetext=\"Find Plans\"]")))
  
(defn run []
  ;;(new-driver)
  (go "https://www.medicare.gov/find-a-plan/questions/home.aspx")
  (send-key (by-css-selector "div#zip-code-field > input") "17331")
  (click (by-css-selector "input[type=submit][alternatetext=\"Find Plans\"]"))
  (Thread/sleep 1000)
  (wait-for-loading)
  (Thread/sleep 1000)
  (wait-for-display (find-element (by-css-selector "div.SplitCountyModalPopup")))
  (click-labels "YORK" *driver*)
  (click (by-css-selector "input[type=submit][value=Continue]"))
  (Thread/sleep 1000)
  (wait-for-loading)
  )

;;  (go "https://www.medicare.gov/find-a-plan/questions/home.aspx")
;;  (send-key (by-css-selector "div#zip-code-field > input") "17331")
;;  (click (by-css-selector "input[type=submit][alternatetext=\"Find Plans\"]"))

;;  (wait-for (partial click-labels "YORK") (by-css-selector "div.SplitCountyModalPopup"))
;;  (click (by-css-selector "input[type=submit][value=Continue]"))

;;  (wait-for (partial click-labels "I don't know") (by-css-selector "div#GeneralQuestionsContent"))
;;  (click (by-css-selector "input[type=button][alternatetext=\"Continue to Plan Results\")]"))