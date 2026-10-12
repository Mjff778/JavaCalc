package handcalc.use_case.recognize_and_solve;

/**
 * One possible reading of a written symbol, with the classifier's confidence in [0,1].
 */
public final class Candidate {

    private final char symbol;
    private final double confidence;

    public Candidate(char symbol, double confidence) {
        this.symbol = symbol;
        this.confidence = confidence;
    }

    public char getSymbol() {
        return symbol;
    }

    public double getConfidence() {
        return confidence;
    }
}
