(ns wmii-helper.player
  (:refer-clojure :exclude [next])
  (:import java.lang.Runtime)
  (:use clojure.java.io
        [clojure.string :only [split]]
        [clojure.java.shell :only [sh]])
  (:require [wmii-helper.settings :as s]
            [clojure.core.async :as async :refer [chan >!! <!! thread]]
            [clojure-watch.core :refer [start-watch]]))

(def current-music-process (atom nil))
(def folder-watcher (atom nil))
(def music (atom nil))
(def loop-enabled (atom nil))
(def next-call (chan))

(defn all-music [path]
  (->> path
       file
       .listFiles
       vec
       (filter #(.isFile %))
       (map #(.getAbsolutePath %))))

(defn init-filesystem [path]
  (when @folder-watcher (@folder-watcher))
  (spit (:player-current-folder @s/settings) path)
  (reset! music (all-music path))
  (reset! folder-watcher
          (start-watch [{:path path
                         :event-types [:create :modify :delete]
                         :callback (fn [event filename] (reset! music (all-music path)))}])))

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
                      "-really-quiet"
                      "-slave"]
                     (:mplayer-options @s/settings)
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

(defn change-folder [path]
  (init-filesystem path)
  (next))

(defn toggle-loop []
  (if @loop-enabled
    (command "loop -1")
    (command "loop 1"))
  (swap! loop-enabled not))

(defn increase-volume []
  (command "volume 100"))

(defn seek-forward []
  (command (str "seek "
                (:player-seek-seconds @s/settings)
                " 0")))

(defn seek-backward []
  (command (str "seek "
                (- (:player-seek-seconds @s/settings))
                " 0")))

(defn decrease-volume []
  (command "volume 0"))

(defn init []
  (init-filesystem (:music-folder @s/settings))
  (thread
   (<!! next-call)
   (while true
     (when (seq @music)
       (play-file (rand-nth @music)))
     (<!! next-call)))
  {"player-next" (fn [args] (next))
   "player-pause" (fn [args] (pause))
   "player-loop" (fn [args] (toggle-loop))
   "player-play-file" (fn [args] (play-file (:path args)))
   "player-seek-forward" (fn [args] (seek-forward))
   "player-seek-backward" (fn [args] (seek-backward))
   "player-increase-volume" (fn [args] (increase-volume))
   "player-decrease-volume" (fn [args] (decrease-volume))
   "player-change-folder" (fn [args] (change-folder (:path args)))})
