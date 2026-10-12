package handcalc.use_case;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import handcalc.entity.Glyph;
import handcalc.entity.LabeledGlyph;
import handcalc.entity.Point;
import handcalc.entity.Stroke;
import handcalc.use_case.correct_symbol.SampleRepository;
import handcalc.use_case.train_model.ModelTrainer;
import handcalc.use_case.train_model.TrainModelInteractor;
import handcalc.use_case.train_model.TrainModelOutputBoundary;
import handcalc.use_case.train_model.TrainingReport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class TrainModelInteractorTest {

    private static final class FakeRepository implements SampleRepository {
        final List<LabeledGlyph> samples = new ArrayList<>();

        @Override
        public void save(LabeledGlyph sample) {
            samples.add(sample);
        }

        @Override
        public List<LabeledGlyph> loadAll() {
            return samples;
        }
    }

    private static final class RecordingPresenter implements TrainModelOutputBoundary {
        TrainingReport success;
        String failure;

        @Override
        public void prepareSuccessView(TrainingReport report) {
            success = report;
        }

        @Override
        public void prepareFailView(String message) {
            failure = message;
        }
    }

    private final FakeRepository repository = new FakeRepository();
    private final RecordingPresenter presenter = new RecordingPresenter();

    private static LabeledGlyph sample(char symbol) {
        Stroke stroke = new Stroke(Collections.singletonList(new Point(1, 1)));
        return new LabeledGlyph(new Glyph(Collections.singletonList(stroke)), symbol);
    }

    @Test
    void trainsOnAllCollectedSamplesAndReportsTheOutcome() {
        repository.save(sample('1'));
        repository.save(sample('2'));
        List<Integer> seen = new ArrayList<>();
        ModelTrainer trainer = samples -> {
            seen.add(samples.size());
            return new TrainingReport(samples.size(), 0.9, 1);
        };

        new TrainModelInteractor(repository, trainer, presenter).execute();

        assertEquals(Collections.singletonList(2), seen);
        assertEquals(2, presenter.success.getSampleCount());
        assertEquals(0.9, presenter.success.getValidationAccuracy());
        assertEquals(1, presenter.success.getValidationCount());
        assertNull(presenter.failure);
    }

    @Test
    void withoutSamplesTrainingIsRefused() {
        ModelTrainer trainer = samples -> {
            throw new AssertionError("must not be called");
        };

        new TrainModelInteractor(repository, trainer, presenter).execute();

        assertEquals("No samples collected yet", presenter.failure);
        assertNull(presenter.success);
    }

    @Test
    void trainerFailureIsReportedInsteadOfCrashing() {
        repository.save(sample('1'));
        ModelTrainer trainer = samples -> {
            throw new IllegalStateException("disk full");
        };

        new TrainModelInteractor(repository, trainer, presenter).execute();

        assertEquals("Training failed: disk full", presenter.failure);
    }

    @Test
    void reportKnowsWhenNoAccuracyCouldBeMeasured() {
        TrainingReport report = new TrainingReport(3, Double.NaN, 0);
        assertTrue(!report.hasValidation());
        assertTrue(new TrainingReport(30, 0.8, 6).hasValidation());
    }
}
