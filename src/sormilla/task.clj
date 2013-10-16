(ns sormilla.task
  (:import [java.util.concurrent Future Executors TimeUnit ExecutionException CancellationException]))

(defonce ^:private tasks (atom {}))
(defonce ^:private executor (atom nil))

(defn wrap-task [id task-fn args]
  (fn []
    (try
      (apply task-fn args)
      (catch Exception e
        (println "task failure:" id)
        (.printStackTrace e)
        (Thread/sleep 1000)))))

(defn cancel [id]
  (when-let [^Future f (get @tasks id)]
    (swap! tasks dissoc id)
    (.cancel f true)))

(defn- execute [execute-fn id task-fn args]
  (cancel id)
  (let [e @executor
        t (wrap-task id task-fn args)
        f (execute-fn e t)]
    (swap! tasks assoc id f)
    f))

(defn submit [id task-fn & args]
  (execute (fn [e t] (.submit e t)) id task-fn args))

(defn schedule [id interval task-fn & args]
  (execute
    (fn [e t] (.scheduleAtFixedRate e t interval interval TimeUnit/MILLISECONDS))
    id
    task-fn
    args))

;;
;; ============================================================================
;; Lifecycle:
;; ============================================================================
;;

(defn start-subsys! [config]
  (reset! executor (Executors/newScheduledThreadPool 4))
  (reset! tasks {})
  config)

(defn stop-subsys! [config]
  (when-let [es @executor]
    (reset! executor nil)
    (.shutdown es))
  (doseq [id (keys @tasks)]
    (cancel id))
  config)

