package handcalc.app;

import handcalc.view.CalculatorView;
import handcalc.view.HistoryView;
import handcalc.view.Theme;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * Starts the handwritten calculator.
 */
public final class Main {

    private JFrame collectorWindow;
    private JFrame historyWindow;
    private HistoryView historyView;

    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Theme.install();
            new Main().show(new CalculatorApp());
        });
    }

    private void show(CalculatorApp app) {
        JFrame frame = new JFrame("Handwritten Calculator");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        CalculatorView calculator = app.createView(() -> openCollector(app, frame));
        calculator.addHeader(app.createHeaderBar(() -> openHistory(app, frame, calculator)));

        frame.setContentPane(calculator);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void openHistory(CalculatorApp app, JFrame owner, CalculatorView calculator) {
        if (historyWindow == null) {
            historyWindow = new JFrame("History");
            historyWindow.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
            historyView = app.createHistoryView(strokes -> {
                calculator.loadStrokes(strokes);
                owner.toFront();
            });
            historyWindow.setContentPane(historyView);
            historyWindow.setSize(520, 420);
            historyWindow.setLocationRelativeTo(owner);
        }
        historyView.refresh();
        historyWindow.setVisible(true);
        historyWindow.toFront();
    }

    private void openCollector(CalculatorApp app, JFrame owner) {
        if (collectorWindow == null) {
            collectorWindow = new JFrame("Collect handwriting samples");
            collectorWindow.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
            collectorWindow.setContentPane(app.createCollectorView());
            collectorWindow.pack();
            collectorWindow.setLocationRelativeTo(owner);
        }
        collectorWindow.setVisible(true);
        collectorWindow.toFront();
    }
}
