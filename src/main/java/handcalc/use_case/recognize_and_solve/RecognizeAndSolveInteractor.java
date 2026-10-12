package handcalc.use_case.recognize_and_solve;

/**
 * Splits the written strokes into symbols, reads each one, and evaluates the resulting
 * arithmetic expression. When waiting for "=", the writing is a page: each line is read and,
 * once it ends in "=", solved on its own.
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
        if (inputData.isWaitForEquals()) {
            solver.solvePage(reader.readPage(inputData.getStrokes(), inputData.getPlaced()));
        } else {
            solver.solve(reader.read(inputData.getStrokes()));
        }
    }
}
