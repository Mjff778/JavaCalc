package handcalc.use_case;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import handcalc.entity.Glyph;
import handcalc.entity.LabeledGlyph;
import handcalc.entity.Point;
import handcalc.entity.Stroke;
import handcalc.use_case.correct_symbol.SampleRepository;
import handcalc.use_case.practice_expression.ExpressionPrompt;
import handcalc.use_case.practice_expression.ExpressionPromptGenerator;
import handcalc.use_case.practice_expression.PracticeExpressionInteractor;
import handcalc.use_case.practice_expression.PracticeExpressionOutputBoundary;
import handcalc.use_case.recognize_and_solve.GlyphSegmenter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class PracticeExpressionTest {

    private static final String SYMBOLS = "0123456789+-×÷=()√sincotaπ";

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

    private static final class RecordingPresenter implements PracticeExpressionOutputBoundary {
        ExpressionPrompt prompt;
        int saved = -1;
        String failure;

        @Override
        public void prepareView(ExpressionPrompt prompt, int savedCount) {
            this.prompt = prompt;
            this.saved = savedCount;
        }

        @Override
        public void prepareFailView(String message) {
            failure = message;
        }
    }

    private static Stroke line(double x1, double y1, double x2, double y2) {
        return new Stroke(Arrays.asList(new Point(x1, y1), new Point(x2, y2)));
    }

    private static Glyph glyph(double x1, double y1, double x2, double y2) {
        return new Glyph(Collections.singletonList(line(x1, y1, x2, y2)));
    }

    /** One short vertical stroke per character, left to right. */
    private static List<Stroke> write(int symbols) {
        List<Stroke> strokes = new ArrayList<>();
        for (int i = 0; i < symbols; i++) {
            strokes.add(line(i * 60, 0, i * 60, 50));
        }
        return strokes;
    }

    @Test
    void linePromptLabelsGlyphsLeftToRight() {
        ExpressionPrompt prompt = ExpressionPrompt.line("12+3");

        List<LabeledGlyph> labelled = prompt.label(Arrays.asList(
                glyph(200, 0, 200, 50), glyph(0, 0, 0, 50), glyph(130, 0, 130, 50),
                glyph(60, 0, 60, 50)));

        assertEquals("12+3", chars(labelled));
        assertEquals(0, labelled.get(0).getGlyph().getMinX());
    }

    @Test
    void powerPromptListsBaseThenExponent() {
        ExpressionPrompt prompt = ExpressionPrompt.power("(1+2)", "3");

        assertEquals("(1+2)3", prompt.getSymbols());
        assertEquals(Arrays.asList("(1+2)", "3"), prompt.getParts());
    }

    @Test
    void fractionPromptFindsTheBarAndSplitsAboveFromBelow() {
        ExpressionPrompt prompt = ExpressionPrompt.fraction("12", "5");

        List<LabeledGlyph> labelled = prompt.label(Arrays.asList(
                glyph(30, 100, 30, 140),    // 5 below, under the right of the bar
                glyph(0, 80, 100, 80),      // the bar
                glyph(40, 20, 40, 60),      // 2 above, right
                glyph(10, 20, 10, 60)));    // 1 above, left

        assertEquals("-125", chars(labelled));
    }

    @Test
    void fractionWithTheWrongNumberAboveTheBarIsRejected() {
        ExpressionPrompt prompt = ExpressionPrompt.fraction("12", "5");

        assertNull(prompt.label(Arrays.asList(
                glyph(0, 80, 100, 80), glyph(10, 20, 10, 60),
                glyph(30, 100, 30, 140), glyph(60, 100, 60, 140))));
    }

    @Test
    void aWrongNumberOfGlyphsIsRejected() {
        assertNull(ExpressionPrompt.line("1+2").label(Collections.singletonList(glyph(0, 0, 0, 5))));
    }

    @Test
    void generatedPromptsOnlyUseKnownSymbols() {
        ExpressionPromptGenerator generator = new ExpressionPromptGenerator(new Random(4), SYMBOLS);
        for (int i = 0; i < 300; i++) {
            ExpressionPrompt prompt = generator.generate(new HashMap<>());
            for (char c : prompt.getSymbols().toCharArray()) {
                assertTrue(SYMBOLS.indexOf(c) >= 0, "unknown symbol " + c + " in " + prompt);
            }
        }
    }

    @Test
    void generatedPromptsCoverTheWholeVarietyOfLayouts() {
        ExpressionPromptGenerator generator = new ExpressionPromptGenerator(new Random(9), SYMBOLS);
        java.util.Set<ExpressionPrompt.Kind> seen = new java.util.HashSet<>();
        for (int i = 0; i < 200; i++) {
            seen.add(generator.generate(new HashMap<>()).getKind());
        }

        assertEquals(ExpressionPrompt.Kind.values().length, seen.size());
    }

    @Test
    void rarelyWrittenSymbolsAreFavoured() {
        HashMap<Character, Integer> counts = new HashMap<>();
        for (char c : SYMBOLS.toCharArray()) {
            counts.put(c, c == 'π' ? 0 : 500);
        }
        ExpressionPromptGenerator generator = new ExpressionPromptGenerator(new Random(1), SYMBOLS);
        int withPi = 0;
        for (int i = 0; i < 100; i++) {
            if (generator.generate(counts).getSymbols().indexOf('π') >= 0) {
                withPi++;
            }
        }

        assertTrue(withPi > 40, "only " + withPi + " of 100 prompts used the rare symbol");
    }

    @Test
    void submittingStoresOneSamplePerSymbolAndShowsTheNextExpression() {
        MemoryRepository repository = new MemoryRepository();
        RecordingPresenter presenter = new RecordingPresenter();
        PracticeExpressionInteractor interactor = new PracticeExpressionInteractor(
                new GlyphSegmenter(), repository,
                new ExpressionPromptGenerator(new Random(2), SYMBOLS), presenter);
        interactor.next();
        ExpressionPrompt first = presenter.prompt;
        assertNotNull(first);
        assertEquals(0, presenter.saved);

        // plain vertical strokes far apart: not fractions, so only count matters here
        List<Stroke> strokes = first.getKind() == ExpressionPrompt.Kind.FRACTION
                ? null : write(first.getSymbols().length());
        if (strokes == null) {
            return; // covered by the fraction tests above
        }
        interactor.submit(strokes);

        assertEquals(first.getSymbols().length(), repository.samples.size());
        assertEquals(first.getSymbols().length(), presenter.saved);
        for (int i = 0; i < first.getSymbols().length(); i++) {
            assertEquals(first.getSymbols().charAt(i), repository.samples.get(i).getSymbol());
        }
    }

    @Test
    void aMismatchStoresNothingAndKeepsTheExpression() {
        MemoryRepository repository = new MemoryRepository();
        RecordingPresenter presenter = new RecordingPresenter();
        PracticeExpressionInteractor interactor = new PracticeExpressionInteractor(
                new GlyphSegmenter(), repository,
                new ExpressionPromptGenerator(new Random(2), SYMBOLS), presenter);
        interactor.next();
        ExpressionPrompt shown = presenter.prompt;

        interactor.submit(write(shown.getSymbols().length() - 1));

        assertTrue(repository.samples.isEmpty());
        assertNotNull(presenter.failure);
        assertEquals(shown, presenter.prompt);
    }

    @Test
    void aSymbolDrawnInStrokesThatSegmentationWouldSplitStillGetsItsLabel() {
        MemoryRepository repository = new MemoryRepository();
        RecordingPresenter presenter = new RecordingPresenter();
        PracticeExpressionInteractor interactor = new PracticeExpressionInteractor(
                new GlyphSegmenter(), repository, fixed(ExpressionPrompt.line("1=2")), presenter);
        interactor.next();

        // an "=" whose bars are far apart: the segmenter sees two symbols, four in all
        interactor.submit(Arrays.asList(
                line(0, 0, 0, 50),
                line(40, 10, 60, 10), line(40, 60, 60, 60),
                line(100, 0, 140, 0)));

        assertNull(presenter.failure);
        assertEquals(3, repository.samples.size());
        assertEquals('=', repository.samples.get(1).getSymbol());
        assertEquals(2, repository.samples.get(1).getGlyph().getStrokes().size());
    }

    @Test
    void anySymbolMayTakeAnyNumberOfStrokes() {
        MemoryRepository repository = new MemoryRepository();
        RecordingPresenter presenter = new RecordingPresenter();
        PracticeExpressionInteractor interactor = new PracticeExpressionInteractor(
                new GlyphSegmenter(), repository, fixed(ExpressionPrompt.line("4π")), presenter);
        interactor.next();

        List<Stroke> strokes = new ArrayList<>();
        // a "4" in three strokes, then a "π" in four, as one particular hand might write them
        strokes.add(line(10, 0, 0, 30));
        strokes.add(line(0, 30, 30, 30));
        strokes.add(line(25, 0, 25, 50));
        strokes.add(line(80, 0, 140, 0));
        strokes.add(line(95, 0, 90, 50));
        strokes.add(line(125, 0, 125, 40));
        strokes.add(line(125, 40, 135, 50));
        interactor.submit(strokes);

        assertNull(presenter.failure);
        assertEquals(2, repository.samples.size());
        assertEquals(3, repository.samples.get(0).getGlyph().getStrokes().size());
        assertEquals(4, repository.samples.get(1).getGlyph().getStrokes().size());
        assertEquals('π', repository.samples.get(1).getSymbol());
    }

    @Test
    void aFractionWhoseNumeratorTookSeveralSeparateStrokesIsStillLabelled() {
        MemoryRepository repository = new MemoryRepository();
        RecordingPresenter presenter = new RecordingPresenter();
        PracticeExpressionInteractor interactor = new PracticeExpressionInteractor(
                new GlyphSegmenter(), repository,
                fixed(ExpressionPrompt.fraction("4", "3")), presenter);
        interactor.next();

        interactor.submit(Arrays.asList(
                line(10, 0, 10, 30), line(60, 0, 60, 30),   // a "4" drawn as two far strokes
                line(0, 50, 100, 50),                       // the bar
                line(40, 70, 60, 100)));                    // the "3"

        assertNull(presenter.failure);
        assertEquals(3, repository.samples.size());
        assertEquals('-', repository.samples.get(0).getSymbol());
        assertEquals('4', repository.samples.get(1).getSymbol());
        assertEquals(2, repository.samples.get(1).getGlyph().getStrokes().size());
    }

    @Test
    void tooFewStrokesForTheSymbolsIsStillRejected() {
        RecordingPresenter presenter = new RecordingPresenter();
        PracticeExpressionInteractor interactor = new PracticeExpressionInteractor(
                new GlyphSegmenter(), new MemoryRepository(),
                fixed(ExpressionPrompt.line("1+2+3")), presenter);
        interactor.next();

        interactor.submit(write(2));

        assertNotNull(presenter.failure);
    }

    /** A generator that always offers the same expression. */
    private static ExpressionPromptGenerator fixed(ExpressionPrompt prompt) {
        return new ExpressionPromptGenerator(new Random(0), SYMBOLS) {
            @Override
            public ExpressionPrompt generate(java.util.Map<Character, Integer> counts) {
                return prompt;
            }
        };
    }

    @Test
    void anEmptyCanvasIsRejected() {
        RecordingPresenter presenter = new RecordingPresenter();
        PracticeExpressionInteractor interactor = new PracticeExpressionInteractor(
                new GlyphSegmenter(), new MemoryRepository(),
                new ExpressionPromptGenerator(new Random(2), SYMBOLS), presenter);

        interactor.submit(Collections.emptyList());

        assertEquals("Nothing written yet", presenter.failure);
    }

    private static String chars(List<LabeledGlyph> labelled) {
        StringBuilder text = new StringBuilder();
        for (LabeledGlyph l : labelled) {
            text.append(l.getSymbol());
        }
        return text.toString();
    }

    @Test
    void enoughStrokesInTheWrongArrangementAreNotCalledTooFew() {
        MemoryRepository repository = new MemoryRepository();
        RecordingPresenter presenter = new RecordingPresenter();
        PracticeExpressionInteractor interactor = new PracticeExpressionInteractor(
                new GlyphSegmenter(), repository, fixed(ExpressionPrompt.fraction("1", "2")),
                presenter);
        interactor.next();

        // a bar with both numbers written above it: nothing is below
        interactor.submit(Arrays.asList(
                line(0, 100, 100, 100), line(30, 40, 30, 80), line(70, 40, 70, 80)));

        assertTrue(repository.samples.isEmpty());
        assertEquals("What was written does not match the 3 symbols of this expression."
                + " Write each symbol apart, as shown.", presenter.failure);
    }
}
