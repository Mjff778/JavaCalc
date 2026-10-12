package handcalc.data_access;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * A small fully connected network (input → ReLU hidden layer → softmax), trained by
 * per-sample stochastic gradient descent. Written out by hand so training needs no
 * native libraries.
 *
 * <p>Images are mostly black, so only non-zero input pixels are visited.
 */
final class NeuralNetwork {

    private static final double WEIGHT_DECAY = 1e-5;

    private final int inputs;
    private final int hidden;
    private final int outputs;
    /** weights from input i to hidden j at [i * hidden + j] */
    private final float[] w1;
    private final float[] b1;
    /** weights from hidden j to output o at [o * hidden + j] */
    private final float[] w2;
    private final float[] b2;

    private NeuralNetwork(int inputs, int hidden, int outputs, float[] w1, float[] b1,
                          float[] w2, float[] b2) {
        this.inputs = inputs;
        this.hidden = hidden;
        this.outputs = outputs;
        this.w1 = w1;
        this.b1 = b1;
        this.w2 = w2;
        this.b2 = b2;
    }

    static NeuralNetwork create(int inputs, int hidden, int outputs, Random random) {
        float[] w1 = new float[inputs * hidden];
        float[] w2 = new float[outputs * hidden];
        double scale1 = Math.sqrt(2.0 / inputs);
        double scale2 = Math.sqrt(2.0 / hidden);
        for (int i = 0; i < w1.length; i++) {
            w1[i] = (float) (random.nextGaussian() * scale1);
        }
        for (int i = 0; i < w2.length; i++) {
            w2[i] = (float) (random.nextGaussian() * scale2);
        }
        return new NeuralNetwork(inputs, hidden, outputs, w1, new float[hidden], w2,
                new float[outputs]);
    }

    int getInputs() {
        return inputs;
    }

    int getOutputs() {
        return outputs;
    }

    /** @return softmax probabilities over the outputs. */
    double[] predict(float[] x) {
        float[] h = new float[hidden];
        return forward(x, nonZero(x), h);
    }

    /**
     * Trains for the given number of epochs; the learning rate shrinks by {@code decay}
     * after every epoch.
     */
    void fit(List<float[]> xs, int[] labels, int epochs, double learningRate, double decay,
             Random random) {
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < xs.size(); i++) {
            order.add(i);
        }
        int[][] nonZero = new int[xs.size()][];
        for (int i = 0; i < xs.size(); i++) {
            nonZero[i] = nonZero(xs.get(i));
        }
        float[] h = new float[hidden];
        double lr = learningRate;
        for (int epoch = 0; epoch < epochs; epoch++) {
            Collections.shuffle(order, random);
            for (int index : order) {
                step(xs.get(index), nonZero[index], labels[index], lr, h);
            }
            lr *= decay;
        }
    }

    private void step(float[] x, int[] nz, int label, double lr, float[] h) {
        double[] p = forward(x, nz, h);
        float[] dLogits = new float[outputs];
        for (int o = 0; o < outputs; o++) {
            dLogits[o] = (float) (p[o] - (o == label ? 1 : 0));
        }
        float[] dHidden = new float[hidden];
        for (int o = 0; o < outputs; o++) {
            float g = dLogits[o];
            int base = o * hidden;
            for (int j = 0; j < hidden; j++) {
                dHidden[j] += g * w2[base + j];
                w2[base + j] -= (float) (lr * (g * h[j] + WEIGHT_DECAY * w2[base + j]));
            }
            b2[o] -= (float) (lr * g);
        }
        for (int j = 0; j < hidden; j++) {
            if (h[j] <= 0) {
                dHidden[j] = 0;
            }
            b1[j] -= (float) (lr * dHidden[j]);
        }
        for (int i : nz) {
            int base = i * hidden;
            float xi = x[i];
            for (int j = 0; j < hidden; j++) {
                w1[base + j] -= (float) (lr * (dHidden[j] * xi + WEIGHT_DECAY * w1[base + j]));
            }
        }
    }

    /** Fills {@code h} with the hidden activations and returns the output probabilities. */
    private double[] forward(float[] x, int[] nz, float[] h) {
        System.arraycopy(b1, 0, h, 0, hidden);
        for (int i : nz) {
            int base = i * hidden;
            float xi = x[i];
            for (int j = 0; j < hidden; j++) {
                h[j] += xi * w1[base + j];
            }
        }
        for (int j = 0; j < hidden; j++) {
            if (h[j] < 0) {
                h[j] = 0;
            }
        }
        double[] logits = new double[outputs];
        double max = Double.NEGATIVE_INFINITY;
        for (int o = 0; o < outputs; o++) {
            double sum = b2[o];
            int base = o * hidden;
            for (int j = 0; j < hidden; j++) {
                sum += w2[base + j] * h[j];
            }
            logits[o] = sum;
            max = Math.max(max, sum);
        }
        double total = 0;
        for (int o = 0; o < outputs; o++) {
            logits[o] = Math.exp(logits[o] - max);
            total += logits[o];
        }
        for (int o = 0; o < outputs; o++) {
            logits[o] /= total;
        }
        return logits;
    }

    private static int[] nonZero(float[] x) {
        int count = 0;
        for (float v : x) {
            if (v != 0f) {
                count++;
            }
        }
        int[] indices = new int[count];
        int k = 0;
        for (int i = 0; i < x.length; i++) {
            if (x[i] != 0f) {
                indices[k++] = i;
            }
        }
        return indices;
    }

    void write(DataOutputStream out) throws IOException {
        out.writeInt(inputs);
        out.writeInt(hidden);
        out.writeInt(outputs);
        for (float[] array : new float[][] {w1, b1, w2, b2}) {
            for (float v : array) {
                out.writeFloat(v);
            }
        }
    }

    static NeuralNetwork read(DataInputStream in) throws IOException {
        int inputs = in.readInt();
        int hidden = in.readInt();
        int outputs = in.readInt();
        if (inputs <= 0 || hidden <= 0 || outputs <= 0
                || (long) inputs * hidden > 10_000_000L) {
            throw new IOException("Corrupt model file");
        }
        float[] w1 = readArray(in, inputs * hidden);
        float[] b1 = readArray(in, hidden);
        float[] w2 = readArray(in, outputs * hidden);
        float[] b2 = readArray(in, outputs);
        return new NeuralNetwork(inputs, hidden, outputs, w1, b1, w2, b2);
    }

    private static float[] readArray(DataInputStream in, int length) throws IOException {
        float[] array = new float[length];
        for (int i = 0; i < length; i++) {
            array[i] = in.readFloat();
        }
        return array;
    }
}
