# TorfsBot
Twitterbot automatically imitating Rik Torfs's tweets using interpolated Markov models and dynamic templates, live on Twitter on [@TorfsBot](https://twitter.com/TorfsBot).

## Set-up

### Training data
Due to copyright reasons, I can't distribute the tweets & columns I used for training the bot, so you will have to acquire them yourself (or use completely different source material, should work fine), and put them in `\src\main\resources\torfstweets.txt` and `\src\main\resources\torfscolumns.txt`

### Dependencies

The following repositories need to be cloned in folders next to this repository, as they are dependencies of this project:
- [generator-util](https://github.com/twinters/generator-util)
- [language-util](https://github.com/twinters/language-util)
- [chatbot-util](https://github.com/twinters/chatbot-util)
- [text-util](https://github.com/twinters/text-util)
- [twitter-util](https://github.com/twinters/twitter-util)
- [news-scraper](https://github.com/twinters/news-scraper)
- [markov](https://github.com/twinters/markov)

### Twitter connection

While you can run the bots without Twitter, running the main class will require a Twitter connection set-up through the environment to run from.
For more information on how to run these type of bots, see [twitter-util](https://github.com/twinters/twitter-util).

You will need to provide the following values:

```.env
oauth.accessToken=
oauth.accessTokenSecret=
oauth.consumerKey=
oauth.consumerSecret=
```


## Running

Run the bot by running `be.thomaswinters.twitter.torfsbot.TorfsBot` and giving as argument `-debug`.
