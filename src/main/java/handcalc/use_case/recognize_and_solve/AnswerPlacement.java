package handcalc.use_case.recognize_and_solve;

import handcalc.entity.Glyph;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Where the answer goes on the page: just after the written expression (after its "="),
 * as tall as the user's digits, centred on the "=" and spaced like the user spaces symbols.
 */
public final class AnswerPlacement {

    private static final char EQUALS = '=';
    private static final double DEFAULT_HEIGHT = 60;
    private static final double DEFAULT_GAP_RATIO = 0.35;
    private static final double MIN_GAP_RATIO = 0.15;
    private static final double MAX_GAP_RATIO = 0.8;

    private final double x;
    private final double centreY;
    private final double height;

    public AnswerPlacement(double x, double centreY, double height) {
        this.x = x;
        this.centreY = centreY;
        this.height = height;
    }

    /** @param readings the written expression; must not be empty */
    public static AnswerPlacement after(List<SymbolReading> readings) {
        double height = digitHeight(readings);
        SymbolReading last = Collections.max(readings,
                Comparator.comparingDouble(r -> r.getGlyph().getMinX()));
        double right = Double.NEGATIVE_INFINITY;
        for (SymbolReading reading : readings) {
            right = Math.max(right, reading.getGlyph().getMaxX());
        }
        double centre = last.getSymbol() == EQUALS
                ? centreY(last.getGlyph()) : lineCentre(readings);
        return new AnswerPlacement(right + gap(readings, height), centre, height);
    }

    /** @return the left edge of the answer */
    public double getX() {
        return x;
    }

    /** @return the height the answer's digits are centred on */
    public double getCentreY() {
        return centreY;
    }

    /** @return how tall the answer's digits are */
    public double getHeight() {
        return height;
    }

    /** The median height of the written digits, or of the other symbols if there are none. */
    private static double digitHeight(List<SymbolReading> readings) {
        List<Double> digits = new ArrayList<>();
        List<Double> others = new ArrayList<>();
        for (SymbolReading reading : readings) {
            Glyph glyph = reading.getGlyph();
            double height = glyph.getMaxY() - glyph.getMinY();
            if (Character.isDigit(reading.getSymbol())) {
                digits.add(height);
            } else if ("-=.".indexOf(reading.getSymbol()) < 0 && height > 0) {
                others.add(height);
            }
        }
        if (!digits.isEmpty()) {
            return median(digits);
        }
        return others.isEmpty() ? DEFAULT_HEIGHT : median(others);
    }

    /** The median space the user leaves between neighbouring symbols. */
    private static double gap(List<SymbolReading> readings, double height) {
        List<Glyph> glyphs = new ArrayList<>();
        for (SymbolReading reading : readings) {
            glyphs.add(reading.getGlyph());
        }
        glyphs.sort(Comparator.comparingDouble(Glyph::getMinX));
        List<Double> gaps = new ArrayList<>();
        for (int i = 1; i < glyphs.size(); i++) {
            double gap = glyphs.get(i).getMinX() - glyphs.get(i - 1).getMaxX();
            if (gap > 0) {
                gaps.add(gap);
            }
        }
        if (gaps.isEmpty()) {
            return DEFAULT_GAP_RATIO * height;
        }
        return Math.max(MIN_GAP_RATIO * height, Math.min(MAX_GAP_RATIO * height, median(gaps)));
    }

    /** The middle of the line: fractions and powers above or below it balance out. */
    private static double lineCentre(List<SymbolReading> readings) {
        List<Double> centres = new ArrayList<>();
        for (SymbolReading reading : readings) {
            if (reading.getSymbol() != '.') {
                centres.add(centreY(reading.getGlyph()));
            }
        }
        return centres.isEmpty() ? centreY(readings.get(0).getGlyph()) : median(centres);
    }

    private static double centreY(Glyph glyph) {
        return (glyph.getMinY() + glyph.getMaxY()) / 2;
    }

    private static double median(List<Double> values) {
        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int n = sorted.size();
        return n % 2 == 1 ? sorted.get(n / 2) : (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2;
    }
}
