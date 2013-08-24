(ns sormilla.drone-comm-test
  (:require [sormilla.drone-comm :refer :all]
            [midje.sweet :refer :all]))

(facts a->s
  (a->s "foo")    => "\"foo\""
  (a->s 42)       => "42"
  (a->s 3.14159)  => "1078530000"
  (a->s [])       => (str 0x00)
  (a->s [0])      => (str 0x01)
  (a->s [0 3])    => (str 0x09))

(facts "known commands"
  (a->s [18 20 22 24 28]) => "290717696"    ; land
  (a->s [8 18 20 22 24 28]) => "290717952") ; emergency stop

(facts make-at-commands
  (reset! command-id 0)
  (make-at-commands [trim])                    => "AT*FTRIM=1\r"
  (make-at-commands [trim takeoff])            => "AT*FTRIM=2\rAT*REF=3,9,18,20,22,24,28\r"
  (make-at-commands [trim comm-reset takeoff]) => "AT*FTRIM=4\rAT*COMWDG=1\rAT*REF=2,9,18,20,22,24,28\r")

(facts "parse-nav-state"
  (parse-nav-state  0x00000000) => (contains {:flying :landed :emergency :ok})
  (parse-nav-state  0x00000001) => (contains {:flying :flying :emergency :ok})
  (parse-nav-state  0x80000000) => (contains {:flying :landed :emergency :detected})
  (parse-nav-state  0x80000001) => (contains {:flying :flying :emergency :detected}))

