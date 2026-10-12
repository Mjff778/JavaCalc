package handcalc.interface_adapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * What the calculator screen shows: the recognized expression, either a result or an error,
 * and how each written symbol was read.
 */
public final class CalculatorState {

    private final String expression;
    private final String result;
    private final String error;
    private final List<ReadSymbol> symbols;

    private CalculatorState(String expression, String result, String error,
                            List<ReadSymbol> symbols) {
        this.expression = expression;
        this.result = result;
        this.error = error;
        this.symbols = Collections.unmodifiableList(new ArrayList<>(symbols));
    }

    public static CalculatorState empty() {
        return new CalculatorState("", null, null, Collections.emptyList());
    }

    public static CalculatorState solved(String expression, String result,
                                         List<ReadSymbol> symbols) {
        return new CalculatorState(expression, result, null, symbols);
    }

    public static CalculatorState failed(String expression, String error,
                                         List<ReadSymbol> symbols) {
        return new CalculatorState(expression, null, error, symbols);
    }

    public String getExpression() {
        return expression;
    }

    /** @return the formatted result, or null if there is none. */
    public String getResult() {
        return result;
    }

    /** @return the error message, or null if there is none. */
    public String getError() {
        return error;
    }

    public List<ReadSymbol> getSymbols() {
        return symbols;
    }
}
