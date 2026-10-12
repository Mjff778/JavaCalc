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

    /** Like the other fail view, with the symbols at fault and where an answer would go. */
    default void prepareFailView(RecognizeAndSolveFailureData failure) {
        prepareFailView(failure.getExpression(), failure.getMessage(), failure.getReadings());
    }

    /**
     * The writing was read but not solved, because it does not end in "=" yet.
     *
     * @param readings how each symbol was read, so the user can correct a misreading
     */
    default void prepareUnfinishedView(List<SymbolReading> readings) {
    }

    /**
     * A whole page was read line by line; each line that ends in "=" was solved.
     *
     * @param lines what became of each line, from the top line down
     */
    default void preparePageView(List<LineResult> lines) {
    }
}
