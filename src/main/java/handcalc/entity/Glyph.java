package handcalc.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The strokes that together make up one written symbol, e.g. the two strokes of a "+".
 */
public final class Glyph {

    private final List<Stroke> strokes;
    private final double minX;
    private final double maxX;
    private final double minY;
    private final double maxY;

    public Glyph(List<Stroke> strokes) {
        if (strokes.isEmpty()) {
            throw new IllegalArgumentException("A glyph needs at least one stroke");
        }
        this.strokes = Collections.unmodifiableList(new ArrayList<>(strokes));
        double lowX = Double.POSITIVE_INFINITY;
        double highX = Double.NEGATIVE_INFINITY;
        double lowY = Double.POSITIVE_INFINITY;
        double highY = Double.NEGATIVE_INFINITY;
        for (Stroke s : strokes) {
            lowX = Math.min(lowX, s.getMinX());
            highX = Math.max(highX, s.getMaxX());
            lowY = Math.min(lowY, s.getMinY());
            highY = Math.max(highY, s.getMaxY());
        }
        this.minX = lowX;
        this.maxX = highX;
        this.minY = lowY;
        this.maxY = highY;
    }

    public List<Stroke> getStrokes() {
        return strokes;
    }

    public double getMinX() {
        return minX;
    }

    public double getMaxX() {
        return maxX;
    }

    public double getMinY() {
        return minY;
    }

    public double getMaxY() {
        return maxY;
    }
}
