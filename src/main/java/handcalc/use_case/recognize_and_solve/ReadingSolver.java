package handcalc.use_case.recognize_and_solve;

import handcalc.entity.Expression;
import handcalc.entity.Formula;
import handcalc.entity.InvalidExpressionException;
import handcalc.entity.Stroke;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Turns readings into an expression, evaluates it and reports the outcome, for one line of
 * writing or for every line on a page.
 * A trailing "=" is accepted and ignored, the way it is written on paper. When the expression
 * is written wrongly, the symbols at fault are reported with it. On a page, {@code ans} is the
 * answer of the nearest line above that ends in "=".
 */
public class ReadingSolver {

    private static final char EQUALS = '=';
    private static final String NOTHING_WRITTEN = "Nothing written yet";

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
        solve(readings, false);
    }

    /** @param waitForEquals true to only report the readings until the writing ends in "=" */
    public void solve(List<SymbolReading> readings, boolean waitForEquals) {
        LineResult line = evaluate(readings, waitForEquals, null);
        if (line.getSolved() != null) {
            presenter.prepareSuccessView(line.getSolved());
            remember(line.getSolved(), null);
        } else if (line.getFailure() == null) {
            presenter.prepareUnfinishedView(readings);
        } else if (readings.isEmpty()) {
            presenter.prepareFailView("", NOTHING_WRITTEN, Collections.emptyList());
        } else {
            presenter.prepareFailView(line.getFailure());
        }
    }

    /** Solves every line that ends in "=" and reports all of them, from the top line down. */
    public void solvePage(List<List<SymbolReading>> lines) {
        List<LineResult> results = new ArrayList<>();
        Double ans = null;
        for (List<SymbolReading> line : lines) {
            LineResult result = evaluate(line, true, ans);
            results.add(result);
            if (result.getSolved() != null) {
                remember(result.getSolved(), ans);
                ans = result.getSolved().getResult();
            } else if (result.isFinished()) {
                // a line that failed has no answer, and ans does not reach past it
                ans = null;
            }
        }
        presenter.preparePageView(results);
    }

    /** @param ans what {@code ans} stands for, or null if there is no answer to use */
    private LineResult evaluate(List<SymbolReading> readings, boolean waitForEquals,
                                Double ans) {
        MathLayout.Line line = readings.isEmpty() ? null : MathLayout.layOut(readings);
        String full = line == null ? "" : line.getText();
        int end = full.length();
        while (end > 0 && full.charAt(end - 1) == EQUALS) {
            end--;
        }
        if (waitForEquals && (end == full.length() || end == 0)) {
            return LineResult.unfinished(readings);
        }
        if (readings.isEmpty()) {
            return LineResult.failed(new RecognizeAndSolveFailureData("", NOTHING_WRITTEN,
                    readings, Collections.emptySet(), false, null));
        }
        String expression = full.substring(0, end);
        AnswerPlacement placement = AnswerPlacement.after(readings);

        Set<Integer> misplacedEquals = new TreeSet<>();
        for (int i = 0; i < end; i++) {
            if (full.charAt(i) == EQUALS) {
                misplacedEquals.addAll(line.readingsIn(i, i + 1));
            }
        }
        if (!misplacedEquals.isEmpty()) {
            return LineResult.failed(new RecognizeAndSolveFailureData(full,
                    "'=' can only be at the end", readings, misplacedEquals, false, placement));
        }
        try {
            double result = new Expression(expression, ans).evaluate();
            return LineResult.solved(new RecognizeAndSolveOutputData(expression, result,
                    readings, placement, typeset(line, readings, end)));
        } catch (InvalidExpressionException e) {
            Set<Integer> faulty = e.hasPosition()
                    ? line.readingsIn(e.getStart(), e.getEnd()) : Collections.emptySet();
            Formula formula = e.isUndefined() ? typeset(line, readings, end) : null;
            return LineResult.failed(new RecognizeAndSolveFailureData(expression,
                    e.getMessage(), readings, faulty, e.isUndefined(), placement, formula));
        }
    }

    /** Typesetting is only for showing the user: a failure must never spoil the answer. */
    private static Formula typeset(MathLayout.Line line, List<SymbolReading> readings, int end) {
        try {
            return FormulaBuilder.build(line, readings, end);
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "Could not typeset " + line.getText(), e);
            return null;
        }
    }

    /**
     * Keeping a history is best effort: a failure must never spoil the answer. An entry must
     * make sense on its own, so {@code ans} is written out as the number it stood for.
     */
    private void remember(RecognizeAndSolveOutputData solved, Double ans) {
        List<Stroke> strokes = new ArrayList<>();
        for (SymbolReading reading : solved.getReadings()) {
            strokes.addAll(reading.getGlyph().getStrokes());
        }
        try {
            String expression = ans == null ? solved.getExpression()
                    : new Expression(solved.getExpression()).withAnsWrittenAs(
                            ResultFormat.format(ans));
            recorder.record(expression, solved.getResult(), strokes);
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "Could not record the calculation in the history", e);
        }
    }
}
