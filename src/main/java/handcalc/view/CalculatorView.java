package handcalc.view;

import handcalc.entity.Stroke;
import handcalc.interface_adapter.CalculatorState;
import handcalc.interface_adapter.CalculatorViewModel;
import handcalc.interface_adapter.CorrectSymbolController;
import handcalc.interface_adapter.ReadSymbol;
import handcalc.interface_adapter.RecognizeAndSolveController;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.Collections;
import java.util.List;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

/**
 * The calculator screen: a writing canvas, its action buttons, the answer line, and a row
 * showing how each written symbol was read so that a misreading can be corrected.
 */
public class CalculatorView extends FloatingPane {

    private final CanvasPanel canvas = new CanvasPanel();
    private final JLabel resultLabel = new JLabel(" ");
    private final JLabel placeholderLabel =
            Theme.muted(new JLabel("Your answer will appear here"));
    private final JLabel readAsLabel = Theme.muted(new JLabel("Read as — tap a symbol to correct it"));
    private final JPanel symbolsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    private final CorrectSymbolController correctController;

    public CalculatorView(RecognizeAndSolveController solveController,
                          CorrectSymbolController correctController,
                          CalculatorViewModel viewModel) {
        this(solveController, correctController, viewModel, null);
    }

    /**
     * @param openCollector opens the sample collection window; null hides the button
     */
    public CalculatorView(RecognizeAndSolveController solveController,
                          CorrectSymbolController correctController,
                          CalculatorViewModel viewModel, Runnable openCollector) {
        this.correctController = correctController;
        setPreferredSize(new Dimension(900, 640));
        canvas.setFullBleed(true);
        setBackdrop(canvas);

        canvas.setName("canvas");
        // the symbol buttons refer to symbol positions, which change when the writing does
        canvas.addChangeListener(e -> showSymbols(Collections.emptyList()));

        resultLabel.setName("resultLabel");
        resultLabel.setFont(Theme.font(Font.BOLD, 30f));
        resultLabel.setForeground(Theme.TEXT);

        JButton calculate = Theme.primary(new JButton("Calculate"));
        calculate.setName("calculateButton");
        calculate.addActionListener(e -> solveController.execute(canvas.getStrokes()));

        JButton undo = Theme.secondary(new JButton("Undo"));
        undo.setName("undoButton");
        undo.addActionListener(e -> canvas.undo());

        JButton clear = Theme.secondary(new JButton("Clear"));
        clear.setName("clearButton");
        clear.addActionListener(e -> {
            canvas.clear();
            showResult(" ", Theme.TEXT);
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        buttons.add(undo);
        buttons.add(clear);
        buttons.add(calculate);

        GlassPanel buttonRow = new GlassPanel(new BorderLayout(), null, 8);
        if (openCollector != null) {
            JButton collect = Theme.secondary(new JButton("Teach it my handwriting…"));
            collect.setName("collectButton");
            collect.addActionListener(e -> openCollector.run());
            JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            left.setOpaque(false);
            left.add(collect);
            buttonRow.add(left, BorderLayout.WEST);
        }
        buttonRow.add(buttons, BorderLayout.EAST);

        symbolsPanel.setOpaque(false);
        readAsLabel.setVisible(false);

        GlassPanel answer = new GlassPanel(null, null, 10);
        answer.setLayout(new BoxLayout(answer, BoxLayout.Y_AXIS));
        placeholderLabel.setFont(Theme.font(Font.PLAIN, 18f));
        for (JComponent c : new JComponent[] {resultLabel, placeholderLabel, readAsLabel,
                symbolsPanel}) {
            c.setAlignmentX(LEFT_ALIGNMENT);
        }
        answer.add(resultLabel);
        answer.add(placeholderLabel);
        answer.add(Box.createVerticalStrut(6));
        answer.add(readAsLabel);
        answer.add(Box.createVerticalStrut(6));
        answer.add(symbolsPanel);

        // bottom stack, lowest first: the buttons, then the answer above them
        floatAt(buttonRow, Anchor.BOTTOM_CENTER, 600, 900);
        floatAt(answer, Anchor.BOTTOM_CENTER, 600, 900);

        showResult(" ", Theme.TEXT);

        viewModel.addPropertyChangeListener(event -> {
            if (CalculatorViewModel.STATE_PROPERTY.equals(event.getPropertyName())) {
                render((CalculatorState) event.getNewValue());
            }
        });
    }

    /** Floats a bar (such as the title header) across the top of the window. */
    public void addHeader(JComponent header) {
        floatAt(header, Anchor.TOP_STRETCH);
    }

    /** Puts earlier handwriting (for example from the history) back on the canvas. */
    public void loadStrokes(List<Stroke> strokes) {
        canvas.setStrokes(strokes);
        showResult(" ", Theme.TEXT);
    }

    private void render(CalculatorState state) {
        if (state.getError() != null) {
            String read = state.getExpression().isEmpty() ? "" : state.getExpression() + "  –  ";
            showResult(read + state.getError(), Theme.ERROR);
        } else if (state.getResult() != null) {
            showResult(state.getExpression() + " = " + state.getResult(), Theme.ACCENT);
        } else {
            showResult(" ", Theme.TEXT);
        }
        showSymbols(state.getSymbols());
    }

    private void showResult(String text, Color color) {
        resultLabel.setText(text);
        resultLabel.setForeground(color);
        boolean blank = text.trim().isEmpty();
        resultLabel.setVisible(!blank);
        placeholderLabel.setVisible(blank);
    }

    private void showSymbols(List<ReadSymbol> symbols) {
        symbolsPanel.removeAll();
        readAsLabel.setVisible(!symbols.isEmpty());
        for (int i = 0; i < symbols.size(); i++) {
            symbolsPanel.add(symbolButton(i, symbols.get(i)));
            symbolsPanel.add(Box.createHorizontalStrut(8));
        }
        symbolsPanel.revalidate();
        symbolsPanel.repaint();
    }

    private JButton symbolButton(int index, ReadSymbol read) {
        JButton button = Theme.chip(new JButton(String.valueOf(read.getSymbol())));
        button.setName("symbolButton" + index);

        JPopupMenu menu = new JPopupMenu();
        for (ReadSymbol.Alternative alternative : read.getAlternatives()) {
            JMenuItem item = new JMenuItem(
                    alternative.getSymbol() + "      " + alternative.getPercent() + "%");
            item.setName("alternative_" + alternative.getSymbol());
            item.setFont(Theme.font(Font.PLAIN, 16f));
            item.addActionListener(e ->
                    correctController.execute(canvas.getStrokes(), index, alternative.getSymbol()));
            menu.add(item);
        }
        button.setComponentPopupMenu(menu);
        button.addActionListener(e -> menu.show(button, 0, button.getHeight() + 4));
        return button;
    }
}
