;;;; This namespace holds the server and application state.

(ns cohabit.server
  (:require [cohabit.date-crunching :refer [get-status get-today read-database write-database]]
            [org.httpkit.server :as http]
            [clj-time.core :as time]
            [clj-time.format :as fmt]
            [cheshire.core :as json]))

(def database-fname "resources/database/database.json")

;;; Websocket state
(defonce channels (atom #{}))

(defn connect! [channel]
  (println (format "[%s] Connecting client %s", (fmt/unparse (fmt/formatters :date-hour-minute-second) (time/now)) channel))
  (swap! channels conj channel))

(defn disconnect! [channel _]
  (println (format "[%s] Disconnecting client %s", (fmt/unparse (fmt/formatters :date-hour-minute-second) (time/now)) channel))
  (swap! channels disj channel))

(defn reply [ch payload]
  (http/send! ch (payload :body)))

(defn broadcast [payload]
  (mapv #(do (reply % payload)) @channels))

(defn unknown-type-response [ch _]
  (http/send! ch (json/encode {:type "error" :payload "ERROR: unknown message type"})))

;;; Websocket handlers
(defn ws-status [ch payload]
  (reply ch (let [db (read-database database-fname)
                  today (get-today)]
              {:status 200
               :headers {"Content-Type" "text/html"}
               :body (get-status db today)})))

(defn ws-add [ch payload]
  (broadcast (let [db (read-database database-fname)
        today (get-today)
        updated-db (if (not (some #(= today (% :date)) db))
                      (conj db {:date today})
                      db)]
    (write-database database-fname updated-db)
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body (get-status updated-db today)})))

(defn ws-remove [ch payload]
  (broadcast (let [db (read-database database-fname)
        today (get-today)
        updated-db (remove #(= today (% :date)) db)]
    (write-database database-fname updated-db)
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body (get-status updated-db today)})))

(defn log-ws [ch payload handler]
  (println (format "[%s] Got websocket request on channel %s, dispatching to %s: %s"
                   (fmt/unparse (fmt/formatters :date-hour-minute-second) (time/now))
                   ch
                   handler
                   payload)))

(defn dispatch [ch msg]
  (let [payload (json/decode msg)
        handler (case ((payload "HEADERS") "HX-Trigger")
                  "status" ws-status
                  "add" ws-add
                  "remove" ws-remove
                  unknown-type-response)]
    (log-ws ch payload handler)
    (handler ch payload)))

;;; HTTP handlers
(defn handler-ws [request]
  (http/with-channel request channel
    (connect! channel)
    (http/on-close channel #(disconnect! channel %))
    (http/on-receive channel #(dispatch channel %))))

(defn handler-home [_]
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body (slurp "resources/public/index.html")})

;; Entry point
(defn log-http [req]
  (println (format "[%s] Got HTTP request: %s"
    (fmt/unparse (fmt/formatters :date-hour-minute-second) (time/now))
    (req :uri))))

(defn -main []
  (println "Listening at 0.0.0.0:5000...")
  (http/run-server
   (fn [req]
     (log-http req)
     (case (:uri req)
       "/" (handler-home req)
       "/ws" (handler-ws req)
       {:status 404 :body "Not found"}))
   {:port 5000}))
