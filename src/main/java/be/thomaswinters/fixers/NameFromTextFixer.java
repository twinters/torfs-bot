package be.thomaswinters.fixers;

import be.thomaswinters.wordcounter.WordCounter;
import com.google.common.collect.Multiset;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

public class NameFromTextFixer extends AbstractNameReplacerFixer<String> {

    private final String text;

    public NameFromTextFixer(WordCounter corpusWordCounter, String text, int minAmountOfOccurrences) {
        super(corpusWordCounter, minAmountOfOccurrences);
        this.text = text;
    }

    @Override
    public Multiset<String> findNamesInOutsideTextOfTrend(String trend) throws Exception {
        return findNamesInOutsideText(Collections.singletonList(trend));
    }

    @Override
    public Optional<String> findBestTrendWords() throws Exception {
        return Optional.of(text);
    }

}
