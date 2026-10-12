package handcalc.use_case.practice_expression;

import handcalc.entity.Glyph;
import handcalc.entity.LabeledGlyph;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * An expression the user is asked to write, together with the symbols it is made of, so that
 * the written glyphs can be labelled without anyone clicking on them.
 */
public final class ExpressionPrompt {

    /** How the expression is laid out on the page. */
    public enum Kind { LINE, POWER, FRACTION, ROOT, TRIG }

    private final Kind kind;
    private final List<String> parts;
    private final String symbols;
    private final int numeratorLength;

    private ExpressionPrompt(Kind kind, List<String> parts, String symbols, int numeratorLength) {
        this.kind = kind;
        this.parts = Collections.unmodifiableList(new ArrayList<>(parts));
        this.symbols = symbols;
        this.numeratorLength = numeratorLength;
    }

    /** A plain line such as {@code 12+3×4=}. */
    public static ExpressionPrompt line(String text) {
        return new ExpressionPrompt(Kind.LINE, Collections.singletonList(text), text, 0);
    }

    /** {@code base} with {@code exponent} raised at its upper right. */
    public static ExpressionPrompt power(String base, String exponent) {
        return new ExpressionPrompt(Kind.POWER, Arrays.asList(base, exponent), base + exponent, 0);
    }

    /** A fraction bar with the numerator above and the denominator below. */
    public static ExpressionPrompt fraction(String numerator, String denominator) {
        return new ExpressionPrompt(Kind.FRACTION, Arrays.asList(numerator, denominator),
                "-" + numerator + denominator, numerator.length());
    }

    /** A root sign over the radicand. */
    public static ExpressionPrompt root(String radicand) {
        return new ExpressionPrompt(Kind.ROOT, Collections.singletonList(radicand),
                "√" + radicand, 0);
    }

    /** A function of an angle, e.g. {@code sin(30)}. */
    public static ExpressionPrompt trig(String text) {
        return new ExpressionPrompt(Kind.TRIG, Collections.singletonList(text), text, 0);
    }

    public Kind getKind() {
        return kind;
    }

    /** The pieces to display: one text, base and exponent, or numerator and denominator. */
    public List<String> getParts() {
        return parts;
    }

    /** Every symbol to be written, each as the single character the classifier knows. */
    public String getSymbols() {
        return symbols;
    }

    /**
     * Matches written glyphs to the symbols of this prompt.
     *
     * @return one labelled glyph per symbol, or null when the number or arrangement of the
     *         glyphs does not fit the expression
     */
    public List<LabeledGlyph> label(List<Glyph> glyphs) {
        if (glyphs.size() != symbols.length()) {
            return null;
        }
        List<LabeledGlyph> labelled = new ArrayList<>();
        if (kind == Kind.FRACTION) {
            return labelFraction(glyphs);
        }
        List<Glyph> ordered = new ArrayList<>(glyphs);
        ordered.sort(Comparator.comparingDouble(Glyph::getMinX));
        for (int i = 0; i < ordered.size(); i++) {
            labelled.add(new LabeledGlyph(ordered.get(i), symbols.charAt(i)));
        }
        return labelled;
    }

    private List<LabeledGlyph> labelFraction(List<Glyph> glyphs) {
        Glyph bar = glyphs.get(0);
        for (Glyph glyph : glyphs) {
            if (width(glyph) > width(bar)) {
                bar = glyph;
            }
        }
        double barY = (bar.getMinY() + bar.getMaxY()) / 2;
        List<Glyph> above = new ArrayList<>();
        List<Glyph> below = new ArrayList<>();
        for (Glyph glyph : glyphs) {
            if (glyph == bar) {
                continue;
            }
            double y = (glyph.getMinY() + glyph.getMaxY()) / 2;
            (y < barY ? above : below).add(glyph);
        }
        if (above.size() != numeratorLength) {
            return null;
        }
        above.sort(Comparator.comparingDouble(Glyph::getMinX));
        below.sort(Comparator.comparingDouble(Glyph::getMinX));
        List<LabeledGlyph> labelled = new ArrayList<>();
        labelled.add(new LabeledGlyph(bar, '-'));
        String numerator = parts.get(0);
        String denominator = parts.get(1);
        for (int i = 0; i < above.size(); i++) {
            labelled.add(new LabeledGlyph(above.get(i), numerator.charAt(i)));
        }
        for (int i = 0; i < below.size(); i++) {
            labelled.add(new LabeledGlyph(below.get(i), denominator.charAt(i)));
        }
        return labelled;
    }

    private static double width(Glyph glyph) {
        return glyph.getMaxX() - glyph.getMinX();
    }
}
