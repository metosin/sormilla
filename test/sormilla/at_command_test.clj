(ns sormilla.at-command-test
  (:require [sormilla.at-command :refer :all]
            [midje.sweet :refer :all]))

(facts pack
  (pack "foo")    => "\"foo\""
  (pack 42)       => "42"
  (pack 3.14159)  => "1078530000"
  (pack [])       => (str 0x00)
  (pack [0])      => (str 0x01)
  (pack [0 3])    => (str 0x09)
  (pack true)     => "\"TRUE\""
  (pack false)    => "\"FALSE\"")

(facts "known commands"
  (pack [18 20 22 24 28])   => "290717696"   ; land
  (pack [8 18 20 22 24 28]) => "290717952")  ; emergency stop

(facts make-at-commands
  (make-at-commands [[1 :trim]])
    => "AT*FTRIM=1\r"
  (make-at-commands [[2 :trim] [3 :ref [9 18 20 22 24 28]]])
    => "AT*FTRIM=2\rAT*REF=3,290718208\r"
  (make-at-commands [[4 :config "general:navdata_demo" false]])
    => "AT*CONFIG=4,\"general:navdata_demo\",\"FALSE\"\r")
