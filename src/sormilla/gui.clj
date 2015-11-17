(ns sormilla.gui
  (:require [sormilla.world :refer [world]]
            [sormilla.swing :refer [with-transforms] :as swing]
            [sormilla.task :as task])
  (:import [java.awt Graphics2D Canvas Color Toolkit RenderingHints Image]
           [javax.swing JFrame SwingUtilities]))

(set! *warn-on-reflection* true)

(def background-color     (Color.   32   32  32   255))
(def hud-color            (Color.   64  192  64    92))
(def hud-hi-color         (Color.   64  255  64   192))
(def hud-lo-color         (Color.   64  192  64    32))
(def leap-color           (Color.   64  255  64   192))
(def key-color            (Color.  128  128  64   255))
(def telemetry-color      (Color.  255  255  32   255))
(def alt-color            (Color.  255   32  32   192))
(def status-hi-color      (Color.  255  255   0   255))
(def status-lo-color      (Color.  192  192   0   255))

(def quality-colors [(Color.  255   64  64   192)
                     (Color.  255   64  64   192)
                     (Color.  255   64  64   192)
                     (Color.  192   64  64   128)
                     (Color.   64  192  64   128)
                     (Color.   64  192  64   255)])

(defn render [^Graphics2D g ^long w ^long h]
  (let [{:keys [leap telemetry keys intent ^Image image]} @world
        now    (System/currentTimeMillis)
        w2     (/ w 2.0)
        w6     (/ w 6.0)
        h2     (/ h 2.0)
        h6     (/ h 6.0)]

    ; setup
    (.setRenderingHint g RenderingHints/KEY_ANTIALIASING RenderingHints/VALUE_ANTIALIAS_ON)

    ; video feed
    (if image
      (doto g
        (.drawImage image 0 0 w h 0 0 (.getWidth image nil) (.getHeight image nil) nil)
        (.setColor (Color. 0 0 0 96))
        (.fillRect 0 0 w h))
      (doto g
        (.setColor background-color)
        (.fillRect 0 0 w h)
        (.setColor Color/WHITE)
        (.drawString "no image feed" 25 75)))

    ; emergency background
    (when (= (:control-state telemetry) :emergency)
      (.setColor g (if (< (mod (System/currentTimeMillis) 400) 200) (Color. 128 16 16) (Color. 192 16 16)))
      (.fillRect g 0 0 w h))

    ; status
    (let [{:keys [control-state battery-percent]} telemetry
          {:keys [intent-state]} intent]
      (.setColor g status-lo-color)
      (.drawString g (str "trgt: " (name (or intent-state :init))) 10 20)
      (.setColor g (if (= control-state intent-state) status-lo-color status-hi-color))
      (.drawString g (str "stat: " (name (or control-state :init))) 10 35)
      (.setColor g status-lo-color)
      (.drawString g (str " bat: " battery-percent "%") 10 50))

    ; keys:
    (.setColor g key-color)
    (let [{:keys [left right up down space]} keys]
      (when left   (.fill g (swing/->shape w6 h2 (* 2 w6) (* 2 h6) (* 2 w6) (* 4 h6))))
      (when right  (.fill g (swing/->shape (* 5 w6) h2 (* 4 w6) (* 2 h6) (* 4 w6) (* 4 h6))))
      (when up     (.fill g (swing/->shape w2 h6 (* 4 w6) (* 2 h6) (* 2 w6) (* 2 h6))))
      (when down   (.fill g (swing/->shape w2 (* 5 h6) (* 4 w6) (* 4 h6) (* 2 w6) (* 4 h6))))
      (when space  (.fill g (swing/->shape 50 (- h 40) (- w 50) (- h 40) (- w 50) (- h 10) 50 (- h 10)))))

    ; draw grid
    ;(.setColor g hud-lo-color)
    ;(doseq [x (range (/ w 10) w (/ w 10))] (.drawLine g x 0 x h))
    ;(doseq [y (range (/ h 10) h (/ h 10))] (.drawLine g 0 y w y))

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
    (let [{:keys [pitch roll alt]} telemetry]
      (when (and pitch roll alt)
        (with-transforms g
          (.setColor g telemetry-color)
          (.translate g w2 (+ h2 (* h2 (/ pitch (/ Math/PI 4.0)))))
          (.rotate g roll)
          (swing/draw-circle g w6 0 20)
          (swing/draw-circle g (- w6) 0 20)
          (.drawLine g w6 0 (- w6) 0))
        (let [alt-box (* h (/ alt 3000.0))]
          (.setColor g alt-color)
          (.fillRect g (- w 40) (- h alt-box) 40 alt-box))))))

;;
;; Frame:
;;

(defprotocol IFrame
  (paint [this renderer])
  (close [this]))

(defrecord Frame [^JFrame frame ^Canvas canvas]
  IFrame
  (paint [this renderer]
    (let [strategy (.getBufferStrategy canvas)
          g (.getDrawGraphics strategy)]
      (try
        (renderer g (.getWidth canvas) (.getHeight canvas))
        (.show strategy)
        (finally
          (.dispose g)))))
  (close [this]
    (SwingUtilities/invokeLater
      (fn [] (.setVisible frame false)))))

(defn ^IFrame make-frame [& {:keys [max-size top exit-on-close]}]
  (let [frame (JFrame.)
        canvas (Canvas.)]
    (.setIgnoreRepaint frame true)
    (.setIgnoreRepaint canvas true)
    (.add frame canvas)
    (.setSize canvas 672 418)
    (.pack frame)
    (let [screen-size (.getScreenSize (Toolkit/getDefaultToolkit))]
      (.setLocation frame (- (.width screen-size) 672) 0))
    (when max-size
      (.setExtendedState frame JFrame/MAXIMIZED_BOTH))
    (when top
      (.setAlwaysOnTop frame true))
    (when exit-on-close
      (.setDefaultCloseOperation frame JFrame/EXIT_ON_CLOSE))
    (.setVisible frame true)
    (.createBufferStrategy canvas 2)
    (->Frame frame canvas)))

;;
;; =================================================================================
;; Lifecycle:
;; =================================================================================
;;

(defn start-subsys! [config]
  (let [frame (make-frame :top true)
        task  (task/schedule :gui 50 paint frame render)]
    (future
      (try
        (deref task)
        (catch Exception _))
      (close frame)))
  config)

(defn stop-subsys! [config]
  (task/cancel :gui)
  config)

