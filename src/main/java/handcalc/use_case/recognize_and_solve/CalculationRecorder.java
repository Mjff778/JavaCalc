package handcalc.use_case.recognize_and_solve;

import handcalc.entity.Stroke;
import java.util.List;

/**
 * Told about every calculation that was solved successfully.
 */
public interface CalculationRecorder {

    CalculationRecorder NONE = (expression, result, strokes) -> { };

    void record(String expression, double result, List<Stroke> strokes);
}
