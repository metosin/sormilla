(ns sormilla.swing
  (:require [clojure.java.io :as io]
            [com.stuartsierra.component :as component])
  (:import [java.awt Graphics2D Shape Color KeyboardFocusManager KeyEventDispatcher Toolkit]
           [java.awt.geom Path2D$Double Ellipse2D$Double]
           [java.awt.event KeyEvent]))

(set! *warn-on-reflection* true)


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

(doseq [[key-code key-name] (partition 2 [37 :left
                                          38 :up
                                          39 :right
                                          40 :down
                                          27 :esc
                                          32 :space
                                          10 :enter])]
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

(defn init-key-event-dispatcher [key-listeners]
  (let [d (proxy [KeyEventDispatcher] []
            (dispatchKeyEvent [e]
              (let [e (->key-event e)]
                (doseq [f (remove nil? (map (fn [[k v]] (when (= (select-keys e (keys k)) k) v)) @key-listeners))]
                  (f e)))
              true))]
    (.addKeyEventDispatcher (KeyboardFocusManager/getCurrentKeyboardFocusManager) d)
    d))

(defn add-key-listener! [swing event listener]
  (swap! (:key-listeners swing) assoc event listener))

(defn remove-key-listener! [swing event]
  (swap! (:key-listeners swing) dissoc event))

(defrecord Swing [key-listeners world]
  component/Lifecycle
  (start [this]
    (if key-listeners
      this
      (let [this (assoc this :key-listeners (atom {}))]
        (init-key-event-dispatcher (:key-listeners this))
        (doseq [[code key-name] (partition 2 [key-left :left key-right :right key-up :up key-down :down])]
          (add-key-listener! this {:type :pressed :code code} (fn [_] (swap! (:state world) assoc-in [:keys key-name] true)))
          (add-key-listener! this {:type :released :code code} (fn [_] (swap! (:state world) assoc-in [:keys key-name] false))))
        this)))
  (stop [this]
    (doseq [[k _] @key-listeners]
      (remove-key-listener! this k))
    (assoc this :key-listeners nil)))

(defn create []
  (map->Swing {}))
