package handcalc.use_case.recognize_and_solve;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The recognized expression text, its value, and how each symbol was read.
 */
public final class RecognizeAndSolveOutputData {

    private final String expression;
    private final double result;
    private final List<SymbolReading> readings;

    public RecognizeAndSolveOutputData(String expression, double result,
                                       List<SymbolReading> readings) {
        this.expression = expression;
        this.result = result;
        this.readings = Collections.unmodifiableList(new ArrayList<>(readings));
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
}
