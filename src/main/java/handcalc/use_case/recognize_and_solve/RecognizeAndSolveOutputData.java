package handcalc.use_case.recognize_and_solve;

import handcalc.entity.Formula;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The recognized expression text, its value, how each symbol was read, where on the page
 * the answer goes, and the expression typeset as it was calculated.
 */
public final class RecognizeAndSolveOutputData {

    private final String expression;
    private final double result;
    private final List<SymbolReading> readings;
    private final AnswerPlacement placement;
    private final Formula formula;

    public RecognizeAndSolveOutputData(String expression, double result,
                                       List<SymbolReading> readings) {
        this(expression, result, readings, null, null);
    }

    /**
     * @param placement where the answer goes; null if not known
     * @param formula the expression as it was calculated; null if not known
     */
    public RecognizeAndSolveOutputData(String expression, double result,
                                       List<SymbolReading> readings, AnswerPlacement placement,
                                       Formula formula) {
        this.expression = expression;
        this.result = result;
        this.readings = Collections.unmodifiableList(new ArrayList<>(readings));
        this.placement = placement;
        this.formula = formula;
    }

    public String getExpression() {
        return expression;
    }

    public double getResult() {
        return result;
    }

    public List<SymbolReading> getReadings() {
        return readings;
    }

    /** @return where the answer goes on the page; null if not known. */
    public AnswerPlacement getPlacement() {
        return placement;
    }

    /** @return the expression typeset as it was calculated; null if not known. */
    public Formula getFormula() {
        return formula;
    }
}
