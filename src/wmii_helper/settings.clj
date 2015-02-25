(ns wmii-helper.settings
  (:refer-clojure :exclude [read-string])
  (:use [clojure.edn :only [read-string]]
        [clojure.java.io :only [as-file]]
        [clojure.java.shell :only [sh]]))

(def default-settings
  {:images-folder "."
   :music-folder "."
   :switch-time 30 ;;minutes
   ; wmii bar for output current song
   :sound-bar "/rbar/sound"
   ; input/output fifos
   :mplayer-fifo-in "/tmp/.wmii-helper-mplayer-in"
   :fifo-in "/tmp/.wmii-helper-in"})

(def settings (atom nil))

(defn init []
  (reset! settings (merge default-settings
                          (try
                            (read-string (slurp "settings.edn"))
                            (catch Exception e
                              (binding [*out* *err*]
                                (println "Settings file are corrupted or doesn't exists, fallback to default settings"))
                              nil))))
  (when-not (.exists (as-file (:fifo-in @settings)))
    (sh "mkfifo" (:fifo-in @settings)))
  (when-not (.exists (as-file (:mplayer-fifo-in @settings)))
    (sh "mkfifo" (:mplayer-fifo-in @settings))))


