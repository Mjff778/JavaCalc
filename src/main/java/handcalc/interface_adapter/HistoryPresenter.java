package handcalc.interface_adapter;

import handcalc.entity.HistoryEntry;
import handcalc.use_case.history.HistoryOutputBoundary;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Turns history entries into display rows.
 */
public class HistoryPresenter implements HistoryOutputBoundary {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final HistoryViewModel viewModel;
    private final ZoneId zone;

    public HistoryPresenter(HistoryViewModel viewModel, ZoneId zone) {
        this.viewModel = viewModel;
        this.zone = zone;
    }

    @Override
    public void prepareHistoryView(List<HistoryEntry> entries) {
        List<HistoryRow> rows = new ArrayList<>();
        for (HistoryEntry entry : entries) {
            rows.add(new HistoryRow(
                    entry.getExpression() + " = " + ResultFormat.format(entry.getResult()),
                    TIME_FORMAT.format(entry.getTime().atZone(zone)),
                    entry.getStrokes()));
        }
        viewModel.setState(HistoryState.of(rows,
                rows.isEmpty() ? "No calculations yet" : ""));
    }

    @Override
    public void prepareFailView(String message) {
        viewModel.setState(HistoryState.of(Collections.emptyList(), message));
    }
}
