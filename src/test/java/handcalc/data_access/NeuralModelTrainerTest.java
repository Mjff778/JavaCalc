package handcalc.data_access;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import handcalc.entity.LabeledGlyph;
import handcalc.use_case.train_model.TrainingReport;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NeuralModelTrainerTest {

    private static final int SYMBOLS = SymbolTemplates.SYMBOLS.length();

    @TempDir
    Path dir;

    private static List<LabeledGlyph> collected(int perSymbol, long seed) {
        Random random = new Random(seed);
        List<LabeledGlyph> samples = new ArrayList<>();
        for (int i = 0; i < perSymbol; i++) {
            for (char symbol : SymbolTemplates.SYMBOLS.toCharArray()) {
                samples.add(new LabeledGlyph(SyntheticSamples.randomGlyph(symbol, random), symbol));
            }
        }
        return samples;
    }

    @Test
    void firstStartTrainsABootstrapModelAndStoresIt() {
        Path modelFile = dir.resolve("model.bin");
        ReplaceableClassifier classifier = new ReplaceableClassifier();

        new NeuralModelTrainer(classifier, modelFile).initialize(new ArrayList<>());

        assertTrue(Files.exists(modelFile));
        char read = classifier.classify(SyntheticSamples.randomGlyph('8', new Random(4)))
                .get(0).getSymbol();
        assertEquals('8', read);
    }

    @Test
    void trainingReportsHeldOutAccuracyAndReplacesTheLiveModel() {
        Path modelFile = dir.resolve("model.bin");
        ReplaceableClassifier classifier = new ReplaceableClassifier();
        NeuralModelTrainer trainer = new NeuralModelTrainer(classifier, modelFile);
        trainer.initialize(new ArrayList<>());

        TrainingReport report = trainer.train(collected(10, 3));

        assertEquals(10 * SYMBOLS, report.getSampleCount());
        assertTrue(report.hasValidation());
        assertEquals(2 * SYMBOLS, report.getValidationCount());
        assertTrue(report.getValidationAccuracy() >= 0.9,
                "accuracy " + report.getValidationAccuracy());
        assertEquals(10 * SYMBOLS, classifier.getCollectedSampleCount());
    }

    @Test
    void tooFewSamplesStillTrainButMeasureNoAccuracy() {
        NeuralModelTrainer trainer = new NeuralModelTrainer(
                new ReplaceableClassifier(), dir.resolve("model.bin"));

        TrainingReport report = trainer.train(collected(1, 5));

        assertEquals(SYMBOLS, report.getSampleCount());
        assertTrue(!report.hasValidation());
    }

    @Test
    void restartLoadsTheStoredModelAndOnlyLearnsNewSamples() {
        Path modelFile = dir.resolve("model.bin");
        List<LabeledGlyph> samples = collected(4, 9);
        new NeuralModelTrainer(new ReplaceableClassifier(), modelFile).train(samples);

        List<LabeledGlyph> more = new ArrayList<>(samples);
        more.addAll(collected(1, 10));
        ReplaceableClassifier second = new ReplaceableClassifier();
        new NeuralModelTrainer(second, modelFile).initialize(more);
        assertEquals(5 * SYMBOLS, second.getCollectedSampleCount());

        ReplaceableClassifier third = new ReplaceableClassifier();
        new NeuralModelTrainer(third, modelFile).initialize(more);
        assertEquals(5 * SYMBOLS, third.getCollectedSampleCount());
    }

    @Test
    void corruptModelFileFallsBackToTrainingANewOne() throws Exception {
        Path modelFile = dir.resolve("model.bin");
        Files.write(modelFile, new byte[] {1, 2, 3});
        ReplaceableClassifier classifier = new ReplaceableClassifier();

        new NeuralModelTrainer(classifier, modelFile).initialize(new ArrayList<>());

        char read = classifier.classify(SyntheticSamples.randomGlyph('+', new Random(2)))
                .get(0).getSymbol();
        assertEquals('+', read);
    }

    @Test
    void theModelOnlyLearnsSymbolsItKnows() {
        ReplaceableClassifier classifier = new ReplaceableClassifier();
        new NeuralModelTrainer(classifier, dir.resolve("model.bin")).initialize(new ArrayList<>());

        assertTrue(classifier.canLearn('5'));
        assertTrue(classifier.canLearn('π'));
        assertTrue(!classifier.canLearn('.'));
    }

    @Test
    void storedSamplesOfUnknownSymbolsAreSkippedInsteadOfBreakingStartUpAndTraining() {
        Path modelFile = dir.resolve("model.bin");
        List<LabeledGlyph> samples = collected(1, 8);
        samples.add(new LabeledGlyph(SyntheticSamples.randomGlyph('1', new Random(9)), '.'));

        new NeuralModelTrainer(new ReplaceableClassifier(), modelFile).initialize(samples);
        new NeuralModelTrainer(new ReplaceableClassifier(), modelFile).initialize(samples);
        TrainingReport report =
                new NeuralModelTrainer(new ReplaceableClassifier(), modelFile).train(samples);

        assertEquals(SYMBOLS, report.getSampleCount());
    }
}
