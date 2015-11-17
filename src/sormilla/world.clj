(ns sormilla.world
  (:require [com.stuartsierra.component :as component]))

(defrecord World [state]
  component/Lifecycle
  (start [this]
    (if state
      this
      (assoc this :state (atom {}))))
  (stop [this]
    (if state
      (assoc this :state nil)
      this)))

(defn create []
  (map->World {}))
