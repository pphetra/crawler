(ns crawl.plan
  (:use somnium.congomongo))

(defn connect-db []
  (mongo!
   :db "crawl"))

(defn insert-plan [
		   id               ;; H3924-045-0
		   plan_name_long   ;; ?
		   contract_name    ;; Geisinger Gold Preferred 2 (PPO)
		   plan_type        ;; ma | mapd | pdp
		   cover_drugs      ;; true | false
		   fips             ;;
		   ]
  " return true if non-exist plan "
  (let [oldValue (fetch-one :plans :where { :_id id })
	ary (.split id "-")
	contract_h_name (nth ary 0)
	plan_name (nth ary 1)
	segment (nth ary 2)]
    (if (nil? oldValue)
      (do
	(insert! :plans
		 { :_id id
		  :contract_h_name contract_h_name
		  :plan_name plan_name
		  :segment segment
		  :contract_name contract_name
		  :plan_name_long plan_name_long
		  :plan_type plan_type
		  :cover_drugs cover_drugs
		  :fips fips
		  :process_detail false
		  :benefit-detail [] })
	true)
      false)))

(def benefit-type {
		   "Premium and Other Important Information"	1
		   "Doctor and Hospital Choice"	2
		   "Inpatient Hospital Care"	3
		   "Inpatient Mental Health Care"	4
		   "Skilled Nursing Facility"	5
		   "Home Health Care"	6
		   "Hospice"	7
		   "Doctor Office Visits"	8
		   "Chiropractic Services"	9
		   "Podiatry Services"	10
		   "Outpatient Mental Health Care" 11
		   "Outpatient Substance Abuse Care"	12
		   "Outpatient Services/Surgery"	13
		   "Ambulance Services"	14
		   "Emergency Care"	15
		   "Urgently Needed Care"	16
		   "Outpatient Rehabilitation Services"	17
		   "Durable Medical Equipment"	18
		   "Prosthetic Devices"	19
		   "Diabetes Self-Monitoring Training and Supplies" 20
		   "Diagnostic Tests, X-Rays, and Lab Services"	21
		   "Bone Mass Measurement"	22
		   "Colorectal Screening Exams"	23
		   "Immunizations"	24
		   "Mammograms (Annual Screening)"	25
		   "Pap Smears and Pelvic Exams"	26
		   "Prostate Cancer Screening Exams"	27
		   "Prescription Drugs"	29
		   "Dental Services" 30
		   "Hearing Services"	31
		   "Vision Services"	32
		   "Physical Exams"	33
		   "Health/Wellness Education"	34
		   "Transportation"	35
		   "Acupuncture"	36
		   "Point of Service"	37
		   "End-Stage Renal Disease (ESRD)"	28
		   })

(defn insert-plan-detail [plan_id key value]
  (let [cat_id (get benefit-type key)]
    (update! :plans
	     {:_id plan_id}
	     {:$push {:benefit-detail {
				       :name key
				       :value value
				       :cat_id cat_id}}})))

	    
	       
		   