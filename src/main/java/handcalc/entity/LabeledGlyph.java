package handcalc.entity;

/**
 * A written symbol together with the character it really is: one piece of training data.
 */
public final class LabeledGlyph {

    private final Glyph glyph;
    private final char symbol;

    public LabeledGlyph(Glyph glyph, char symbol) {
        this.glyph = glyph;
        this.symbol = symbol;
    }

    public Glyph getGlyph() {
        return glyph;
    }

    public char getSymbol() {
        return symbol;
    }
}
