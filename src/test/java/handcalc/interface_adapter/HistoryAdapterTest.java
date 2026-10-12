package handcalc.interface_adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import handcalc.entity.HistoryEntry;
import handcalc.entity.Point;
import handcalc.entity.Stroke;
import handcalc.use_case.history.HistoryInputBoundary;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class HistoryAdapterTest {

    private final HistoryViewModel historyModel = new HistoryViewModel();
    private final HistoryPresenter historyPresenter =
            new HistoryPresenter(historyModel, ZoneOffset.UTC);

    @Test
    void historyRowsShowExpressionAnswerAndTime() {
        Stroke stroke = new Stroke(Collections.singletonList(new Point(1, 1)));
        HistoryEntry entry = new HistoryEntry("1÷3", 1.0 / 3,
                Instant.parse("2026-10-11T20:15:30Z"), Collections.singletonList(stroke));

        historyPresenter.prepareHistoryView(Collections.singletonList(entry));

        HistoryRow row = historyModel.getState().getRows().get(0);
        assertEquals("1÷3 = 0.33333333", row.getText());
        assertEquals("2026-10-11 20:15", row.getTime());
        assertEquals(1, row.getStrokes().size());
    }

    @Test
    void emptyHistoryHasAFriendlyMessage() {
        historyPresenter.prepareHistoryView(Collections.emptyList());

        assertTrue(historyModel.getState().getRows().isEmpty());
        assertEquals("No calculations yet", historyModel.getState().getMessage());
    }

    @Test
    void historyFailureClearsTheRows() {
        historyPresenter.prepareFailView("Could not read your history");

        assertTrue(historyModel.getState().getRows().isEmpty());
        assertEquals("Could not read your history", historyModel.getState().getMessage());
    }

    @Test
    void historyControllerRefreshes() {
        int[] calls = {0};
        HistoryInputBoundary useCase = () -> calls[0]++;

        new HistoryController(useCase).refresh();

        assertEquals(1, calls[0]);
    }
}
