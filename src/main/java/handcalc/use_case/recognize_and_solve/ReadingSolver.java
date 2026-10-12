package handcalc.use_case.recognize_and_solve;

import handcalc.entity.Expression;
import handcalc.entity.InvalidExpressionException;
import handcalc.entity.Stroke;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Turns readings into an expression, evaluates it and reports the outcome.
 * A trailing "=" is accepted and ignored, the way it is written on paper.
 */
public class ReadingSolver {

    private static final char EQUALS = '=';

    private static final Logger LOGGER = Logger.getLogger(ReadingSolver.class.getName());

    private final RecognizeAndSolveOutputBoundary presenter;
    private final CalculationRecorder recorder;

    public ReadingSolver(RecognizeAndSolveOutputBoundary presenter) {
        this(presenter, CalculationRecorder.NONE);
    }

    public ReadingSolver(RecognizeAndSolveOutputBoundary presenter,
                         CalculationRecorder recorder) {
        this.presenter = presenter;
        this.recorder = recorder;
    }

    public void solve(List<SymbolReading> readings) {
        if (readings.isEmpty()) {
            presenter.prepareFailView("", "Nothing written yet", Collections.emptyList());
            return;
        }

        String full = MathLayout.toLinear(readings);
        int end = full.length();
        while (end > 0 && full.charAt(end - 1) == EQUALS) {
            end--;
        }
        String expression = full.substring(0, end);

        if (expression.indexOf(EQUALS) >= 0) {
            presenter.prepareFailView(full, "'=' can only be at the end", readings);
            return;
        }
        try {
            double result = new Expression(expression).evaluate();
            presenter.prepareSuccessView(
                    new RecognizeAndSolveOutputData(expression, result, readings));
            remember(expression, result, readings);
        } catch (InvalidExpressionException e) {
            presenter.prepareFailView(expression, e.getMessage(), readings);
        }
    }

    /** Keeping a history is best effort: a failure must never spoil the answer. */
    private void remember(String expression, double result, List<SymbolReading> readings) {
        List<Stroke> strokes = new ArrayList<>();
        for (SymbolReading reading : readings) {
            strokes.addAll(reading.getGlyph().getStrokes());
        }
        try {
            recorder.record(expression, result, strokes);
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "Could not record the calculation in the history", e);
        }
    }
}
