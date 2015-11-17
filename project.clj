(defproject sormilla "0.4.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/core.async "0.2.374"]
                 [metosin/leaplib "2.3.1"]
                 [metosin/leaplib-natives "2.3.1"]
                 [h264-decoder/h264-decoder "1.0"]
                 [amalloy/ring-buffer "1.2"]
                 [commons-io/commons-io "2.4"]
                 [metosin/system "0.2.1"]]
  :main sormilla.main
  :repl-options {:init-ns user}
  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [[midje "1.8.2"]
                                  [org.clojure/tools.namespace "0.2.10"]
                                  [org.clojure/java.classpath "0.2.3"]]
                   :plugins [[lein-midje "3.2"]]}
             :video-sim {:main sormilla.video-sim}}
  :min-lein-version "2.3.2")
