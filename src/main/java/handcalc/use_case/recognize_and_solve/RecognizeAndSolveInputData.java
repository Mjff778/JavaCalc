package handcalc.use_case.recognize_and_solve;

import handcalc.entity.Stroke;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Everything the user has written on the canvas.
 */
public final class RecognizeAndSolveInputData {

    private final List<Stroke> strokes;

    public RecognizeAndSolveInputData(List<Stroke> strokes) {
        this.strokes = Collections.unmodifiableList(new ArrayList<>(strokes));
    }

    public List<Stroke> getStrokes() {
        return strokes;
    }
}
