package handcalc.data_access;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import handcalc.entity.Glyph;
import handcalc.entity.Point;
import handcalc.entity.Stroke;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import org.junit.jupiter.api.Test;

class GlyphAugmenterTest {

    private static Glyph plus() {
        return new Glyph(Arrays.asList(
                new Stroke(Arrays.asList(new Point(100, 150), new Point(200, 150))),
                new Stroke(Arrays.asList(new Point(150, 100), new Point(150, 200)))));
    }

    @Test
    void keepsStrokeAndPointCounts() {
        Glyph distorted = GlyphAugmenter.distort(plus(), new Random(1));

        assertEquals(2, distorted.getStrokes().size());
        assertEquals(2, distorted.getStrokes().get(0).getPoints().size());
    }

    @Test
    void staysAroundTheSamePlaceAndSize() {
        Random random = new Random(2);
        for (int i = 0; i < 50; i++) {
            Glyph g = GlyphAugmenter.distort(plus(), random);
            double centreX = (g.getMinX() + g.getMaxX()) / 2;
            double width = g.getMaxX() - g.getMinX();
            assertTrue(Math.abs(centreX - 150) < 25, "centre moved to " + centreX);
            assertTrue(width > 50 && width < 160, "width became " + width);
        }
    }

    @Test
    void sameSeedGivesSameResultAndDifferentSeedsDiffer() {
        Glyph a = GlyphAugmenter.distort(plus(), new Random(3));
        Glyph b = GlyphAugmenter.distort(plus(), new Random(3));
        Glyph c = GlyphAugmenter.distort(plus(), new Random(4));

        assertEquals(a.getMinX(), b.getMinX());
        assertNotEquals(a.getMinX(), c.getMinX());
    }

    @Test
    void aSingleDotCannotBeDistortedAndIsReturnedAsIs() {
        Glyph dot = new Glyph(Collections.singletonList(
                new Stroke(Collections.singletonList(new Point(5, 5)))));
        assertSame(dot, GlyphAugmenter.distort(dot, new Random(1)));
    }
}
