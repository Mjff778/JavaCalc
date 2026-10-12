package handcalc.use_case.train_model;

/**
 * Outcome of a training run: how many samples were used and, if enough were available,
 * the accuracy on samples that were held back from training.
 */
public final class TrainingReport {

    private final int sampleCount;
    private final double validationAccuracy;
    private final int validationCount;

    public TrainingReport(int sampleCount, double validationAccuracy, int validationCount) {
        this.sampleCount = sampleCount;
        this.validationAccuracy = validationAccuracy;
        this.validationCount = validationCount;
    }

    public int getSampleCount() {
        return sampleCount;
    }

    /** @return accuracy in [0,1] on held-out samples; meaningless unless {@link #hasValidation}. */
    public double getValidationAccuracy() {
        return validationAccuracy;
    }

    public int getValidationCount() {
        return validationCount;
    }

    public boolean hasValidation() {
        return validationCount > 0 && !Double.isNaN(validationAccuracy);
    }
}
