package be.thomaswinters.fixers;

import be.thomaswinters.replacement.Replacer;
import be.thomaswinters.replacement.Replacers;
import be.thomaswinters.sentence.SentenceUtil;
import be.thomaswinters.text.fixers.ISentenceFixer;
import be.thomaswinters.wordcounter.WordCounter;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import com.google.common.collect.Multisets;
import twitter4j.TwitterException;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AbstractNameReplacerFixer<E> implements ISentenceFixer {

    private final WordCounter corpusWordCounter;
    private final int minNumberOccurrences;

    public AbstractNameReplacerFixer(WordCounter corpusWordCounter, int minNumberOccurrences) {
        this.corpusWordCounter = corpusWordCounter;
        this.minNumberOccurrences = minNumberOccurrences;
    }

    public static Collection<String> getMentions(String text) {
        return Stream.of(text.split(" "))
                .filter(e -> e.startsWith("@"))
                .map(SentenceUtil::removePunctuations)
                .collect(Collectors.toList());
    }

    public static boolean hasNames(String text) {
        return !SentenceUtil.findNames(text).isEmpty();
    }

    @Override
    public String fix(String tweet) {
        // make the tweets about people that are trending right now if they
        // mention someone
        try {
            return replaceNamesWithTrendingNames(tweet);
        } catch (Exception e) {
            return tweet;
        }

    }

    public abstract Multiset<String> findNamesInOutsideTextOfTrend(E trend) throws Exception;

    public Multiset<String> findNamesInOutsideText(Collection<String> lines) throws TwitterException {
        Multiset<String> names = HashMultiset.create();
        lines.stream()
                .flatMap(e -> SentenceUtil.findNames(e).stream())
                .forEach(names::add);

        Multiset<String> ordenedNames = Multisets.copyHighestCountFirst(names);
        Multiset<String> filteredNames = HashMultiset.create();

        int averageCorpusWordCount = getCorpusWordCounter().getAverageWordCount();

        for (Entry<String> ordened : ordenedNames.entrySet()) {
            String name = ordened.getElement();
            int count = ordened.getCount();
            names.remove(name, count);

            // Is not a longer version of whatever came before (more often)
            if (filteredNames.entrySet().stream().noneMatch(f -> name.contains(f.getElement())) &&
                    // And not common in corpus
                    SentenceUtil
                            .splitOnSpaces(name)
                            .anyMatch(e -> getCorpusWordCounter().getCount(e) < averageCorpusWordCount)) {
                filteredNames.add(name, count);
            }
        }

        return filteredNames;
    }

    private String replaceNamesWithTrendingNames(String text) {

        // Find all names in tweet
        Set<String> namesInTweet = new HashSet<>(SentenceUtil.findNames(text));

        int maxOccurrencesOfUnusualNames = getCorpusWordCounter().getAverageWordCount();

        // If there are names, change them if any of them is an "unusual" name
        // (not often appearing)
        if (!namesInTweet.isEmpty() && hasAnyUnusualNames(namesInTweet, maxOccurrencesOfUnusualNames)) {

            Optional<E> bestTrend;
            try {
                bestTrend = findBestTrendWords();
            } catch (Exception e) {
                return text;
            }

            if (bestTrend.isPresent()) {
                Multiset<String> relatedNames;

                // Find most common related names
                try {
                    Multiset<String> names = findNamesInOutsideTextOfTrend(bestTrend.get());
                    relatedNames = Multisets.copyHighestCountFirst(names);
                } catch (Exception e) {
                    e.printStackTrace();
                    return text;
                }

                System.out.println("I'm going to replace " + namesInTweet + " with " + relatedNames);

                return replaceNames(text, namesInTweet, relatedNames);

            }

        }
        return text;

    }

    public abstract Optional<E> findBestTrendWords() throws Exception;

    private boolean hasAnyUnusualNames(Set<String> namesInTweet, int maxOccurrencesOfUnusualNames) {
        return namesInTweet
                .stream()
                .flatMap(e -> Stream.of(e.split(" ")))
                .anyMatch(e -> getCorpusWordCounter().getCount(e) < maxOccurrencesOfUnusualNames);
    }

    private String replaceNames(String text, Set<String> namesInTweet, Multiset<String> relatedNames) {
        List<Replacer> replacersList = new ArrayList<>();
        Iterator<Entry<String>> relatedNamesIterator = relatedNames.entrySet().iterator();
        Iterator<String> namesInTweetIterator = namesInTweet.iterator();

        while (namesInTweetIterator.hasNext()) {

            if (relatedNamesIterator.hasNext()) {
                Entry<String> next = relatedNamesIterator.next();
                // Has to have appeared multiple times, otherwise: restart!
                if (next.getCount() < minNumberOccurrences) {
                    relatedNamesIterator = relatedNames.entrySet().iterator();
                    continue;
                }
                replacersList.add(new Replacer(namesInTweetIterator.next(), next.getElement(), false, false));
            } else {
                break;
            }

        }

        Replacers replacers = new Replacers(replacersList);

        String replaced = replacers.replace(text);

        System.out.println("\n" + text + "\n" + replaced + "\n\n");
        return replaced;
    }

    public WordCounter getCorpusWordCounter() {
        return corpusWordCounter;
    }

}