package handcalc.interface_adapter;

import handcalc.entity.PlacedSymbol;
import handcalc.entity.Stroke;
import handcalc.use_case.recognize_and_solve.RecognizeAndSolveInputBoundary;
import handcalc.use_case.recognize_and_solve.RecognizeAndSolveInputData;
import java.util.List;

/**
 * Turns the user's writing into a use case call.
 */
public class RecognizeAndSolveController {

    private final RecognizeAndSolveInputBoundary useCase;

    public RecognizeAndSolveController(RecognizeAndSolveInputBoundary useCase) {
        this.useCase = useCase;
    }

    public void execute(List<Stroke> strokes) {
        useCase.execute(new RecognizeAndSolveInputData(strokes));
    }

    /** Reads the writing, and solves it if it ends in "=". */
    public void executeWhenFinished(List<Stroke> strokes) {
        useCase.execute(new RecognizeAndSolveInputData(strokes, true));
    }

    /** Reads the page, with the symbols the user picked, and solves each finished line. */
    public void executeWhenFinished(List<Stroke> strokes, List<PlacedSymbol> placed) {
        useCase.execute(new RecognizeAndSolveInputData(strokes, placed, true));
    }
}
