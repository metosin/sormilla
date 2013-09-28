(ns sormilla.drone-comm-test
  (:require [sormilla.drone-comm :refer :all]
            [midje.sweet :refer :all]))

(facts "parse-nav-state"
  (parse-nav-state  0x00000000) => (contains {:flying :landed :emergency :ok})
  (parse-nav-state  0x00000001) => (contains {:flying :flying :emergency :ok})
  (parse-nav-state  0x80000000) => (contains {:flying :landed :emergency :detected})
  (parse-nav-state  0x80000001) => (contains {:flying :flying :emergency :detected}))

