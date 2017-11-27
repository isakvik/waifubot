# waifubot
Discord bot for collecting pictures from imageboards, developed using [JDA](https://github.com/DV8FromTheWorld/JDA).
It defaults to SFW pictures unless otherwise specified, using tags -n(for nonsafe searches) and -x(explicit searches only).

List of commands:
* !ping - "Pong!"
* !post (flag) \<interval\> \<tags\> - posts picture matching tags each interval
* !picture (flag) (tags) - posts once picture matching tags
* !cancel \<tags\> - cancels request in channel matching tags
* !cancel - cancels all requests in channel
* !list - lists all posting cycles in channel currently running
* !bestgirl (set \<tags\>) - posts a picture of the user's favorite character (1girl tag is included in searches, so no boys).
  
Currently gets its pictures from Konachan, Safebooru, Gelbooru and yande.re. 

Linux installation/update script can be found [here](https://gist.github.com/isakvik/94c277239430dba43f9844118f48a981).
