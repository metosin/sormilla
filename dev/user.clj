(ns user
  (:require [clojure.java.io :as io]
            [clojure.string :as s]
            [clojure.pprint :refer [pprint]]
            [clojure.repl :refer :all]
            [clojure.tools.namespace.repl :refer [refresh refresh-all set-refresh-dirs]]
            [sormilla.system :refer [start! stop!]]
            [sormilla.world :refer [world]]))

(set-refresh-dirs "./src" "./dev")

(defn restart []
  (stop!)
  (start!))

(defn reset []
  (stop!)
  (refresh :after 'sormilla.system/start!))

"commence hacking"
