(ns sormilla.main
  (:require [sormilla.gui :as gui]
            [sormilla.core :as core]))

(defn -main []
  (println "starting...")
  (gui/make-frame core/source core/render :exit-on-close true)
  (println "running..."))
