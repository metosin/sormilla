(ns sormilla.main
  (:require [sormilla.gui :as gui]
            [sormilla.leap :as leap]
            [sormilla.core :as core]))

(defn -main [& args]
  (println "starting...")
  (gui/make-frame leap/frame core/render :exit-on-close true)
  (println "running..."))
