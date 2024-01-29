;;;; This namespace holds the server and application state.

(ns cohabit.server
  (:require [cohabit.date-crunching :refer :all]
            [org.httpkit.server :as http]
            [clj-time.core :as time]
            [clj-time.format :as fmt]))

(def database-fname "resources/database/database.json")

(defn handler-home [_]
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body (slurp "resources/public/index.html")})

(defn handler-count [_]
  (let [db (read-database database-fname)
        today (get-today)]
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body (get-status db today)}))

(defn handler-add [_]
  (let [db (read-database database-fname)
        today (get-today)
        updated-db (if (not (some #(= today (% :date)) db))
                      (conj db {:date today})
                      db)]
    (write-database database-fname updated-db)
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body (get-status updated-db today)}))

(defn handler-delete [_]
  (let [db (read-database database-fname)
        today (get-today)
        updated-db (remove #(= today (% :date)) db)]
    (write-database database-fname updated-db)
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body (get-status updated-db today)}))

(defn print-request [req]
  (println (format "[%s] Got request: %s"
    (fmt/unparse (fmt/formatters :date-hour-minute-second) (time/now))
    (req :uri))))

;; TODO: use websockets instead of polling
;; https://http-kit.github.io/server.html#websocket
;; https://github.com/hashrocket/websocket-shootout/blob/master/clojure/httpkit/src/websocket/server.clj
(defn -main []
  (println "Listening at 0.0.0.0:5000...")
  (http/run-server
   (fn [req]
     (print-request req)
     (case (:uri req)
       "/" (handler-home req)
       "/count" (handler-count req)
       "/add" (handler-add req)
       "/delete" (handler-delete req)
       {:status 404 :body "Not found"}))
   {:port 5000}))
