(ns sormilla.main
  (:gen-class))

(defn -main [& args]
  (require 'sormilla.system)
  ((resolve 'sormilla.system/start-system!)))
