(ns sormilla.drone
  (:require [sormilla.drone-comm :as comm]
            [sormilla.system :refer [task]]))

(defn init []
  (comm/enable-navdata)
  (comm/ctrl-ack))

(defn upstream [s]
  )

(require '[clojure.java.io :as io])
(def log-file (io/writer (io/file "./log.data")))

(defn log [data]
  (.write log-file (str (select-keys data [:pitch :yaw :roll :altitude :vel-x :vel-y :vel-z :control-state])))
  (.write log-file "\n"))

(.flush log-file)

(defn telemetry [_]
  #_(let [data (comm/get-nav-data)]
    (when @comm/foo (log data))
    data)
  {:pitch 0.0
   :yaw 0.0
   :roll 0.0
   :alt 758.0
   :vel-x 0.0
   :vel-y 0.0
   :vel-z 0.0
   :control-state :soaring
   :battery-percent 120.0})
