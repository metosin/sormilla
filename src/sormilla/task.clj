(ns sormilla.task
  (:require [com.stuartsierra.component :as component])
  (:import [java.util.concurrent Future Executors TimeUnit ExecutionException CancellationException]))

(defn wrap-task [id task-fn args]
  (fn []
    (try
      (apply task-fn args)
      (catch Exception e
        (println "task failure:" id)
        (.printStackTrace e)
        (Thread/sleep 1000)))))

(defn cancel [task id]
  (when-let [^Future f (get (:tasks task) id)]
    (swap! (:tasks task) dissoc id)
    (.cancel f true)))

(defn- execute [task execute-fn id task-fn args]
  (cancel task id)
  (let [e (:executor task)
        t (wrap-task id task-fn args)
        f (execute-fn e t)]
    (swap! (:tasks task) assoc id f)
    f))

(defn submit [task id task-fn & args]
  (execute task (fn [e t] (.submit e t)) id task-fn args))

(defn schedule [task id interval task-fn & args]
  (execute
    task
    (fn [e t] (.scheduleAtFixedRate e t interval interval TimeUnit/MILLISECONDS))
    id
    task-fn
    args))

;;
;; ============================================================================
;; Lifecycle:
;; ============================================================================
;;

(defrecord Task [tasks executor]
  component/Lifecycle
  (start [this]
    (assoc this
           :tasks (atom {})
           :executor (Executors/newScheduledThreadPool 4)))
  (stop [this]
    (when executor
      (.shutdown executor))
    (when tasks
      (doseq [id (keys @tasks)]
        (cancel this id)))
    (assoc this :tasks nil :exectutor nil)))

(defn create []
  (map->Task {}))


