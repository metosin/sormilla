(ns sormilla.gui
  (:require [sormilla.system :refer [status] :as system]
            [sormilla.swing :refer [with-transforms] :as swing]
            [sormilla.math :as math])
  (:import [java.awt Color Graphics2D RenderingHints]))

(set! *warn-on-reflection* true)

(def background-color     (Color.   32   32  32   255))
(def hud-color            (Color.   64  192  64    92))
(def hud-hi-color         (Color.   64  255  64   192))
(def hud-lo-color         (Color.   64  192  64    32))
(def leap-color           (Color.   64  255  64    64))
(def key-color            (Color.   64  128  64    32))
(def telemetry-color      (Color.  255   32  32   192))
(def alt-color            (Color.  255   32  32    64))

(def quality-colors [(Color.  255   64  64   192)
                     (Color.  255   64  64   192)
                     (Color.  255   64  64   192)
                     (Color.  192   64  64   128)
                     (Color.   64  192  64   128)
                     (Color.   64  192  64   255)])

(defn key->status! [code id]
  (swing/add-key-listener! {:type :pressed :code code} (fn [_] (swap! status assoc-in [:keys id] true)))
  (swing/add-key-listener! {:type :released :code code} (fn [_] (swap! status assoc-in [:keys id] false))))

(key->status! 37 :left)
(key->status! 38 :up)
(key->status! 39 :right)
(key->status! 40 :down)
(key->status! 32 :space)

(defn render [^Graphics2D g ^long w ^long h]
  (let [{:keys [leap telemetry keys]} @status
        w2     (/ w 2.0)
        w6     (/ w 6.0)
        h2     (/ h 2.0)
        h6     (/ h 6.0)]

    ; setup
    (.setRenderingHint g RenderingHints/KEY_ANTIALIASING RenderingHints/VALUE_ANTIALIAS_ON)
    
    ; clear background
    (.setColor g background-color)
    (.fillRect g 0 0 w h)

    (when (= (:control-state telemetry) :emergency)
      (.setColor g (if (< (mod (System/currentTimeMillis) 400) 200) (Color. 128 16 16) (Color. 192 16 16)))
      (.fillRect g 0 0 w h))

    ; status
    (.setColor g key-color)
    (let [{:keys [left right up down space]} keys]
      (when left   (.fill g (swing/->shape w6 h2 (* 2 w6) (* 2 h6) (* 2 w6) (* 4 h6))))
      (when right  (.fill g (swing/->shape (* 5 w6) h2 (* 4 w6) (* 2 h6) (* 4 w6) (* 4 h6))))
      (when up     (.fill g (swing/->shape w2 h6 (* 4 w6) (* 2 h6) (* 2 w6) (* 2 h6))))
      (when down   (.fill g (swing/->shape w2 (* 5 h6) (* 4 w6) (* 4 h6) (* 2 w6) (* 4 h6))))
      (when space  (.fill g (swing/->shape 50 (- h 40) (- w 50) (- h 40) (- w 50) (- h 10) 50 (- h 10)))))
    
    ; draw grid
    (.setColor g hud-lo-color)
    (doseq [x (range (/ w 10) w (/ w 10))] (.drawLine g x 0 x h))
    (doseq [y (range (/ h 10) h (/ h 10))] (.drawLine g 0 y w y))
    
    ; draw zero axis
    (.setColor g hud-color)
    (.drawLine g w2 0 w2 h)
    (.drawLine g 0 h2 w h2)
    (doseq [x (range (/ w 50) w (/ w 50))] (.drawLine g x (- h2 5) x (+ h2 5)))
    (doseq [y (range (/ h 50) h (/ h 50))] (.drawLine g (- w2 5) y (+ w2 5) y))

    ; draw "aim"
    (when leap
      ; draw quality boxes
      (let [quality (:quality leap)]
        (.setColor g (nth quality-colors quality))
        (doseq [y (range 5)]
          (when (>= y quality) (.setColor g hud-lo-color))
          (.fillRect g 5 (- h (* y 10) 10) 30 5)))
      (let [pitch  (* (:pitch leap) (/ h2 100.0) -1.0)
            yaw    (* (:yaw leap) (/ w2 100.0))
            roll   (:roll leap)
            aim-w  (/ w 2.0)
            aim-h  (/ h 10.0)
            aim-x  (/ aim-w -2.0)
            aim-y  (/ aim-h -2.0)]
        (with-transforms g
          (.translate g (+ w2 yaw) (+ h2 pitch))
          (.rotate g roll)
          (.setColor g leap-color)
          (.fillOval g aim-x aim-y aim-w aim-h)
          (.drawOval g aim-x aim-y aim-w aim-h)
          (.drawLine g -2000 0 2000 0)
          (.drawLine g 0 -2000 0 2000))))
    
    ; draw telemetry
    (when-let [{:keys [pitch yaw roll alt vel-x vel-y vel-z control-state battery-percent]} telemetry]
      (with-transforms g
        (.setColor g telemetry-color)
        (.drawString g (str (name control-state)) 10 20)
        (.drawString g (str "bat: " battery-percent "%") 10 40)
        (.drawString g (format "yaw: %5.1f pitch: %5.1f roll: %5.1f alt: %8.3f" yaw pitch roll alt) 10 60)
        (.drawString g (format "x: %5.1f y: %5.1f z: %5.1f" vel-x vel-y vel-z) 10 80)
        (.translate g w2 (+ h2 (* h2 (/ pitch (/ Math/PI 4.0)))))
        (.rotate g roll)
        (swing/draw-circle g w6 0 20)
        (swing/draw-circle g (- w6) 0 20)
        (.drawLine g w6 0 (- w6) 0))
      (let [alt-box (* h (/ alt 3000.0))]
        (.setColor g alt-color)
        (.fillRect g (- w 40) (- h alt-box) 40 alt-box)))))
