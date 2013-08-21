(ns sormilla.drone
  (:require [sormilla.drone-comm :as comm]))

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

(defn upstream [s]
  )

(defn telemetry [s]
  {:pitch  0.0
   :yaw    0.0
   :roll   0.2
   :alt    1500.0})
