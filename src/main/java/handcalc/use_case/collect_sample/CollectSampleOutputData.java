package handcalc.use_case.collect_sample;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * How many samples exist per symbol and which symbol to ask for next.
 */
public final class CollectSampleOutputData {

    private final Map<Character, Integer> counts;
    private final char nextSymbol;
    private final int targetPerSymbol;
    private final Character savedSymbol;

    /**
     * @param savedSymbol the symbol just saved, or null if this is only a progress report
     */
    public CollectSampleOutputData(Map<Character, Integer> counts, char nextSymbol,
                                   int targetPerSymbol, Character savedSymbol) {
        this.counts = Collections.unmodifiableMap(new LinkedHashMap<>(counts));
        this.nextSymbol = nextSymbol;
        this.targetPerSymbol = targetPerSymbol;
        this.savedSymbol = savedSymbol;
    }

    /** @return sample count per symbol, in the order symbols are listed. */
    public Map<Character, Integer> getCounts() {
        return counts;
    }

    public char getNextSymbol() {
        return nextSymbol;
    }

    public int getTargetPerSymbol() {
        return targetPerSymbol;
    }

    public Character getSavedSymbol() {
        return savedSymbol;
    }
}
