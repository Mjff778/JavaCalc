package handcalc.use_case.recognize_and_solve;

import handcalc.entity.Glyph;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * How one written symbol was read: the symbol chosen and the other likely candidates.
 */
public final class SymbolReading {

    private final Glyph glyph;
    private final char symbol;
    private final List<Candidate> alternatives;

    public SymbolReading(Glyph glyph, char symbol, List<Candidate> alternatives) {
        this.glyph = glyph;
        this.symbol = symbol;
        this.alternatives = Collections.unmodifiableList(new ArrayList<>(alternatives));
    }

    public Glyph getGlyph() {
        return glyph;
    }

    public char getSymbol() {
        return symbol;
    }

    /** @return the most likely candidates, best first (the chosen symbol included). */
    public List<Candidate> getAlternatives() {
        return alternatives;
    }
}
