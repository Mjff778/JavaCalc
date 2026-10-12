package handcalc.use_case;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import handcalc.entity.Glyph;
import handcalc.entity.Point;
import handcalc.entity.Stroke;
import handcalc.use_case.recognize_and_solve.Candidate;
import handcalc.use_case.recognize_and_solve.GlyphSegmenter;
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
import org.junit.jupiter.api.Test;

class RecognizeAndSolveInteractorTest {

    /** Answers with the given symbols in order of left-to-right glyphs. */
    private static final class ScriptedClassifier implements SymbolClassifier {
        private final String script;
        private int calls;

        ScriptedClassifier(String script) {
            this.script = script;
        }

        @Override
        public List<Candidate> classify(Glyph glyph) {
            char symbol = script.charAt(calls++);
            return Arrays.asList(new Candidate(symbol, 0.9), new Candidate('0', 0.07),
                    new Candidate('1', 0.02), new Candidate('2', 0.01));
        }
    }

    private static final class RecordingPresenter implements RecognizeAndSolveOutputBoundary {
        RecognizeAndSolveOutputData success;
        String failedExpression;
        String failure;
        List<SymbolReading> failedReadings;

        @Override
        public void prepareSuccessView(RecognizeAndSolveOutputData outputData) {
            success = outputData;
        }

        @Override
        public void prepareFailView(String recognizedExpression, String errorMessage,
                                    List<SymbolReading> readings) {
            failedExpression = recognizedExpression;
            failure = errorMessage;
            failedReadings = readings;
        }
    }

    /** One vertical stroke per symbol, one every 100px, drawn right to left. */
    private static List<Stroke> strokesForSymbols(int count) {
        List<Stroke> strokes = new ArrayList<>();
        for (int i = count - 1; i >= 0; i--) {
            strokes.add(new Stroke(Arrays.asList(new Point(i * 100, 0), new Point(i * 100, 80))));
        }
        return strokes;
    }

    private static RecognizeAndSolveInteractor interactor(
            String script, RecordingPresenter presenter) {
        return new RecognizeAndSolveInteractor(
                new GlyphSegmenter(), new ScriptedClassifier(script), presenter);
    }

    @Test
    void recognisedSymbolsAreJoinedLeftToRightAndEvaluated() {
        RecordingPresenter presenter = new RecordingPresenter();
        interactor("12+3", presenter).execute(new RecognizeAndSolveInputData(strokesForSymbols(4)));

        assertEquals("12+3", presenter.success.getExpression());
        assertEquals(15.0, presenter.success.getResult());
        assertNull(presenter.failure);
    }

    @Test
    void operatorPrecedenceIsRespected() {
        RecordingPresenter presenter = new RecordingPresenter();
        interactor("2+3×4", presenter)
                .execute(new RecognizeAndSolveInputData(strokesForSymbols(5)));

        assertEquals(14.0, presenter.success.getResult());
    }

    @Test
    void trailingEqualsSignIsIgnored() {
        RecordingPresenter presenter = new RecordingPresenter();
        interactor("12+3=", presenter)
                .execute(new RecognizeAndSolveInputData(strokesForSymbols(5)));

        assertEquals("12+3", presenter.success.getExpression());
        assertEquals(15.0, presenter.success.getResult());
        assertEquals(5, presenter.success.getReadings().size());
    }

    @Test
    void equalsSignInTheMiddleIsAnError() {
        RecordingPresenter presenter = new RecordingPresenter();
        interactor("1=2", presenter).execute(new RecognizeAndSolveInputData(strokesForSymbols(3)));

        assertEquals("1=2", presenter.failedExpression);
        assertEquals("'=' can only be at the end", presenter.failure);
    }

