package handcalc.use_case;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import handcalc.entity.Glyph;
import handcalc.entity.Point;
import handcalc.entity.Stroke;
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
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

/**
 * {@code ans} on a page of working: the answer of the nearest finished line above.
 */
class AnsTest {

    private static final String NO_ANSWER = "ans needs a solved line above it";

    /** Reads the glyphs, in the order they are read, as the characters of the script. */
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

    private static final class RecordingPresenter implements RecognizeAndSolveOutputBoundary {
        List<LineResult> lines;
        RecognizeAndSolveFailureData failure;

        @Override
        public void prepareSuccessView(RecognizeAndSolveOutputData outputData) {
        }

        @Override
        public void prepareFailView(RecognizeAndSolveFailureData failureData) {
            failure = failureData;
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

    private final RecordingPresenter presenter = new RecordingPresenter();
    private final List<String> recorded = new ArrayList<>();

    /** One 80px tall stroke per symbol, 100px apart, and each line 200px below the last. */
    private RecordingPresenter solvePage(String... lines) {
        List<Stroke> strokes = new ArrayList<>();
        for (int line = 0; line < lines.length; line++) {
            for (int i = 0; i < lines[line].length(); i++) {
                int y = line * 200;
                strokes.add(new Stroke(Arrays.asList(
                        new Point(i * 100, y), new Point(i * 100 + 30, y + 80))));
            }
        }
        new RecognizeAndSolveInteractor(new GlyphSegmenter(),
                new ScriptedClassifier(String.join("", lines)), presenter,
                (expression, result, written) -> recorded.add(expression))
                .execute(new RecognizeAndSolveInputData(strokes, true));
        return presenter;
    }

    private double answer(int line) {
        return presenter.lines.get(line).getSolved().getResult();
    }

    private RecognizeAndSolveFailureData failure(int line) {
        return presenter.lines.get(line).getFailure();
    }

    @Test
    void ansIsTheAnswerOfTheLineAbove() {
        solvePage("12×3=", "ans÷4=");

        assertEquals(9.0, answer(1));
    }

    @Test
    void ansCanBeChainedDownThePage() {
        solvePage("12×3=", "ans÷4=", "ans+1=");

        assertEquals(10.0, answer(2));
    }

    @Test
    void aLineStillBeingWrittenIsPassedOver() {
        solvePage("2+2=", "7×", "ans×2=");

        assertFalse(presenter.lines.get(1).isFinished());
        assertEquals(8.0, answer(2));
    }

    @Test
    void ansOnTheFirstLineHasNoAnswerToUse() {
        solvePage("ans+1=");

        assertEquals(NO_ANSWER, failure(0).getMessage());
        assertEquals(new TreeSet<>(Arrays.asList(0, 1, 2)), failure(0).getFaultySymbols());
        assertFalse(failure(0).isUndefined());
    }

    @Test
    void ansBelowAFailedLineDoesNotReachFurtherUp() {
        solvePage("1+1=", "5÷0=", "ans+1=");

        assertEquals(NO_ANSWER, failure(2).getMessage());
    }

    @Test
    void aSingleLineHasNoAnswerAbove() {
        new RecognizeAndSolveInteractor(new GlyphSegmenter(), new ScriptedClassifier("ans"),
                presenter).execute(new RecognizeAndSolveInputData(Arrays.asList(
                        new Stroke(Arrays.asList(new Point(0, 0), new Point(30, 80))),
                        new Stroke(Arrays.asList(new Point(100, 0), new Point(130, 80))),
                        new Stroke(Arrays.asList(new Point(200, 0), new Point(230, 80)))),
                false));

        assertEquals(NO_ANSWER, presenter.failure.getMessage());
        assertNull(presenter.lines);
    }

    @Test
    void theHistoryHasTheAnswerWrittenOut() {
        solvePage("12×3=", "ans÷4=");

        assertEquals(Arrays.asList("12×3", "36÷4"), recorded);
    }

    @Test
    void anAnswerWrittenOutIsBracketedWhereItWouldOtherwiseBeMisread() {
        solvePage("2-5=", "2ans=", "ans^2=", "2+ans=");

        assertEquals(Arrays.asList("2-5", "2(-3)", "(-6)^2", "2+36"), recorded);
    }

    @Test
    void anAnswerNextToADigitIsBracketed() {
        solvePage("1+2=", "2ans=");

        assertEquals("2(3)", recorded.get(1));
    }

    @Test
    void theCalculationIsTypesetWithAnsAsWritten() {
        solvePage("1+2=", "ans×2=");

        assertEquals("ans×2", presenter.lines.get(1).getSolved().getFormula().toString());
    }
}
