package handcalc.interface_adapter;

import handcalc.entity.Stroke;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One symbol as the screen shows it: what it was read as, what else it might be, where it is
 * written, and whether it is at fault in the expression.
 */
public final class ReadSymbol {

    private final char symbol;
    private final List<Alternative> alternatives;
    private final double minX;
    private final double minY;
    private final double maxX;
    private final double maxY;
    private final boolean faulty;
    private final List<Stroke> strokes;

    public ReadSymbol(char symbol, List<Alternative> alternatives) {
        this(symbol, alternatives, 0, 0, 0, 0, false);
    }

    /** The symbol's ink lies in the box from (minX, minY) to (maxX, maxY). */
    public ReadSymbol(char symbol, List<Alternative> alternatives, double minX, double minY,
                      double maxX, double maxY, boolean faulty) {
        this(symbol, alternatives, minX, minY, maxX, maxY, faulty, Collections.emptyList());
    }

    /** @param strokes what the symbol is written with */
    public ReadSymbol(char symbol, List<Alternative> alternatives, double minX, double minY,
                      double maxX, double maxY, boolean faulty, List<Stroke> strokes) {
        this.strokes = Collections.unmodifiableList(new ArrayList<>(strokes));
        this.symbol = symbol;
        this.alternatives = Collections.unmodifiableList(new ArrayList<>(alternatives));
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
        this.faulty = faulty;
    }

    public char getSymbol() {
        return symbol;
    }

    public List<Alternative> getAlternatives() {
        return alternatives;
    }

    public double getMinX() {
        return minX;
    }

    public double getMinY() {
        return minY;
    }

    public double getMaxX() {
        return maxX;
    }

    public double getMaxY() {
        return maxY;
    }

    /** @return the strokes the symbol is written with (the outline of a picked symbol) */
    public List<Stroke> getStrokes() {
        return strokes;
    }

    /** @return true if this symbol makes the expression wrongly written. */
    public boolean isFaulty() {
        return faulty;
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
