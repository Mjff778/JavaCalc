package handcalc.use_case;

import static org.junit.jupiter.api.Assertions.assertEquals;

import handcalc.entity.Point;
import handcalc.entity.Stroke;
import handcalc.use_case.recognize_and_solve.DistinctCalculationRecorder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class DistinctCalculationRecorderTest {

    private final List<String> recorded = new ArrayList<>();
    private final DistinctCalculationRecorder recorder =
            new DistinctCalculationRecorder((expression, result, strokes) -> recorded.add(expression));

    private static List<Stroke> strokes() {
        return Collections.singletonList(
                new Stroke(Arrays.asList(new Point(0, 0), new Point(10, 10))));
    }

    @Test
    void theSameLineReadAgainIsRecordedOnce() {
        List<Stroke> line = strokes();

        recorder.record("1+1", 2, line);
        recorder.record("1+1", 2, new ArrayList<>(line));

        assertEquals(Collections.singletonList("1+1"), recorded);
    }

    @Test
    void aCorrectedReadingOfTheSameLineIsRecorded() {
        List<Stroke> line = strokes();

        recorder.record("1+1", 2, line);
        recorder.record("1×1", 1, line);

        assertEquals(Arrays.asList("1+1", "1×1"), recorded);
    }

    @Test
    void theSameSumWrittenAgainIsRecordedAgain() {
        recorder.record("1+1", 2, strokes());
        recorder.record("1+1", 2, strokes());

        assertEquals(2, recorded.size());
    }
}
