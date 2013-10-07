(ns sormilla.task
  (:require [metosin.system :as system])
  (:import [java.util.concurrent Future Executors TimeUnit ExecutionException CancellationException]))

(defonce ^:private tasks (atom {}))
(defonce ^:private executor (atom nil))

(defn wrap-task [id task-fn]
  (fn []
    (try
      (task-fn)
      (catch Exception e
        (println "task failure:" id)
        (.printStackTrace e)
        (Thread/sleep 1000)))))

(defn cancel [id]
  (when-let [^Future f (get @tasks id)]
    (swap! tasks dissoc id)
    (.cancel f true)))

(defn- execute [execute-fn id task-fn]
  (cancel id)
  (let [e @executor
        t (wrap-task id task-fn)
        f (execute-fn e t)]
    (swap! tasks assoc id f)
    f))

(defn submit [id task-fn]
  (execute (fn [e t] (.submit e t)) id task-fn))

(defn schedule [id task-fn & {:keys [interval] :or {interval 100}}]
  (execute
    (fn [e t] (.scheduleAtFixedRate e t interval interval TimeUnit/MILLISECONDS))
    id
    task-fn))

;;
;; ============================================================================
;; Lifecycle:
;; ============================================================================
;;

(def service (reify system/Service
               (start! [this config]
                 (.stop! this)
                 (reset! executor (Executors/newScheduledThreadPool 4))
                 (reset! tasks {})
                 config)
               (stop! [this]
                 (when-let [es @executor]
                   (reset! executor nil)
                   (.shutdown es))
                 (doseq [id (keys @tasks)]
                   (cancel id)))))
