(ns sormilla.video-test
  (:require [sormilla.video :refer :all]
            [sormilla.bin :refer [ubyte uint]]
            [midje.sweet :refer :all]))

(facts ba->ia
  (let [source (byte-array (map ubyte [0x00 0x55 0x88 0xFF]))
        target (int-array 4)
        expected (int-array (map uint [0x00 0x55 0x88 0xFF]))]
    (ba->ia source target 4)
    (java.util.Arrays/equals target expected) => true))
