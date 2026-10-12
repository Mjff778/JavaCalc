package handcalc.use_case;

import static org.junit.jupiter.api.Assertions.assertEquals;

import handcalc.entity.Glyph;
import handcalc.entity.Point;
import handcalc.entity.Stroke;
import handcalc.use_case.recognize_and_solve.LineSplitter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class LineSplitterTest {

    /** A glyph whose ink is a diagonal across the given box. */
    private static Glyph box(double x1, double y1, double x2, double y2) {
        return new Glyph(Collections.singletonList(
                new Stroke(Arrays.asList(new Point(x1, y1), new Point(x2, y2)))));
    }

    /** Symbols 60px tall and 40px wide, 20px apart, with their tops at y. */
    private static List<Glyph> row(int count, double x, double y) {
        List<Glyph> glyphs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            glyphs.add(box(x + i * 60, y, x + i * 60 + 40, y + 60));
        }
        return glyphs;
    }

    @Test
    void oneLineStaysOneLine() {
        assertEquals(1, LineSplitter.split(row(5, 0, 100)).size());
    }

    @Test
    void linesWrittenOneBelowTheOtherAreSeparated() {
        List<Glyph> page = new ArrayList<>(row(4, 0, 300));
        page.addAll(row(5, 0, 100));

        List<List<Glyph>> lines = LineSplitter.split(page);

        assertEquals(2, lines.size());
        assertEquals(5, lines.get(0).size(), "the upper line comes first");
        assertEquals(100, lines.get(0).get(0).getMinY());
        assertEquals(4, lines.get(1).size());
    }

    @Test
    void symbolsInALineStayLeftToRight() {
        List<Glyph> line = row(3, 0, 100);
        List<Glyph> shuffled = Arrays.asList(line.get(2), line.get(0), line.get(1));

        List<Glyph> read = LineSplitter.split(shuffled).get(0);

        assertEquals(0, read.get(0).getMinX());
        assertEquals(60, read.get(1).getMinX());
        assertEquals(120, read.get(2).getMinX());
    }

    @Test
    void aFractionBelongsToTheLineItSitsIn() {
        List<Glyph> page = new ArrayList<>(row(2, 0, 100));
        // a numerator above and a denominator below a bar across the middle of the line
        page.add(box(140, 50, 170, 100));
        page.add(box(130, 130, 180, 130));
        page.add(box(140, 160, 170, 210));
        page.addAll(row(2, 0, 400));

        List<List<Glyph>> lines = LineSplitter.split(page);

        assertEquals(2, lines.size());
        assertEquals(5, lines.get(0).size());
    }

    @Test
    void aRaisedExponentBelongsToItsLine() {
        List<Glyph> page = new ArrayList<>(row(2, 0, 100));
        page.add(box(110, 70, 130, 100));

        assertEquals(1, LineSplitter.split(page).size());
    }

    @Test
    void nothingWrittenIsNoLines() {
        assertEquals(0, LineSplitter.split(Collections.emptyList()).size());
    }
}
