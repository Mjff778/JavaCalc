package handcalc.entity;

/**
 * Thrown when an {@link Expression} cannot be evaluated: either it is not written correctly,
 * in which case the span of the text at fault is known where possible, or it is written
 * correctly but its value is undefined, such as a division by zero.
 */
public class InvalidExpressionException extends Exception {

    private final int start;
    private final int end;
    private final boolean undefined;

    public InvalidExpressionException(String message) {
        this(message, -1, -1, false);
    }

    /** A writing error in the characters from {@code start} up to (excluding) {@code end}. */
    public InvalidExpressionException(String message, int start, int end) {
        this(message, start, end, false);
    }

    private InvalidExpressionException(String message, int start, int end, boolean undefined) {
        super(message);
        this.start = start;
        this.end = end;
        this.undefined = undefined;
    }

    /** A correctly written expression whose value does not exist. */
    public static InvalidExpressionException undefined(String message) {
        return new InvalidExpressionException(message, -1, -1, true);
    }

    /** @return true if the expression is written correctly but has no value. */
    public boolean isUndefined() {
        return undefined;
    }

    /** @return true if the characters at fault are known. */
    public boolean hasPosition() {
        return start >= 0;
    }

    /** @return the index of the first character at fault, or -1 if unknown. */
    public int getStart() {
        return start;
    }

    /** @return the index just past the last character at fault, or -1 if unknown. */
    public int getEnd() {
        return end;
    }
}
