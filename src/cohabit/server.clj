;;;; This namespace holds the server and application state.

(ns cohabit.server
  (:require [cohabit.date-crunching :refer [get-status get-today read-database write-database]]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [buddy.auth :refer [authenticated? throw-unauthorized]]
            [buddy.auth.backends.session :refer [session-backend]]
            [buddy.auth.middleware :refer (wrap-authentication wrap-authorization)]
            [buddy.hashers :as hashers]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.session :refer [wrap-session]]
            [org.httpkit.server :as http]
            [clj-time.core :as time]
            [clj-time.format :as fmt]
            [cheshire.core :as json]))

(def database-fname "resources/database/database.json")

(defn- hash-password [user]
  (dissoc
   (assoc user
    :hashed-password (hashers/derive (user :password)))
   :password))

;;; Authentication
(def users (into {} (map
                     (juxt :username #(select-keys % [:hashed-password]))
                     (map hash-password
                          (json/parse-string (slurp "resources/database/users.json") true)))))

(defn lookup-user [username password]
  (when-let [user (get users username)] ; TODO: use a database
    (when (hashers/verify password (get user :hashed-password))
      (dissoc user :hashed-password))))

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
(defn ws-status [ch _]
  (reply ch (let [db (read-database database-fname)
                  today (get-today)]
              {:status 200
               :headers {"Content-Type" "text/html"}
               :body (get-status db today)})))

(defn ws-add [_ _]
  (broadcast (let [db (read-database database-fname)
                   today (get-today)
                   updated-db (if (not (some #(= today (% :date)) db))
                                 (conj db {:date today})
                                 db)]
              (write-database database-fname updated-db)
              {:status 200
               :headers {"Content-Type" "text/html"}
               :body (get-status updated-db today)})))

(defn ws-remove [_ _]
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
#_{:clj-kondo/ignore [:unresolved-symbol]}
(defn handler-ws [req]
  (if (authenticated? req)
    (http/with-channel req channel
      (connect! channel)
      (http/on-close channel #(disconnect! channel %))
      (http/on-receive channel #(dispatch channel %)))
    (throw-unauthorized)))

(defn handler-home [req]
  (if (authenticated? req)
    {:status 200
     :headers {"Content-Type" "text/html; charset=utf-8"}
     :body (slurp "resources/public/index.html")}
    (throw-unauthorized)))

(defn handler-login [_]
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body (slurp "resources/public/login.html")})

; To unsign jws token to string: (apply str (map char (jws/unsign "<encoded-token>" secret)))
(defn handler-authenticate [{{username "username" password "password" next "next"} :params
                             session :session}]
  (if-let [user (lookup-user username password)]
    {:status 302
     :headers {"Location" (or next "/")}
     :session (assoc session :identity user)}
    (throw-unauthorized)))

(defn handler-logout [{session :session}]
  (-> {:status 302
       :headers {"Location" "/"}
       :session (dissoc session :identity)}))

;; Entry point
(defn log-http [req]
  (println (format "[%s] Got HTTP request: %s"
            (fmt/unparse (fmt/formatters :date-hour-minute-second) (time/now))
            (req :uri))))

(defn handler-unauthorized
  [request _]
  (let [current-url (:uri request)]
    {:status 302
     :headers {"Location" (format "/login?next=%s" current-url)}}))

(def backend (session-backend {:unauthorized-handler handler-unauthorized}))

#_{:clj-kondo/ignore [:unresolved-symbol]}
(defroutes app-routes
  (GET "/" [] handler-home)
  (GET "/ws" [] handler-ws)
  (GET "/login" [] handler-login)
  (POST "/login" [] handler-authenticate)
  (GET "/logout" [] handler-logout)
  (route/files "/" {:root "resources/public"})
  (route/not-found "<h1>Page not found</h1>"))

(def app (-> app-routes
             (wrap-authorization backend)
             (wrap-authentication backend)
             wrap-session
             wrap-params))

(defn -main []
  (println "Listening at 0.0.0.0:5000...")
  (http/run-server
   #'app
   {:port 5000}))
