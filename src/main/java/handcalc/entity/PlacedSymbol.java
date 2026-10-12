package handcalc.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A symbol the user picked for something they wrote, set in type in its place on the page.
 * Its reading is settled: it is never recognised again. Its outline, as strokes around each
 * part of the letter, says where it is.
 */
public final class PlacedSymbol {

    private final char symbol;
    private final String alternatives;
    private final List<Stroke> outline;

    public PlacedSymbol(char symbol, List<Stroke> outline) {
        this(symbol, "", outline);
    }

    /** @param alternatives what else the writing it replaced might have been, most likely first */
    public PlacedSymbol(char symbol, String alternatives, List<Stroke> outline) {
        if (outline.isEmpty()) {
            throw new IllegalArgumentException("A placed symbol needs an outline");
        }
        this.symbol = symbol;
        this.alternatives = alternatives;
        this.outline = Collections.unmodifiableList(new ArrayList<>(outline));
    }

    public char getSymbol() {
        return symbol;
    }

    /** @return what else the writing it replaced might have been, most likely first */
    public String getAlternatives() {
        return alternatives;
    }

    /** @return the outline of the typeset symbol, one closed stroke per contour */
    public List<Stroke> getOutline() {
        return outline;
    }
}
