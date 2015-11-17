(ns sormilla.system
  (:require [com.stuartsierra.component :refer [system-map using]]
            [sormilla.world :as world]
            [sormilla.task :as task]
            [sormilla.gui :as gui]
            [sormilla.drone-navdata :as navdata]
            [sormilla.drone :as drone]
            [sormilla.leap :as leap]
            [sormilla.video :as video]
            [sormilla.swing :as swing]))

(defn base-system []
  (system-map
    :world   (world/create)
    :task    (task/create)
    :gui     (-> (gui/create)
                 (using [:world :task]))
    :navdata (navdata/create)
    :drone   (-> (drone/create)
                 (using [:world :swing :task :navdata]))
    :leap    (-> (leap/create)
                 (using [:task :world]))
    :video   (-> (video/create)
                 (using [:world]))
    :swing   (-> (swing/create)
                 (using [:world]))))
