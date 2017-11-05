# waifubot
Discord bot for collecting pictures from imageboards, developed using [JDA](https://github.com/DV8FromTheWorld/JDA).
It defaults to SFW pictures unless otherwise specified(using rating: tags).

List of commands:
* !ping - "Pong!"
* !post <interval> <tags> - posts picture matching tags each interval
* !picture <tags> - posts once picture matching tags
* !cancel <tags> - cancels request in channel matching tags
* !cancel - cancels all requests in channel
* !list - lists all posting cycles in channel currently running
* !bestgirl (set <tags>) - posts a picture of the user's favorite character (1girl tag implicitly included, so no boys).
  
Currently gets its pictures from Konachan, Safebooru and Gelbooru. 
