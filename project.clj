(defproject sormilla "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.5.1"]]
  :profiles {:dev {:dependencies [[midje "1.5.1"]]
                   :plugins [[lein-midje "3.1.1"]]}}
  :main ^:skip-aot sormilla.main
  :repl-options {:init-ns sormilla.main}
  :min-lein-version "2.0.0")
