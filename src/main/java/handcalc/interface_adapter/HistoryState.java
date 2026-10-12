package handcalc.interface_adapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * What the history window shows: the rows, or a message explaining why there are none.
 */
public final class HistoryState {

    private final List<HistoryRow> rows;
    private final String message;

    private HistoryState(List<HistoryRow> rows, String message) {
        this.rows = Collections.unmodifiableList(new ArrayList<>(rows));
        this.message = message;
    }

    public static HistoryState empty() {
        return new HistoryState(Collections.emptyList(), "");
    }

    public static HistoryState of(List<HistoryRow> rows, String message) {
        return new HistoryState(rows, message);
    }

    public List<HistoryRow> getRows() {
        return rows;
    }

    public String getMessage() {
        return message;
    }
}
