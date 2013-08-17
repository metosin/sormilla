(ns sormilla.math)

(defn avg [coll]
  (/ (reduce + coll) (double (count coll))))

(defn lin-scale [v fmin fmax tmin tmax]
  (let [r (/ (- tmax tmin) (- fmax fmin))]
    (double (+ (* (- v fmin) r) tmin))))

(defn bound [lo hi v]
  (if (< lo v hi)
    v
    (if (< lo v)
      hi
      lo)))

(defn abs [v]
  (if (pos? v) v (- v)))

(defn clip-to-zero [v]
  (if (< (abs v) 0.1) 0.0 (- v (if (pos? v) 0.1 -0.1))))

