package handcalc.data_access;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import handcalc.entity.Glyph;
import handcalc.entity.LabeledGlyph;
import handcalc.entity.Point;
import handcalc.entity.Stroke;
import handcalc.use_case.correct_symbol.SampleRepository;
import handcalc.use_case.train_model.TrainingReport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class RetrainingSampleRepositoryTest {

    private final List<LabeledGlyph> stored = new CopyOnWriteArrayList<>();
    private final SampleRepository memory = new SampleRepository() {
        @Override
        public void save(LabeledGlyph sample) {
            stored.add(sample);
        }

        @Override
        public List<LabeledGlyph> loadAll() {
            return new ArrayList<>(stored);
        }
    };
    private final List<Integer> trainedOn = new CopyOnWriteArrayList<>();
    private final CountDownLatch trained = new CountDownLatch(1);

    private RetrainingSampleRepository repository(long delayMs) {
        return new RetrainingSampleRepository(memory, samples -> {
            trainedOn.add(samples.size());
            trained.countDown();
            return new TrainingReport(samples.size(), Double.NaN, 0);
        }, delayMs);
    }

    private static LabeledGlyph sample(char symbol) {
        return new LabeledGlyph(new Glyph(Collections.singletonList(
                new Stroke(Collections.singletonList(new Point(0, 0))))), symbol);
    }

    @Test
    void aSavedSampleIsStoredAtOnceAndTheModelRetrainedOnEverySampleSoonAfter()
            throws Exception {
        stored.add(sample('1'));
        RetrainingSampleRepository repository = repository(50);

        repository.save(sample('s'));

        assertEquals(2, repository.loadAll().size());
        assertTrue(trained.await(5, TimeUnit.SECONDS), "the model was retrained");
        assertEquals(Collections.singletonList(2), trainedOn);
    }

    @Test
    void severalCorrectionsInARowAreTrainedOnOnce() throws Exception {
        RetrainingSampleRepository repository = repository(300);

        repository.save(sample('s'));
        repository.save(sample('i'));
        repository.save(sample('n'));

        assertTrue(trained.await(5, TimeUnit.SECONDS));
        Thread.sleep(500);
        assertEquals(Collections.singletonList(3), trainedOn);
    }
}
