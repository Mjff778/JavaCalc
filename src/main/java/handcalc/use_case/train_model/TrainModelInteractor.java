package handcalc.use_case.train_model;

import handcalc.entity.LabeledGlyph;
import handcalc.use_case.correct_symbol.SampleRepository;
import java.util.List;

/**
 * Trains the model on everything collected so far.
 */
public class TrainModelInteractor implements TrainModelInputBoundary {

    private final SampleRepository repository;
    private final ModelTrainer trainer;
    private final TrainModelOutputBoundary presenter;

    public TrainModelInteractor(SampleRepository repository, ModelTrainer trainer,
                                TrainModelOutputBoundary presenter) {
        this.repository = repository;
        this.trainer = trainer;
        this.presenter = presenter;
    }

    @Override
    public void execute() {
        List<LabeledGlyph> samples = repository.loadAll();
        if (samples.isEmpty()) {
            presenter.prepareFailView("No samples collected yet");
            return;
        }
        try {
            presenter.prepareSuccessView(trainer.train(samples));
        } catch (RuntimeException e) {
            presenter.prepareFailView("Training failed: " + e.getMessage());
        }
    }
}
