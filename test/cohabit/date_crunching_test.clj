(ns cohabit.date-crunching-test
  #_{:clj-kondo/ignore [:refer-all]}
  (:require [clojure.test :refer :all]
            [cohabit.date-crunching :refer :all]
            [clj-time.core :as time]))

(def example-today (time/local-date 2024 01 05))

(deftest date-math
  (testing "day math simple"
    (is (=
         (time/day (time/minus example-today (time/days 1)))
         04))))

(deftest streaks
  (testing "good streak"
    (is (= (get-days-in-a-row (let [today example-today] [{:date today}
                                                          {:date (time/minus today (time/days 1))}
                                                          {:date (time/minus today (time/days 2))}]) example-today)
           3)))
  (testing "broken streak"
    (is (= (get-days-in-a-row (let [today example-today] [{:date today}
                                                          {:date (time/minus today (time/days 2))}
                                                          {:date (time/minus today (time/days 3))}]) example-today)
           1)))
  (testing "not-started streak"
    (is (= (get-days-in-a-row (let [today example-today] [{:date (time/minus today (time/days 1))}
                                                          {:date (time/minus today (time/days 2))}
                                                          {:date (time/minus today (time/days 3))}]) example-today)
           0))))

(deftest database
  (let [fname "resources/test/database.json"
        data [{:date (time/local-date 2024 01 05)}
              {:date (time/local-date 2024 01 04)}
              {:date (time/local-date 2024 01 03)}
              {:date (time/local-date 2024 01 02)}
              {:date (time/local-date 2024 01 01)}]]
    (testing "symmetric"
      (is (= (do
               (write-database fname data)
               (read-database fname))
             data)))
    (testing "repeated read"
      (is (= (read-database fname) data)))))
