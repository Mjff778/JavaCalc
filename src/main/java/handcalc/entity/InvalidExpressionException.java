package handcalc.entity;

/**
 * Thrown when an {@link Expression} cannot be evaluated.
 */
public class InvalidExpressionException extends Exception {

    public InvalidExpressionException(String message) {
        super(message);
    }
}
