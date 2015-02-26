(ns wmii-helper.core
  (:refer-clojure :exclude [read-string])
  (:use clojure.java.io
        [clojure.string :only [split join]]
        [clojure.edn :only [read-string]])
  (:require [wmii-helper.settings :as s]
            [wmii-helper.wallpapers :as wallpapers]
            [wmii-helper.player :as player])
  (:gen-class))

(def reactions (atom {}))
(def plugin-defs {:wallpapers wallpapers/init
                  :player player/init})

(defn add-reaction [command callback]
  (swap! reactions assoc command callback))

(defn exec [line]
  (let [line-tokens (split line #"\s|\n")
        command (first line-tokens)
        args (try
               (read-string (join " " (rest line-tokens)))
               (catch Exception _
                 nil))]
    (when-let [callback (@reactions command)]
      (try
        (callback args)
        (catch Exception e
          (binding [*out* *err*]
            (println "Error when process command: " (.getMessage e))))))))

(add-reaction "exit" (fn [args] (System/exit 0)))

(defn -main [& args]
  (s/init)
  (doseq [plugin (:plugins @s/settings)]
    (swap! reactions merge ((plugin-defs plugin))))
  (while true
    (exec (slurp (:fifo-in @s/settings)))))
