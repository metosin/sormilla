(ns sormilla.swing
  (:require [clojure.java.io :as io]
            [sormilla.system :refer [run?]])
  (:import [java.awt Graphics2D Shape Canvas Color KeyboardFocusManager KeyEventDispatcher]
           [java.awt.geom Path2D$Double Ellipse2D$Double]
           [java.awt.event KeyEvent]
           [javax.swing JFrame SwingUtilities]))

(set! *warn-on-reflection* true)

(defn ->safe [throttle f]
  (fn [& args]
    (try
      (apply f args)
      (catch Throwable e
        (println "error:" e)
        (.printStackTrace e)
        (Thread/sleep throttle)
        nil))))

(defn make-frame [render & {:keys [interval safe safe-throttle max-size top exit-on-close] :or {interval 50 safe-throttle 500 max-size false}}]
  (let [frame (JFrame.)
        canvas (Canvas.)
        render (if safe (->safe safe-throttle render) render)]
    (.setIgnoreRepaint frame true)
    (.setIgnoreRepaint canvas true)
    (.add frame canvas)
    (.pack frame)
    (.setSize canvas 800 500)
    (.setSize frame 800 500)
    (if max-size
      (.setExtendedState frame JFrame/MAXIMIZED_BOTH))
    (when top
      (.setAlwaysOnTop frame true))
    (when exit-on-close
      (.setDefaultCloseOperation frame JFrame/EXIT_ON_CLOSE))
    (.setVisible frame true)
    (.createBufferStrategy canvas 2)
    (future
      (let [strategy (.getBufferStrategy canvas)]
        (try
          (while (run?)
            (let [start (System/currentTimeMillis)]
              (loop [contents-lost? true]
                (loop [restored? true]
                  (let [g (.getDrawGraphics strategy)]
                    (render g (.getWidth canvas) (.getHeight canvas))
                    (.dispose g))
                  (when (.contentsRestored strategy) (recur true)))
                (.show strategy)
                (when (.contentsLost strategy) (recur true)))
              (let [time-left (- interval (- (System/currentTimeMillis) start))]
                (when (pos? time-left)
                  (Thread/sleep time-left)))))
          (SwingUtilities/invokeLater
            (fn []
              (.setVisible frame false)))
          (catch Throwable e
            (println "Oh shit!" e)
            (throw e)))))
    frame))

(defn close! [^JFrame frame]
  (SwingUtilities/invokeLater
    (fn []
      (.setVisible frame false))))

(defn ->shape ^Shape [& [x y & points]]
  (let [p (Path2D$Double.)]
    (.moveTo p x y)
    (doseq [[x y] (partition 2 points)]
      (.lineTo p x y))
    p))

(defn draw-circle [^Graphics2D g x y r]
  (let [x1 (- x r)
        y1 (- y r)
        d (* 2 r)]
    (.draw g (Ellipse2D$Double. x1 y1 d d))))

(defmacro with-transforms [^Graphics2D g & body]
  `(let [a# (.getTransform ~g)]
     (do ~@body)
     (.setTransform ~g a#)))
;;
;; Keys
;;

(def key-codes [37 :left
                38 :up
                39 :right
                40 :down
                27 :esc
                32 :space
                10 :enter])

(doseq [[key-code key-name] (partition 2 key-codes)]
  (intern *ns* (symbol (str "key-" (name key-name))) key-code))

(def key-types {KeyEvent/KEY_TYPED     :typed
                KeyEvent/KEY_PRESSED   :pressed
                KeyEvent/KEY_RELEASED  :released})

(defn ->key-event [^KeyEvent e]
  {:type   (key-types (.getID e))
   :ch     (.getKeyChar e)
   :code   (.getKeyCode e)
   :action (.isActionKey e)
   :shift  (.isShiftDown e)
   :ctrl   (.isControlDown e)
   :meta   (.isMetaDown e)
   :alt    (.isAltDown e)})

(def key-listeners (atom {}))

(defonce key-event-dispatcher
  (let [d (proxy [KeyEventDispatcher] []
            (dispatchKeyEvent [e]
              (let [e (->key-event e)]
                (doseq [f (remove nil? (map (fn [[k v]] (when (= (select-keys e (keys k)) k) v)) @key-listeners))]
                  (f e)))
              true))]
    (.addKeyEventDispatcher (KeyboardFocusManager/getCurrentKeyboardFocusManager) d)
    d))

(defn add-key-listener! [event listener]
  (swap! key-listeners assoc event listener))

(defn remove-key-listener! [event]
  (swap! key-listeners dissoc event))

