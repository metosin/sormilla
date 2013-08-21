(defproject sormilla "0.1.1-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [amalloy/ring-buffer "1.0"]
                 [ardrone/ardrone "0.1.0-SNAPSHOT" :exclusions [org.clojure/clojure]]]
  :profiles {:dev {:dependencies [[midje "1.5.1"]]
                   :plugins [[lein-midje "3.1.1"]]}}
  :resource-paths ["./LeapSDK/lib/LeapJava.jar"]
  :jvm-opts ["-Djava.library.path=./LeapSDK/lib"]
  :main ^:skip-aot sormilla.main
  :repl-options {:init-ns sormilla.main}
  :min-lein-version "2.0.0")
