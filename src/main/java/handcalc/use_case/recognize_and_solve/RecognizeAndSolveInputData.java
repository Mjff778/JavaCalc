package handcalc.use_case.recognize_and_solve;

import handcalc.entity.PlacedSymbol;
import handcalc.entity.Stroke;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Everything the user has written on the canvas, with the symbols they picked in place of
 * their writing, and whether it is one expression to solve now, or a page of lines each to be
 * solved once it ends in "=".
 */
public final class RecognizeAndSolveInputData {

    private final List<Stroke> strokes;
    private final List<PlacedSymbol> placed;
    private final boolean waitForEquals;

    public RecognizeAndSolveInputData(List<Stroke> strokes) {
        this(strokes, false);
    }

    /** @param waitForEquals true for a page whose lines are solved once they end in "=" */
    public RecognizeAndSolveInputData(List<Stroke> strokes, boolean waitForEquals) {
        this(strokes, Collections.emptyList(), waitForEquals);
    }

    /** @param placed symbols the user picked, read as picked */
    public RecognizeAndSolveInputData(List<Stroke> strokes, List<PlacedSymbol> placed,
                                      boolean waitForEquals) {
        this.strokes = Collections.unmodifiableList(new ArrayList<>(strokes));
        this.placed = Collections.unmodifiableList(new ArrayList<>(placed));
        this.waitForEquals = waitForEquals;
    }

    public List<PlacedSymbol> getPlaced() {
        return placed;
    }

    public List<Stroke> getStrokes() {
        return strokes;
    }

    public boolean isWaitForEquals() {
        return waitForEquals;
    }
}
