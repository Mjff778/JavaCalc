package handcalc.interface_adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import handcalc.entity.Point;
import handcalc.entity.Stroke;
import handcalc.use_case.collect_sample.CollectSampleInputBoundary;
import handcalc.use_case.collect_sample.CollectSampleInputData;
import handcalc.use_case.collect_sample.CollectSampleOutputData;
import handcalc.use_case.train_model.TrainModelInputBoundary;
import handcalc.use_case.train_model.TrainingReport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CollectorAdapterTest {

    private final CollectorViewModel viewModel = new CollectorViewModel();
    private final CollectSamplePresenter samplePresenter = new CollectSamplePresenter(viewModel);
    private final TrainModelPresenter trainPresenter = new TrainModelPresenter(viewModel);

    private static Map<Character, Integer> counts(int a, int b) {
        Map<Character, Integer> counts = new LinkedHashMap<>();
        counts.put('1', a);
        counts.put('2', b);
        return counts;
    }

    @Test
    void savedSampleUpdatesCountsPromptAndMessage() {
        samplePresenter.prepareProgressView(
                new CollectSampleOutputData(counts(3, 1), '2', 20, '1'));

        CollectorState state = viewModel.getState();
        assertEquals(3, state.getCounts().get('1'));
        assertEquals('2', state.getNextSymbol());
        assertEquals(20, state.getTargetPerSymbol());
        assertTrue(state.isSaved());
        assertEquals("Saved a sample of '1'", state.getMessage());
        assertFalse(state.isError());
    }

    @Test
    void plainProgressReportIsNotASave() {
        samplePresenter.prepareProgressView(
                new CollectSampleOutputData(counts(0, 0), '1', 20, null));

        assertFalse(viewModel.getState().isSaved());
        assertEquals("", viewModel.getState().getMessage());
    }

    @Test
    void failureKeepsTheCountsAndShowsTheError() {
        samplePresenter.prepareProgressView(
                new CollectSampleOutputData(counts(3, 1), '2', 20, null));

        samplePresenter.prepareFailView("Nothing written yet");

        CollectorState state = viewModel.getState();
        assertEquals(3, state.getCounts().get('1'));
        assertEquals('2', state.getNextSymbol());
        assertEquals("Nothing written yet", state.getMessage());
        assertTrue(state.isError());
        assertFalse(state.isSaved());
    }

    @Test
    void trainingWithAccuracyIsSummarisedInWords() {
        trainPresenter.prepareSuccessView(new TrainingReport(150, 0.9333, 30));

        assertEquals("Trained on 150 samples. Accuracy on 30 held-out samples: 93%",
                viewModel.getState().getTrainingStatus());
    }

    @Test
    void trainingWithoutHeldOutSamplesSaysWhy() {
        trainPresenter.prepareSuccessView(new TrainingReport(12, Double.NaN, 0));

        assertEquals("Trained on 12 samples. Accuracy not measured: "
                        + "collect at least 5 samples of a symbol to hold some back",
                viewModel.getState().getTrainingStatus());
    }

    @Test
    void trainingFailureIsShown() {
        trainPresenter.prepareFailView("No samples collected yet");

        assertEquals("No samples collected yet", viewModel.getState().getTrainingStatus());
    }

    @Test
    void trainingStatusSurvivesLaterSampleUpdates() {
        trainPresenter.prepareFailView("No samples collected yet");

        samplePresenter.prepareProgressView(
                new CollectSampleOutputData(counts(1, 0), '2', 20, '1'));

        assertEquals("No samples collected yet", viewModel.getState().getTrainingStatus());
        assertEquals(1, viewModel.getState().getCounts().get('1'));
    }

    @Test
    void listenersAreNotified() {
        List<Object> seen = new ArrayList<>();
        viewModel.addPropertyChangeListener(e -> seen.add(e.getNewValue()));

        samplePresenter.prepareFailView("x");

        assertEquals(1, seen.size());
        assertSame(viewModel.getState(), seen.get(0));
    }

    @Test
    void controllersForwardToTheUseCases() {
        List<CollectSampleInputData> saved = new ArrayList<>();
        int[] refreshes = {0};
        CollectSampleInputBoundary collect = new CollectSampleInputBoundary() {
            @Override
            public void execute(CollectSampleInputData inputData) {
                saved.add(inputData);
            }

            @Override
            public void refresh() {
                refreshes[0]++;
            }
        };
        int[] trains = {0};
        TrainModelInputBoundary train = () -> trains[0]++;
        Stroke stroke = new Stroke(Collections.singletonList(new Point(1, 2)));

        CollectSampleController collectController = new CollectSampleController(collect);
        collectController.execute(Collections.singletonList(stroke), '7');
        collectController.refresh();
        new TrainModelController(train).execute();

        assertEquals('7', saved.get(0).getSymbol());
        assertSame(stroke, saved.get(0).getStrokes().get(0));
        assertEquals(1, refreshes[0]);
        assertEquals(1, trains[0]);
    }

    @Test
    void trainingOutcomesAndProgressReportsFromDifferentThreadsAreNeverLost()
            throws InterruptedException {
        int rounds = 20000;
        Thread training = new Thread(() -> {
            for (int i = 0; i < rounds; i++) {
                trainPresenter.prepareFailView("failed");
            }
        });
        Thread collecting = new Thread(() -> {
            for (int i = 0; i < rounds; i++) {
                samplePresenter.prepareProgressView(
                        new CollectSampleOutputData(counts(i, 0), '2', 20, null));
            }
        });
        training.start();
        collecting.start();
        training.join();
        collecting.join();

        assertEquals(rounds, viewModel.getState().getTrainingRevision());
        assertEquals(rounds - 1, (int) viewModel.getState().getCounts().get('1'));
    }
}
