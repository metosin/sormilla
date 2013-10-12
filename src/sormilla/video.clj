(ns sormilla.video
  (:import [com.twilight.h264.decoder AVFrame AVPacket H264Decoder MpegEncContext]
           [com.twilight.h264.player FrameUtils]
           [java.awt RenderingHints Image]
           [java.awt.image BufferedImage]
           [java.net Socket InetSocketAddress]
           [javax.imageio ImageIO]
           [java.io InputStream OutputStream ByteArrayInputStream BufferedInputStream]
           [java.net InetAddress]
           [org.apache.commons.io IOUtils])
  (:require [clojure.java.io :as io]
            [metosin.system :as system]
            [sormilla.task :as task]
            [sormilla.world :refer [world]]
            [sormilla.bin :as bin :refer [ba->ia]]
            [sormilla.drone :as drone]
            [sormilla.drone-comm :as comm]))

(set! *warn-on-reflection* true)

(defn read-fully [^InputStream in ^bytes buffer ^long offset ^long size]
  (IOUtils/readFully in buffer offset size)
  buffer)

(defn make-reader [^InputStream in]
  (fn []
    (let [header   (byte-array 256)
          payload  (byte-array (+ 65535 MpegEncContext/FF_INPUT_BUFFER_PADDING_SIZE))]
      (try
        (read-fully in header 0 12)
        (let [signature    (bin/get-int header 0)
              header-size  (bin/get-short header 6)
              payload-size (bin/get-int header 8)
              payload      (byte-array payload-size)]
          (when-not (= signature 0x45566150) (throw (java.io.IOException. (format "out of sync (0x%08X)" signature))))
          (read-fully in header 12 (- header-size 12))
          (read-fully in payload 0 payload-size))
        (catch java.io.EOFException _
          nil)))))

(defn make-decoder []
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
        (when (zero? (first got-picture?)) (throw (java.io.IOException. "Could not decode frame")))
        (let [picture         (.displayPicture (.priv_data context))
              width           (.imageWidth picture)
              height          (.imageHeight picture)
              picture-buffer  (int-array (* width height))
              image           (BufferedImage. width height BufferedImage/TYPE_INT_RGB)]
          (FrameUtils/YUV2RGB picture picture-buffer)
          (.setRGB image 0 0 width height picture-buffer 0 width)
          image)))))

(def video-source (atom nil))

(defn open-socket ^Socket []
  (println "Connecting to video stream:" (str @video-source))
  (doto (Socket.)
    (.setSoTimeout 2000)
    (.connect (InetSocketAddress. ^InetAddress @video-source 5555))))

(def run (atom false))

(defn send-init-video [^Socket socket]
  (doto (.getOutputStream socket)
    (.write (byte-array (map bin/ubyte [1 0 0 0])))
    (.flush)))

(defn video-streaming-task []
  (try
    (while @run
      (let [socket      (open-socket)
            reader      (make-reader (-> socket .getInputStream BufferedInputStream.))
            decoder     (make-decoder)]
        (try
          (send-init-video socket)
          (while @run
            (->> (reader) (decoder) (swap! world assoc :image)))
          (catch java.io.IOException e
            (println "I/O error:" e ": reconnecting...")
            (Thread/sleep 1000))
          (finally
            (try (.close socket) (catch Exception _))))))
    (catch Throwable e
      (println "exception while processing video stream" e)
      (.printStackTrace e))))

;;
;; ============================================================================
;; Lifecycle:
;; ============================================================================
;;

(def service (reify system/Service
               (start! [this config]
                 (reset! run true)
                 (reset! video-source (if (:video-sim config)
                                        (InetAddress/getByName nil)
                                        comm/drone-ip))
                 (task/submit :video video-streaming-task)
                 config)
               (stop! [this]
                 (reset! run false)
                 (task/cancel :video))))

; ffmpeg -f h264 -an -i capture.h264 stream.m4v
