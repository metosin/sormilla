(ns user
  (:require [reloaded.repl :refer [go start stop reset]]))

(reloaded.repl/set-init! (fn []
                           (require 'sormilla.system)
                           ((resolve 'sormilla.system/base-system))))

"commence hacking"
