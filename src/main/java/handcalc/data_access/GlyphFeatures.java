package handcalc.data_access;

import handcalc.entity.Glyph;
import handcalc.entity.Point;
import handcalc.entity.Stroke;
import java.util.List;

/**
 * The numbers the recognition network sees for one written symbol: its rasterized image
 * followed by the number of pen strokes (one-hot, 4 meaning "four or more").
 *
 * <p>The stroke count separates look-alikes such as "=" (two strokes) and "2" (usually one).
 * A click that lands on ink already drawn, as when the pen goes down twice, adds nothing to
 * the shape and is not counted, so a "−" with a click on its end is still one stroke.
 */
public final class GlyphFeatures {

    public static final int PIXELS = GlyphRasterizer.SIZE * GlyphRasterizer.SIZE;
    public static final int STROKE_BUCKETS = 4;
    public static final int LENGTH = PIXELS + STROKE_BUCKETS;
    /**
     * The stroke count is one number against hundreds of pixels, yet it decides between
     * look-alikes ("=" and "√"), so it is given extra weight.
     */
    public static final float STROKE_COUNT_WEIGHT = 4f;
    /** How small, and how close to other ink, a click is, relative to the symbol. */
    private static final double CLICK_RATIO = 0.05;

    private GlyphFeatures() {
    }

    public static float[] extract(Glyph glyph) {
        float[] features = new float[LENGTH];
        float[] pixels = GlyphRasterizer.rasterize(glyph);
        System.arraycopy(pixels, 0, features, 0, PIXELS);
        int bucket = Math.min(Math.max(1, drawnStrokes(glyph)), STROKE_BUCKETS) - 1;
        features[PIXELS + bucket] = STROKE_COUNT_WEIGHT;
        return features;
    }

    /** The number of strokes, leaving out clicks on ink drawn by another stroke. */
    static int drawnStrokes(Glyph glyph) {
        List<Stroke> strokes = glyph.getStrokes();
        double reach = CLICK_RATIO * Math.max(glyph.getMaxX() - glyph.getMinX(),
                glyph.getMaxY() - glyph.getMinY());
        int count = 0;
        for (int i = 0; i < strokes.size(); i++) {
            if (!isClickOnInk(strokes, i, reach)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Is stroke {@code i} a click on another stroke? Of two clicks on the same spot, the
     * first is kept, so a dot tapped twice still counts once.
     */
    private static boolean isClickOnInk(List<Stroke> strokes, int i, double reach) {
        Stroke click = strokes.get(i);
        if (Math.max(click.getMaxX() - click.getMinX(), click.getMaxY() - click.getMinY()) > reach) {
            return false;
        }
        for (int j = 0; j < strokes.size(); j++) {
            Stroke other = strokes.get(j);
            boolean earlierOrLarger = j < i || !isClickSized(other, reach);
            if (j != i && earlierOrLarger && distance(click.getPoints().get(0), other) <= reach) {
                return true;
            }
        }
        return false;
    }

    private static boolean isClickSized(Stroke stroke, double reach) {
        return Math.max(stroke.getMaxX() - stroke.getMinX(),
                stroke.getMaxY() - stroke.getMinY()) <= reach;
    }

    private static double distance(Point p, Stroke stroke) {
        List<Point> points = stroke.getPoints();
        double best = Double.POSITIVE_INFINITY;
        for (int k = 0; k < points.size(); k++) {
            Point a = points.get(k);
            Point b = points.get(Math.min(k + 1, points.size() - 1));
            double dx = b.getX() - a.getX();
            double dy = b.getY() - a.getY();
            double lengthSquared = dx * dx + dy * dy;
            double t = lengthSquared == 0 ? 0
                    : ((p.getX() - a.getX()) * dx + (p.getY() - a.getY()) * dy) / lengthSquared;
            t = Math.max(0, Math.min(1, t));
            best = Math.min(best, Math.hypot(p.getX() - (a.getX() + t * dx),
                    p.getY() - (a.getY() + t * dy)));
        }
        return best;
    }
}
