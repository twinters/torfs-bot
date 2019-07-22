package be.thomaswinters.fixers;

import be.thomaswinters.fixers.TrendingNameFixer.Trend;
import be.thomaswinters.sentence.SentenceUtil;
import be.thomaswinters.twitter.tweetsfetcher.SearchTweetsFetcher;
import be.thomaswinters.twitter.util.analysis.TwitterTrendsFinder;
import be.thomaswinters.wordcounter.WordCounter;
import com.google.common.collect.Multiset;
import twitter4j.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Deprecated
public class TrendingNameFixer extends AbstractNameReplacerFixer<Trend> {
    public static final int BELGIUM = 23424757;
    private final Twitter twitter = TwitterFactory.getSingleton();
    private double BASE_TRENDING_DECAY = 0.93d;

    public TrendingNameFixer(WordCounter corpusWordCounter, int minAmountOfOccurrencesInAllTweets) {
        super(corpusWordCounter, minAmountOfOccurrencesInAllTweets);
    }

    public TrendingNameFixer(WordCounter corpusWordCounter) {
        this(corpusWordCounter, 2);
    }

    @Override
    public Optional<Trend> findBestTrendWords() throws TwitterException {
        final List<String> trending = new ArrayList<String>();
        TwitterTrendsFinder
                .getCurrentTrends(BELGIUM).stream()
                .filter(SentenceUtil::isCapitalizedSentence)
                .forEach(trending::add);

        if (trending.isEmpty()) {
            return Optional.empty();
        }

        String bestTrend = trending.get(0);
        Collection<String> bestTweets =
                new SearchTweetsFetcher(twitter, Optional.of("nl"), Query.POPULAR, bestTrend, true)
                        .retrieve()
                        .map(Status::getText)
                        .collect(Collectors.toSet());
        double bestWcAmount = new WordCounter(bestTweets).getAmountOfSameWordsAs(this.getCorpusWordCounter());
        double currentTrendingDecay = 1;
        for (int i = 1; i < trending.size(); i++) {
            String potentialTrend = trending.get(i);
            Collection<String> potentialTweets =
                    new SearchTweetsFetcher(twitter, Optional.of("nl"), Query.POPULAR, potentialTrend, true)
                            .retrieve()
                            .map(Status::getText)
                            .collect(Collectors.toSet());
            int potentialWc = (new WordCounter(potentialTweets)).getAmountOfSameWordsAs(this.getCorpusWordCounter());

            // Check whose best
            currentTrendingDecay = currentTrendingDecay * BASE_TRENDING_DECAY;
            if (currentTrendingDecay * potentialWc > bestWcAmount) {
                bestTrend = potentialTrend;
                bestWcAmount = currentTrendingDecay * potentialWc;
                bestTweets = potentialTweets;
            }
        }

        System.out.println("The best trend is " + bestTrend);
        return Optional.of(new Trend(bestTrend, bestTweets));
    }

    @Override
    public Multiset<String> findNamesInOutsideTextOfTrend(Trend trend) throws TwitterException {
        Multiset<String> names = findNamesInOutsideText(trend.getTweets());
        names.add(trend.getTrend(), 10000);
        return names;
    }

    public static class Trend {
        private final String trend;
        private final Collection<String> tweets;

        public Trend(String trend, Collection<String> tweets) {
            this.trend = trend;
            this.tweets = tweets;
        }

        public String getTrend() {
            return trend;
        }

        public Collection<String> getTweets() {
            return tweets;
        }
    }

    /*-********************************************-*
     *  GETTERS
     *-********************************************-*/

    /*-********************************************-*/

}
