(ns crawl.main
  (:use crawl.core crawl.plan crawl.data somnium.congomongo crawl.stat crawl.server)
  (:import
   (java.util.concurrent LinkedBlockingQueue)
   (org.openqa.selenium.chrome ChromeDriver)))

(def *fips-queue* (LinkedBlockingQueue.))

(def *plan-queue* (LinkedBlockingQueue.))

(def *error-fips-queue* (LinkedBlockingQueue.))

(defn test-update-stat []
  (stat-update-process "hello"))

(defn test-binding []
  (.start
   (Thread.
    (fn []
      (binding [*tid* (.toString (rand))]
	(stat-new-thread "hi")
	(test-update-stat)))))
  (.start
   (Thread.
    (fn []
      (binding [*tid* (.toString (rand))]
	(stat-new-thread "hi")
	(test-update-stat))))))


(defn new-extract-by-fips-task []
  (.start
   (Thread.
    (fn []
      (binding [*driver* (ChromeDriver.)
		*tid* (.toString (rand))]
	(mongo! :db "medicare")
	(stat-new-thread "zip")
	(doseq [fip (repeatedly #(.take  *fips-queue*))]
	  (try (doseq [plan (remove nil? (extract-plan-by-fip fip))]
		 (.put *plan-queue* plan))
	       (catch Exception e
		 (.put *error-fips-queue* {:e e :fip fip})))
	  (Thread/sleep 1000)))))))

(defn new-extract-benefit-task []
  (.start
   (Thread.
    (fn []
      (binding [*driver* (ChromeDriver.)
		*tid* (.toString (rand))]
	(mongo! :db "medicare")
	(stat-new-thread "plan")
	(goto-plan-result)
	(doseq [plan (repeatedly #(.take *plan-queue*))]
	  (stat-begin-process plan "go-plan")
	  (go-plan plan)
	  (stat-update-process "save-benefit")
	  (save-benefits plan)
	  (mark-completed plan)
	  (stat-finish-process)))))))

(defn monitor-start []
  (start-server))

(defn queue-all-fips []
  (doseq [fip *fips-zips*]
    (.put *fips-queue* fip)))

(defn reset-db []
  (mongo! :db "medicare")
  (destroy! :plans {}))


(defn queue-unprocess-fips []
  (mongo! :db "medicare")
  (let [processed-fip (get-extracted-fip)]
    (doseq [fip-zip *fips-zips*]
      (let [fip (first fip-zip)]
	(if-not (.contains processed-fip fip)
	  (.put *fips-queue* fip-zip))))))

(defn queue-unprocess-plan []
  (mongo! :db "medicare")
  (doseq [plan (get-unprocess-plan)]
    (.put *plan-queue* plan)))
