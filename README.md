Switchable wallpapers (by timeout or shortcut) and mplayer wrapper for wmii.
Application is controlled by named pipe.
It uses config file for settings, config must place in folder with program with name `settings.edn`

Config example:
---------------

```clojure
{  :plugins [:wallpapers :player] ; what functionality do you need
   :images-folder "/path/to" ; folder with wallpaper images
   :music-folder "/path/to"  ; default folder with music
   :switch-time 30           ; timeout for switching wallpaper
   :player-cache 20          ; mplayer -cache-min option
   :sound-bar "/rbar/sound"  ; wmii bar for output current song name
   :player-current-folder "/tmp/.wmii-helper-player-folder" ; file that stores current folder of player
   :mplayer-fifo-in "/tmp/.wmii-helper-mplayer-in"  ; fifo used by program to control mplayer
   :fifo-in "/tmp/.wmii-helper-in"} ; fifo used by wmii to control program
```

Available commands:
-------------------

 - `exit` - shutdown program
 - `wallpaper-next` - switch wallpaper
 - `player-next` - start next random song
 - `player-pause` - pause player
 - `player-loop` - infinitely play current song / disable infinite loop
 - `player-play-file {:path "/path/to/song"}` - plays concrete file, path can be an url or playlist(.m3u extension)
 - `player-change-folder {:path "/path/to/music"}` - change music folder dynamically

Usage example:
-------

```bash
... # your wmiirc
# wmii-helper fifo
HELPER_FIFO_IN=/tmp/.wmii-helper-in
startup() {
	# run wmii-helper at startup
	cd /path/to/jar
	java -jar wmii-helper-1.0.0-SNAPSHOT-standalone.jar &
	cd ~
	# cd's is necessary for the program to find the configuration file
}
...
# somewhere at shortcut definitions
KeyGroup Helper
Key Mod3-q
	echo player-pause > $HELPER_FIFO_IN &
Key Mod3-x
	echo player-next > $HELPER_FIFO_IN &
Key Mod3-z
	echo player-loop > $HELPER_FIFO_IN &
Key Mod3-p
	echo wallpaper-next > $HELPER_FIFO_IN &
Key Mod3-s
	{
	MUSIC=/path/to/music/`ls /path/to/music| wimenu`
	echo player-play-file {:path \"$MUSIC\"} > $HELPER_FIFO_IN
	} &
```

Copyright © 2015 Alexey Kolpakov

Distributed under DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE.
