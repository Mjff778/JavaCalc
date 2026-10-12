package handcalc.use_case.train_model;

/**
 * Implemented by the presenter of the collection window.
 */
public interface TrainModelOutputBoundary {

    void prepareSuccessView(TrainingReport report);

    void prepareFailView(String message);
}
