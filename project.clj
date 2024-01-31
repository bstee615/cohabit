(defproject cohabit "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [http-kit/http-kit "2.5.1"]
                 [cheshire "5.10.1"]
                 [clj-time "0.13.0"]
                 [buddy/buddy-auth "3.0.1"]
                 [buddy/buddy-hashers "2.0.167"]
                 [ring "1.11.0"]
                 [compojure "1.7.0"]]
  :main ^:skip-aot cohabit.server
  :target-path "target/%s")
