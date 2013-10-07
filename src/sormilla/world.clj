(ns sormilla.world
  (:require [metosin.system :as system]))

(def world (atom nil))

;;
;; ============================================================================
;; Lifecycle:
;; ============================================================================
;;

(def service (reify system/Service
               (start! [this config]
                 (reset! world {})
                 config)
               (stop! [this])))
