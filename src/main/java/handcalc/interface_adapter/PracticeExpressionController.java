package handcalc.interface_adapter;

import handcalc.entity.Stroke;
import handcalc.use_case.practice_expression.PracticeExpressionInputBoundary;
import java.util.List;

/** Passes the collection window's expression practice actions to the use case. */
public class PracticeExpressionController {

    private final PracticeExpressionInputBoundary useCase;

    public PracticeExpressionController(PracticeExpressionInputBoundary useCase) {
        this.useCase = useCase;
    }

    /** Asks for a different expression to write. */
    public void next() {
        useCase.next();
    }

    public void submit(List<Stroke> strokes) {
        useCase.submit(strokes);
    }
}
