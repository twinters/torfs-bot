package be.thomaswinters.twitter.torfsbot;

import be.thomaswinters.chatbot.bots.WordCounterBasedReplier;
import be.thomaswinters.chatbot.bots.experimental.ExperimentalWordCountingReplyGenerator;
import be.thomaswinters.chatbot.data.IChatMessage;
import be.thomaswinters.chatbot.util.ConversationCollector;
import be.thomaswinters.fixers.DutchDayFixer;
import be.thomaswinters.fixers.HeadlineQuoteFixer;
import be.thomaswinters.fixers.NewsInjectorFixer;
import be.thomaswinters.generator.generators.IGenerator;
import be.thomaswinters.generator.selection.RouletteWheelSelection;
import be.thomaswinters.markov.model.builder.MarkovAdditiveGeneratorBuilder;
import be.thomaswinters.markov.model.builder.MarkovGeneratorBuilder;
import be.thomaswinters.markov.model.data.Weighted;
import be.thomaswinters.markov.model.data.seed.NSeedCache;
import be.thomaswinters.markov.model.data.seed.NSeedCreator;
import be.thomaswinters.sentencemarkov.MarkovSentenceGenerator;
import be.thomaswinters.sentencemarkov.NSeedSentenceTransformer;
import be.thomaswinters.sentencemarkov.learner.MarkovSentenceLearner;
import be.thomaswinters.similarreplacer.TorfsSimilarWordReplacer;
import be.thomaswinters.text.checkers.OriginalityTextChecker;
import be.thomaswinters.text.fixers.BracketFixer;
import be.thomaswinters.text.fixers.SentenceShortener;
import be.thomaswinters.twitter.bot.GeneratorTwitterBot;
import be.thomaswinters.twitter.bot.TwitterBot;
import be.thomaswinters.twitter.bot.arguments.TwitterBotArguments;
import be.thomaswinters.twitter.bot.executor.TwitterBotExecutor;
import be.thomaswinters.twitter.tweetsfetcher.TimelineTweetsFetcher;
import be.thomaswinters.twitter.tweetsfetcher.filter.RandomFilter;
import be.thomaswinters.twitter.util.TwitterLogin;
import be.thomaswinters.twitter.util.TwitterUtil;
import be.thomaswinters.util.DataLoader;
import be.thomaswinters.wordcounter.WordCounter;
import com.beust.jcommander.JCommander;
import twitter4j.Twitter;
import twitter4j.TwitterException;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class TorfsBot {

    private final boolean ADD_SENTENCE_STARTS = true;
    private final List<String> torfsTweets = DataLoader.readLines("torfstweets.txt");
    private final List<String> torfsColumns = DataLoader.readLines("torfscolumns.txt");
    private final List<Weighted<List<String>>> corpora = Arrays.asList(
            new Weighted<>(torfsTweets, 10),
            new Weighted<>(torfsColumns, 1));

    /*-********************************************-*
     *  STATIC METHODS
     *-********************************************-*/

    private final OriginalityTextChecker originalityChecker;
    private final WordCounter wordCounter;

    public TorfsBot() throws IOException {
        WordCounter.Builder b = WordCounter.builder();
        corpora.forEach(weightedList -> b.addWeighted(weightedList.getElement(), weightedList.getWeight()));
        this.wordCounter = b.build();
        this.originalityChecker = new OriginalityTextChecker(
                corpora.stream()
                        .flatMap(corpus -> corpus.getElement().stream())
        );


    }

    /*-********************************************-*
     *  MAIN
     *-********************************************-*/
    public static void main(String[] args) throws TwitterException, InterruptedException, IOException {
        TwitterBotArguments twitterBotArgument = new TwitterBotArguments();
        TorfsBotArguments torfsBotArguments = new TorfsBotArguments();
        JCommander.newBuilder().addObject(twitterBotArgument).addObject(torfsBotArguments).build().parse(args);

        new TwitterBotExecutor(new TorfsBot().create(torfsBotArguments))
                .run(twitterBotArgument);
    }

    /*-********************************************-*
     *  Markov generator
     *-********************************************-*/

    private List<String> readLines(URL url) {
        try {
            return DataLoader.readLines(url);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private IGenerator<String> createSubstitutionBot() throws IOException {
        return TorfsSimilarWordReplacer.create(torfsTweets, torfsColumns);
    }

    private IGenerator<String> createMarkovGenerator() {
        MarkovSentenceLearner learner = createLearner();
        learner.addWeightedCorpora(corpora);
        MarkovSentenceGenerator generator = new MarkovSentenceGenerator(learner.build());
        return () -> Optional.of(generator.generateText());

    }

    /*-********************************************-*/

    /*-********************************************-*
     *  Creators
     *-********************************************-*/

    /**
     * Specifies all the learners that will be deployed when learning the
     * words
     *
     * @return
     */
    private MarkovSentenceLearner createLearner() {
        // Creator
        Optional<NSeedCache<String, String>> creator = Optional.of(
                new NSeedCache<>(new NSeedCreator<>(new NSeedSentenceTransformer())));
        // Optional<NSeedCache<String, String>> posCreator = Optional
        // .of(new NSeedCache<String, String>(new NSeedCreator<String,
        // String>(new PosMarkovTransformer(new Dutch()))));

        MarkovAdditiveGeneratorBuilder<String, ?> builder = new MarkovAdditiveGeneratorBuilder<>(

                Arrays.asList(

                        // Word normalisation
                        // Last 5 words
                        // new MarkovGeneratorBuilder<>(5, 0, 400, creator),
                        // Last 4 words
                        new MarkovGeneratorBuilder<>(4, 0, 400, creator),
                        // Last 3 words
                        new MarkovGeneratorBuilder<>(3, 0, 200, creator),
                        // Last 2
                        new MarkovGeneratorBuilder<>(2, 0, 1, creator)

                        // Word normalisation
                        // new MarkovGeneratorBuilder<>(4, 0, 20, posCreator),
                        // new MarkovGeneratorBuilder<>(3, 0, 10, posCreator)

                        //

                        // End
                ));

        return new MarkovSentenceLearner(ADD_SENTENCE_STARTS, builder);
    }

    private WordCounterBasedReplier createReplyingBot(Twitter twitter, IGenerator<String> generator) throws TwitterException {
        return new ExperimentalWordCountingReplyGenerator(
                applyTorfsBotFixersAndCheckers(generator, true), wordCounter,
                1200,
                new ConversationCollector(
                        twitter.getScreenName(), 10,
                        ConversationCollector.MAP_TO_ONE,
                        this::mapConversationWeight),
                ExperimentalWordCountingReplyGenerator.SECOND_MAPPER);
    }

    private int mapConversationWeight(IChatMessage iChatMessage, int chatIdx) {
        return Integer.max(1, 4 * (6 - chatIdx));
    }

    /*-********************************************-*/

    private IGenerator<String> applyTorfsBotFixersAndCheckers(IGenerator<String> generator, boolean replyingBot) {
        IGenerator<String> result =
                generator
                        .map(new SentenceShortener(200))
                        .map(new BracketFixer("\"", "\""))
                        .map(new DutchDayFixer(4));


        // If not a replying chatbot:
        if (!replyingBot) {
            result = result.map(new NewsInjectorFixer(
                    new HeadlineQuoteFixer(4, TwitterUtil.MAX_TWEET_LENGTH),
                    wordCounter));
        }

        return result
                .filter(TwitterUtil::hasValidLength)
                .filter(originalityChecker);
    }

    /*-********************************************-*/

    public TwitterBot create(TorfsBotArguments arguments) throws IOException, InterruptedException, TwitterException {

        Twitter twitter = TwitterLogin.getTwitterFromEnvironment("oauth.");

        IGenerator<String> markovGenerator = createMarkovGenerator();

        boolean markovMode = arguments.isMarkovMode();
        boolean substitutionMode = arguments.isSubstitionMode();

        IGenerator<String> generatorBot;
        if (markovMode && !substitutionMode) {
            generatorBot = createMarkovGenerator();
        } else if (substitutionMode && !markovMode) {
            generatorBot = createSubstitutionBot();
        } else {
            IGenerator<String> substitutionGenerator = createSubstitutionBot();
            generatorBot = () -> {
                if (Math.random() < 0.4) {
                    return markovGenerator.generate();
                } else {
                    return substitutionGenerator.generate();
                }
            };
        }

        return new GeneratorTwitterBot(
                twitter,
                applyTorfsBotFixersAndCheckers(generatorBot, false)
                        .select(3,
                                new RouletteWheelSelection<>(
                                        text -> Math.pow(TwitterUtil.MAX_TWEET_LENGTH / ((double) text.length()), 2))
                        ),
                createReplyingBot(twitter, applyTorfsBotFixersAndCheckers(markovGenerator, true)),
                TwitterBot.MENTIONS_RETRIEVER.apply(twitter)
                        .filterOutOwnTweets(twitter)
                        .combineWith(
                                new TimelineTweetsFetcher(twitter)
                                        .filter(new RandomFilter(twitter, 1, 40))
                        )
        );

    }
    /*-********************************************-*/

}
