(ns sormilla.leap
  (:import [com.leapmotion.leap Listener Controller Hand Frame FingerList Vector Gesture GestureList Gesture$Type Pointable]))

(set! *warn-on-reflection* true)

(def gesture-type {:swipe       Gesture$Type/TYPE_SWIPE
                   :circle      Gesture$Type/TYPE_CIRCLE
                   :screen-tap  Gesture$Type/TYPE_SCREEN_TAP
                   :key-tap     Gesture$Type/TYPE_KEY_TAP})

(defn connect []
  (Controller.)
  #_(doseq [g (vals gesture-type)]
    (.enableGesture c g)))

(defn ->vector [^Vector v]
  [(.getX v) (.getY v) (.getZ v)])

(defn ->finger [^Pointable f]
  (-> f .tipPosition ->vector))

(defn valid? [^Pointable f]
  (.isValid f))

(defn ->sphere [^Hand h]
  (when h
    (conj (->vector (.sphereCenter h)) (double (.sphereRadius h)))))

(defn frame [^Controller c]
  (let [f (.frame c)
        hands (.hands f)
        both? (> (.count hands) 1)
        lh (.leftmost hands)
        rh (.rightmost hands)
        lh (when (and lh (.isValid lh)) lh)
        rh (when (and both? rh (.isValid rh)) rh)
        lf (when lh (->> lh .fingers seq (filter valid?) (map ->finger)))
        rf (when rh (->> rh .fingers seq (filter valid?) (map ->finger)))]
    [lf (->sphere lh) rf (->sphere rh)]))

(comment
  
  (def c (connect))
  (frame c)
  
  (dotimes [n 100]
    (Thread/sleep 100)
    (dorun (map (fn [[x y z]] (printf "%10.3f  %10.3f  %10.3f\n" x y z) (flush)) (first (frame c)))))

)
