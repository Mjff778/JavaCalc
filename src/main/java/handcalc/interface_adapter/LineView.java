package handcalc.interface_adapter;

import handcalc.entity.Formula;
import handcalc.entity.Stroke;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One line of writing as the screen shows it: its symbols, the text written after its "=",
 * the calculation typeset for checking, and the strokes the line is made of.
 */
public final class LineView {

    private final List<ReadSymbol> symbols;
    private final WrittenAnswer answer;
    private final Formula formula;
    private final List<Stroke> strokes;
    private final boolean finished;

    /**
     * @param answer the text after the "=", or null if there is none
     * @param formula the calculation typeset, or null if there is none
     * @param finished true if the line ends in "=" and was solved or failed
     */
    public LineView(List<ReadSymbol> symbols, WrittenAnswer answer, Formula formula,
                    List<Stroke> strokes, boolean finished) {
        this.symbols = Collections.unmodifiableList(new ArrayList<>(symbols));
        this.answer = answer;
        this.formula = formula;
        this.strokes = Collections.unmodifiableList(new ArrayList<>(strokes));
        this.finished = finished;
    }

    public List<ReadSymbol> getSymbols() {
        return symbols;
    }

    /** @return the text to write after the line's "=", or null if there is none. */
    public WrittenAnswer getAnswer() {
        return answer;
    }

    /** @return the calculation typeset as it was done, or null if there is none. */
    public Formula getFormula() {
        return formula;
    }

    /** @return the strokes this line is made of */
    public List<Stroke> getStrokes() {
        return strokes;
    }

    public boolean isFinished() {
        return finished;
    }
}
