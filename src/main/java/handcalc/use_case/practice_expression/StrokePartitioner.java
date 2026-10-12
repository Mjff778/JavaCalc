package handcalc.use_case.practice_expression;

import handcalc.entity.Glyph;
import handcalc.entity.Stroke;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Divides what was written into exactly as many symbols as the expression has, whatever
 * number of strokes each symbol took. Everyone draws an "=", a "4" or a "÷" with a different
 * number of strokes, so the symbols are not found by looking at the strokes alone: the
 * expression says how many there are, and the strokes are cut into that many consecutive
 * groups at the places where the pen jumps furthest to the right, clear of what came before.
 */
final class StrokePartitioner {

    private static final int MAX_STROKES = 120;

    private StrokePartitioner() {
    }

    /**
     * @param strokes everything written, in the order it was drawn
     * @return one glyph per symbol of the prompt, or null if there are too few strokes
     */
    static List<Glyph> partition(List<Stroke> strokes, ExpressionPrompt prompt) {
        int symbols = prompt.getSymbols().length();
        if (strokes.size() < symbols || strokes.size() > MAX_STROKES) {
            return null;
        }
        if (prompt.getKind() != ExpressionPrompt.Kind.FRACTION) {
            return toGlyphs(cut(strokes, symbols));
        }
        return fraction(strokes, prompt.getParts().get(0).length(),
                prompt.getParts().get(1).length());
    }

    private static List<Glyph> fraction(List<Stroke> strokes, int numerators, int denominators) {
        Stroke bar = strokes.get(0);
        for (Stroke stroke : strokes) {
            if (width(stroke) > width(bar)) {
                bar = stroke;
            }
        }
        double barY = (bar.getMinY() + bar.getMaxY()) / 2;
        List<Stroke> above = new ArrayList<>();
        List<Stroke> below = new ArrayList<>();
        for (Stroke stroke : strokes) {
            if (stroke == bar) {
                continue;
            }
            ((stroke.getMinY() + stroke.getMaxY()) / 2 < barY ? above : below).add(stroke);
        }
        if (above.size() < numerators || below.size() < denominators) {
            return null;
        }
        above.sort(Comparator.comparingDouble(Stroke::getMinX));
        below.sort(Comparator.comparingDouble(Stroke::getMinX));
        List<Glyph> glyphs = new ArrayList<>();
        glyphs.add(new Glyph(java.util.Collections.singletonList(bar)));
        glyphs.addAll(toGlyphs(cut(above, numerators)));
        glyphs.addAll(toGlyphs(cut(below, denominators)));
        return glyphs;
    }

    private static List<Glyph> toGlyphs(List<List<Stroke>> groups) {
        List<Glyph> glyphs = new ArrayList<>();
        for (List<Stroke> group : groups) {
            glyphs.add(new Glyph(group));
        }
        return glyphs;
    }

    /**
     * Cuts the ordered strokes into {@code groups} consecutive non-empty groups, maximising
     * how far each group's first stroke lies to the right of the group before it.
     */
    private static List<List<Stroke>> cut(List<Stroke> strokes, int groups) {
        int n = strokes.size();
        double unit = unit(strokes);
        // best[k][end][start]: best score with k groups so far, the last being start..end
        double[][][] best = new double[groups + 1][n][n];
        int[][][] from = new int[groups + 1][n][n];
        for (double[][] layer : best) {
            for (double[] row : layer) {
                Arrays.fill(row, Double.NEGATIVE_INFINITY);
            }
        }
        for (int end = 0; end < n; end++) {
            best[1][end][0] = 0;
        }
        for (int k = 1; k < groups; k++) {
            for (int end = 0; end < n - 1; end++) {
                for (int start = 0; start <= end; start++) {
                    if (best[k][end][start] == Double.NEGATIVE_INFINITY) {
                        continue;
                    }
                    double right = maxX(strokes, start, end);
                    double jump = (strokes.get(end + 1).getMinX() - right) / unit;
                    double score = best[k][end][start] + jump;
                    for (int next = end + 1; next < n; next++) {
                        if (score > best[k + 1][next][end + 1]) {
                            best[k + 1][next][end + 1] = score;
                            from[k + 1][next][end + 1] = start;
                        }
                    }
                }
            }
        }
        int bestStart = -1;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (int start = 0; start < n; start++) {
            if (best[groups][n - 1][start] > bestScore) {
                bestScore = best[groups][n - 1][start];
                bestStart = start;
            }
        }
        List<List<Stroke>> result = new ArrayList<>();
        int end = n - 1;
        int start = bestStart;
        for (int k = groups; k >= 1; k--) {
            result.add(0, new ArrayList<>(strokes.subList(start, end + 1)));
            int previousStart = k > 1 ? from[k][end][start] : 0;
            end = start - 1;
            start = previousStart;
        }
        return result;
    }

    private static double maxX(List<Stroke> strokes, int start, int end) {
        double max = Double.NEGATIVE_INFINITY;
        for (int i = start; i <= end; i++) {
            max = Math.max(max, strokes.get(i).getMaxX());
        }
        return max;
    }

    /** About the size of one written symbol, to make the jumps independent of handwriting size. */
    private static double unit(List<Stroke> strokes) {
        double tallest = 0;
        for (Stroke stroke : strokes) {
            tallest = Math.max(tallest, Math.max(stroke.getMaxY() - stroke.getMinY(), width(stroke)));
        }
        return tallest > 0 ? tallest : 1;
    }

    private static double width(Stroke stroke) {
        return stroke.getMaxX() - stroke.getMinX();
    }
}
