package handcalc.use_case;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import handcalc.entity.Glyph;
import handcalc.entity.LabeledGlyph;
import handcalc.entity.Point;
import handcalc.entity.Stroke;
import handcalc.use_case.correct_symbol.CorrectSymbolInputData;
import handcalc.use_case.correct_symbol.CorrectSymbolInteractor;
import handcalc.use_case.correct_symbol.SampleRepository;
import handcalc.use_case.recognize_and_solve.Candidate;
import handcalc.use_case.recognize_and_solve.GlyphSegmenter;
import handcalc.use_case.recognize_and_solve.RecognizeAndSolveOutputBoundary;
import handcalc.use_case.recognize_and_solve.RecognizeAndSolveOutputData;
import handcalc.use_case.recognize_and_solve.SymbolReading;
import handcalc.use_case.recognize_and_solve.TrainableSymbolClassifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class CorrectSymbolInteractorTest {

    private static class FakeClassifier implements TrainableSymbolClassifier {
        private final String script;
        private int calls;
        final List<LabeledGlyph> learned = new ArrayList<>();

        FakeClassifier(String script) {
            this.script = script;
        }

        @Override
        public List<Candidate> classify(Glyph glyph) {
            return Collections.singletonList(new Candidate(script.charAt(calls++), 1.0));
        }

        @Override
        public void learn(Glyph glyph, char symbol) {
            learned.add(new LabeledGlyph(glyph, symbol));
        }
    }

    private static final class FakeRepository implements SampleRepository {
        final List<LabeledGlyph> saved = new ArrayList<>();

        @Override
        public void save(LabeledGlyph sample) {
            saved.add(sample);
        }

        @Override
        public List<LabeledGlyph> loadAll() {
            return saved;
        }
    }

    private static final class RecordingPresenter implements RecognizeAndSolveOutputBoundary {
        RecognizeAndSolveOutputData success;
        String failure;

        @Override
        public void prepareSuccessView(RecognizeAndSolveOutputData outputData) {
            success = outputData;
        }

        @Override
        public void prepareFailView(String recognizedExpression, String errorMessage,
                                    List<SymbolReading> readings) {
            failure = errorMessage;
        }
    }

    private static List<Stroke> strokesForSymbols(int count) {
        List<Stroke> strokes = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            strokes.add(new Stroke(Arrays.asList(new Point(i * 100, 0), new Point(i * 100, 80))));
        }
        return strokes;
    }

    private final FakeRepository repository = new FakeRepository();
    private final RecordingPresenter presenter = new RecordingPresenter();

    private CorrectSymbolInteractor interactor(FakeClassifier classifier) {
        return new CorrectSymbolInteractor(
                new GlyphSegmenter(), classifier, repository, presenter);
    }

    @Test
    void chosenSymbolReplacesTheMisreadOneAndTheResultIsRecomputed() {
        FakeClassifier classifier = new FakeClassifier("5×3");

        interactor(classifier).execute(new CorrectSymbolInputData(strokesForSymbols(3), 1, '+'));

        assertEquals("5+3", presenter.success.getExpression());
        assertEquals(8.0, presenter.success.getResult());
    }

    @Test
    void correctionIsSavedAsATrainingSampleAndLearnedImmediately() {
        FakeClassifier classifier = new FakeClassifier("5×3");

        interactor(classifier).execute(new CorrectSymbolInputData(strokesForSymbols(3), 1, '+'));

        assertEquals(1, repository.saved.size());
        assertEquals('+', repository.saved.get(0).getSymbol());
        assertEquals(100.0, repository.saved.get(0).getGlyph().getMinX());
        assertEquals(1, classifier.learned.size());
        assertEquals('+', classifier.learned.get(0).getSymbol());
    }

    @Test
    void indexOutsideTheWrittenSymbolsIsRejectedAndNothingIsLearned() {
        FakeClassifier classifier = new FakeClassifier("5×3");

        interactor(classifier).execute(new CorrectSymbolInputData(strokesForSymbols(3), 7, '+'));

        assertEquals("No such symbol on the canvas", presenter.failure);
        assertNull(presenter.success);
        assertTrue(repository.saved.isEmpty());
        assertTrue(classifier.learned.isEmpty());
    }

    @Test
    void aSymbolTheClassifierCannotLearnIsAppliedButNeitherStoredNorLearned() {
        FakeClassifier classifier = new FakeClassifier("1+5") {
            @Override
            public boolean canLearn(char symbol) {
                return symbol != '.';
            }
        };

        interactor(classifier).execute(new CorrectSymbolInputData(strokesForSymbols(3), 1, '.'));

        assertEquals("1.5", presenter.success.getExpression());
        assertTrue(repository.saved.isEmpty());
        assertTrue(classifier.learned.isEmpty());
    }
}
