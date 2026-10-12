package handcalc.use_case.recognize_and_solve;

import handcalc.entity.Glyph;
import java.util.List;

/**
 * Reads one written symbol. Implemented by whatever recognition model the application uses,
 * so the use case does not depend on how recognition is done.
 */
public interface SymbolClassifier {

    /**
     * @return every known symbol, most likely first, with confidences that sum to 1
     */
    List<Candidate> classify(Glyph glyph);
}
