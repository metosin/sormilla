(ns sormilla.drone
  (:require [sormilla.system :refer [task]]))

(set! *warn-on-reflection* true)

(defn telemetry []
  {:pitch 0.1
   :yaw   0.2
   :roll  0.3
   :alt   120})

(task 50 telemetry)
