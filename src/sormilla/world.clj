(ns sormilla.world
  (:require [metosin.system :as system]))

(def world (atom {}))

;;
;; ============================================================================
;; Lifecycle:
;; ============================================================================
;;

(def service (reify system/Service
               (start! [this config]
                 (reset! world {})
                 config)
               (stop! [this config]
                 config)))
