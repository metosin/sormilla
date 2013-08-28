(ns sormilla.video
  (:import [com.twilight.h264.decoder AVFrame AVPacket H264Decoder MpegEncContext]
           [com.twilight.h264.player FrameUtils]
           [java.awt RenderingHints Image]
           [java.awt.image BufferedImage]
           [java.net Socket InetSocketAddress]
           [javax.imageio ImageIO]
           [java.io InputStream OutputStream ByteArrayInputStream]
           [org.apache.commons.io IOUtils])
  (:require [sormilla.bin :as bin]
            [sormilla.drone :as drone]
            [sormilla.drone-comm :as comm]
            [sormilla.system :refer [run?]]
            [clojure.java.io :as io]))

(set! *warn-on-reflection* true)

(def image (atom nil))

(defn ba->ia [^bytes source ^ints target ^Long size]
  (loop [i 0]
    (aset-int target i (bit-and 0xFF (aget source i)))
    (when (< (inc i) size) (recur (inc i))))
  target)

(defn buffer->image ^BufferedImage [^Long w ^Long h ^ints buffer]
  (let [image (BufferedImage. w h BufferedImage/TYPE_INT_RGB)]
    (.setRGB image 0 0 w h buffer 0 w)
    image))

(defn read-buffer [^InputStream in buffer offset size]
  (when (neg? (.read in buffer offset size)) (throw (java.io.EOFException.))))

(defn make-reader [^InputStream in]
  (let [header   (byte-array 256)
        payload  (byte-array (+ 65535 MpegEncContext/FF_INPUT_BUFFER_PADDING_SIZE))]
    (fn []
      (try
        (read-buffer in header 0 12)
        (let [signature    (bin/get-int header 0)
              header-size  (bin/get-short header 6)
              payload-size (bin/get-int header 8)]
          (when-not (= signature 0x45566150) (throw (java.io.IOException. (format "out of sync (0x%08X)" signature))))
          (read-buffer in header 12 (- header-size 12))
          (read-buffer in payload 0 payload-size)
          [[header header-size] [payload payload-size]])
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

    (fn ^BufferedImage [[^bytes buffer size]]
      (set! (.size packet) size)
      (set! (.data_base packet) (ba->ia buffer ibuffer size))
      (set! (.data_offset packet) 0)
      
      (.avcodec_decode_video2 context frame got-picture? packet)
      (when (zero? (first got-picture?)) (throw (java.io.IOException. "Could not decode frame")))
      
      (let [picture         (.displayPicture (.priv_data context))
            width           (.imageWidth picture)
            height          (.imageHeight picture)
            picture-buffer  (int-array (* width height))]
        (FrameUtils/YUV2RGB picture picture-buffer)
        (buffer->image width height picture-buffer)))))

(defn open-socket ^Socket []
  (doto (Socket.)
    (.setSoTimeout 2000)
    (.connect (InetSocketAddress.  "localhost" #_ comm/drone-ip 5555))))

(defn save [^OutputStream out data]
  (doseq [[buffer size] data]
    (.write out buffer 0 size))
  out)

(defmacro while-let [[l r] & body]
  `(loop []
     (when-let [v# ~r]
       (let [~l v#]
         ~@body)
       (when v# (recur)))))

(defn init-video-streaming! []
  (future
    (try
      (while (run?)          
        (let [socket      (open-socket)
              out         (io/output-stream (io/file (str "sormilla-" (.format (java.text.SimpleDateFormat. "yyyyMMdd-HHmmss") (java.util.Date.)) ".h264")))
              reader      (make-reader (.getInputStream socket))
              decoder     (make-decoder)]
          (try
            (doto (.getOutputStream socket)
              (.write (byte-array (map bin/ubyte [1 0 0 0])))
              (.flush))
            (while (run?)
              (let [data (reader)]
                #_(save out data)
                (let [i (decoder (second data))]
                  (reset! image i))))
            (catch java.io.IOException e
              (println "I/O error:" e ": reconnecting...")
              (Thread/sleep 1000))
            (finally
              (try (.close socket) (catch Exception _))
              (try (.close ^OutputStream @out) (catch Exception _))))))
      (catch Throwable e
        (println "exception while processing video stream" e)
        (.printStackTrace e))
      (finally
        (reset! image nil)))))

;;
;; here be dragons...
;;

(comment

  (drone/init-drone)
  (init-video-streaming!)
  (:video (:telemetry @sormilla.system/status))
  
  (sormilla.system/shutdown!)
  (reset! sormilla.system/status {:run true})
  (comm/send-commands! [[:led 0 1.0 0]])
  (comm/send-commands! [[:config "video:video_on_usb" true]])
  (comm/send-commands! [comm/comm-reset
                        comm/trim
                        comm/enable-navdata
                        comm/ctrl-ack
                        comm/land
                        comm/leds-active])
  
  (:video (:telemetry @sormilla.system/status))
  
  
(defn parse-file []
  (let [in (io/input-stream (io/file "sormilla-20130825-164933.h264"))
        decoder (make-decoder in)]
    (try
      (doseq [i (range 10)]
        (ImageIO/write (decoder) "png" (io/file (str "image-" i ".png"))))
      (println "success!")
      (catch Exception e
        (println "failure" e)
        (.printStackTrace e))
      (finally
        (try (.close in) (catch Exception e))))))

(parse-file)

; ffmpeg -f h264 -an -i capture.h264 stream.m4v
)

(defn capture []
  (let [socket (doto (Socket.)
                 (.setSoTimeout 2000)
                 (.connect (InetSocketAddress. comm/drone-ip 5555)))
        in (.getInputStream socket) #_(io/input-stream socket)
        out (io/output-stream (io/file "capture.h264"))
        buffer (byte-array 1024)]
    (try
      (doto (.getOutputStream socket)
        (.write (byte-array (map bin/ubyte [1 0 0 0])))
        (.flush))
      (doseq [n (range 2048)]
        (when (zero? (mod n 100)) (println (str "frame " n "...")))
        (.write out buffer 0 (.read in buffer 0 1024)))
      (println "success!")
      (catch Exception e
        (println "failure" e)
        (.printStackTrace e))
      (finally
        (try (.close socket) (catch Exception e))
        (try (.close out) (catch Exception e))))))

(comment
  (capture)
) 
