package handcalc.interface_adapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One symbol as the screen shows it: what it was read as, and what else it might be.
 */
public final class ReadSymbol {

    private final char symbol;
    private final List<Alternative> alternatives;

    public ReadSymbol(char symbol, List<Alternative> alternatives) {
        this.symbol = symbol;
        this.alternatives = Collections.unmodifiableList(new ArrayList<>(alternatives));
    }

    public char getSymbol() {
        return symbol;
    }

    public List<Alternative> getAlternatives() {
        return alternatives;
    }

    /** A possible reading with its likelihood as a whole percentage. */
    public static final class Alternative {
        private final char symbol;
        private final int percent;

        public Alternative(char symbol, int percent) {
            this.symbol = symbol;
            this.percent = percent;
        }

        public char getSymbol() {
            return symbol;
        }

        public int getPercent() {
            return percent;
        }
    }
}
