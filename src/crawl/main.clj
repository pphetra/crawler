(ns crawl.main
  (:use crawl.core crawl.plan crawl.data somnium.congomongo)
  (:import
   (java.util.concurrent LinkedBlockingQueue)
   (org.openqa.selenium.chrome ChromeDriver)))

(def *fips-queue* (LinkedBlockingQueue.))

(def *plan-queue* (LinkedBlockingQueue.))

(def *error-fips-queue* (LinkedBlockingQueue.))

(defn new-extract-by-fips-task []
  (.start
   (Thread.
    (fn []
      (binding [*driver* (ChromeDriver.)]
	(mongo! :db "medicare")
	(doseq [fip (repeatedly #(.take  *fips-queue*))]
	  (try (doseq [plan (remove nil? (extract-plan-by-fip fip))]
		 (.put *plan-queue* plan))
	       (catch Exception e
		 (.put *error-fips-queue* {:e e :fip fip})))))))))

(defn new-extract-benefit-task []
  (.start
   (Thread.
    (fn []
      (binding [*driver* (ChromeDriver.)]
	(mongo! :db "medicare")
	(goto-plan-result)
	(doseq [plan (repeatedly #(.take *plan-queue*))]
	  (go-plan plan)
	  (save-benefits plan)
	  (mark-completed plan)))))))

(defn queue-all-fips []
  (doseq [fip *fips-zips*]
    (.put *fips-queue* fip)))

(defn reset-db []
  (mongo! :db "medicare")
  (destroy! :plans {}))