package handcalc.interface_adapter;

import handcalc.entity.Stroke;
import handcalc.use_case.recognize_and_solve.RecognizeAndSolveInputBoundary;
import handcalc.use_case.recognize_and_solve.RecognizeAndSolveInputData;
import java.util.List;

/**
 * Turns the user's "calculate" action into a use case call.
 */
public class RecognizeAndSolveController {

    private final RecognizeAndSolveInputBoundary useCase;

    public RecognizeAndSolveController(RecognizeAndSolveInputBoundary useCase) {
        this.useCase = useCase;
    }

    public void execute(List<Stroke> strokes) {
        useCase.execute(new RecognizeAndSolveInputData(strokes));
    }
}
