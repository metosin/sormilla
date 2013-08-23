(ns sormilla.navdata-logging
  (:require [clojure.java.io :as io]
            [clojure.string :as s]))

(set! *warn-on-reflection* true)

(defonce navdata-log (agent nil))

(defn navdata-logging! [enable?]
  (send navdata-log (if enable?
                      (constantly (io/writer (io/file "navdata.log") :append true))
                      (fn [^java.io.Writer w] (when w (.close w)) nil))))

(defn write-navdata [^java.io.Writer w navdata navdata-len]
  (when w
    (.write w (format "%016X %04X" (System/currentTimeMillis) navdata-len))
    (doseq [i (range navdata-len)]
      (.write w (format "%02X " (bit-and 0xFF (nth navdata i)))))
    (.write w "\n")
    (.flush w)
    w))

(defn log [navdata navdata-len]
  (send-off navdata-log write-navdata navdata navdata-len))
