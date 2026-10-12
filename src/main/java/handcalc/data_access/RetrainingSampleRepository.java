package handcalc.data_access;

import handcalc.entity.LabeledGlyph;
import handcalc.use_case.correct_symbol.SampleRepository;
import handcalc.use_case.train_model.ModelTrainer;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Stores samples, and soon after a new one retrains the model on all of them in the
 * background.
 *
 * <p>A correction also fine-tunes the model at once, so the next reading already uses it,
 * but fine-tuning on one sample at a time slowly makes the model forget the rest of the
 * user's writing; retraining on every sample puts that right. Samples saved in quick
 * succession are trained on together.
 */
public class RetrainingSampleRepository implements SampleRepository {

    private static final Logger LOGGER =
            Logger.getLogger(RetrainingSampleRepository.class.getName());

    private final SampleRepository samples;
    private final ModelTrainer trainer;
    private final long delayMs;
    private final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor(
            task -> {
                Thread thread = new Thread(task, "model-retraining");
                thread.setDaemon(true);
                return thread;
            });
    private ScheduledFuture<?> pending;

    /** @param delayMs how long after the last new sample the model is retrained */
    public RetrainingSampleRepository(SampleRepository samples, ModelTrainer trainer,
                                      long delayMs) {
        this.samples = samples;
        this.trainer = trainer;
        this.delayMs = delayMs;
    }

    @Override
    public void save(LabeledGlyph sample) {
        samples.save(sample);
        scheduleRetraining();
    }

    @Override
    public List<LabeledGlyph> loadAll() {
        return samples.loadAll();
    }

    private synchronized void scheduleRetraining() {
        if (pending != null) {
            pending.cancel(false);
        }
        pending = worker.schedule(this::retrain, delayMs, TimeUnit.MILLISECONDS);
    }

    private void retrain() {
        try {
            trainer.train(samples.loadAll());
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "Could not retrain the recognition model", e);
        }
    }
}
