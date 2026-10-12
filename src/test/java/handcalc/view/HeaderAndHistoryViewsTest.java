package handcalc.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import handcalc.entity.Point;
import handcalc.entity.Stroke;
import handcalc.interface_adapter.HistoryController;
import handcalc.interface_adapter.HistoryRow;
import handcalc.interface_adapter.HistoryState;
import handcalc.interface_adapter.HistoryViewModel;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import org.junit.jupiter.api.Test;

class HeaderAndHistoryViewsTest {

    @SuppressWarnings("unchecked")
    private static <T extends Component> T find(Container root, String name, Class<T> type) {
        for (Component c : root.getComponents()) {
            if (name.equals(c.getName()) && type.isInstance(c)) {
                return (T) c;
            }
            if (c instanceof Container) {
                T found = find((Container) c, name, type);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    @Test
    void headerHistoryButtonOpensTheHistory() {
        int[] opened = {0};
        HeaderBar bar = new HeaderBar(() -> opened[0]++);

        find(bar, "historyButton", JButton.class).doClick();

        assertEquals(1, opened[0]);
    }

    @Test
    void historyViewListsRowsAndLoadsTheSelectedOne() {
        HistoryViewModel model = new HistoryViewModel();
        List<List<Stroke>> loaded = new ArrayList<>();
        int[] refreshes = {0};
        Stroke stroke = new Stroke(Collections.singletonList(new Point(5, 6)));
        HistoryView view = new HistoryView(new HistoryController(() -> refreshes[0]++), model,
                loaded::add, Runnable::run);

        view.refresh();
        model.setState(HistoryState.of(Arrays.asList(
                new HistoryRow("2+2 = 4", "2026-10-11 20:15", Collections.singletonList(stroke)),
                new HistoryRow("1+1 = 2", "2026-10-11 20:10", Collections.emptyList())), ""));

        JList<?> list = find(view, "historyList", JList.class);
        assertEquals(2, list.getModel().getSize());
        assertTrue(list.getModel().getElementAt(0).toString().contains("2+2 = 4"));

        find(view, "loadButton", JButton.class).doClick();
        assertTrue(loaded.isEmpty(), "nothing selected yet");

        list.setSelectedIndex(0);
        find(view, "loadButton", JButton.class).doClick();

        assertEquals(1, refreshes[0]);
        assertEquals(1, loaded.size());
        assertEquals(5, loaded.get(0).get(0).getMinX());
    }

    @Test
    void historyViewShowsTheMessageWhenThereAreNoRows() {
        HistoryViewModel model = new HistoryViewModel();
        HistoryView view = new HistoryView(new HistoryController(() -> { }), model,
                strokes -> { }, Runnable::run);

        model.setState(HistoryState.of(Collections.emptyList(), "No calculations yet"));

        assertEquals("No calculations yet",
                find(view, "historyMessage", JLabel.class).getText());
    }
}
