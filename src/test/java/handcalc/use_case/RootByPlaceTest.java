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
 * A root sign is easily read as an "n" or a "c", but those never have anything written under
 * them: a symbol that might be a root and covers other symbols is a root.
 */
class RootByPlaceTest {

    /** Reads wide symbols as the given candidates, and narrow ones as a 9. */
    private static SymbolClassifier classifier(Candidate... wide) {
        return glyph -> glyph.getMaxX() - glyph.getMinX() > 60
                ? Arrays.asList(wide)
                : Arrays.asList(new Candidate('9', 0.95), new Candidate('n', 0.05));
    }

    /** A root-like sign: a long down stroke, then a roof to the right. */
    private static Stroke roof() {
        return new Stroke(Arrays.asList(new Point(0, 80), new Point(5, 120), new Point(10, 0),
                new Point(100, 0)));
    }

    private static Stroke nine() {
        return new Stroke(Arrays.asList(new Point(40, 30), new Point(70, 30), new Point(70, 100),
                new Point(40, 60), new Point(70, 60)));
    }

    private static List<SymbolReading> read(SymbolClassifier classifier, Stroke... strokes) {
        return new HandwritingReader(new GlyphSegmenter(), classifier)
                .read(new ArrayList<>(Arrays.asList(strokes)));
    }

    @Test
    void aSignThatMightBeARootAndCoversASymbolIsARoot() {
        List<SymbolReading> readings = read(
                classifier(new Candidate('n', 0.78), new Candidate('c', 0.19),
                        new Candidate('√', 0.01)),
                roof(), nine());

        assertEquals('√', readings.get(0).getSymbol());
        assertEquals('9', readings.get(1).getSymbol());
    }

    @Test
    void theSameSignWithNothingUnderItIsLeftAsRead() {
        List<SymbolReading> readings = read(
                classifier(new Candidate('n', 0.78), new Candidate('√', 0.01)), roof());

        assertEquals('n', readings.get(0).getSymbol());
    }

    @Test
    void aSignThatCannotBeARootIsLeftAsRead() {
        List<SymbolReading> readings = read(
                classifier(new Candidate('n', 0.78), new Candidate('c', 0.2)), roof(), nine());

        assertEquals('n', readings.get(0).getSymbol());
    }
}
