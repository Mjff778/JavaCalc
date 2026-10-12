package handcalc.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * An expression as it is typeset: text set in a row, fractions stacked, roots with a bar,
 * powers raised.
 */
public final class Formula {

    /** The kinds of piece a formula is built from. */
    public enum Kind {
        /** Plain text: a number, a symbol, an operator or a function name. */
        TEXT,
        /** Pieces side by side. */
        ROW,
        /** The first child over the second. */
        FRACTION,
        /** The square root of the only child. */
        ROOT,
        /** The first child raised to the second. */
        POWER,
        /** The only child in brackets. */
        BRACKETS
    }

    private final Kind kind;
    private final String text;
    private final List<Formula> children;

    private Formula(Kind kind, String text, List<Formula> children) {
        this.kind = kind;
        this.text = text;
        this.children = Collections.unmodifiableList(new ArrayList<>(children));
    }

    public static Formula text(String text) {
        return new Formula(Kind.TEXT, text, Collections.emptyList());
    }

    public static Formula row(List<Formula> pieces) {
        return pieces.size() == 1 ? pieces.get(0) : new Formula(Kind.ROW, "", pieces);
    }

    public static Formula fraction(Formula numerator, Formula denominator) {
        return new Formula(Kind.FRACTION, "", Arrays.asList(numerator, denominator));
    }

    public static Formula root(Formula radicand) {
        return new Formula(Kind.ROOT, "", Collections.singletonList(radicand));
    }

    public static Formula power(Formula base, Formula exponent) {
        return new Formula(Kind.POWER, "", Arrays.asList(base, exponent));
    }

    public static Formula brackets(Formula inside) {
        return new Formula(Kind.BRACKETS, "", Collections.singletonList(inside));
    }

    public Kind getKind() {
        return kind;
    }

    /** @return the text of a TEXT piece; empty for the other kinds */
    public String getText() {
        return text;
    }

    public List<Formula> getChildren() {
        return children;
    }

    /** A plain-text rendering, e.g. {@code 7^(2)+sin[π/2]}; for tests and debugging. */
    @Override
    public String toString() {
        switch (kind) {
            case TEXT:
                return text;
            case FRACTION:
                return "{" + children.get(0) + "/" + children.get(1) + "}";
            case ROOT:
                return "√{" + children.get(0) + "}";
            case POWER:
                return children.get(0) + "^{" + children.get(1) + "}";
            case BRACKETS:
                return "(" + children.get(0) + ")";
            default:
                StringBuilder row = new StringBuilder();
                for (Formula child : children) {
                    row.append(child);
                }
                return row.toString();
        }
    }
}
