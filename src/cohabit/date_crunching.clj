;;;; This namespace holds the functions for reading/writing to/from the database and calculating dates.

(ns cohabit.date-crunching
  (:require [clojure.string :as string]
            [cheshire.core :as json]
            [clj-time.core :as time]
            [clj-time.format :as fmt]
            [clj-time.coerce :as coerce])
  (:import (java.io FileNotFoundException)))

;;; Database
(def date-format (fmt/formatters :date))
(def month-year-format (fmt/formatter "MMMM yyyy"))

(defn read-database [fname]
  (locking fname
   (try
    (let [file-content (slurp fname)]
      (reverse
        (sort-by :date
          (map
            #(update % :date
              (fn [arg] (coerce/to-local-date (fmt/parse date-format arg))))
            (json/parse-string file-content true)))))
    (catch FileNotFoundException _ (spit fname "[]") []))))

(defn write-database [fname data]
  (locking fname
    (spit fname
          (json/generate-string (map #(update % :date (fn [arg] (str arg))) data)))))

;;; Date crunching
(defn get-today []
  (time/today))

(defn get-days-in-a-row [data today]
  (reduce (fn [counter record]
    (if
      (=
        (time/minus today (time/days counter))
        (coerce/to-local-date (record :date)))
      (inc counter)
      (reduced counter)))
    0
    data))

(defn get-streak [data today]
  (if (zero? (get-days-in-a-row data today))
    (get-days-in-a-row data (time/minus today (time/days 1)))
    (get-days-in-a-row data today)))

(defn fill-blank-dates [dates today]
  (let [first-date (time/first-day-of-the-month- today)
        days-before-first-day (mod (time/day-of-week first-date) 7)
        blank {:class "" :date ""}]
    (concat
     (repeat days-before-first-day blank)
     dates)))

(defn get-dates-in-month [today]
  (let [last-date (time/day (time/last-day-of-the-month- today))]
    (range 1 (+ last-date 1))))

;; match dates with data and fill data into dates, such as whether we did cookie time on that date
(defn decorate-dates [dates data today]
  (let [active-dates (zipmap (map #(% :date) data) data)]
    (map (fn [date]
           {:class (string/join " " [(if (contains? active-dates date)
                                      "active"
                                      "")
                                    (if (= date today)
                                      "today"
                                      "")])
            :date (time/day date)})
         (map #(time/nth-day-of-the-month today %) dates))))

(defn get-status [data today clients]
  (let [days (get-streak data today)
        message (if (zero? days) "Shame" "Yay us")
        suffix (if (and (not-empty data) (not= today (coerce/to-local-date ((last (sort-by :date data)) :date)))) " Do it today!" "")
        dates (fill-blank-dates (decorate-dates (get-dates-in-month today) data today) today)
        now-online (map #(% :username) clients)]
    (str "<div id=\"status\">"
         "<div>" message "! We've kept our habit up for the last " days " days." suffix "</div>"
         "<div class=\"calendar\" role=\"list\">"
         "<span class=\"month-year-banner\">"(fmt/unparse-local-date month-year-format today)"</span>"
         "<span class=\"day-of-week\">Sun</span><span class=\"day-of-week\">Mon</span><span class=\"day-of-week\">Tue</span><span class=\"day-of-week\">Wed</span><span class=\"day-of-week\">Thu</span><span class=\"day-of-week\">Fri</span><span class=\"day-of-week\">Sat</span>"
         (string/join "" (map #(str "<span><div class=\""(% :class)"\">"(% :date)"</div></span>") dates))
         "</div>"
         "</div>"
         "<ul id=\"now-online-row\" class=\"f-row dense\" role=\"list\" hx-swap=\"innerHTML\">"
         (string/join "" (map #(str "<li class=\"now-online\">" % "</li>") now-online))
         "</ul>")))
