(defproject wmii-helper "2.0.0-SNAPSHOT"
  :description "Dynamic wallpapers and mplayer wrapper for wmii"
  :main wmii-helper.core
  :aot :all
  :license {:name "DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE"
            :url "http://www.wtfpl.net/txt/copying/"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [clojure-watch "0.1.10"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]])
