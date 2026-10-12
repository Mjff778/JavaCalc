package handcalc.use_case;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import handcalc.entity.Glyph;
import handcalc.entity.LabeledGlyph;
import handcalc.entity.PlacedSymbol;
import handcalc.entity.Point;
import handcalc.entity.Stroke;
import handcalc.use_case.correct_symbol.CorrectSymbolInputData;
import handcalc.use_case.correct_symbol.CorrectSymbolInteractor;
import handcalc.use_case.correct_symbol.SampleRepository;
import handcalc.use_case.recognize_and_solve.Candidate;
import handcalc.use_case.recognize_and_solve.GlyphSegmenter;
import handcalc.use_case.recognize_and_solve.LineResult;
import handcalc.use_case.recognize_and_solve.RecognizeAndSolveInputData;
import handcalc.use_case.recognize_and_solve.RecognizeAndSolveInteractor;
import handcalc.use_case.recognize_and_solve.RecognizeAndSolveOutputBoundary;
import handcalc.use_case.recognize_and_solve.RecognizeAndSolveOutputData;
import handcalc.use_case.recognize_and_solve.SymbolReading;
import handcalc.use_case.recognize_and_solve.TrainableSymbolClassifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * A symbol the user picked is read as picked, never recognised again, and is not
 * handwriting to learn from.
 */
class PlacedSymbolTest {

    /** Reads handwritten glyphs, left to right, as the characters of the script. */
    private static final class ScriptedClassifier implements TrainableSymbolClassifier {
        private final String script;
        private int calls;
        final List<Character> learned = new ArrayList<>();

        ScriptedClassifier(String script) {
            this.script = script;
        }

        @Override
        public List<Candidate> classify(Glyph glyph) {
            return Collections.singletonList(new Candidate(script.charAt(calls++), 0.9));
        }

        @Override
        public void learn(Glyph glyph, char symbol) {
            learned.add(symbol);
        }
    }

    private static final class Samples implements SampleRepository {
        final List<LabeledGlyph> saved = new ArrayList<>();

        @Override
        public void save(LabeledGlyph sample) {
            saved.add(sample);
        }

        @Override
        public List<LabeledGlyph> loadAll() {
            return saved;
        }
    }

    private static final class Page implements RecognizeAndSolveOutputBoundary {
        List<LineResult> lines;

        @Override
        public void prepareSuccessView(RecognizeAndSolveOutputData outputData) {
        }

        @Override
        public void prepareFailView(String recognizedExpression, String errorMessage,
                                    List<SymbolReading> readings) {
        }

        @Override
        public void preparePageView(List<LineResult> page) {
            lines = page;
        }
    }

    /** "1", "+", then a placed "9" and a handwritten "=": one symbol every 100px. */
    private static List<Stroke> handwriting() {
        List<Stroke> strokes = new ArrayList<>();
        for (int x : new int[] {0, 100, 300}) {
            strokes.add(new Stroke(Arrays.asList(new Point(x, 0), new Point(x + 30, 80))));
        }
        return strokes;
    }

    private static List<PlacedSymbol> nine() {
        return Collections.singletonList(new PlacedSymbol('9', Collections.singletonList(
                new Stroke(Arrays.asList(new Point(200, 0), new Point(230, 0),
                        new Point(230, 80), new Point(200, 80), new Point(200, 0))))));
    }

    @Test
    void aPlacedSymbolIsReadAsPickedWithoutRecognisingIt() {
        ScriptedClassifier classifier = new ScriptedClassifier("1+=");
        Page page = new Page();

        new RecognizeAndSolveInteractor(new GlyphSegmenter(), classifier, page)
                .execute(new RecognizeAndSolveInputData(handwriting(), nine(), true));

        LineResult line = page.lines.get(0);
        assertEquals(10.0, line.getSolved().getResult());
        assertEquals('9', line.getReadings().get(2).getSymbol());
        assertTrue(line.getReadings().get(2).isPlaced());
        assertFalse(line.getReadings().get(1).isPlaced());
    }

    @Test
    void correctingAPlacedSymbolTeachesNothing() {
        ScriptedClassifier classifier = new ScriptedClassifier("1+=");
        Samples samples = new Samples();
        Page page = new Page();

        new CorrectSymbolInteractor(new GlyphSegmenter(), classifier, samples, page)
                .execute(new CorrectSymbolInputData(handwriting(), nine(), 2, '8', true));

        assertEquals(9.0, page.lines.get(0).getSolved().getResult());
        assertTrue(samples.saved.isEmpty());
        assertTrue(classifier.learned.isEmpty());
    }

    @Test
    void correctingHandwritingStillTeaches() {
        ScriptedClassifier classifier = new ScriptedClassifier("1+=");
        Samples samples = new Samples();
        Page page = new Page();

        new CorrectSymbolInteractor(new GlyphSegmenter(), classifier, samples, page)
                .execute(new CorrectSymbolInputData(handwriting(), nine(), 0, '7', true));

        assertEquals(16.0, page.lines.get(0).getSolved().getResult());
        assertEquals(1, samples.saved.size());
        assertEquals(Collections.singletonList('7'), classifier.learned);
    }
}
