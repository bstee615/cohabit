;; shared resource
(def fname "foo")

(defn a [o] (future (locking o
                      (println "start1")
                      (Thread/sleep 5000)
                      (println "done1"))))

(defn b [o] (future (locking o
                      (println "start2")
                      (Thread/sleep 1000)
                      (println "done2"))))

;; Now run this before 5 seconds is up and you'll 
;; find the second instance waits for the first instance to print done1
;; and release the lock, and then it waits for 1 second and prints done2
(defn both []
  (a fname)
  (Thread/sleep 1000) ; give first instance 1 sec to acquire the lock
  (b fname))

;; Expected output:
;; start1
;; <5 sec delay>
;; done1
;; start2
;; <1 sec delay>
;; done2
(both)
