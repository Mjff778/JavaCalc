package handcalc.interface_adapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * What the expression practice shows: the expression to write and how the last try went.
 * Immutable.
 */
public final class PracticeExpressionState {

    /** How the parts are arranged on screen. */
    public enum Layout { LINE, POWER, FRACTION, ROOT }

    private final Layout layout;
    private final List<String> parts;
    private final String message;
    private final boolean saved;
    private final boolean error;
    private final int revision;

    public PracticeExpressionState(Layout layout, List<String> parts, String message,
                                   boolean saved, boolean error, int revision) {
        this.layout = layout;
        this.parts = Collections.unmodifiableList(new ArrayList<>(parts));
        this.message = message;
        this.saved = saved;
        this.error = error;
        this.revision = revision;
    }

    public static PracticeExpressionState empty() {
        return new PracticeExpressionState(Layout.LINE, Collections.singletonList(""), "",
                false, false, 0);
    }

    public PracticeExpressionState withPrompt(Layout newLayout, List<String> newParts,
                                              String newMessage, boolean wasSaved) {
        return new PracticeExpressionState(newLayout, newParts, newMessage, wasSaved, false,
                revision + 1);
    }

    public PracticeExpressionState withError(String newMessage) {
        return new PracticeExpressionState(layout, parts, newMessage, false, true, revision + 1);
    }

    public Layout getLayout() {
        return layout;
    }

    public List<String> getParts() {
        return parts;
    }

    public String getMessage() {
        return message;
    }

    /** @return true if the latest update stored samples, so the canvas can be cleared. */
    public boolean isSaved() {
        return saved;
    }

    public boolean isError() {
        return error;
    }

    /** Changes with every update, so listeners are told even when the text is the same. */
    public int getRevision() {
        return revision;
    }
}
