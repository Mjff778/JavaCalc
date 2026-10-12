package handcalc.data_access;

import handcalc.entity.Glyph;

/**
 * The numbers the recognition network sees for one written symbol: its rasterized image
 * followed by the number of pen strokes (one-hot, 4 meaning "four or more").
 *
 * <p>The stroke count separates look-alikes such as "=" (two strokes) and "2" (usually one).
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

    private GlyphFeatures() {
    }

    public static float[] extract(Glyph glyph) {
        float[] features = new float[LENGTH];
        float[] pixels = GlyphRasterizer.rasterize(glyph);
        System.arraycopy(pixels, 0, features, 0, PIXELS);
        int bucket = Math.min(glyph.getStrokes().size(), STROKE_BUCKETS) - 1;
        features[PIXELS + bucket] = STROKE_COUNT_WEIGHT;
        return features;
    }
}
