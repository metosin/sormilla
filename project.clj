(defproject sormilla "0.3.1-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [rogerallen/leaplib "0.8.1"]
                 [rogerallen/leaplib-natives "0.8.1"]
                 [amalloy/ring-buffer "1.0"]
                 [h264-decoder/h264-decoder "1.0"]
                 [commons-io/commons-io "2.4"]]
  :profiles {:dev {:dependencies [[midje "1.5.1"]]
                   :plugins [[lein-midje "3.1.1"]]}}
  :main sormilla.main
  :repl-options {:init-ns sormilla.main}
  :min-lein-version "2.0.0")
