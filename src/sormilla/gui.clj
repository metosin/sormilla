(ns sormilla.gui
  (:require [clojure.java.io :as io])
  (:import [java.awt Canvas Color Graphics2D Font KeyboardFocusManager KeyEventDispatcher]
           [java.awt.event KeyEvent]
           [java.awt.geom Ellipse2D$Double]
           [javax.swing JFrame JComponent]))

(set! *warn-on-reflection* true)

(defn ->safe [throttle f]
  (fn [& args]
    (try
      (apply f args)
      (catch Throwable e
        (println "error:" e)
        (Thread/sleep throttle)
        nil))))

(defn make-frame [source render & {:keys [interval safe safe-throttle max-size top exit-on-close] :or {interval 50 safe-throttle 500 max-size true}}]
  (let [run (atom true)
        frame (JFrame.)
        canvas (Canvas.)
        source (if safe (->safe safe-throttle source) source)
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
          (while (deref run)
            (let [start (System/currentTimeMillis)
                  data (source)]
              (loop [contents-lost? true]
                (loop [restored? true]
                  (let [g (.getDrawGraphics strategy)]
                    (render g (.getWidth canvas) (.getHeight canvas) data)
                    (.dispose g))
                  (when (.contentsRestored strategy) (recur true)))
                (.show strategy)
                (when (.contentsLost strategy) (recur true)))
              (let [time-left (- interval (- (System/currentTimeMillis) start))]
                (when (pos? time-left)
                  (Thread/sleep time-left)))))
          (catch Throwable e
            (println "Oh shit!" e)
            (throw e)))))
    {:run run
     :frame frame}))

(defn close-frame! [f]
  (when-let [{run :run ^JFrame frame :frame} f]
    (reset! run false)
    (doto frame
      (.setVisible false)
      (.dispose))))

;;
;; Keys
;;

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

(defn dispatch-key-event [e]
  (doseq [f (filter identity (map (fn [[k v]] (when (= (select-keys e (keys k)) k) v)) @listeners))]
    (f e)))

(defonce key-event-dispatcher (let [d (proxy [KeyEventDispatcher] [] (dispatchKeyEvent [e] (dispatch-key-event (->key-event e)) true))]
                                (.addKeyEventDispatcher (KeyboardFocusManager/getCurrentKeyboardFocusManager) d)
                                d))

(defn add-key-listener! [event listener]
  (swap! key-listeners assoc event listener))

(defn remove-key-listener! [event]
  (swap! key-listeners dissoc event))

(comment
  
  (add-key-listener! {:type :pressed} (fn [e] (println "pressed:" e)))
  (add-key-listener! {:ch \a} (fn [e] (println "a:" e)))
  (add-key-listener! {:ch \b :type :typed} (fn [e] (println "b:" e)))
  (add-key-listener! {:type :pressed :code 65 :ctrl true} (fn [_] (println "ctrl-a")))
  
  (defn r [^Graphics2D g ^long w ^long h data]
    (.setColor g Color/DARK_GRAY)
    (.fillRect g 0 0 w h)
    (.setColor g Color/RED)
    (.drawLine g 0 0 w h)
    (.drawLine g 0 h w 0))
  
  (defn s []
    "foo")
  
  (def f (make-frame #'s #'r))
  (close-frame! f))
