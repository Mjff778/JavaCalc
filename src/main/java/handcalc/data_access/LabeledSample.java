package handcalc.data_access;

/**
 * A symbol as feature vector (see {@link GlyphFeatures}) together with the character it stands for.
 */
public final class LabeledSample {

    private final char label;
    private final float[] features;

    public LabeledSample(char label, float[] features) {
        this.label = label;
        this.features = features;
    }

    public char getLabel() {
        return label;
    }

    public float[] getFeatures() {
        return features;
    }
}
