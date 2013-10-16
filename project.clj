(defproject sormilla "0.3.2-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/core.async "0.1.242.0-44b1e3-alpha"]
                 [rogerallen/leaplib "0.8.1"]
                 [rogerallen/leaplib-natives "0.8.1"]
                 [h264-decoder/h264-decoder "1.0"]
                 [amalloy/ring-buffer "1.0"]
                 [commons-io/commons-io "2.4"]
                 [metosin/system "0.2.1"]]
  :repositories {"sonatype-oss-public" "https://oss.sonatype.org/content/groups/public/"}
  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [[midje "1.5.1"]
                                  [org.clojure/tools.namespace "0.2.4"]
                                  [org.clojure/java.classpath "0.2.0"]]
                   :plugins [[lein-midje "3.1.1"]]}
             :uberjar {:source-paths ["main"]
                       :main sormilla.main
                       :aot [sormilla.main]}
             :video-sim {:source-paths ["src"]
                         :main sormilla.video-sim}}
  :min-lein-version "2.3.2")
