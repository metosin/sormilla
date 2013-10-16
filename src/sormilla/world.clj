(ns sormilla.world)

(def world (atom {}))

;;
;; ============================================================================
;; Lifecycle:
;; ============================================================================
;;

(defn start-subsys! [config]
  (reset! world {})
  config)

(defn stop-subsys! [config]
  config)
