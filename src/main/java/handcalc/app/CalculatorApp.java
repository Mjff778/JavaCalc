package handcalc.app;

import handcalc.data_access.FileHistoryRepository;
import handcalc.data_access.FileSampleRepository;
import handcalc.data_access.NeuralModelTrainer;
import handcalc.data_access.ReplaceableClassifier;
import handcalc.data_access.RetrainingSampleRepository;
import handcalc.data_access.SymbolTemplates;
import handcalc.interface_adapter.CalculatorViewModel;
import handcalc.interface_adapter.CollectSampleController;
import handcalc.interface_adapter.CollectSamplePresenter;
import handcalc.interface_adapter.CollectorViewModel;
import handcalc.interface_adapter.CorrectSymbolController;
import handcalc.interface_adapter.PracticeExpressionController;
import handcalc.interface_adapter.PracticeExpressionPresenter;
import handcalc.interface_adapter.PracticeExpressionViewModel;
import handcalc.interface_adapter.HistoryController;
import handcalc.interface_adapter.HistoryPresenter;
import handcalc.interface_adapter.HistoryViewModel;
import handcalc.interface_adapter.RecognizeAndSolveController;
import handcalc.interface_adapter.RecognizeAndSolvePresenter;
import handcalc.interface_adapter.TrainModelController;
import handcalc.interface_adapter.TrainModelPresenter;
import handcalc.use_case.collect_sample.CollectSampleInteractor;
import handcalc.use_case.practice_expression.ExpressionPromptGenerator;
import handcalc.use_case.practice_expression.PracticeExpressionInteractor;
import handcalc.use_case.correct_symbol.CorrectSymbolInteractor;
import handcalc.use_case.correct_symbol.SampleRepository;
import handcalc.use_case.history.HistoryRecorder;
import handcalc.use_case.history.ShowHistoryInteractor;
import handcalc.use_case.recognize_and_solve.CalculationRecorder;
import handcalc.use_case.recognize_and_solve.DistinctCalculationRecorder;
import handcalc.use_case.recognize_and_solve.GlyphSegmenter;
import handcalc.use_case.recognize_and_solve.RecognizeAndSolveInteractor;
import handcalc.use_case.train_model.TrainModelInteractor;
import handcalc.view.HeaderBar;
import handcalc.view.CalculatorView;
import handcalc.view.CollectorView;
import handcalc.view.HistoryView;
import handcalc.entity.Stroke;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.ZoneId;
import java.util.Random;
import java.util.List;
import java.util.function.Consumer;

/**
 * Composition root: the only place that knows which concrete classes are plugged together.
 */
public final class CalculatorApp {

    /** Collected handwriting samples; commit this file so teammates can combine theirs. */
    public static final Path DEFAULT_SAMPLES_FILE = Paths.get("data", "samples.txt");

    private static final String MODEL_FILE_NAME = "model.bin";
    private static final String HISTORY_FILE_NAME = "history.jsonl";
    private static final int SAMPLES_WANTED_PER_SYMBOL = 20;
    /** How long after the last correction the model is retrained on all samples. */
    private static final long RETRAIN_DELAY_MS = 1500;

    private final CalculatorViewModel viewModel = new CalculatorViewModel();
    private final CollectorViewModel collectorViewModel = new CollectorViewModel();
    private final PracticeExpressionViewModel practiceViewModel =
            new PracticeExpressionViewModel();
    private final HistoryViewModel historyViewModel = new HistoryViewModel();
    private final HistoryController historyController;
    private final RecognizeAndSolveController controller;
    private final CorrectSymbolController correctController;
    private final CollectSampleController collectController;
    private final TrainModelController trainController;
    private final PracticeExpressionController practiceController;

    public CalculatorApp() {
        this(DEFAULT_SAMPLES_FILE);
    }

    /**
     * The trained model is kept in {@code model.bin} and the history in
     * {@code history.jsonl}, both next to the samples file.
     */
    public CalculatorApp(Path samplesFile) {
        SampleRepository stored = new FileSampleRepository(samplesFile);
        ReplaceableClassifier classifier = new ReplaceableClassifier();
        NeuralModelTrainer trainer = new NeuralModelTrainer(
                classifier, samplesFile.resolveSibling(MODEL_FILE_NAME));
        trainer.initialize(stored.loadAll());
        SampleRepository repository =
                new RetrainingSampleRepository(stored, trainer, RETRAIN_DELAY_MS);

        GlyphSegmenter segmenter = new GlyphSegmenter();
        FileHistoryRepository history =
                new FileHistoryRepository(samplesFile.resolveSibling(HISTORY_FILE_NAME));
        // a page is read again at every pause: only new calculations go into the history
        CalculationRecorder recorder = new DistinctCalculationRecorder(
                new HistoryRecorder(history, Clock.systemUTC()));

        RecognizeAndSolvePresenter presenter = new RecognizeAndSolvePresenter(viewModel);
        controller = new RecognizeAndSolveController(
                new RecognizeAndSolveInteractor(segmenter, classifier, presenter, recorder));
        correctController = new CorrectSymbolController(new CorrectSymbolInteractor(
                segmenter, classifier, repository, presenter, recorder));

        historyController = new HistoryController(new ShowHistoryInteractor(history,
                new HistoryPresenter(historyViewModel, ZoneId.systemDefault())));
        collectController = new CollectSampleController(new CollectSampleInteractor(
                repository, SymbolTemplates.SYMBOLS, SAMPLES_WANTED_PER_SYMBOL,
                new CollectSamplePresenter(collectorViewModel)));
        practiceController = new PracticeExpressionController(new PracticeExpressionInteractor(
                segmenter, repository,
                new ExpressionPromptGenerator(new Random(), SymbolTemplates.SYMBOLS),
                new PracticeExpressionPresenter(practiceViewModel)));
        trainController = new TrainModelController(new TrainModelInteractor(
                repository, trainer, new TrainModelPresenter(collectorViewModel)));
    }

    public RecognizeAndSolveController getController() {
        return controller;
    }

    public CorrectSymbolController getCorrectController() {
        return correctController;
    }

    public HistoryController getHistoryController() {
        return historyController;
    }

    public HistoryViewModel getHistoryViewModel() {
        return historyViewModel;
    }

    public CalculatorViewModel getViewModel() {
        return viewModel;
    }

    /** @param openCollector what the "Collect samples" button does; null hides the button */
    public CalculatorView createView(Runnable openCollector) {
        return new CalculatorView(controller, viewModel, openCollector);
    }

    public CollectorView createCollectorView() {
        return new CollectorView(collectController, practiceController, practiceViewModel,
                trainController, collectorViewModel);
    }

    public HeaderBar createHeaderBar(Runnable openHistory) {
        return new HeaderBar(openHistory);
    }

    public HistoryView createHistoryView(Consumer<List<Stroke>> onLoad) {
        return new HistoryView(historyController, historyViewModel, onLoad);
    }
}
