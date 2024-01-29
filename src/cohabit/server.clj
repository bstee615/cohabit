(ns cohabit.server
  (:require [org.httpkit.server :as http]
            [cheshire.core :as json]
            [clj-time.core :as time]
            [clj-time.format :as fmt]
            [clj-time.coerce :as coerce])
  (:import (java.io FileNotFoundException)))

(def database-fname "resources/database/database.json")
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

(defn get-today []
  (time/today))

(defn get-days-in-a-row [data]
  (let [today (time/today)]
    (reduce (fn [counter record]
              (if (= 
                    (time/minus today (time/days counter))
                    (coerce/to-local-date (record :date)))
                (inc counter)
                (reduced counter)))
            0
            data)))

(defn get-status [db]
  (let [days (get-days-in-a-row db)
        message (if (zero? days) "Shame" "Yay us")
        suffix (if (and (not-empty db) (not= (get-today) (coerce/to-local-date ((last (sort-by :date db)) :date)))) " Do it today!" "")]
    (str "<div>" message "! We've kept our habit up for the last " days " days." suffix "</div>")))

(defn handler-home [_]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (slurp "resources/public/index.html")})

(defn handler-count [_]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (get-status (read-database database-fname))})

(defn handler-add [_]
  (let [db (read-database database-fname)
        today (get-today)
        updated-db (if (not (some #(= today (coerce/to-local-date (% :date))) db))
                      (conj db {:date today})
                      db)]
    (write-database database-fname updated-db)
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body (get-status updated-db)}))

(defn handler-delete [_]
  (let [db (read-database database-fname)
        today (get-today)
        updated-db (remove #(= today (coerce/to-local-date (% :date))) db)]
    (write-database database-fname updated-db)
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body (get-status updated-db)}))

(defn -main []
  (http/run-server
   (fn [req]
     (case (:uri req)
       "/" (handler-home req)
       "/count" (handler-count req)
       "/add" (handler-add req)
       "/delete" (handler-delete req)
       {:status 404 :body "Not found"}))
   {:port 5000}))
