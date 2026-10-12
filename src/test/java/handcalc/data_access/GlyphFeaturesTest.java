package handcalc.data_access;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import handcalc.entity.Glyph;
import handcalc.entity.Point;
import handcalc.entity.Stroke;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class GlyphFeaturesTest {

    private static Glyph glyphWithStrokes(int count) {
        List<Stroke> strokes = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            strokes.add(new Stroke(Arrays.asList(new Point(0, i * 10), new Point(50, i * 10))));
        }
        return new Glyph(strokes);
    }

    private static float[] strokeCountPart(float[] features) {
        return Arrays.copyOfRange(features, GlyphFeatures.PIXELS, GlyphFeatures.LENGTH);
    }

    @Test
    void startsWithTheRasterizedImage() {
        Glyph glyph = glyphWithStrokes(2);

        float[] features = GlyphFeatures.extract(glyph);

        assertEquals(GlyphFeatures.LENGTH, features.length);
        assertArrayEquals(GlyphRasterizer.rasterize(glyph),
                Arrays.copyOf(features, GlyphFeatures.PIXELS));
    }

    @Test
    void strokeCountIsOneHotEncodedAndCappedAtFourOrMore() {
        float w = GlyphFeatures.STROKE_COUNT_WEIGHT;
        assertArrayEquals(new float[] {w, 0, 0, 0},
                strokeCountPart(GlyphFeatures.extract(glyphWithStrokes(1))));
        assertArrayEquals(new float[] {0, w, 0, 0},
                strokeCountPart(GlyphFeatures.extract(glyphWithStrokes(2))));
        assertArrayEquals(new float[] {0, 0, w, 0},
                strokeCountPart(GlyphFeatures.extract(glyphWithStrokes(3))));
        assertArrayEquals(new float[] {0, 0, 0, w},
                strokeCountPart(GlyphFeatures.extract(glyphWithStrokes(4))));
        assertArrayEquals(new float[] {0, 0, 0, w},
                strokeCountPart(GlyphFeatures.extract(glyphWithStrokes(7))));
    }
}
