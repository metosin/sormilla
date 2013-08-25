(defproject sormilla "0.2.1-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [amalloy/ring-buffer "1.0"]
                 [h264-decoder/h264-decoder "1.0"]
                 [commons-io/commons-io "2.4"]]
  :profiles {:dev {:dependencies [[midje "1.5.1"]]
                   :plugins [[lein-midje "3.1.1"]]}}
  :resource-paths ["./LeapSDK/lib/LeapJava.jar" "./opencv/opencv-246.jar"]
  :jvm-opts ["-Djava.library.path=./LeapSDK/lib:./opencv"]
  :main ^:skip-aot sormilla.main
  :repl-options {:init-ns sormilla.main}
  :min-lein-version "2.0.0")
