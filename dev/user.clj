(ns user
  (:require [clojure.java.io :as io]
            [clojure.string :as s]
            [clojure.pprint :refer [pprint]]
            [clojure.repl :refer :all]
            [clojure.tools.namespace.repl :refer [refresh refresh-all] :as repl]
            [sormilla.system :refer [start-system! stop-system!]]
            [sormilla.world :refer [world]]))

(repl/set-refresh-dirs "./src")

(defn restart []
  (stop-system!)
  (start-system!))

(defn reset []
  (stop-system!)
  (refresh :after 'sormilla.system/start-system!))

"commence hacking"
