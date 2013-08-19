(ns sormilla.main
  (:require [sormilla.system :as system]
            [sormilla.leap]
            [sormilla.drone]
            [sormilla.swing :as swing]
            [sormilla.gui :as gui]))

(defn -main [& args]
  (println "starting...")
  (let [f (swing/make-frame
            gui/render
            :exit-on-close true
            :max-size true)]
    (println "running. press any key to exit...")
    (.read (System/in))
    (println "closing...")
    (system/shutdown!)))

"application ready"
