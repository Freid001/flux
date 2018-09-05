(defproject flux "1.0.165-SNAPSHOT"
  :description ""
  :plugins [[lein-environ "1.0.2"]
            [lein-print "0.1.0"]
            [lein-deploy-tar "0.1.0"]
            [lein-tar "3.3.0"]
            [lein-kibit "0.1.3"]
            [lein-eftest "0.3.1"]]
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.cli "0.3.5"]
                 [org.slf4j/slf4j-log4j12 "1.7.25"]
                 [org.apache.kafka/kafka-clients "0.11.0.1"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [metosin/compojure-api "1.1.11"]
                 [metosin/ring-swagger "0.24.3"]
                 [com.fzakaria/slf4j-timbre "0.3.7"]
                 [prismatic/schema "1.1.7"]
                 [compojure "1.6.0"]
                 [cheshire "5.8.0"]
                 [environ "1.1.0"]
                 [ring "1.6.3"]
                 [ring/ring-json "0.4.0"]
                 [ring-logger "0.7.7"]
                 [siili/humanize "0.1.1"]
                 [clj-time "0.14.2"]
                 [amazonica "0.3.132"]
                 [log4j/log4j "1.2.17" :exclusions [javax.mail/mail
                                                    javax.jms/jms
                                                    com.sun.jmdk/jmxtools
                                                    com.sun.jmx/jmxri]]
                 [ovotech/kafka-avro-confluent "0.8.0" :exclusions [org.clojure/clojure]]]

  :main app.cli.core
  :profiles {:uberjar {:aot :all}}
  :prep-tasks ["compile"])