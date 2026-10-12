package handcalc.use_case.practice_expression;

import handcalc.entity.Stroke;
import java.util.List;

/** Hands the user a made-up expression to write and turns what they wrote into samples. */
public interface PracticeExpressionInputBoundary {

    /** Shows a new expression to write. */
    void next();

    /** Labels and stores the symbols of what was written for the current expression. */
    void submit(List<Stroke> strokes);
}
