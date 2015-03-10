(ns wmii-helper.player
  (:refer-clojure :exclude [next])
  (:import java.lang.Runtime)
  (:use clojure.java.io
        [clojure.string :only [split]]
        [clojure.java.shell :only [sh]])
  (:require [wmii-helper.settings :as s]
            [clojure.core.async :as async :refer [chan >!! <!! thread]]
            [clojure-watch.core :refer [start-watch]]))

(defn all-music []
  (->> @s/settings
       :music-folder
       file
       file-seq
       (filter #(.isFile %))
       (map #(.getAbsolutePath %))))
(def current-music-process (atom nil))
(def music (atom nil))
(def loop-enabled (atom nil))
(def next-call (chan))

(defn command [cmd]
  (spit (:mplayer-fifo-in @s/settings) (str cmd "\n")))

(defn next []
  (>!! next-call 0))

(defn play-file [file-path]
  (reset! loop-enabled nil)
  (let [sound-bar (:sound-bar @s/settings)
        filename (last (split file-path #"/"))
        process (. (Runtime/getRuntime) exec
                   (into-array
                    (concat
                    ["mplayer"
                     "-input"
                     (str "file=" (:mplayer-fifo-in @s/settings))
                     "-cache-min" (str (:player-cache @s/settings))
                     "-really-quiet"
                     "-slave"]
                     (if (= (last (split filename #"\.")) "m3u") ;; if it is playlist
                       ["-playlist" file-path]
                       [file-path]))))]
    (when @current-music-process
      (.destroy @current-music-process))
    (reset! current-music-process process)
    (when-not (= "" sound-bar)
      (sh "wmiir" "xwrite" sound-bar (str "label "
                                          (subs filename 0 (min 40 (count filename)))
                                          "...")))
    (thread
     (when (= 0 (.waitFor process)) ; run next only when sound ends, but not when user switch
       (next)))))

(defn pause []
  (if @current-music-process
    (command "pause")
    (next)))

(defn play [path]
  (play-file path))

(defn toggle-loop []
  (if @loop-enabled
    (command "loop -1")
    (command "loop 1"))
  (swap! loop-enabled not))

(defn init []
  (reset! music (all-music))
  (start-watch [{:path (:music-folder @s/settings)
                 :event-types [:create :modify :delete]
                 :callback (fn [event filename] (reset! music (all-music)))}])
  (thread
   (<!! next-call)
   (while true
     (play-file (rand-nth @music))
     (<!! next-call)))
  {"player-next" (fn [args] (next))
   "player-pause" (fn [args] (pause))
   "player-loop" (fn [args] (toggle-loop))
   "player-play-file" (fn [args] (play (:path args)))})
