(ns wmii-helper.wallpapers
  (:refer-clojure :exclude [next])
  (:use clojure.java.io
        [clojure.java.shell :only [sh]])
  (:require [wmii-helper.settings :as s]
            [clojure.core.async :as async :refer [chan >!! alts!! timeout thread]]
            [clojure-watch.core :refer [start-watch]]))

(defn all-wallpapers []
  (sort-by #(.getName %)
           (filter #(.isFile %)
                   (file-seq (file (:images-folder @s/settings))))))

(def wallpapers (atom nil))

(def current-wallpaper-idx (atom 0))
(def user-call (chan))

(defn inc-idx [idx]
  (mod (inc idx) (count @wallpapers)))

(defn set-wallpaper [file]
  (let [file-path (.getAbsolutePath file)]
    (sh "display" "-window" "root" file-path)))

(defn next []
  (>!! user-call 0))

(defn init []
  (reset! wallpapers (all-wallpapers))
  (start-watch [{:path (:images-folder @s/settings)
                 :event-types [:create :modify :delete]
                 :callback (fn [event filename] (reset! wallpapers (all-wallpapers)))}])
  (thread
   (while true
     (set-wallpaper (nth @wallpapers @current-wallpaper-idx))
     (alts!! [user-call (timeout (* 60000 (:switch-time @s/settings)))])
     (swap! current-wallpaper-idx inc-idx)))
  {"wallpaper-next" (fn [args] (next))})
