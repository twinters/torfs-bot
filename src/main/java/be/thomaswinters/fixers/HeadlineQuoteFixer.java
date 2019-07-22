package be.thomaswinters.fixers;

import be.thomaswinters.newsminer.INewsRetriever;
import be.thomaswinters.newsminer.data.NewsArticle;
import be.thomaswinters.newsminer.dutch.VrtNwsRetriever;
import be.thomaswinters.random.Picker;
import be.thomaswinters.text.extractors.QuoteExtractor;
import be.thomaswinters.text.fixers.ISentenceFixer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HeadlineQuoteFixer implements ISentenceFixer {

    private static final INewsRetriever NEWS_MINER = new VrtNwsRetriever();
    private final static Comparator<String> LENGTH_SORTER = Comparator.comparingInt(String::length);
    private final QuoteExtractor quoteExtractor;
    private final int maxTextLength;

    public HeadlineQuoteFixer(int minAmountOfWords, int maxTextLength) {
        this.quoteExtractor = new QuoteExtractor(minAmountOfWords);
        this.maxTextLength = maxTextLength;
    }

    public static void main(String[] args) {
        System.out.println(new HeadlineQuoteFixer(4, 140).fix(
                "Neen. \"Er zijn zo al meer dan 80 Nederlanders voor de klas in Antwerpen.\" Wordt het Antwerps bedreigd als onderwijstaal?"));
    }

    public QuoteExtractor getQuoteExtractor() {
        return quoteExtractor;
    }

    @Override
    public String fix(String text) {
        if (quoteExtractor.hasQuotes(text)) {
            List<String> textMatches = quoteExtractor.getAllMatches(text);

            int maxQuoteLength = calculateMaxQuoteLength(text);
            Optional<NewsArticle> article = pickRandomArticle(maxQuoteLength, textMatches.size());

            if (article.isPresent()) {
                return replaceQuotesUsingArticleHeadline(text, article.get());
            }

        }
        return text;
    }

    public String replaceQuotesUsingArticleHeadline(String text, NewsArticle article) {
        List<String> textMatches = quoteExtractor.getAllMatches(text);
        List<String> articleMatches = quoteExtractor.getAllMatches(article.getTitle());

        for (int i = 0; i < textMatches.size(); i++) {
            text = text.replace(textMatches.get(i), articleMatches.get(i));
        }
        return text;
    }

    public int calculateMaxQuoteLength(String text) {
        return maxTextLength - quoteExtractor.removeQuotes(text).length();
    }

    public Stream<NewsArticle> getEligableArticles(String text) {
        return getEligableArticles(calculateMaxQuoteLength(text), quoteExtractor.getAllMatches(text).size());
    }

    public Stream<NewsArticle> getEligableArticles(int maxTotalQuoteLength, int amountOfMatches) {
        return getArticlesWithQuotes().stream()
                // At least as many quotes
                .filter(e -> quoteExtractor.getAllMatches(e.getTitle()).size() >= amountOfMatches)
                // Minimum isn't higher than max length
                .filter(e -> getSmallestTotalQuoteLength(quoteExtractor.getAllMatches(e.getTitle()),
                        amountOfMatches) < maxTotalQuoteLength);
    }

    public Optional<NewsArticle> pickRandomArticle(int maxTotalQuoteLength, int amountOfMatches) {
        return Picker.pickOptional(
                getEligableArticles(maxTotalQuoteLength, amountOfMatches)
                        .collect(Collectors.toList()));
    }

    public int getSmallestTotalQuoteLength(List<String> quotes, int numberOfQuotes) {
        return quotes
                .stream()
                .sorted(LENGTH_SORTER)
                .limit(numberOfQuotes)
                .mapToInt(String::length)
                .sum();
    }

    public List<NewsArticle> getArticlesWithQuotes() {
        try {
            return NEWS_MINER
                    .retrieveFullArticles()
                    .stream()
                    .filter(e -> quoteExtractor.hasQuotes(e.getTitle()))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }


}
