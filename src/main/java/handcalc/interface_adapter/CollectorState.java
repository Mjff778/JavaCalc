package handcalc.interface_adapter;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * What the sample collection window shows. Immutable; the {@code with...} methods return a
 * changed copy.
 */
public final class CollectorState {

    private final Map<Character, Integer> counts;
    private final char nextSymbol;
    private final int targetPerSymbol;
    private final String message;
    private final boolean saved;
    private final boolean error;
    private final String trainingStatus;
    private final int trainingRevision;

    private CollectorState(Map<Character, Integer> counts, char nextSymbol, int targetPerSymbol,
                           String message, boolean saved, boolean error, String trainingStatus,
                           int trainingRevision) {
        this.counts = Collections.unmodifiableMap(new LinkedHashMap<>(counts));
        this.nextSymbol = nextSymbol;
        this.targetPerSymbol = targetPerSymbol;
        this.message = message;
        this.saved = saved;
        this.error = error;
        this.trainingStatus = trainingStatus;
        this.trainingRevision = trainingRevision;
    }

    public static CollectorState empty() {
        return new CollectorState(Collections.emptyMap(), ' ', 0, "", false, false, "", 0);
    }

    public CollectorState withProgress(Map<Character, Integer> newCounts, char newNextSymbol,
                                       int newTarget, String newMessage, boolean wasSaved,
                                       boolean isError) {
        return new CollectorState(newCounts, newNextSymbol, newTarget, newMessage, wasSaved,
                isError, trainingStatus, trainingRevision);
    }

    public CollectorState withTrainingStatus(String newStatus) {
        return new CollectorState(counts, nextSymbol, targetPerSymbol, message, false, error,
                newStatus, trainingRevision + 1);
    }

    public Map<Character, Integer> getCounts() {
        return counts;
    }

    public char getNextSymbol() {
        return nextSymbol;
    }

    public int getTargetPerSymbol() {
        return targetPerSymbol;
    }

    public String getMessage() {
        return message;
    }

    /** @return true if the latest update stored a sample, so the canvas can be cleared. */
    public boolean isSaved() {
        return saved;
    }

    public boolean isError() {
        return error;
    }

    public String getTrainingStatus() {
        return trainingStatus;
    }

    /** Changes every time a training outcome arrives, even if its text is the same. */
    public int getTrainingRevision() {
        return trainingRevision;
    }
}
