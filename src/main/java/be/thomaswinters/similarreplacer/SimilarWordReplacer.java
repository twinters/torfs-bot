package be.thomaswinters.similarreplacer;

import be.thomaswinters.LambdaExceptionUtil;
import be.thomaswinters.markov.model.data.bags.Bag;
import be.thomaswinters.markov.model.data.bags.WriteableBag;
import be.thomaswinters.markov.model.data.bags.impl.ExclusionBag;
import be.thomaswinters.markov.model.data.bags.impl.MutableBag;
import be.thomaswinters.random.Picker;
import be.thomaswinters.replacement.Replacer;
import be.thomaswinters.replacement.Replacers;
import org.languagetool.AnalyzedSentence;
import org.languagetool.AnalyzedTokenReadings;
import org.languagetool.JLanguageTool;
import org.languagetool.language.Dutch;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class SimilarWordReplacer {
    /*-********************************************-*
     *  STATIC TOOLS
     *-********************************************-*/
    private static final JLanguageTool langTool = new JLanguageTool(new Dutch());
    protected static final Random RANDOM = new Random();

    /*-********************************************-*/

    /*-********************************************-*
     *  INSTANCE VARIABLES
     *-********************************************-*/

    private Map<Set<String>, WriteableBag<String>> mappings = new HashMap<>();

    /*-********************************************-*/

    /*-********************************************-*
     *  TAGS & TOKEN FILTERING
     *-********************************************-*/

    protected Set<String> getTags(AnalyzedTokenReadings token) {
        return token.getReadings().stream().filter(e -> !e.hasNoTag()).map(e -> e.getPOSTag())
                .filter(e -> e != null && !e.equals("SENT_END") && !e.equals("PARA_END")).collect(Collectors.toSet());
    }

    private static final Set<String> REPLACEMENT_TOKEN_BLACKLIST = new HashSet<>(Arrays.asList(
            // Lidwoorden
            "de", "het", "een",
            // Algemene onderwerpen
            "ik", "jij", "je", "u", "wij", "we", "jullie", "hij", "zij", "ze",
            // Algemene persoonlijke voornaamwoorden
            "hen", "hem", "haar", "mijn", "uw", "jouw", "onze", "ons",
            // Algemene werkwoorden
            "ben", "bent", "is", "was", "waren", "geweest", "heb", "hebt", "heeft", "hebben", "gehad", "word", "wordt",
            "worden", "geworden", "werd", "werden", "laat", "laten", "liet", "lieten", "gelaten", "ga", "gaat", "gaan",
            "gegaan", "ging", "gingen", "moet", "moeten", "moest", "moesten", "gemoeten", "mag", "mogen", "mocht",
            "mochten", "gemogen", "zal", "zullen", "zult", "zou", "zouden", "kan", "kunnen", "gekunt", "gekunnen",
            "hoef", "hoeft", "hoeven", "hoefde", "hoefden", "gehoeven",
            // Veelgebruikte woorden
            "niet", "iets", "dan", "voort", "erna", "welke", "maar", "van", "voor", "met", "binnenkort", "in", "en",
            "teveel", "om", "alles", "elke", "al", "echt", "waar", "waarom", "hoe", "o.a.", "beetje", "enkel", "goed",
            "best", "werkende", "meer", "voor", "zit", "echt", "uit", "even", "wel"));
    private static final Set<String> REPLACEMENT_TAG_BLACKLIST = new HashSet<>(
            Arrays.asList("AVwaar", "AVwr", "DTh", "DTd", "DTe", "DTp", "PRte", "PRnaar", "PRvan", "PN2", "PRVoor",
                    "PRmet", "PRop", "PRin", "PRom", "PRaan", "AVdr", "CJo"));

    private List<AnalyzedTokenReadings> filterTokens(List<AnalyzedTokenReadings> tokens) {
        return tokens.stream().filter(e -> e.getToken().trim().length() > 0).filter(e -> !e.getReadings().isEmpty())
                .filter(token -> !getTags(token).isEmpty())
                .filter(token -> getTags(token).stream().allMatch(tag -> !REPLACEMENT_TAG_BLACKLIST.contains(tag)))
                .filter(token -> !REPLACEMENT_TOKEN_BLACKLIST.contains(token.getToken().toLowerCase()))
                .collect(Collectors.toList());
    }

    /*-********************************************-*/

    /*-********************************************-*
     *  PROCESSING KNOWLEDGE INPUT
     *-********************************************-*/
    public void process(String line) throws IOException {
        List<AnalyzedSentence> answers = langTool.analyzeText(line);

        for (AnalyzedSentence analyzedSentence : answers) {
            List<AnalyzedTokenReadings> tokens = filterTokens(Arrays.asList(analyzedSentence.getTokens()));
            for (AnalyzedTokenReadings token : tokens) {
                if (token.getToken().trim().length() > 0) {
                    Set<String> tags = getTags(token);

                    // Add if valid
                    if (tags != null && tags.size() > 0) {
                        if (!mappings.containsKey(tags)) {
                            mappings.put(tags, new MutableBag<String>());
                        }
                        mappings.get(tags).add(token.getToken());
                    }
                }
            }
        }

    }

    public void process(List<String> lines) {
        lines.forEach(LambdaExceptionUtil.rethrowConsumer(this::process));
    }
    /*-********************************************-*/

    /*-********************************************-*
     *  REPLACEABLE CALCULATION
     *-********************************************-*/
    public int getReplaceableSize(Set<String> tags) {
        if (!mappings.containsKey(tags)) {
            return 0;
        }
        return mappings.get(tags).size();
    }

    public List<AnalyzedTokenReadings> getReplaceableTokens(String line) {
        List<AnalyzedSentence> answers;
        try {
            answers = langTool.analyzeText(line);
        } catch (IOException e1) {
            throw new RuntimeException(e1);
        }
        List<AnalyzedTokenReadings> tokens = new ArrayList<>();
        for (AnalyzedSentence analyzedSentence : answers) {
            tokens.addAll(Arrays.asList(analyzedSentence.getTokens()));

        }

        tokens = filterTokens(tokens).stream().filter(e -> getReplaceableSize(getTags(e)) > 1)
                .collect(Collectors.toList());

        return tokens;

    }

    public Optional<Replacer> createReplacer(AnalyzedTokenReadings token, Bag<String> replacePossibilities) {
        // Check if there is another possibility than to replace with itself
        if (replacePossibilities.isEmpty() || (replacePossibilities.getAmountOfUniqueElements() == 1
                && replacePossibilities.get(0).toLowerCase().equals(token.getToken().toLowerCase()))) {
            return Optional.empty();
        }
        Bag<String> bag = new ExclusionBag<String>(replacePossibilities, Arrays.asList(token.getToken()));
        String replacement = pickReplacement(token.getToken(), bag);

        Replacer replacer = new Replacer(token.getToken(), replacement, false, true);

        return Optional.of(replacer);
    }

    public String pickReplacement(String replacement, Bag<String> bag) {
        return bag.get(RANDOM.nextInt(bag.getAmountOfElements()));
    }

    public List<Replacer> calculatePossibleReplacements(String line) {
        Set<AnalyzedTokenReadings> tokens = new LinkedHashSet<>(getReplaceableTokens(line));
        List<Replacer> replacers = new ArrayList<>();
        for (AnalyzedTokenReadings token : tokens) {
            Bag<String> replacePossibilities = mappings.get(getTags(token));

            createReplacer(token, replacePossibilities).ifPresent(replacer -> replacers.add(replacer));

//			System.out.println((createReplacer(token, replacePossibilities).isPresent()) + " mapping of " + token
//					+ "\n=>" + replacePossibilities);
        }
        return replacers;
    }

    /*-********************************************-*/

    public String replaceSomething(String line, int amountOfReplacements) {
        List<Replacer> replacers = calculatePossibleReplacements(line);
        Set<Replacer> chosenReplacers = new LinkedHashSet<>(
                Picker.pickRandomUniqueIndices(Math.min(replacers.size(), amountOfReplacements), replacers.size())
                        .stream().map(idx -> replacers.get(idx)).collect(Collectors.toList()));

        return (new Replacers(chosenReplacers)).replace(line);

    }

}
