(ns user
  (:require [clojure.java.io :as io]
            [clojure.string :as s]
            [clojure.pprint :refer [pprint]]
            [clojure.repl :refer :all]
            [clojure.tools.namespace.repl :refer [refresh refresh-all] :as repl]
            [sormilla.system :refer [start! stop!]]
            [sormilla.world :refer [world]]))

(repl/set-refresh-dirs "./src" "./dev")

(defn restart [& [config]]
  (stop!)
  (start! config))

(defn reset []
  (stop!)
  (refresh :after 'sormilla.system/start!))

"commence hacking"
