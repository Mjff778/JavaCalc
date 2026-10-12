package handcalc.interface_adapter;

import handcalc.use_case.practice_expression.ExpressionPrompt;
import handcalc.use_case.practice_expression.PracticeExpressionOutputBoundary;

/** Turns the practice use case's output into state for the collection window. */
public class PracticeExpressionPresenter implements PracticeExpressionOutputBoundary {

    private final PracticeExpressionViewModel viewModel;

    public PracticeExpressionPresenter(PracticeExpressionViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @Override
    public void prepareView(ExpressionPrompt prompt, int savedCount) {
        String message = savedCount == 0 ? ""
                : "Saved " + savedCount + (savedCount == 1 ? " sample" : " samples");
        viewModel.setState(viewModel.getState().withPrompt(layoutOf(prompt),
                prompt.getParts(), message, savedCount > 0));
    }

    @Override
    public void prepareFailView(String message) {
        viewModel.setState(viewModel.getState().withError(message));
    }

    private static PracticeExpressionState.Layout layoutOf(ExpressionPrompt prompt) {
        switch (prompt.getKind()) {
            case POWER:
                return PracticeExpressionState.Layout.POWER;
            case FRACTION:
                return PracticeExpressionState.Layout.FRACTION;
            case ROOT:
                return PracticeExpressionState.Layout.ROOT;
            default:
                return PracticeExpressionState.Layout.LINE;
        }
    }
}
