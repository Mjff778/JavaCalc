package handcalc.use_case;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import handcalc.entity.LabeledGlyph;
import handcalc.entity.Point;
import handcalc.entity.Stroke;
import handcalc.use_case.collect_sample.CollectSampleInputData;
import handcalc.use_case.collect_sample.CollectSampleInteractor;
import handcalc.use_case.collect_sample.CollectSampleOutputBoundary;
import handcalc.use_case.collect_sample.CollectSampleOutputData;
import handcalc.use_case.correct_symbol.SampleRepository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class CollectSampleInteractorTest {

    private static final String SYMBOLS = "0123";

    private static final class MemoryRepository implements SampleRepository {
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

    private static final class RecordingPresenter implements CollectSampleOutputBoundary {
        CollectSampleOutputData progress;
        String failure;

        @Override
        public void prepareProgressView(CollectSampleOutputData outputData) {
            progress = outputData;
        }

        @Override
        public void prepareFailView(String message) {
            failure = message;
        }
    }

    private final MemoryRepository repository = new MemoryRepository();
    private final RecordingPresenter presenter = new RecordingPresenter();
    private final CollectSampleInteractor interactor = new CollectSampleInteractor(
            repository, SYMBOLS, 5, presenter);

    private static Stroke line(double x1, double y1, double x2, double y2) {
        return new Stroke(Arrays.asList(new Point(x1, y1), new Point(x2, y2)));
    }

    private void save(char symbol, List<Stroke> strokes) {
        interactor.execute(new CollectSampleInputData(strokes, symbol));
    }

    @Test
    void refreshShowsZeroCountsAndStartsWithTheFirstSymbol() {
        interactor.refresh();

        assertEquals(0, presenter.progress.getCounts().get('2'));
        assertEquals('0', presenter.progress.getNextSymbol());
        assertEquals(5, presenter.progress.getTargetPerSymbol());
        assertNull(presenter.progress.getSavedSymbol());
    }

    @Test
    void writtenSymbolIsStoredWithTheRequestedLabel() {
        save('2', Collections.singletonList(line(0, 0, 0, 50)));

        assertEquals(1, repository.samples.size());
        assertEquals('2', repository.samples.get(0).getSymbol());
        assertEquals(1, presenter.progress.getCounts().get('2'));
        assertEquals(Character.valueOf('2'), presenter.progress.getSavedSymbol());
    }

    @Test
    void multiStrokeSymbolIsOneSample() {
        save('1', Arrays.asList(line(0, 50, 100, 50), line(50, 0, 50, 100)));

        assertEquals(1, repository.samples.size());
        assertEquals(2, repository.samples.get(0).getGlyph().getStrokes().size());
    }

    @Test
    void nextSymbolIsTheLeastCollectedOne() {
        save('0', Collections.singletonList(line(0, 0, 0, 50)));
        save('1', Collections.singletonList(line(0, 0, 0, 50)));
        save('2', Collections.singletonList(line(0, 0, 0, 50)));

        assertEquals('3', presenter.progress.getNextSymbol());

        save('3', Collections.singletonList(line(0, 0, 0, 50)));
        assertEquals('0', presenter.progress.getNextSymbol());
    }

    @Test
    void everythingWrittenIsOneSampleEvenWhenThePartsAreFarApart() {
        // a "÷" is a dot, a bar and a dot; the user declared it as one symbol
        save('1', Arrays.asList(line(100, 0, 100, 2), line(0, 50, 200, 50),
                line(100, 98, 100, 100)));

        assertEquals(1, repository.samples.size());
        assertEquals(3, repository.samples.get(0).getGlyph().getStrokes().size());
        assertNull(presenter.failure);
    }

    @Test
    void thereIsNoLimitOnSamplesPerSymbol() {
        for (int i = 0; i < 12; i++) { // the target per symbol is only 5
            save('1', Collections.singletonList(line(0, 0, 0, 50)));
        }

        assertEquals(12, repository.samples.size());
        assertEquals(12, presenter.progress.getCounts().get('1'));
        assertNull(presenter.failure);
    }

    @Test
    void emptyCanvasIsRejected() {
        save('1', Collections.emptyList());

        assertEquals("Nothing written yet", presenter.failure);
        assertTrue(repository.samples.isEmpty());
    }

    @Test
    void unsupportedSymbolIsRejected() {
        save('x', Collections.singletonList(line(0, 0, 0, 50)));

        assertEquals("Unsupported symbol: x", presenter.failure);
        assertTrue(repository.samples.isEmpty());
    }
}
