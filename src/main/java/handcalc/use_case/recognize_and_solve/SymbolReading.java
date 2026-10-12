package handcalc.use_case.recognize_and_solve;

import handcalc.entity.Glyph;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * How one written symbol was read: the symbol chosen and the other likely candidates. A
 * placed symbol was picked by the user rather than recognised.
 */
public final class SymbolReading {

    private final Glyph glyph;
    private final char symbol;
    private final List<Candidate> alternatives;
    private final boolean placed;

    public SymbolReading(Glyph glyph, char symbol, List<Candidate> alternatives) {
        this(glyph, symbol, alternatives, false);
    }

    /** @param placed true if the user picked the symbol, which is then not recognised */
    public SymbolReading(Glyph glyph, char symbol, List<Candidate> alternatives,
                         boolean placed) {
        this.glyph = glyph;
        this.symbol = symbol;
        this.alternatives = Collections.unmodifiableList(new ArrayList<>(alternatives));
        this.placed = placed;
    }

    /** @return true if the user picked this symbol, so it is not handwriting */
    public boolean isPlaced() {
        return placed;
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
