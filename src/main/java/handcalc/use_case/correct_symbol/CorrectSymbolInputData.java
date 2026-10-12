package handcalc.use_case.correct_symbol;

import handcalc.entity.Stroke;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The user says: in this writing, the {@code glyphIndex}-th symbol (from the left) is
 * actually {@code symbol}.
 */
public final class CorrectSymbolInputData {

    private final List<Stroke> strokes;
    private final int glyphIndex;
    private final char symbol;

    public CorrectSymbolInputData(List<Stroke> strokes, int glyphIndex, char symbol) {
        this.strokes = Collections.unmodifiableList(new ArrayList<>(strokes));
        this.glyphIndex = glyphIndex;
        this.symbol = symbol;
    }

    public List<Stroke> getStrokes() {
        return strokes;
    }

    public int getGlyphIndex() {
        return glyphIndex;
    }

    public char getSymbol() {
        return symbol;
    }
}
