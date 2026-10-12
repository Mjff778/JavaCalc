package handcalc.use_case.recognize_and_solve;

/**
 * Splits the written strokes into symbols, reads each one, and evaluates the resulting
 * arithmetic expression.
 */
public class RecognizeAndSolveInteractor implements RecognizeAndSolveInputBoundary {

    private final HandwritingReader reader;
    private final ReadingSolver solver;

    public RecognizeAndSolveInteractor(GlyphSegmenter segmenter, SymbolClassifier classifier,
                                       RecognizeAndSolveOutputBoundary presenter) {
        this(segmenter, classifier, presenter, CalculationRecorder.NONE);
    }

    public RecognizeAndSolveInteractor(GlyphSegmenter segmenter, SymbolClassifier classifier,
                                       RecognizeAndSolveOutputBoundary presenter,
                                       CalculationRecorder recorder) {
        this.reader = new HandwritingReader(segmenter, classifier);
        this.solver = new ReadingSolver(presenter, recorder);
    }

    @Override
    public void execute(RecognizeAndSolveInputData inputData) {
        solver.solve(reader.read(inputData.getStrokes()));
    }
}
