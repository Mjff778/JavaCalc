package handcalc.data_access;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import handcalc.entity.Glyph;
import handcalc.entity.Point;
import handcalc.entity.Stroke;
import handcalc.use_case.recognize_and_solve.Candidate;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class NeuralSymbolClassifierTest {

    private static NeuralSymbolClassifier trained;

    @BeforeAll
    static void train() {
        trained = NeuralSymbolClassifier.train(
                SyntheticSamples.generate(new Random(1), 40), new Random(7), 25);
    }

    @Test
    void templatesExistForAllSymbolsInsideTheUnitBox() {
        assertEquals("0123456789+-×÷=()√sincotaπ", SymbolTemplates.SYMBOLS);
        for (char symbol : SymbolTemplates.SYMBOLS.toCharArray()) {
            List<Stroke> strokes = SymbolTemplates.strokesFor(symbol);
            assertTrue(!strokes.isEmpty(), "no strokes for " + symbol);
            for (Stroke s : strokes) {
                for (Point p : s.getPoints()) {
                    assertTrue(p.getX() >= 0 && p.getX() <= 3 && p.getY() >= 0 && p.getY() <= 1,
                            "point outside the template box for " + symbol);
                }
            }
        }
    }

    @Test
    void recognisesFreshlyDistortedSymbols() {
        Random random = new Random(99);
        int perSymbol = 25;
        int correct = 0;
        StringBuilder errors = new StringBuilder();
        for (char symbol : SymbolTemplates.SYMBOLS.toCharArray()) {
            for (int i = 0; i < perSymbol; i++) {
                Glyph glyph = SyntheticSamples.randomGlyph(symbol, random);
                char predicted = trained.classify(glyph).get(0).getSymbol();
                if (predicted == symbol) {
                    correct++;
                } else {
                    errors.append(symbol).append("→").append(predicted).append(' ');
                }
            }
        }
        double accuracy = (double) correct / (perSymbol * SymbolTemplates.SYMBOLS.length());
        assertTrue(accuracy >= 0.95, "accuracy " + accuracy + ", mistakes: " + errors);
    }

    @Test
    void candidatesAreRankedAndCoverEverySymbolOnce() {
        Glyph glyph = SyntheticSamples.randomGlyph('7', new Random(5));
        List<Candidate> candidates = trained.classify(glyph);

        assertEquals(SymbolTemplates.SYMBOLS.length(), candidates.size());
        Set<Character> seen = new HashSet<>();
        double sum = 0;
        for (int i = 0; i < candidates.size(); i++) {
            seen.add(candidates.get(i).getSymbol());
            sum += candidates.get(i).getConfidence();
            if (i > 0) {
                assertTrue(candidates.get(i - 1).getConfidence() >= candidates.get(i).getConfidence());
            }
        }
        assertEquals(candidates.size(), seen.size());
        assertEquals(1.0, sum, 1e-6);
        assertEquals('7', candidates.get(0).getSymbol());
    }

    @Test
    void learningACorrectionMakesThatWritingComeOutAsTheChosenSymbol() throws IOException {
        NeuralSymbolClassifier copy = roundTrip(trained);
        Glyph squiggle = new Glyph(Arrays.asList(
                new Stroke(Arrays.asList(new Point(0, 0), new Point(30, 10), new Point(0, 20),
                        new Point(30, 30), new Point(0, 40)))));
        char before = copy.classify(squiggle).get(0).getSymbol();
        char target = before == '9' ? '8' : '9';

        copy.learn(squiggle, target);

        assertEquals(target, copy.classify(squiggle).get(0).getSymbol());
    }

    @Test
    void learningOneCorrectionDoesNotBreakTheOtherSymbols() throws IOException {
        NeuralSymbolClassifier copy = roundTrip(trained);
        Glyph squiggle = new Glyph(Arrays.asList(
                new Stroke(Arrays.asList(new Point(0, 0), new Point(30, 10), new Point(0, 20),
                        new Point(30, 30), new Point(0, 40)))));
        copy.learn(squiggle, '5');

        Random random = new Random(123);
        int correct = 0;
        int total = 0;
        for (char symbol : SymbolTemplates.SYMBOLS.toCharArray()) {
            for (int i = 0; i < 10; i++) {
                total++;
                if (copy.classify(SyntheticSamples.randomGlyph(symbol, random))
                        .get(0).getSymbol() == symbol) {
                    correct++;
                }
            }
        }
        assertTrue((double) correct / total >= 0.9, "accuracy dropped to " + correct + "/" + total);
    }

    @Test
    void savedModelGivesIdenticalPredictionsAfterLoading() throws IOException {
        NeuralSymbolClassifier loaded = roundTrip(trained);
        Glyph glyph = SyntheticSamples.randomGlyph('4', new Random(8));

        assertEquals(trained.classify(glyph).get(0).getConfidence(),
                loaded.classify(glyph).get(0).getConfidence(), 1e-9);
        assertEquals(trained.classify(glyph).get(0).getSymbol(),
                loaded.classify(glyph).get(0).getSymbol());
    }

    @Test
    void collectedSampleCountIsStoredWithTheModel() throws IOException {
        NeuralSymbolClassifier model = roundTrip(trained);
        model.setCollectedSampleCount(42);

        assertEquals(42, roundTrip(model).getCollectedSampleCount());
        assertNotEquals(42, trained.getCollectedSampleCount());
    }

    private static NeuralSymbolClassifier roundTrip(NeuralSymbolClassifier model)
            throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        model.save(out);
        return NeuralSymbolClassifier.load(new ByteArrayInputStream(out.toByteArray()));
    }

    @Test
    void aModelTrainedForAnotherSymbolSetIsRejected() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        trained.save(out);
        byte[] bytes = out.toByteArray();
        // the symbol set is stored as a UTF string right after the 4-byte magic number
        String stored = SymbolTemplates.SYMBOLS;
        int at = 4 + 2;
        byte[] first = stored.substring(0, 1).getBytes("UTF-8");
        bytes[at] = (byte) (first[0] == '0' ? '1' : '0');

        org.junit.jupiter.api.Assertions.assertThrows(IOException.class,
                () -> NeuralSymbolClassifier.load(new ByteArrayInputStream(bytes)));
    }
}
