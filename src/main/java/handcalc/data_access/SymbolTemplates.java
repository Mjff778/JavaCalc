package handcalc.data_access;

import handcalc.entity.Point;
import handcalc.entity.Stroke;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Idealised pen strokes for each supported symbol, in a box one unit tall (y grows downwards)
 * and up to a few units wide (a root sign has a long bar).
 *
 * <p>These bootstrap the classifier until real handwriting samples have been collected.
 */
public final class SymbolTemplates {

    public static final String SYMBOLS = "0123456789+-×÷=()√sincotaπ";

    private static final Map<Character, List<List<Stroke>>> VARIANTS = new HashMap<>();

    static {
        Stroke six = line(0.8, 0.05, 0.4, 0.2, 0.15, 0.55, 0.2, 0.9, 0.5, 1, 0.8, 0.85,
                0.8, 0.6, 0.5, 0.5, 0.2, 0.65);
        Stroke nine = line(0.5, 0, 0.2, 0.3, 0.5, 0.6, 0.8, 0.3, 0.5, 0, 0.78, 0.7, 0.6, 1);

        add('0', oval(0.35, 0.5));
        add('1', line(0.5, 0, 0.5, 1));
        add('1', line(0.3, 0.2, 0.55, 0, 0.55, 1));
        add('2', line(0.15, 0.2, 0.3, 0.05, 0.5, 0, 0.7, 0.05, 0.85, 0.2, 0.8, 0.4, 0.15, 1, 0.9, 1));
        add('3', line(0.15, 0.1, 0.5, 0, 0.85, 0.15, 0.5, 0.45, 0.85, 0.7, 0.5, 1, 0.1, 0.9));
        add('4', line(0.65, 0, 0.1, 0.65, 0.95, 0.65), line(0.65, 0.2, 0.65, 1));
        add('4', line(0.2, 0, 0.15, 0.6, 0.85, 0.6), line(0.75, 0, 0.75, 1));
        add('4', line(0.05, 0, 0, 0.55, 1, 0.55), line(0.55, 0, 0.55, 1));
        add('5', line(0.85, 0, 0.2, 0, 0.15, 0.45, 0.6, 0.4, 0.9, 0.65, 0.6, 1, 0.1, 0.9));
        add('5', line(0.2, 0, 0.15, 0.45, 0.6, 0.4, 0.9, 0.65, 0.6, 1, 0.1, 0.9),
                line(0.85, 0, 0.2, 0));
        add('6', six);
        add('6', rotate180(nine));
        add('7', line(0.1, 0, 0.9, 0, 0.4, 1));
        add('7', line(0.1, 0, 0.9, 0, 0.4, 1), line(0.2, 0.55, 0.7, 0.55));
        add('8', line(0.5, 0.5, 0.15, 0.3, 0.5, 0, 0.85, 0.3, 0.5, 0.5, 0.1, 0.75, 0.5, 1,
                0.9, 0.75, 0.5, 0.5));
        add('9', rotate180(six));
        add('9', nine);
        add('+', line(0.1, 0.5, 0.9, 0.5), line(0.5, 0.1, 0.5, 0.9));
        add('-', line(0, 0.5, 1, 0.5));
        add('×', line(0.1, 0.1, 0.9, 0.9), line(0.9, 0.1, 0.1, 0.9));
        add('÷', line(0, 0.5, 1, 0.5), line(0.5, 0.12), line(0.5, 0.88));
        add('=', line(0.05, 0.3, 0.95, 0.3), line(0.1, 0.7, 0.9, 0.7));
        add('=', line(0, 0.165, 1, 0.165), line(0.05, 0.835, 0.95, 0.835));
        add('(', line(0.75, 0, 0.4, 0.15, 0.25, 0.5, 0.4, 0.85, 0.75, 1));
        add(')', line(0.25, 0, 0.6, 0.15, 0.75, 0.5, 0.6, 0.85, 0.25, 1));
        add('√', line(0, 0.6, 0.12, 0.5, 0.3, 1, 0.45, 0, 1.3, 0));
        add('√', line(0, 0.6, 0.12, 0.5, 0.3, 1, 0.45, 0, 2.5, 0));
        add('√', line(0, 0.5, 0.2, 1, 0.4, 0, 1.6, 0));
        add('s', line(0.8, 0.2, 0.5, 0, 0.2, 0.2, 0.3, 0.45, 0.7, 0.6, 0.85, 0.8, 0.5, 1,
                0.15, 0.8));
        add('i', line(0.5, 0.3, 0.5, 1), line(0.5, 0.05));
        add('n', line(0.2, 1, 0.2, 0.3, 0.2, 0.5, 0.45, 0.3, 0.7, 0.35, 0.8, 0.6, 0.8, 1));
        add('c', line(0.85, 0.15, 0.5, 0, 0.2, 0.25, 0.15, 0.6, 0.3, 0.9, 0.6, 1, 0.85, 0.85));
        add('o', oval(0.5, 0.5));
        add('t', line(0.5, 0, 0.5, 1), line(0.15, 0.3, 0.85, 0.3));
        add('t', line(0.45, 0, 0.45, 0.85, 0.7, 1), line(0.15, 0.3, 0.85, 0.3));
        add('π', line(0.02, 0.15, 0.98, 0.15), line(0.3, 0.15, 0.25, 1),
                line(0.7, 0.15, 0.72, 0.8, 0.85, 1));
        add('π', line(0.0, 0.2, 0.5, 0.05, 1, 0.15), line(0.35, 0.1, 0.25, 1),
                line(0.7, 0.1, 0.7, 0.85, 0.9, 1));
        add('π', line(0.02, 0.15, 0.98, 0.15), line(0.3, 0.15, 0.3, 1), line(0.7, 0.15, 0.7, 1));
        add('a', line(0.75, 0.3, 0.4, 0.3, 0.2, 0.55, 0.3, 0.9, 0.6, 0.95, 0.75, 0.75,
                0.75, 0.3, 0.75, 1));
    }

