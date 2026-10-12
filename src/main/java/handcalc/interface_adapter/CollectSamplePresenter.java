package handcalc.interface_adapter;

import handcalc.use_case.collect_sample.CollectSampleOutputBoundary;
import handcalc.use_case.collect_sample.CollectSampleOutputData;

/**
 * Shows sample collection progress in the collection window.
 */
public class CollectSamplePresenter implements CollectSampleOutputBoundary {

    private final CollectorViewModel viewModel;

    public CollectSamplePresenter(CollectorViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @Override
    public void prepareProgressView(CollectSampleOutputData data) {
        Character saved = data.getSavedSymbol();
        String message = saved == null ? "" : "Saved a sample of '" + saved + "'";
        viewModel.update(state -> state.withProgress(data.getCounts(),
                data.getNextSymbol(), data.getTargetPerSymbol(), message, saved != null, false));
    }

    @Override
    public void prepareFailView(String message) {
        viewModel.update(old -> old.withProgress(old.getCounts(), old.getNextSymbol(),
                old.getTargetPerSymbol(), message, false, true));
    }
}
