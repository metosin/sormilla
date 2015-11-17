sormilla
========

Playing with Leap Motion and Parrot AR.Drone 2

# What is this?

Playing with [LeapMotion](http://www.leapmotion.com) and [AR.Drone](http://ardrone2.parrot.com) using [Clojure](http://www.clojure.org).

# How to install?

## H264 decoder

This application depends on H264 decoder that I could not find from any usual repos, so unfortunately you will need
to install that manually _(I'm looking for better solution, suggestions are welcome)_. Here's how to do it with
[lein localrepo plugin](https://github.com/kumarshantanu/lein-localrepo):

```
$ wget 'https://github.com/gigasquid/clj-drone/blob/master/h264/h264-decoder-1.0.jar?raw=true' -O h264-decoder-1.0.jar
$ lein localrepo install ./h264-decoder-1.0.jar h264-decoder/h264-decoder "1.0"
```

## Sormilla

```
$ git clone https://github.com/jarppe/sormilla.git
$ cd sormilla
$ lein run
```

Make sure you have LeapMotion attached to your computer and an AR.Drone up and running. Note that you need to be in the same WLAN with the drone.

## TODO

- Use [component](https://github.com/stuartsierra/component)
- Simplify the task handler implementation

# Thanks

Special thanks to https://github.com/nakkaya/ardrone and https://github.com/gigasquid/clj-drone.
