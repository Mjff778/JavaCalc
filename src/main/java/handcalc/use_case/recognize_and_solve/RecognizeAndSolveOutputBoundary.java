package handcalc.use_case.recognize_and_solve;

import java.util.List;

/**
 * Implemented by the presenter so the use case can report back without knowing about the UI.
 */
public interface RecognizeAndSolveOutputBoundary {

    void prepareSuccessView(RecognizeAndSolveOutputData outputData);

    /**
     * @param recognizedExpression what was read from the canvas, or an empty string if nothing
     * @param errorMessage why it could not be solved
     * @param readings how each symbol was read, so the user can correct a misreading
     */
    void prepareFailView(String recognizedExpression, String errorMessage,
                         List<SymbolReading> readings);
}
