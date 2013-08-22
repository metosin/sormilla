(ns sormilla.drone
  (:require [sormilla.drone-comm :as comm]
            [sormilla.system :refer [task]]))

(def actions [[[[[:keys :left]  true]  [[:keys :right] false]] [:left true]]
              [[[[:keys :left]  false] [[:keys :right] true]]  [:right true]]])

(defn- condition-active? [s [path value]]
  (= (get-in s path) value))

(defn- active? [s [conditions]]
  (every? (partial condition-active? s) conditions))

(def s {:keys {:left true :right false}
        })

#_(reduce (partial apply assoc) {} (map second (filter (partial active? s) actions)))
#_(f)

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
  (let [data (comm/get-nav-data)]
    (when @comm/foo (log data))
    data)
  #_{:pitch 0.0
   :yaw 0.0
   :roll 0.0
   :altitude 758.0
   :vel-x 0.0
   :vel-y 0.0
   :vel-z 0.0
   :control-state :soaring
   :battery-percent 120.0})
