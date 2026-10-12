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
        if (inputData.isWaitForEquals()) {
            correctPage(inputData);
            return;
        }
        List<SymbolReading> readings = reader.read(inputData.getStrokes());
        int index = inputData.getGlyphIndex();
        if (index < 0 || index >= readings.size()) {
            presenter.prepareFailView("", "No such symbol on the canvas", readings);
            return;
        }

        List<SymbolReading> corrected = new ArrayList<>(readings);
        corrected.set(index, corrected(readings.get(index), inputData.getSymbol()));
        solver.solve(corrected);
    }

    /** Symbols are counted line by line from the top; the whole page is solved again. */
    private void correctPage(CorrectSymbolInputData inputData) {
        List<List<SymbolReading>> lines =
                reader.readPage(inputData.getStrokes(), inputData.getPlaced());
        int index = inputData.getGlyphIndex();
        List<List<SymbolReading>> corrected = new ArrayList<>();
        boolean found = false;
        for (List<SymbolReading> line : lines) {
            List<SymbolReading> copy = new ArrayList<>(line);
            if (!found && index >= 0 && index < copy.size()) {
                copy.set(index, corrected(copy.get(index), inputData.getSymbol()));
                found = true;
            }
            index -= line.size();
            corrected.add(copy);
        }
        if (!found) {
            presenter.prepareFailView("", "No such symbol on the canvas", new ArrayList<>());
            return;
        }
        solver.solvePage(corrected);
    }

    /** Remembers the writing as a sample of the symbol, and reads it as that symbol. */
    private SymbolReading corrected(SymbolReading original, char symbol) {
        // a decimal point is told by its place, not its shape, so it is nothing to learn; nor
        // is a symbol the user picked before, as it is not their handwriting
        if (!original.isPlaced() && classifier.canLearn(symbol)) {
            repository.save(new LabeledGlyph(original.getGlyph(), symbol));
            classifier.learn(original.getGlyph(), symbol);
        }
        return new SymbolReading(original.getGlyph(), symbol, original.getAlternatives());
    }
}
