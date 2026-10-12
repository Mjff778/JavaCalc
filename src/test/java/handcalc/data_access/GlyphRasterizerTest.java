package handcalc.data_access;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import handcalc.entity.Glyph;
import handcalc.entity.Point;
import handcalc.entity.Stroke;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class GlyphRasterizerTest {

    private static final int SIZE = GlyphRasterizer.SIZE;

    private static Glyph glyphOf(Stroke... strokes) {
        return new Glyph(Arrays.asList(strokes));
    }

    private static Stroke line(double x1, double y1, double x2, double y2) {
        return new Stroke(Arrays.asList(new Point(x1, y1), new Point(x2, y2)));
    }

    private static boolean[] inkedColumns(float[] pixels) {
        boolean[] cols = new boolean[SIZE];
        for (int i = 0; i < pixels.length; i++) {
            if (pixels[i] > 0.3f) {
                cols[i % SIZE] = true;
            }
        }
        return cols;
    }

    private static boolean[] inkedRows(float[] pixels) {
        boolean[] rows = new boolean[SIZE];
        for (int i = 0; i < pixels.length; i++) {
            if (pixels[i] > 0.3f) {
                rows[i / SIZE] = true;
            }
        }
        return rows;
    }

    private static int count(boolean[] flags) {
        int n = 0;
        for (boolean f : flags) {
            if (f) {
                n++;
            }
        }
        return n;
    }

    @Test
    void producesOneValuePerPixelInZeroToOneRange() {
        float[] pixels = GlyphRasterizer.rasterize(glyphOf(line(0, 0, 10, 10)));
        assertEquals(SIZE * SIZE, pixels.length);
        for (float p : pixels) {
            assertTrue(p >= 0f && p <= 1f);
        }
    }

    @Test
    void horizontalLineIsWideAndFlatAndCentred() {
        float[] pixels = GlyphRasterizer.rasterize(glyphOf(line(100, 300, 400, 300)));
        assertTrue(count(inkedColumns(pixels)) >= 18, "should span most of the 20px box");
        assertTrue(count(inkedRows(pixels)) <= 4, "should stay thin");
        assertTrue(inkedRows(pixels)[SIZE / 2] || inkedRows(pixels)[SIZE / 2 - 1], "centred");
    }

    @Test
    void verticalLineKeepsItsAspectRatio() {
        float[] pixels = GlyphRasterizer.rasterize(glyphOf(line(50, 0, 50, 90)));
        assertTrue(count(inkedRows(pixels)) >= 18);
        assertTrue(count(inkedColumns(pixels)) <= 4, "must not be stretched to full width");
    }

    @Test
    void resultDoesNotDependOnPositionOrSize() {
        Glyph small = glyphOf(line(0, 0, 10, 10), line(10, 0, 0, 10));
        Glyph moved = glyphOf(line(500, 300, 700, 500), line(700, 300, 500, 500));
        assertArrayEquals(GlyphRasterizer.rasterize(small), GlyphRasterizer.rasterize(moved), 0.05f);
    }

    @Test
    void singleDotStillLeavesInk() {
        Glyph dot = glyphOf(new Stroke(Collections.singletonList(new Point(42, 42))));
        float[] pixels = GlyphRasterizer.rasterize(dot);
        assertTrue(count(inkedColumns(pixels)) >= 1);
    }
}
