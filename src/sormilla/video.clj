(ns sormilla.video
  (:import [com.twilight.h264.decoder AVFrame AVPacket H264Decoder MpegEncContext]
           [com.twilight.h264.player FrameUtils]
           [java.awt.image BufferedImage]
           [java.io InputStream BufferedInputStream]
           [java.net Socket InetAddress])
  (:require [clojure.java.io :as io]
            [clojure.core.async :as async :refer [>!! <!! alts!! go]]
            [sormilla.world :refer [world]]
            [sormilla.bin :as bin :refer [ba->ia]]
            [sormilla.io-utils :as io-utils]
            [sormilla.drone :as drone]
            [sormilla.drone-comm :as comm]))

(set! *warn-on-reflection* true)

(defn make-frame-reader [^InputStream in]
  (fn []
    (try
      (let [header       (io-utils/read-fully in (byte-array 12) 0 12)
            signature    (bin/get-int header 0)
            header-size  (bin/get-short header 6)
            payload-size (bin/get-int header 8)
            payload      (byte-array payload-size)]
        (when-not (= signature 0x45566150) (throw (java.io.IOException. (format "out of sync (0x%08X)" signature))))
        (io-utils/skip-fully in (- header-size 12))
        (io-utils/read-fully in payload 0 payload-size))
      (catch java.io.IOException _
        nil)
      (catch InterruptedException _
        nil))))

(defn make-frame-decoder []
  (let [codec           (H264Decoder.)
        context         (MpegEncContext/avcodec_alloc_context)
        packet          (AVPacket.)
        frame           (AVFrame/avcodec_alloc_frame)
        ibuffer         (int-array (+ 65535 MpegEncContext/FF_INPUT_BUFFER_PADDING_SIZE))
        got-picture?    (int-array [0])]

    (if-not (zero? (bit-and (.capabilities codec) H264Decoder/CODEC_CAP_TRUNCATED)) (throw (Exception. "need to configure CODEC_FLAG_TRUNCATED")))
    (if (neg? (.avcodec_open context codec)) (throw (Exception. "Could not open codec")))
    (.av_init_packet packet)

    (fn ^BufferedImage [^bytes buffer]
      (let [size (alength buffer)]
        (set! (.size packet) size)
        (set! (.data_base packet) (ba->ia buffer ibuffer size))
        (set! (.data_offset packet) 0)
        (.avcodec_decode_video2 context frame got-picture? packet)
        ;(when (zero? (first got-picture?)) (throw (java.io.IOException. "Could not decode frame")))
        (when-not (zero? (first got-picture?))
          (let [picture         (.displayPicture (.priv_data context))
                width           (.imageWidth picture)
                height          (.imageHeight picture)
                picture-buffer  (int-array (* width height))
                image           (BufferedImage. width height BufferedImage/TYPE_INT_RGB)]
            (FrameUtils/YUV2RGB picture picture-buffer)
            (.setRGB image 0 0 width height picture-buffer 0 width)
            image))))))

(defmacro while-let [[l r] & body]
  `(loop [~l ~r]
     (when ~l
       ~@body
       (recur ~r))))

(defmacro thread* [& body]
  `(async/thread
     (try
       ~@body
       (catch Throwable e#
         (println "exception in thread:" e#)
         (.printStackTrace e#)
         nil))))

(defn read-frame-task [^Socket socket frame-ch]
  (when-let [frame-reader (some-> socket .getInputStream BufferedInputStream. make-frame-reader)]
    (while-let [frame (frame-reader)]
      (>!! frame-ch frame))))

(defn decode-frame-task [frame-ch image-ch]
  (let [frame-decoder (make-frame-decoder)]
    (while-let [frame (<!! frame-ch)]
      (when-let [image (frame-decoder frame)]
        (>!! image-ch image)))))

(defn update-image-task [image-ch]
  (while-let [image (<!! image-ch)]
    (swap! world assoc :image image)))

(defn video-streaming-task [cancel-ch]
  (loop []
    (println "video: connecting...")
    (let [socket   (io-utils/open-video-socket comm/drone-ip #_ (InetAddress/getByName nil))
          frame-ch (async/chan (async/sliding-buffer 10))
          image-ch (async/chan (async/sliding-buffer 1))
          [v ch]   (alts!! [cancel-ch
                            (thread* (read-frame-task socket frame-ch))
                            (thread* (decode-frame-task frame-ch image-ch))
                            (thread* (update-image-task image-ch))])]
      (println "video: cleanup...")
      (async/close! frame-ch)
      (async/close! image-ch)
      (io-utils/close-socket! socket)
      (when-not (= ch cancel-ch)
        (println "video: reconnecting in 1 sec...")
        (Thread/sleep 1000)
        (recur)))))

;;
;; ============================================================================
;; Lifecycle:
;; ============================================================================
;;

(defn start-subsys! [config]
  (let [ch (async/chan)]
    (thread* (video-streaming-task ch))
    (assoc config :video ch)))

(defn stop-subsys! [config]
  (when-let [ch (:video config)]
    (async/close! ch))
  (dissoc config :video))

; ffmpeg -f h264 -an -i capture.h264 stream.m4v
