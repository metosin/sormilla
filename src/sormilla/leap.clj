(ns sormilla.leap
  (:import [com.leapmotion.leap Listener Controller Hand Frame Finger FingerList Vector Gesture GestureList Gesture$Type Pointable]))

(set! *warn-on-reflection* true)

(defn valid? [^Finger f] (.isValid f))
(defn direction ^Vector [^Finger finger] (.direction finger))
(defn directions [fingers] (map direction fingers))
(defn pitch ^double [^Vector direction] (.pitch direction))
(defn yaw ^double [^Vector direction] (.yaw direction))

(defn fingers [^Hand hand]
  (let [fs (filter valid? (.fingers hand))
        c (count fs)]
    (if (= c 5)
      [5 [(nth fs 1) (nth fs 2) (nth fs 3)]]
      [c fs])))

(defn avg [directions value-fn]
  (/ (reduce + (map value-fn directions)) (double (count directions))))

(defn ->hand [^Hand hand]
  (when (and hand (.isValid hand))
    (let [[quality fingers]  (fingers hand)
          directions         (directions fingers)]
      {:quality       quality
       :pitch         (avg directions pitch)
       :yaw           (avg directions yaw)
       :roll          (-> hand .palmNormal .roll double)})))

(defn connect ^Controller []
  (Controller.))

(defonce ^Controller connection (connect))

(defn frame []
  (let [hands         (-> connection .frame .hands) 
        hand-count    (.count hands)]
    {:right (when (pos? hand-count) (->hand (.rightmost hands)))
     :left  (when (> hand-count 1) (->hand (.leftmost hands)))}))

(comment
  
  (:right (frame ))
  
  (require '[clojure.pprint :refer [pprint]])
  
  (dotimes [n 500]
    (Thread/sleep 100)
    (when-let [hand (:right (frame))]
      (let [[quality pitch yaw roll] ((juxt :quality :pitch :yaw :roll) hand)]
        (printf "%3d: %15.3f %15.3f %15.3f\n" quality pitch yaw roll)
        (flush))))

)
