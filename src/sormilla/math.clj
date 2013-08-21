(ns sormilla.math
  (:require [amalloy.ring-buffer :refer [ring-buffer]]))

(defn avg [coll]
  (/ (reduce + coll) (double (count coll))))

(defn lin-scale [[fmin fmax] [tmin tmax]]
  (let [r (/ (- tmax tmin) (- fmax fmin))]
    (fn [v] (double (+ (* (- v fmin) r) tmin)))))

(defn scale [v vmax rmax]
  (* v (/ rmax vmax)))

(defn bound [lo hi]
  (fn [v]
    (if (< lo v hi) v (if (< lo v) hi lo))))

(defn abs [v]
  (if (pos? v) v (- v)))

(defn clip-to-zero [treshold]
  (fn [v]
    (if (< (abs v) treshold) 0.0 (- v (if (pos? v) treshold (- treshold))))))

(defn averager [c]
  (let [buffer (atom (ring-buffer c))]
    (fn [v]
      (/ (reduce + (swap! buffer conj v)) c))))
