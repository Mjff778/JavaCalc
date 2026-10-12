package handcalc.use_case;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import handcalc.entity.Glyph;
import handcalc.entity.Point;
import handcalc.entity.Stroke;
import handcalc.use_case.recognize_and_solve.AnswerPlacement;
import handcalc.use_case.recognize_and_solve.Candidate;
import handcalc.use_case.recognize_and_solve.GlyphSegmenter;
import handcalc.use_case.recognize_and_solve.LineResult;
import handcalc.use_case.recognize_and_solve.RecognizeAndSolveFailureData;
import handcalc.use_case.recognize_and_solve.RecognizeAndSolveInputData;
import handcalc.use_case.recognize_and_solve.RecognizeAndSolveInteractor;
import handcalc.use_case.recognize_and_solve.RecognizeAndSolveOutputBoundary;
import handcalc.use_case.recognize_and_solve.RecognizeAndSolveOutputData;
import handcalc.use_case.recognize_and_solve.SymbolClassifier;
import handcalc.use_case.recognize_and_solve.SymbolReading;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

/**
 * Solving once the writing ends in "=": what is reported before that, which symbols are
 * blamed for a wrongly written expression, and where the answer goes.
 */
class WaitForEqualsTest {

    /** Reads the glyphs, left to right, as the characters of the script. */
    private static final class ScriptedClassifier implements SymbolClassifier {
        private final String script;
        private int calls;

        ScriptedClassifier(String script) {
            this.script = script;
        }

        @Override
        public List<Candidate> classify(Glyph glyph) {
            return Collections.singletonList(new Candidate(script.charAt(calls++), 0.9));
        }
    }

    /** Keeps the first line of the page; the page itself is in {@code lines}. */
    private static final class RecordingPresenter implements RecognizeAndSolveOutputBoundary {
        List<LineResult> lines;
        RecognizeAndSolveOutputData success;
        RecognizeAndSolveFailureData failure;
        List<SymbolReading> unfinished;

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
            LineResult first = page.get(0);
            success = first.getSolved();
            failure = first.getFailure();
            unfinished = first.isFinished() ? null : first.getReadings();
        }
    }

    /** One 80px tall stroke per symbol, 100px apart (so 70px gaps between 30px wide boxes). */
    private static List<Stroke> strokes(int count) {
        List<Stroke> strokes = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            strokes.add(new Stroke(Arrays.asList(
                    new Point(i * 100, 0), new Point(i * 100 + 30, 80))));
        }
        return strokes;
    }

    private static RecordingPresenter solve(String script) {
        RecordingPresenter presenter = new RecordingPresenter();
        new RecognizeAndSolveInteractor(new GlyphSegmenter(), new ScriptedClassifier(script),
                presenter).execute(new RecognizeAndSolveInputData(strokes(script.length()), true));
        return presenter;
    }

    private static Set<Integer> indices(Integer... values) {
        return new TreeSet<>(Arrays.asList(values));
    }

    @Test
    void writingThatDoesNotEndInEqualsIsOnlyRead() {
        RecordingPresenter presenter = solve("12+3");

        assertEquals(4, presenter.unfinished.size());
        assertNull(presenter.success);
        assertNull(presenter.failure);
    }

    /** Two lines, the second 200px below the first; each 80px tall. */
    private static RecordingPresenter solvePage(String top, String bottom) {
        List<Stroke> strokes = new ArrayList<>(strokes(top.length()));
        for (Stroke stroke : strokes(bottom.length())) {
            List<Point> lower = new ArrayList<>();
            for (Point p : stroke.getPoints()) {
                lower.add(new Point(p.getX(), p.getY() + 200));
            }
            strokes.add(new Stroke(lower));
        }
        RecordingPresenter presenter = new RecordingPresenter();
        new RecognizeAndSolveInteractor(new GlyphSegmenter(),
                new ScriptedClassifier(top + bottom), presenter)
                .execute(new RecognizeAndSolveInputData(strokes, true));
        return presenter;
    }

    @Test
    void eachLineOnThePageIsSolvedOnItsOwn() {
        RecordingPresenter presenter = solvePage("1+2=", "3×4=");

        assertEquals(2, presenter.lines.size());
        assertEquals(3.0, presenter.lines.get(0).getSolved().getResult());
        assertEquals(12.0, presenter.lines.get(1).getSolved().getResult());
        assertEquals(240, presenter.lines.get(1).getSolved().getPlacement().getCentreY(), 1e-9);
    }

    @Test
    void aLineStillBeingWrittenDoesNotStopTheOthers() {
        RecordingPresenter presenter = solvePage("1+2=", "3×");

        assertEquals(3.0, presenter.lines.get(0).getSolved().getResult());
        assertFalse(presenter.lines.get(1).isFinished());
    }

    @Test
    void theCalculationIsTypesetForChecking() {
        assertEquals("12+3", solve("12+3=").success.getFormula().toString());
    }

    @Test
    void anUndefinedCalculationIsTypesetToo() {
        assertEquals("5÷0", solve("5÷0=").failure.getFormula().toString());
    }

    @Test
    void aLoneEqualsSignIsNothingToSolveYet() {
        RecordingPresenter presenter = solve("=");

        assertNotNull(presenter.unfinished);
        assertNull(presenter.failure);
    }

    @Test
    void writingThatEndsInEqualsIsSolvedAndTheAnswerPlacedAfterIt() {
        RecordingPresenter presenter = solve("12+3=");

        assertEquals(15.0, presenter.success.getResult());
        AnswerPlacement placement = presenter.success.getPlacement();
        // the 70px gaps are wider than the widest gap kept, 0.8 of the 80px digit height
        assertEquals(430 + 64, placement.getX(), 1e-9);
        assertEquals(40, placement.getCentreY(), 1e-9);
        assertEquals(80, placement.getHeight(), 1e-9);
    }

    @Test
    void anUnfinishedExpressionBlamesItsLastSymbol() {
        RecordingPresenter presenter = solve("12+=");

        assertEquals(indices(2), presenter.failure.getFaultySymbols());
        assertFalse(presenter.failure.isUndefined());
    }

    @Test
    void anUnclosedBracketIsBlamed() {
        assertEquals(indices(0), solve("(3+4=").failure.getFaultySymbols());
    }

    @Test
    void aStrayClosingBracketIsBlamed() {
        assertEquals(indices(3), solve("3+4)=").failure.getFaultySymbols());
    }

    @Test
    void twoOperatorsInARowBlameTheSecond() {
        assertEquals(indices(2), solve("5+×3=").failure.getFaultySymbols());
    }

    @Test
    void anEqualsSignInTheMiddleIsBlamed() {
        assertEquals(indices(1), solve("1=2=").failure.getFaultySymbols());
    }

    @Test
    void aCorrectlyWrittenExpressionWithNoValueBlamesNoSymbol() {
        RecordingPresenter presenter = solve("5÷0=");

        assertTrue(presenter.failure.isUndefined());
        assertTrue(presenter.failure.getFaultySymbols().isEmpty());
        assertNotNull(presenter.failure.getPlacement());
    }
}
