package handcalc.use_case.collect_sample;

import handcalc.entity.Stroke;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The strokes on the collection canvas and the symbol the user was asked to write.
 */
public final class CollectSampleInputData {

    private final List<Stroke> strokes;
    private final char symbol;

    public CollectSampleInputData(List<Stroke> strokes, char symbol) {
        this.strokes = Collections.unmodifiableList(new ArrayList<>(strokes));
        this.symbol = symbol;
    }

    public List<Stroke> getStrokes() {
        return strokes;
    }

    public char getSymbol() {
        return symbol;
    }
}
