package handcalc.data_access;

import handcalc.entity.Glyph;
import handcalc.entity.Point;
import handcalc.entity.Stroke;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generates varied, hand-like versions of the {@link SymbolTemplates}, densely resampled
 * like a real pen trace. Used to bootstrap the recognizer before real samples exist.
 */
public final class SyntheticSamples {

    private static final double CANVAS_SIZE = 120;
    private static final double RESAMPLE_STEP = 0.04;

    private SyntheticSamples() {
    }

    /** @return {@code perSymbol} feature-vector samples for every supported symbol. */
    public static List<LabeledSample> generate(Random random, int perSymbol) {
        List<LabeledSample> samples = new ArrayList<>();
        for (char symbol : SymbolTemplates.SYMBOLS.toCharArray()) {
            for (int i = 0; i < perSymbol; i++) {
                samples.add(new LabeledSample(symbol,
                        GlyphFeatures.extract(randomGlyph(symbol, random))));
            }
        }
        return samples;
    }

    /** @return a randomly distorted handwritten-looking version of the symbol. */
    public static Glyph randomGlyph(char symbol, Random random) {
        List<List<Stroke>> variants = SymbolTemplates.variantsFor(symbol);
        List<Stroke> template = variants.get(random.nextInt(variants.size()));

        List<Stroke> scaled = new ArrayList<>();
        for (Stroke stroke : template) {
            List<Point> points = new ArrayList<>();
            for (Point p : resample(stroke.getPoints())) {
                points.add(new Point(p.getX() * CANVAS_SIZE, p.getY() * CANVAS_SIZE));
            }
            scaled.add(new Stroke(points));
        }
        return GlyphAugmenter.distort(new Glyph(scaled), random);
    }

    private static List<Point> resample(List<Point> points) {
        List<Point> result = new ArrayList<>();
        result.add(points.get(0));
        for (int i = 1; i < points.size(); i++) {
            Point a = points.get(i - 1);
            Point b = points.get(i);
            double length = Math.hypot(b.getX() - a.getX(), b.getY() - a.getY());
            int pieces = Math.max(1, (int) Math.ceil(length / RESAMPLE_STEP));
            for (int k = 1; k <= pieces; k++) {
                double t = (double) k / pieces;
                result.add(new Point(a.getX() + (b.getX() - a.getX()) * t,
                        a.getY() + (b.getY() - a.getY()) * t));
            }
        }
        return result;
    }
}
