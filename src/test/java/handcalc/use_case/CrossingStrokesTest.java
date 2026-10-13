package handcalc.use_case;

import static org.junit.jupiter.api.Assertions.assertEquals;

import handcalc.entity.Point;
import handcalc.entity.Stroke;
import handcalc.use_case.recognize_and_solve.Candidate;
import handcalc.use_case.recognize_and_solve.GlyphSegmenter;
import handcalc.use_case.recognize_and_solve.HandwritingReader;
import handcalc.use_case.recognize_and_solve.SymbolClassifier;
import handcalc.use_case.recognize_and_solve.SymbolReading;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The strokes of an "i", "=" or "÷" never touch, so a symbol whose strokes cross is none of
 * them, however much a tall, narrow "+" looks like an "i".
 */
class CrossingStrokesTest {

    private static final SymbolClassifier SAYS_I = glyph -> Arrays.asList(
            new Candidate('i', 0.87), new Candidate('+', 0.13));

    private static Stroke line(double x1, double y1, double x2, double y2) {
        return new Stroke(Arrays.asList(new Point(x1, y1), new Point(x2, y2)));
    }

    private static List<SymbolReading> read(Stroke... strokes) {
        return new HandwritingReader(new GlyphSegmenter(), SAYS_I)
                .read(new ArrayList<>(Arrays.asList(strokes)));
    }

    @Test
    void aSymbolWhoseStrokesCrossIsNotAnI() {
        List<SymbolReading> readings = read(line(401, 334, 420, 335), line(410, 312, 411, 350));

        assertEquals(1, readings.size());
        assertEquals('+', readings.get(0).getSymbol());
    }

    @Test
    void aStemWithADotAboveItIsLeftAsRead() {
        List<SymbolReading> readings = read(line(50, 20, 50, 60),
                new Stroke(Arrays.asList(new Point(50, 5))));

        assertEquals(1, readings.size());
        assertEquals('i', readings.get(0).getSymbol());
    }
}
