package handcalc.use_case.practice_expression;

/** Presents the expression to write and what became of the last attempt. */
public interface PracticeExpressionOutputBoundary {

    /**
     * @param prompt the expression to write now
     * @param savedCount how many samples the last submission stored (0 if none)
     */
    void prepareView(ExpressionPrompt prompt, int savedCount);

    /** The last attempt could not be used; the same expression stays up. */
    void prepareFailView(String message);
}
