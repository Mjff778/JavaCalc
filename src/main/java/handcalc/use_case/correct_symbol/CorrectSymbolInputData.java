package handcalc.use_case.correct_symbol;

import handcalc.entity.PlacedSymbol;
import handcalc.entity.Stroke;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The user says: in this writing, the {@code glyphIndex}-th symbol (from the left) is
 * actually {@code symbol}. If {@code waitForEquals} is set, the writing is a page: symbols are
 * counted line by line from the top, and each line is solved once it ends in "=".
 */
public final class CorrectSymbolInputData {

    private final List<Stroke> strokes;
    private final int glyphIndex;
    private final char symbol;
    private final boolean waitForEquals;
    private final List<PlacedSymbol> placed;

    public CorrectSymbolInputData(List<Stroke> strokes, int glyphIndex, char symbol) {
        this(strokes, glyphIndex, symbol, false);
    }

    public CorrectSymbolInputData(List<Stroke> strokes, int glyphIndex, char symbol,
                                  boolean waitForEquals) {
        this(strokes, Collections.emptyList(), glyphIndex, symbol, waitForEquals);
    }

    /** @param placed symbols the user picked earlier, read as picked */
    public CorrectSymbolInputData(List<Stroke> strokes, List<PlacedSymbol> placed,
                                  int glyphIndex, char symbol, boolean waitForEquals) {
        this.placed = Collections.unmodifiableList(new ArrayList<>(placed));
        this.strokes = Collections.unmodifiableList(new ArrayList<>(strokes));
        this.glyphIndex = glyphIndex;
        this.symbol = symbol;
        this.waitForEquals = waitForEquals;
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

    public List<PlacedSymbol> getPlaced() {
        return placed;
    }

    public boolean isWaitForEquals() {
        return waitForEquals;
    }
}
