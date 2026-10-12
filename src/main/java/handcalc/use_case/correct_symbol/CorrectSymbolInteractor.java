package handcalc.use_case.correct_symbol;

import handcalc.entity.LabeledGlyph;
import handcalc.use_case.recognize_and_solve.CalculationRecorder;
import handcalc.use_case.recognize_and_solve.GlyphSegmenter;
import handcalc.use_case.recognize_and_solve.HandwritingReader;
import handcalc.use_case.recognize_and_solve.ReadingSolver;
import handcalc.use_case.recognize_and_solve.RecognizeAndSolveOutputBoundary;
import handcalc.use_case.recognize_and_solve.SymbolReading;
import handcalc.use_case.recognize_and_solve.TrainableSymbolClassifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Applies the user's correction, remembers the handwriting as a training sample so the
 * classifier improves, and shows the recomputed answer.
 */
public class CorrectSymbolInteractor implements CorrectSymbolInputBoundary {

    private final HandwritingReader reader;
    private final TrainableSymbolClassifier classifier;
    private final SampleRepository repository;
    private final ReadingSolver solver;
    private final RecognizeAndSolveOutputBoundary presenter;

    public CorrectSymbolInteractor(GlyphSegmenter segmenter, TrainableSymbolClassifier classifier,
                                   SampleRepository repository,
                                   RecognizeAndSolveOutputBoundary presenter) {
        this(segmenter, classifier, repository, presenter, CalculationRecorder.NONE);
    }

    public CorrectSymbolInteractor(GlyphSegmenter segmenter, TrainableSymbolClassifier classifier,
                                   SampleRepository repository,
                                   RecognizeAndSolveOutputBoundary presenter,
                                   CalculationRecorder recorder) {
        this.reader = new HandwritingReader(segmenter, classifier);
        this.classifier = classifier;
        this.repository = repository;
        this.solver = new ReadingSolver(presenter, recorder);
        this.presenter = presenter;
    }

    @Override
    public void execute(CorrectSymbolInputData inputData) {
        List<SymbolReading> readings = reader.read(inputData.getStrokes());
        int index = inputData.getGlyphIndex();
        if (index < 0 || index >= readings.size()) {
            presenter.prepareFailView("", "No such symbol on the canvas", readings);
            return;
        }

        SymbolReading original = readings.get(index);
        char symbol = inputData.getSymbol();
        // a decimal point is told by its place, not its shape, so it is nothing to learn
        if (classifier.canLearn(symbol)) {
            repository.save(new LabeledGlyph(original.getGlyph(), symbol));
            classifier.learn(original.getGlyph(), symbol);
        }

        List<SymbolReading> corrected = new ArrayList<>(readings);
        corrected.set(index,
                new SymbolReading(original.getGlyph(), symbol, original.getAlternatives()));
        solver.solve(corrected);
    }
}
