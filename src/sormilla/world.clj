(ns sormilla.world
  (:require [metosin.system :as system]))

(def world (atom {}))

;;
;; ============================================================================
;; Lifecycle:
;; ============================================================================
;;

(def service (reify system/Service
               (start! [_ config]
                 (reset! world {})
                 config)
               (stop! [_ config]
                 config)))
