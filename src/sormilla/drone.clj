(ns sormilla.drone
  (:require [sormilla.drone-comm :as comm]
            [sormilla.system :refer [task]]))

(defn init []
  (comm/enable-navdata)
  (comm/ctrl-ack))

(defn upstream [s]
  ; here...
  )

(defn telemetry [_]
  #_(comm/get-nav-data)
  {:pitch 0.0
   :yaw 0.0
   :roll 0.0
   :alt 758.0
   :vel-x 0.0
   :vel-y 0.0
   :vel-z 0.0
   :control-state :soaring
   :battery-percent 120.0})
