package handcalc.use_case.recognize_and_solve;

import handcalc.entity.Formula;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Why the written expression could not be solved: either it is written wrongly, and these
 * symbols are at fault, or it is written correctly but its value is undefined.
 */
public final class RecognizeAndSolveFailureData {

    private final String expression;
    private final String message;
    private final List<SymbolReading> readings;
    private final Set<Integer> faultySymbols;
    private final boolean undefined;
    private final AnswerPlacement placement;
    private final Formula formula;

    /**
     * @param faultySymbols indices into {@code readings} of the symbols that are at fault
     * @param undefined true if the expression is written correctly but has no value
     * @param placement where an answer would go; null if nothing was written
     */
    public RecognizeAndSolveFailureData(String expression, String message,
                                        List<SymbolReading> readings, Set<Integer> faultySymbols,
                                        boolean undefined, AnswerPlacement placement) {
        this(expression, message, readings, faultySymbols, undefined, placement, null);
    }

    /** @param formula the expression typeset, when it is written correctly; else null */
    public RecognizeAndSolveFailureData(String expression, String message,
                                        List<SymbolReading> readings, Set<Integer> faultySymbols,
                                        boolean undefined, AnswerPlacement placement,
                                        Formula formula) {
        this.expression = expression;
        this.message = message;
        this.readings = Collections.unmodifiableList(new ArrayList<>(readings));
        this.faultySymbols = Collections.unmodifiableSet(new TreeSet<>(faultySymbols));
        this.undefined = undefined;
        this.placement = placement;
        this.formula = formula;
    }

    public String getExpression() {
        return expression;
    }

    public String getMessage() {
        return message;
    }

    public List<SymbolReading> getReadings() {
        return readings;
    }

    /** @return the indices of the readings at fault; empty if no symbol is to blame. */
    public Set<Integer> getFaultySymbols() {
        return faultySymbols;
    }

    /** @return true if the expression is written correctly but has no value. */
    public boolean isUndefined() {
        return undefined;
    }

    /** @return where an answer would go; null if nothing was written. */
    public AnswerPlacement getPlacement() {
        return placement;
    }

    /** @return the expression typeset, if it is written correctly; else null. */
    public Formula getFormula() {
        return formula;
    }
}
