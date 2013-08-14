(ns sormilla.main
  (:require [sormilla.gui :as gui]
            [sormilla.leap :as leap]
            [sormilla.core :as core]))

(defn -main []
  (println "starting...")
  (gui/make-frame (partial core/source (leap/connect)) core/render :exit-on-close true)
  (println "running..."))