    private SymbolTemplates() {
    }

    /** @return the primary template for the symbol. */
    public static List<Stroke> strokesFor(char symbol) {
        return variantsFor(symbol).get(0);
    }

    /** @return all templates for the symbol, e.g. a "1" with and without a flag. */
    public static List<List<Stroke>> variantsFor(char symbol) {
        List<List<Stroke>> variants = VARIANTS.get(symbol);
        if (variants == null) {
            throw new IllegalArgumentException("Unsupported symbol: " + symbol);
        }
        return variants;
    }

    private static void add(char symbol, Stroke... strokes) {
        VARIANTS.computeIfAbsent(symbol, c -> new ArrayList<>())
                .add(Collections.unmodifiableList(Arrays.asList(strokes)));
    }

    /** A polyline through the given x,y pairs; a single pair makes a dot. */
    private static Stroke line(double... xy) {
        List<Point> points = new ArrayList<>();
        for (int i = 0; i < xy.length; i += 2) {
            points.add(new Point(xy[i], xy[i + 1]));
        }
        return new Stroke(points);
    }

    /** A closed oval around the centre of the unit box, starting at the top. */
    private static Stroke oval(double radiusX, double radiusY) {
        List<Point> points = new ArrayList<>();
        int steps = 16;
        for (int i = 0; i <= steps; i++) {
            double t = 2 * Math.PI * i / steps - Math.PI / 2;
            points.add(new Point(0.5 + radiusX * Math.cos(t), 0.5 + radiusY * Math.sin(t)));
        }
        return new Stroke(points);
    }

    private static Stroke rotate180(Stroke stroke) {
        List<Point> points = new ArrayList<>();
        for (Point p : stroke.getPoints()) {
            points.add(new Point(1 - p.getX(), 1 - p.getY()));
        }
        return new Stroke(points);
    }
}
