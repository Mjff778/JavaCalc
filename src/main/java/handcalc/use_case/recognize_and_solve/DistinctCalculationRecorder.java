package handcalc.use_case.recognize_and_solve;

import handcalc.entity.Stroke;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Passes a calculation on only if it is new: a page is read again every time the user
 * pauses, and an unchanged line must not be added to the history each time.
 *
 * <p>A calculation is the same when it is the same expression made of the very same strokes.
 */
public class DistinctCalculationRecorder implements CalculationRecorder {

    /** How many recent calculations are remembered. */
    private static final int REMEMBERED = 200;

    private final CalculationRecorder next;
    private final Map<StrokesKey, String> recorded =
            new LinkedHashMap<StrokesKey, String>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<StrokesKey, String> eldest) {
                    return size() > REMEMBERED;
                }
            };

    public DistinctCalculationRecorder(CalculationRecorder next) {
        this.next = next;
    }

    @Override
    public void record(String expression, double result, List<Stroke> strokes) {
        StrokesKey key = new StrokesKey(strokes);
        if (!expression.equals(recorded.get(key))) {
            next.record(expression, result, strokes);
            recorded.put(key, expression);
        }
    }

    /** The strokes themselves, not equal-looking ones: writing them again is a new line. */
    private static final class StrokesKey {
        private final List<Stroke> strokes;

        StrokesKey(List<Stroke> strokes) {
            this.strokes = new ArrayList<>(strokes);
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof StrokesKey)) {
                return false;
            }
            List<Stroke> those = ((StrokesKey) other).strokes;
            if (those.size() != strokes.size()) {
                return false;
            }
            for (int i = 0; i < strokes.size(); i++) {
                if (those.get(i) != strokes.get(i)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 1;
            for (Stroke stroke : strokes) {
                hash = 31 * hash + System.identityHashCode(stroke);
            }
            return hash;
        }
    }
}
