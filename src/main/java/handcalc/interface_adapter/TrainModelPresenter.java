package handcalc.interface_adapter;

import handcalc.use_case.train_model.TrainModelOutputBoundary;
import handcalc.use_case.train_model.TrainingReport;

/**
 * Describes the outcome of a training run in words.
 */
public class TrainModelPresenter implements TrainModelOutputBoundary {

    private final CollectorViewModel viewModel;

    public TrainModelPresenter(CollectorViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @Override
    public void prepareSuccessView(TrainingReport report) {
        String text = "Trained on " + report.getSampleCount() + " samples. ";
        if (report.hasValidation()) {
            text += "Accuracy on " + report.getValidationCount() + " held-out samples: "
                    + Math.round(report.getValidationAccuracy() * 100) + "%";
        } else {
            text += "Accuracy not measured: collect at least 5 samples of a symbol "
                    + "to hold some back";
        }
        String status = text;
        viewModel.update(state -> state.withTrainingStatus(status));
    }

    @Override
    public void prepareFailView(String message) {
        viewModel.update(state -> state.withTrainingStatus(message));
    }
}
