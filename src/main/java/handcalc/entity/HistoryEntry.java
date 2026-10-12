package handcalc.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One solved calculation: what was read, the answer, when, and the original handwriting so
 * it can be put back on the canvas.
 */
public final class HistoryEntry {

    private final String expression;
    private final double result;
    private final Instant time;
    private final List<Stroke> strokes;

    public HistoryEntry(String expression, double result, Instant time, List<Stroke> strokes) {
        this.expression = expression;
        this.result = result;
        this.time = time;
        this.strokes = Collections.unmodifiableList(new ArrayList<>(strokes));
    }

    public String getExpression() {
        return expression;
    }

    public double getResult() {
        return result;
    }

    public Instant getTime() {
        return time;
    }

    public List<Stroke> getStrokes() {
        return strokes;
    }
}
