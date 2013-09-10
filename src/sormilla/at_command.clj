(ns sormilla.at-command
  (:require [clojure.string :as s]
            [sormilla.bin :refer :all]))

(set! *warn-on-reflection* true)

;;
;; AT command argument packing:
;;

(defprotocol Argument
  (pack [this]))

(extend-protocol Argument
  String
  (pack [this] (str \" this \"))
  
  Double
  (pack [this] (str (f->i this)))

  Long
  (pack [this] (str this))

  Boolean
  (pack [this] (if this "\"TRUE\"" "\"FALSE\""))

  clojure.lang.PersistentVector
  (pack [this] (str (bits->i this))))

;;
;; AT command construction:
;;

(def at-command-name {:led     "AT*LED"
                      :trim    "AT*FTRIM"
                      :comwdg  "AT*COMWDG"
                      :ref     "AT*REF"
                      :config  "AT*CONFIG"
                      :ctrl    "AT*CTRL"
                      :pcmd    "AT*PCMD"})

(defn make-at-command [[id command & args]]
  (str
    (at-command-name command)
    \=
    id
    (when (seq args) (str \, (s/join \, (map pack args))))
    \return))

(defn make-at-commands ^String [commands]
  (s/join (map make-at-command commands)))
