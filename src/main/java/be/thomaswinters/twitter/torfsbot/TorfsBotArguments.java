package be.thomaswinters.twitter.torfsbot;

import com.beust.jcommander.Parameter;

public class TorfsBotArguments {

    @Parameter(names = "-markov", description = "Markov mode")
    private boolean markovMode = false;

    @Parameter(names = "-substitution", description = "Substitution mode")
    private boolean substitionMode = false;

    public boolean isMarkovMode() {
        return markovMode;
    }

    public boolean isSubstitionMode() {
        return substitionMode;
    }


}
