package handcalc.use_case.recognize_and_solve;

import handcalc.entity.Glyph;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Splits the symbols on a page into lines, like lines of working on paper.
 *
 * <p>Symbols are on one line when their heights overlap or nearly do: a numerator or
 * denominator sits close to its fraction bar, and an exponent overlaps its base, so both stay
 * on the line, while the next line is usually about a line's height further down.
 */
public final class LineSplitter {

    /** How far above and below its ink a symbol reaches, relative to a typical symbol. */
    private static final double REACH_RATIO = 0.35;

    private LineSplitter() {
    }

    /** @return the lines from top to bottom, each with its symbols from left to right */
    public static List<List<Glyph>> split(List<Glyph> glyphs) {
        if (glyphs.isEmpty()) {
            return Collections.emptyList();
        }
        double reach = REACH_RATIO * typicalSize(glyphs);
        List<Glyph> byTop = new ArrayList<>(glyphs);
        byTop.sort(Comparator.comparingDouble(Glyph::getMinY));

        List<List<Glyph>> lines = new ArrayList<>();
        List<Glyph> line = new ArrayList<>();
        double bottom = Double.NEGATIVE_INFINITY;
        for (Glyph glyph : byTop) {
            if (!line.isEmpty() && glyph.getMinY() - reach > bottom + reach) {
                lines.add(line);
                line = new ArrayList<>();
            }
            line.add(glyph);
            bottom = line.size() == 1 ? glyph.getMaxY() : Math.max(bottom, glyph.getMaxY());
        }
        lines.add(line);
        for (List<Glyph> each : lines) {
            each.sort(Comparator.comparingDouble(Glyph::getMinX));
        }
        return lines;
    }

    /** The median size of the symbols, so flat ones such as "-" do not count as small. */
    private static double typicalSize(List<Glyph> glyphs) {
        List<Double> sizes = new ArrayList<>();
        for (Glyph glyph : glyphs) {
            sizes.add(Math.max(glyph.getMaxX() - glyph.getMinX(), glyph.getMaxY() - glyph.getMinY()));
        }
        Collections.sort(sizes);
        return sizes.get(sizes.size() / 2);
    }
}
