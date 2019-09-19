# TorfsBot
Twitterbot automatically imitating Rik Torfs's tweets using interpolated Markov models and dynamic templates, live on Twitter on [@TorfsBot](https://twitter.com/TorfsBot).

## Set-up

### Training data
Due to copyright reasons, the tweets & columns used for training the bot can not be distributed.
However, you can acquire such data yourself or use completely different source material and put them in `\src\main\resources\torfstweets.txt` and `\src\main\resources\torfscolumns.txt`.
Both are required to just be new-line separated plain texts, without any mark-up or annotations.
To download tweets, you can use [this script](https://github.com/twinters/twitter-util/blob/master/src/main/java/be/thomaswinters/twitter/util/download/TweetDownloader.java).
For the columns, just pasting the text into a the `torfscolumns.txt` will do the trick.

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

You will need to provide the following values in your environment (which can easily be set when running the code from an editor like [IntellJ](https://www.jetbrains.com/idea/)):

```.env
oauth.accessToken=
oauth.accessTokenSecret=
oauth.consumerKey=
oauth.consumerSecret=
```


## Running

Run the bot by running `be.thomaswinters.twitter.torfsbot.TorfsBot` and giving as argument `-debug`.

## Citation
To cite TorfsBot in an academic paper, the following BibTex entry to the [paper](http://esslli2019.folli.info/wp-content/uploads/2019/08/tentative_proceedings.pdf) (page 181) can be used:

```
@inproceedings{winters2019torfsbot,
  author = {Winters, T},
  booktitle = {31st European Summer School in Logic, Language and Information Student Session Proceedings},
  month = {Aug},
  pages = {181-189},
  organization = {Riga, Latvia},
  publisher = {ESSLLI},
  title = {Generating Philosophical Statements using Interpolated Markov Models and Dynamic Templates},
  year = {2019},
  startyear = {2019},
  startmonth = {Aug},
  startday = {5},
  finishyear = {2019},
  finishmonth = {Aug},
  finishday = {16},
  language = {English},
  conference = {European Summer School in Logic, Language and Information},
  day = {5},
  publicationstatus = {online-published},
}
```


Or as:

```
Winters T. (2019) Imitating Philosophical Statements using Stacked Markov Chains and Dynamic Templates, In: 31st European Summer School in Logic, Language and Information (ESSLLI2019): Student Session, University of Latvia
```
