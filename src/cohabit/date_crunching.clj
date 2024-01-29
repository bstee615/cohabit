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

(defn read-database [fname]
  (try
    (let [file-content (slurp fname)]
      (reverse
        (sort-by :date
          (map
            #(update % :date
              (fn [arg] (coerce/to-local-date (fmt/parse date-format arg))))
            (json/parse-string file-content true)))))
    (catch FileNotFoundException _ (spit fname "[]") [])))

(defn write-database [fname data]
  (spit fname (json/generate-string (map #(update % :date (fn [arg] (str arg))) data))))

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

(defn get-status [data today]
  (let [days (get-streak data today)
        message (if (zero? days) "Shame" "Yay us")
        suffix (if (and (not-empty data) (not= today (coerce/to-local-date ((last (sort-by :date data)) :date)))) " Do it today!" "")]
    (str "<div>"
         "<div>" message "! We've kept our habit up for the last " days " days." suffix "</div>"
         "<ul class=\"f-col dense\" role=\"list\">"
         (string/join "" (map #(str "<li>" (% :date) "</li>") data))
         "</ul>"
         "</div>")))