    @Test
    void readingsKeepEachSymbolWithItsRankedAlternatives() {
        RecordingPresenter presenter = new RecordingPresenter();
        interactor("1+2", presenter).execute(new RecognizeAndSolveInputData(strokesForSymbols(3)));

        List<SymbolReading> readings = presenter.success.getReadings();
        assertEquals(3, readings.size());
        assertEquals('+', readings.get(1).getSymbol());
        assertEquals(4, readings.get(1).getAlternatives().size());
        assertEquals('0', readings.get(1).getAlternatives().get(1).getSymbol());
        assertEquals(100.0, readings.get(1).getGlyph().getMinX());
    }

    @Test
    void emptyCanvasFailsWithoutCallingTheClassifier() {
        RecordingPresenter presenter = new RecordingPresenter();
        interactor("", presenter)
                .execute(new RecognizeAndSolveInputData(Collections.emptyList()));

        assertEquals("Nothing written yet", presenter.failure);
        assertNull(presenter.success);
        assertEquals(0, presenter.failedReadings.size());
    }

    @Test
    void unevaluableExpressionFailsButStillOffersTheReadingsForCorrection() {
        RecordingPresenter presenter = new RecordingPresenter();
        interactor("5÷0", presenter).execute(new RecognizeAndSolveInputData(strokesForSymbols(3)));

        assertEquals("5÷0", presenter.failedExpression);
        assertEquals("Division by zero", presenter.failure);
        assertEquals(3, presenter.failedReadings.size());
        assertNull(presenter.success);
    }

    @Test
    void aBarWithSymbolsAboveAndBelowIsSolvedAsAFraction() {
        RecordingPresenter presenter = new RecordingPresenter();
        List<Stroke> strokes = Arrays.asList(
                new Stroke(Arrays.asList(new Point(0, 100), new Point(100, 100))),
                new Stroke(Arrays.asList(new Point(30, 40), new Point(30, 80))),
                new Stroke(Arrays.asList(new Point(45, 120), new Point(45, 160))));

        interactor("-12", presenter).execute(new RecognizeAndSolveInputData(strokes));

        assertEquals("(1÷2)", presenter.success.getExpression());
        assertEquals(0.5, presenter.success.getResult());
    }

    @Test
    void aTinyMarkAtTheBaselineIsADecimalPointWithoutAskingTheClassifier() {
        RecordingPresenter presenter = new RecordingPresenter();
        List<Stroke> strokes = Arrays.asList(
                new Stroke(Arrays.asList(new Point(0, 0), new Point(0, 40))),
                new Stroke(Collections.singletonList(new Point(18, 38))),
                new Stroke(Arrays.asList(new Point(40, 0), new Point(40, 40))));

        interactor("15", presenter).execute(new RecognizeAndSolveInputData(strokes));

        assertEquals("1.5", presenter.success.getExpression());
        assertEquals(1.5, presenter.success.getResult());
        assertEquals('.', presenter.success.getReadings().get(1).getSymbol());
    }

    @Test
    void aLoneSymbolIsNeverTakenForADecimalPoint() {
        RecordingPresenter presenter = new RecordingPresenter();
        List<Stroke> strokes = Collections.singletonList(
                new Stroke(Arrays.asList(new Point(0, 0), new Point(0, 3))));

        interactor("7", presenter).execute(new RecognizeAndSolveInputData(strokes));

        assertEquals("7", presenter.success.getExpression());
    }

    @Test
    void trigonometryAndPowersAreSolved() {
        RecordingPresenter presenter = new RecordingPresenter();
        List<Stroke> strokes = new ArrayList<>();
        // s i n ( 3 0 )  written as seven separate vertical strokes, then nothing raised
        for (int i = 0; i < 7; i++) {
            strokes.add(new Stroke(Arrays.asList(new Point(i * 60, 0), new Point(i * 60, 40))));
        }

        interactor("sin(30)", presenter).execute(new RecognizeAndSolveInputData(strokes));

        assertEquals("sin(30)", presenter.success.getExpression());
        assertEquals(0.5, presenter.success.getResult(), 1e-9);
    }
}
