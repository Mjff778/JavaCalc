package handcalc.interface_adapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * What the calculator screen shows: the recognized expression, either a result or an error,
 * how each written symbol was read, and the text written on the page after the "=". A page of
 * several lines is shown line by line.
 */
public final class CalculatorState {

    private final String expression;
    private final String result;
    private final String error;
    private final List<ReadSymbol> symbols;
    private final WrittenAnswer answer;
    private final List<LineView> lines;

    private CalculatorState(String expression, String result, String error,
                            List<ReadSymbol> symbols, WrittenAnswer answer) {
        this(expression, result, error, symbols, answer, Collections.emptyList());
    }

    private CalculatorState(String expression, String result, String error,
                            List<ReadSymbol> symbols, WrittenAnswer answer,
                            List<LineView> lines) {
        this.expression = expression;
        this.result = result;
        this.error = error;
        this.symbols = Collections.unmodifiableList(new ArrayList<>(symbols));
        this.answer = answer;
        this.lines = Collections.unmodifiableList(new ArrayList<>(lines));
    }

    public static CalculatorState empty() {
        return new CalculatorState("", null, null, Collections.emptyList(), null);
    }

    /** A page read line by line; its symbols are those of every line, from the top down. */
    public static CalculatorState page(List<LineView> lines) {
        List<ReadSymbol> symbols = new ArrayList<>();
        for (LineView line : lines) {
            symbols.addAll(line.getSymbols());
        }
        return new CalculatorState("", null, null, symbols, null, lines);
    }

    /** The writing was read, but there is nothing to solve yet. */
    public static CalculatorState read(List<ReadSymbol> symbols) {
        return new CalculatorState("", null, null, symbols, null);
    }

    public static CalculatorState solved(String expression, String result,
                                         List<ReadSymbol> symbols) {
        return solved(expression, result, symbols, null);
    }

    public static CalculatorState solved(String expression, String result,
                                         List<ReadSymbol> symbols, WrittenAnswer answer) {
        return new CalculatorState(expression, result, null, symbols, answer);
    }

    public static CalculatorState failed(String expression, String error,
                                         List<ReadSymbol> symbols) {
        return failed(expression, error, symbols, null);
    }

    public static CalculatorState failed(String expression, String error,
                                         List<ReadSymbol> symbols, WrittenAnswer answer) {
        return new CalculatorState(expression, null, error, symbols, answer);
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

    /** @return the text to write on the page after the "=", or null if there is none. */
    public WrittenAnswer getAnswer() {
        return answer;
    }

    /** @return the lines of a page, from the top down; empty if this is not a page. */
    public List<LineView> getLines() {
        return lines;
    }
}
