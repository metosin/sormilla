(ns user
  (:require [clojure.java.io :as io]
            [clojure.string :as s]
            [clojure.pprint :refer [pprint]]
            [clojure.repl :refer :all]
            [clojure.tools.namespace.repl :refer [refresh refresh-all set-refresh-dirs]]
            [sormilla.system :refer [start! stop!]]
            [sormilla.world :refer [world]]))

(set-refresh-dirs "./src" "./dev")

(def base-config {:video-sim true})

(defn restart [& [config]]
  (stop!)
  (start! (merge base-config config)))

(defn reset []
  (stop!)
  (refresh :after 'sormilla.system/start!)) ; FIXME: config?

"commence hacking"
