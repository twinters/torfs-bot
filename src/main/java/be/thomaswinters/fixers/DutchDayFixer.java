package be.thomaswinters.fixers;

import be.thomaswinters.ner.DutchDateUtil;
import be.thomaswinters.text.fixers.ISentenceFixer;

import java.time.LocalDateTime;
import java.util.Random;

public class DutchDayFixer implements ISentenceFixer {
    private final Random random = new Random();
    private final int maxDaysInTheFuture;

    public DutchDayFixer(int maxDaysInTheFuture) {
        this.maxDaysInTheFuture = maxDaysInTheFuture;
    }

    @Override
    public String fix(String text) {
        // Make the tweets about the upcoming future
        return DutchDateUtil.replaceAllDates(text, LocalDateTime.now().plusDays(random.nextInt(maxDaysInTheFuture)));
    }

}
