package be.thomaswinters.fixers;

import be.thomaswinters.chatbot.bots.experimental.ExperimentalWordCountingReplyGenerator;
import be.thomaswinters.newsminer.data.NewsArticle;
import be.thomaswinters.text.fixers.ISentenceFixer;
import be.thomaswinters.wordcounter.WordCounter;

import java.util.Comparator;
import java.util.Optional;

public class NewsInjectorFixer implements ISentenceFixer {
    private final HeadlineQuoteFixer headlineQuoteFixer;
    private final WordCounter corpusWordCounter;

    public NewsInjectorFixer(HeadlineQuoteFixer headlineQuoteFixer, WordCounter corpusWordCounter) {
        this.headlineQuoteFixer = headlineQuoteFixer;
        this.corpusWordCounter = corpusWordCounter;
    }

    @Override
    public String fix(String text) {
        boolean hasQuotes = headlineQuoteFixer.getQuoteExtractor().hasQuotes(text);
        boolean hasNames = AbstractNameReplacerFixer.hasNames(text);
        if (hasQuotes || hasNames) {
            Optional<NewsArticle> article = getBestArticle(text);

            if (article.isPresent()) {
                String newText = text;
                if (hasNames) {
                    ISentenceFixer nameFixer = new NameFromTextFixer(corpusWordCounter, article.get().getText(), 0);
                    newText = nameFixer.fix(text);
                }
                if (hasQuotes) {
                    newText = headlineQuoteFixer.replaceQuotesUsingArticleHeadline(newText, article.get());

                }

                System.out.println("\n===INSERTED NEWS===:\nNEWS: " + article.get().getTitle() + "\nFROM: " + text
                        + "\nTO:   " + newText + "\n\n");
                return newText;

            }
        }
        return text;

    }

    public Optional<NewsArticle> getBestArticle(String text) {
        return headlineQuoteFixer
                .getEligableArticles(text)
                .max(new ArticleComparer(headlineQuoteFixer.getQuoteExtractor().removeQuotes(text)));
    }

    private class ArticleComparer implements Comparator<NewsArticle> {
        private final WordCounter textWordCounter;

        public ArticleComparer(String text) {
            this.textWordCounter = new WordCounter(text);
        }

        public double getNumber(NewsArticle article) {
            return ExperimentalWordCountingReplyGenerator.getRelativeAmountOfSameWordsAs(new WordCounter(article.getText()), textWordCounter,
                    corpusWordCounter, ExperimentalWordCountingReplyGenerator.SECOND_MAPPER);

        }

        @Override
        public int compare(NewsArticle article1, NewsArticle article2) {
            return (int) Math.signum(getNumber(article1) - getNumber(article2));
        }

    }

}
