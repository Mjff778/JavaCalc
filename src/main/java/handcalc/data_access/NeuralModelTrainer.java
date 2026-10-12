package handcalc.data_access;

import handcalc.entity.Glyph;
import handcalc.entity.LabeledGlyph;
import handcalc.use_case.recognize_and_solve.Candidate;
import handcalc.use_case.train_model.ModelTrainer;
import handcalc.use_case.train_model.TrainingReport;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Trains {@link NeuralSymbolClassifier}s from collected handwriting and keeps the trained
 * model in a file. One training runs at a time, as training may happen in the background.
 *
 * <p>Built-in synthetic symbols are always part of the training set so every symbol is
 * known, while each real sample is expanded into several distorted copies so real writing
 * dominates as soon as there is some.
 */
public class NeuralModelTrainer implements ModelTrainer {

    private static final int SYNTHETIC_PER_SYMBOL = 40;
    private static final int COPIES_PER_REAL_SAMPLE = 4;
    private static final int EPOCHS = 25;
    private static final int HOLD_OUT_EVERY = 5;
    private static final long SEED = 207;

    private final ReplaceableClassifier target;
    private final Path modelFile;

    public NeuralModelTrainer(ReplaceableClassifier target, Path modelFile) {
        this.target = target;
        this.modelFile = modelFile;
    }

    /**
     * Makes a model available at program start: the stored one if there is a usable file
     * (taught any samples collected since), otherwise a freshly trained one.
     */
    public synchronized void initialize(List<LabeledGlyph> collected) {
        NeuralSymbolClassifier model = loadStoredModel();
        if (model == null) {
            model = trainModel(collected);
            model.setCollectedSampleCount(collected.size());
            store(model);
        } else if (model.getCollectedSampleCount() < collected.size()) {
            for (int i = model.getCollectedSampleCount(); i < collected.size(); i++) {
                LabeledGlyph sample = collected.get(i);
                if (model.canLearn(sample.getSymbol())) {
                    model.learn(sample.getGlyph(), sample.getSymbol());
                }
            }
            model.setCollectedSampleCount(collected.size());
            store(model);
        }
        target.replace(model);
    }

    @Override
    public synchronized TrainingReport train(List<LabeledGlyph> collected) {
        List<LabeledGlyph> samples = knownSymbolsOnly(collected);
        List<LabeledGlyph> training = new ArrayList<>();
        List<LabeledGlyph> heldOut = new ArrayList<>();
        Map<Character, Integer> seen = new HashMap<>();
        Map<Character, Integer> totals = new HashMap<>();
        for (LabeledGlyph sample : samples) {
            totals.merge(sample.getSymbol(), 1, Integer::sum);
        }
        for (LabeledGlyph sample : samples) {
            int index = seen.merge(sample.getSymbol(), 1, Integer::sum) - 1;
            boolean enough = totals.get(sample.getSymbol()) >= HOLD_OUT_EVERY;
            if (enough && index % HOLD_OUT_EVERY == HOLD_OUT_EVERY - 1) {
                heldOut.add(sample);
            } else {
                training.add(sample);
            }
        }

        double accuracy = Double.NaN;
        if (!heldOut.isEmpty()) {
            NeuralSymbolClassifier trial = trainModel(training);
            int correct = 0;
            for (LabeledGlyph sample : heldOut) {
                List<Candidate> ranked = trial.classify(sample.getGlyph());
                if (ranked.get(0).getSymbol() == sample.getSymbol()) {
                    correct++;
                }
            }
            accuracy = (double) correct / heldOut.size();
        }

        NeuralSymbolClassifier model = trainModel(samples);
        model.setCollectedSampleCount(collected.size());
        store(model);
        target.replace(model);
        return new TrainingReport(samples.size(), accuracy, heldOut.size());
    }

    /**
     * Drops samples of symbols this program does not know, e.g. from a teammate's newer
     * version, so one odd line in the samples file cannot stop training or start-up.
     */
    private static List<LabeledGlyph> knownSymbolsOnly(List<LabeledGlyph> samples) {
        List<LabeledGlyph> known = new ArrayList<>();
        for (LabeledGlyph sample : samples) {
            if (SymbolTemplates.SYMBOLS.indexOf(sample.getSymbol()) >= 0) {
                known.add(sample);
            }
        }
        return known;
    }

    private NeuralSymbolClassifier trainModel(List<LabeledGlyph> real) {
        Random random = new Random(SEED);
        List<LabeledSample> expanded = new ArrayList<>(
                SyntheticSamples.generate(random, SYNTHETIC_PER_SYMBOL));
        for (LabeledGlyph sample : knownSymbolsOnly(real)) {
            Glyph glyph = sample.getGlyph();
            expanded.add(new LabeledSample(sample.getSymbol(), GlyphFeatures.extract(glyph)));
            for (int i = 0; i < COPIES_PER_REAL_SAMPLE; i++) {
                expanded.add(new LabeledSample(sample.getSymbol(),
                        GlyphFeatures.extract(GlyphAugmenter.distort(glyph, random))));
            }
        }
        return NeuralSymbolClassifier.train(expanded, random, EPOCHS);
    }

    /** @return the stored model, or null if there is none or it cannot be read. */
    private NeuralSymbolClassifier loadStoredModel() {
        if (!Files.exists(modelFile)) {
            return null;
        }
        try (InputStream in = Files.newInputStream(modelFile)) {
            return NeuralSymbolClassifier.load(in);
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    private void store(NeuralSymbolClassifier model) {
        try {
            Path parent = modelFile.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Path temporary = modelFile.resolveSibling(modelFile.getFileName() + ".tmp");
            try (OutputStream out = Files.newOutputStream(temporary)) {
                model.save(out);
            }
            Files.move(temporary, modelFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not store the model in " + modelFile, e);
        }
    }
}
