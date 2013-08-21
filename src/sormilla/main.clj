(ns sormilla.main
  (:require [sormilla.system :refer [task] :as system]
            [sormilla.leap :as leap]
            [sormilla.drone :as drone]
            [sormilla.swing :as swing]
            [sormilla.gui :as gui]))

(defn -main [& args]
  (println "starting...")
  (task 40 drone/upstream)
  (task 50 leap/leap)
  (let [f (swing/make-frame
            gui/render
            :exit-on-close true
            :max-size true)]
    (println "running. press any key to exit...")
    (.read (System/in))
    (println "closing...")
    (system/shutdown!)))

"application ready"

(comment
  (task 50 drone/upstream)
  (task 50 drone/telemetry)
  (task 50 leap/leap)
  (def f (swing/make-frame
           #'gui/render
           :safe true
           :top true))
  (swing/close! f)
  (system/shutdown!)
)