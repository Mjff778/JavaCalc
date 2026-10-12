package handcalc.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class GeometryTest {

    @Test
    void strokeBoundsCoverAllPoints() {
        Stroke s = new Stroke(Arrays.asList(new Point(3, 8), new Point(10, 2), new Point(5, 5)));
        assertEquals(3, s.getMinX());
        assertEquals(10, s.getMaxX());
        assertEquals(2, s.getMinY());
        assertEquals(8, s.getMaxY());
    }

    @Test
    void strokeWithoutPointsIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Stroke(Collections.emptyList()));
    }

    @Test
    void glyphBoundsCoverAllStrokes() {
        Stroke a = new Stroke(Arrays.asList(new Point(0, 5), new Point(10, 5)));
        Stroke b = new Stroke(Arrays.asList(new Point(5, 0), new Point(5, 12)));
        Glyph g = new Glyph(Arrays.asList(a, b));
        assertEquals(0, g.getMinX());
        assertEquals(10, g.getMaxX());
        assertEquals(0, g.getMinY());
        assertEquals(12, g.getMaxY());
        assertEquals(2, g.getStrokes().size());
    }
}
