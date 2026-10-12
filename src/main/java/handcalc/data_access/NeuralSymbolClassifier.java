package handcalc.data_access;

import handcalc.entity.Glyph;
import handcalc.use_case.recognize_and_solve.Candidate;
import handcalc.use_case.recognize_and_solve.TrainableSymbolClassifier;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Reads symbols with a small neural network trained on rasterized handwriting.
 */
public class NeuralSymbolClassifier implements TrainableSymbolClassifier {

    static final int HIDDEN_UNITS = 64;
    private static final int MAGIC = 0x48435633; // "HCV3"
    private static final int FINE_TUNE_VARIANTS = 16;
    private static final int FINE_TUNE_REPLAY = 32;
    private static final int FINE_TUNE_ROUNDS = 20;

    private final NeuralNetwork network;
    private final String symbols;
    private final Random random = new Random(207);
    private int collectedSampleCount;

    private NeuralSymbolClassifier(NeuralNetwork network, String symbols) {
        this.network = network;
        this.symbols = symbols;
    }

    /**
     * Trains a new network from scratch.
     *
     * @param samples labeled feature vectors (see {@link GlyphFeatures}); labels must be in {@link SymbolTemplates#SYMBOLS}
     * @param epochs passes over the samples
     */
    public static NeuralSymbolClassifier train(List<LabeledSample> samples, Random random,
                                               int epochs) {
        String symbols = SymbolTemplates.SYMBOLS;
        List<float[]> xs = new ArrayList<>();
        int[] labels = new int[samples.size()];
        for (int i = 0; i < samples.size(); i++) {
            xs.add(samples.get(i).getFeatures());
            labels[i] = indexOf(symbols, samples.get(i).getLabel());
        }
        NeuralNetwork network = NeuralNetwork.create(
                GlyphFeatures.LENGTH, HIDDEN_UNITS, symbols.length(),
                random);
        network.fit(xs, labels, epochs, 0.03, 0.9, random);
        return new NeuralSymbolClassifier(network, symbols);
    }

    /** How many collected samples the model has seen, so a restart knows what is new. */
    public synchronized int getCollectedSampleCount() {
        return collectedSampleCount;
    }

    public synchronized void setCollectedSampleCount(int count) {
        this.collectedSampleCount = count;
    }

    @Override
    public synchronized List<Candidate> classify(Glyph glyph) {
        double[] probabilities = network.predict(GlyphFeatures.extract(glyph));
        List<Candidate> candidates = new ArrayList<>();
        for (int i = 0; i < probabilities.length; i++) {
            candidates.add(new Candidate(symbols.charAt(i), probabilities[i]));
        }
        candidates.sort(Comparator.comparingDouble(Candidate::getConfidence).reversed());
        return candidates;
    }

    /**
     * Fine-tunes on a corrected writing, mixed with synthetic samples of every symbol so the
     * rest of the model is not forgotten. Repeats until the writing is read as {@code symbol}.
     */
    @Override
    public synchronized void learn(Glyph glyph, char symbol) {
        int target = indexOf(symbols, symbol);
        for (int round = 0; round < FINE_TUNE_ROUNDS; round++) {
            List<float[]> xs = new ArrayList<>();
            List<Integer> labels = new ArrayList<>();
            float[] original = GlyphFeatures.extract(glyph);
            for (int i = 0; i < 2; i++) {
                xs.add(original);
                labels.add(target);
            }
            for (int i = 0; i < FINE_TUNE_VARIANTS; i++) {
                xs.add(GlyphFeatures.extract(GlyphAugmenter.distort(glyph, random)));
                labels.add(target);
            }
            for (int i = 0; i < FINE_TUNE_REPLAY; i++) {
                int other = random.nextInt(symbols.length());
                xs.add(GlyphFeatures.extract(
                        SyntheticSamples.randomGlyph(symbols.charAt(other), random)));
                labels.add(other);
            }
            int[] labelArray = new int[labels.size()];
            for (int i = 0; i < labelArray.length; i++) {
                labelArray[i] = labels.get(i);
            }
            network.fit(xs, labelArray, 3, 0.03, 1.0, random);

            double[] p = network.predict(original);
            if (argMax(p) == target) {
                return;
            }
        }
    }

    @Override
    public boolean canLearn(char symbol) {
        return symbols.indexOf(symbol) >= 0;
    }

    public synchronized void save(OutputStream stream) throws IOException {
        DataOutputStream out = new DataOutputStream(stream);
        out.writeInt(MAGIC);
        out.writeUTF(symbols);
        out.writeInt(collectedSampleCount);
        network.write(out);
        out.flush();
    }

    public static NeuralSymbolClassifier load(InputStream stream) throws IOException {
        DataInputStream in = new DataInputStream(stream);
        if (in.readInt() != MAGIC) {
            throw new IOException("Not a handwriting model file");
        }
        String symbols = in.readUTF();
        if (!symbols.equals(SymbolTemplates.SYMBOLS)) {
            throw new IOException("Model was trained for a different set of symbols");
        }
        int collected = in.readInt();
        NeuralNetwork network = NeuralNetwork.read(in);
        if (network.getOutputs() != symbols.length()
                || network.getInputs() != GlyphFeatures.LENGTH) {
            throw new IOException("Model does not match this program");
        }
        NeuralSymbolClassifier classifier = new NeuralSymbolClassifier(network, symbols);
        classifier.collectedSampleCount = collected;
        return classifier;
    }

    private static int indexOf(String symbols, char symbol) {
        int index = symbols.indexOf(symbol);
        if (index < 0) {
            throw new IllegalArgumentException("Unsupported symbol: " + symbol);
        }
        return index;
    }

    private static int argMax(double[] values) {
        int best = 0;
        for (int i = 1; i < values.length; i++) {
            if (values[i] > values[best]) {
                best = i;
            }
        }
        return best;
    }
}
