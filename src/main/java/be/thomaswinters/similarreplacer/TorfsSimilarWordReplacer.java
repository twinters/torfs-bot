package be.thomaswinters.similarreplacer;

import be.thomaswinters.generator.generators.IGenerator;
import be.thomaswinters.markov.model.data.bags.Bag;
import be.thomaswinters.random.Picker;
import be.thomaswinters.replacement.Replacer;
import be.thomaswinters.replacement.Replacers;
import be.thomaswinters.wordcounter.WordCounter;
import org.languagetool.AnalyzedTokenReadings;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class TorfsSimilarWordReplacer extends SimilarWordReplacer implements IGenerator<String> {

    private final List<String> baseTexts;
    private final WordCounter wc;

    public TorfsSimilarWordReplacer(List<String> baseTexts, WordCounter wc) {
        this.baseTexts = baseTexts;
        this.wc = wc;
    }


    private static final double MIN_QUARTILE_FOR_REPLACEMENT = 0.62;

    @Override
    public Optional<String> generate() {
        String randomTweet = baseTexts.get(RANDOM.nextInt(baseTexts.size()));
        List<Replacer> replacers = calculatePossibleReplacements(randomTweet);
        List<Replacer> chosenReplacers = pickReplacers(randomTweet.length() / 25, wc.getQuartileCount(MIN_QUARTILE_FOR_REPLACEMENT), replacers);

        String result = new Replacers(chosenReplacers).replace(randomTweet);

//        System.out.println("\n\nTOTAL:\nFrom: " + randomTweet + "\nTo:   " + result);
        return Optional.of(result);
    }

    private List<Replacer> pickReplacers(int minAmount, int quartileCount, Collection<Replacer> replacers) {
        List<Replacer> sorted = new ArrayList<>(replacers);
        sorted.sort(new ReplacerQuartileComparator());

        List<Replacer> result = new ArrayList<>();

        for (Replacer replacer : sorted) {
            if (result.size() < minAmount) {
//                System.out.println("Adding to min amount:" + replacer + ", " + wc.getCount(replacer.getWord()) + " / "
//                        + quartileCount);
                result.add(replacer);
            } else if (wc.getCount(replacer.getWord()) < quartileCount) {
//                System.out.println("Adding quartile count:" + replacer + ", " + wc.getCount(replacer.getWord()) + " / "
//                        + quartileCount);
                result.add(replacer);
            } else {
//                System.out.println("Not adding: " + replacer + ", " + wc.getCount(replacer.getWord()) + " / "
//                        + quartileCount);
            }
        }
        return result;

    }

    private class ClosestWordCountComparator implements Comparator<String> {
        private final String baseWord;

        public ClosestWordCountComparator(String baseWord) {
            this.baseWord = baseWord;
        }

        @Override
        public int compare(String word1, String word2) {
            return Math.abs(wc.getCount(word1) - wc.getCount(baseWord))
                    - Math.abs(wc.getCount(word2) - wc.getCount(baseWord));
        }
    }

    private class ReplacerQuartileComparator implements Comparator<Replacer> {
        @Override
        public int compare(Replacer r1, Replacer r2) {
            return wc.getCount(r1.getWord()) - wc.getCount(r2.getWord());
        }
    }

    @Override
    public String pickReplacement(String replacement, Bag<String> bag) {
        Comparator<String> comp = new ClosestWordCountComparator(replacement);

        return bag.toMultiset().stream().min(comp).get();
    }

    public static TorfsSimilarWordReplacer create(List<String> tweets, List<String> columns) throws IOException {
        List<String> allLines = new ArrayList<String>(tweets);
        allLines.addAll(columns);

        WordCounter wc = new WordCounter(allLines);

        List<String> learningColumnLines = Picker.pickConsequtiveIndices(2, columns.size()).stream()
                .map(columns::get).collect(Collectors.toList());
        List<String> learningTweetLines = Picker.pickRandomUniqueIndices(1, tweets.size()).stream()
                .map(tweets::get).collect(Collectors.toList());

        TorfsSimilarWordReplacer wordReplacer = new TorfsSimilarWordReplacer(tweets, wc);
        wordReplacer.process(learningColumnLines);
        wordReplacer.process(learningTweetLines);

        return wordReplacer;
    }

    @Override
    public Optional<Replacer> createReplacer(AnalyzedTokenReadings token, Bag<String> replacePossibilities) {
        // Null check
        if (token == null || token.getToken().length() == 0) {
            return Optional.empty();
        }
        // Check if name:
        if (getTags(token).stream().allMatch(tag -> tag.startsWith("PN"))) {
            return Optional.empty();
        }

        return super.createReplacer(token, replacePossibilities);
    }

}
