(ns sormilla.system)

(set! *warn-on-reflection* true)

(def status (atom {:run true}))

(defn run? []
  (:run @status))

(defn shutdown! []
  (swap! status assoc :run false))

(defmacro task [interval f]
  `(future
     (while (run?)
       (try
         (let [start# (System/currentTimeMillis)
               result# (~f @status)
               time-left# (- ~interval (- (System/currentTimeMillis) start#))]
           (swap! status assoc ~(keyword (name f)) result#)
           (when (pos? time-left#)
             (Thread/sleep time-left#)))
         (catch Throwable e#
           (println "task failure:" ~(name f))
           (.printStackTrace e#)
           (Thread/sleep 1000))))))
