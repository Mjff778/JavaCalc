package handcalc.view;

import handcalc.entity.Stroke;
import handcalc.interface_adapter.HistoryController;
import handcalc.interface_adapter.HistoryRow;
import handcalc.interface_adapter.HistoryState;
import handcalc.interface_adapter.HistoryViewModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

/**
 * Lists the earlier calculations; one can be put back on the canvas.
 */
public class HistoryView extends FloatingPane {

    private final HistoryController controller;
    private final DefaultListModel<HistoryRow> listModel = new DefaultListModel<>();
    private final JList<HistoryRow> list = new JList<>(listModel);
    private final JLabel message = Theme.muted(new JLabel(" "));
    private final Consumer<List<Stroke>> onLoad;

    public HistoryView(HistoryController controller, HistoryViewModel viewModel,
                       Consumer<List<Stroke>> onLoad) {
        this(controller, viewModel, onLoad, SwingUtilities::invokeLater);
    }

    public HistoryView(HistoryController controller, HistoryViewModel viewModel,
                       Consumer<List<Stroke>> onLoad, Executor ui) {
        this.controller = controller;
        this.onLoad = onLoad;
        setPreferredSize(new Dimension(520, 460));
        setBackdrop(new PaperPanel());

        JLabel title = new JLabel("History");
        title.setFont(Theme.font(Font.BOLD, 22f));

        list.setName("historyList");
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setFixedCellHeight(62);
        list.setCellRenderer(new RowRenderer());
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    loadSelected();
                }
            }
        });

        message.setName("historyMessage");

        JButton load = Theme.primary(new JButton("Load onto canvas"));
        load.setName("loadButton");
        load.addActionListener(e -> loadSelected());

        list.setOpaque(false);
        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);

        GlassPanel header = new GlassPanel(new BorderLayout(), null, 6);
        header.add(title, BorderLayout.CENTER);
        GlassPanel listCard = new GlassPanel(new BorderLayout(), null, 6);
        listCard.add(scroll, BorderLayout.CENTER);
        GlassPanel bottom = new GlassPanel(new BorderLayout(12, 0), null, 6);
        bottom.add(message, BorderLayout.CENTER);
        bottom.add(load, BorderLayout.EAST);

        floatAt(header, Anchor.TOP_STRETCH);
        floatAt(bottom, Anchor.BOTTOM_STRETCH);
        floatAt(listCard, Anchor.FILL);

        viewModel.addPropertyChangeListener(event -> {
            HistoryState state = (HistoryState) event.getNewValue();
            ui.execute(() -> render(state));
        });
    }

    /** Asks for the history again. */
    public void refresh() {
        controller.refresh();
    }

    private void render(HistoryState state) {
        listModel.clear();
        for (HistoryRow row : new ArrayList<>(state.getRows())) {
            listModel.addElement(row);
        }
        message.setText(state.getMessage().isEmpty() ? " " : state.getMessage());
    }

    private void loadSelected() {
        HistoryRow row = list.getSelectedValue();
        if (row != null) {
            onLoad.accept(row.getStrokes());
        }
    }

    /** Two lines per entry: the calculation in bold, the time underneath. */
    private static final class RowRenderer implements ListCellRenderer<HistoryRow> {
        private final JPanel panel = new JPanel(new BorderLayout());
        private final JLabel text = new JLabel();
        private final JLabel time = new JLabel();

        RowRenderer() {
            panel.setBorder(new EmptyBorder(8, 14, 8, 14));
            text.setFont(Theme.font(Font.BOLD, 18f));
            time.setFont(Theme.font(Font.PLAIN, 12f));
            time.setForeground(Theme.MUTED);
            panel.add(text, BorderLayout.CENTER);
            panel.add(time, BorderLayout.SOUTH);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends HistoryRow> list,
                HistoryRow row, int index, boolean selected, boolean focused) {
            text.setText(row.getText());
            time.setText(row.getTime());
            panel.setOpaque(selected);
            panel.setBackground(Theme.ACCENT_SOFT);
            text.setForeground(selected ? Theme.ACCENT : Theme.TEXT);
            return panel;
        }
    }
}
