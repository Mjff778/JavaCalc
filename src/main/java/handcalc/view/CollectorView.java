package handcalc.view;

import handcalc.interface_adapter.CollectSampleController;
import handcalc.interface_adapter.CollectorState;
import handcalc.interface_adapter.CollectorViewModel;
import handcalc.interface_adapter.PracticeExpressionController;
import handcalc.interface_adapter.PracticeExpressionState;
import handcalc.interface_adapter.PracticeExpressionViewModel;
import handcalc.interface_adapter.TrainModelController;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ButtonGroup;
import javax.swing.JProgressBar;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

/**
 * The sample collection window: it asks for one symbol at a time (or, in expression mode,
 * for a whole made-up expression), stores what is written as training samples, and can train
 * the recognition model from everything collected.
 */
public class CollectorView extends FloatingPane {

    private static final java.awt.Color CELL = new java.awt.Color(255, 255, 255, 140);

    private final CanvasPanel canvas = new CanvasPanel();
    private final JLabel promptLabel = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel progressLabel = new JLabel(" ");
    private final JLabel messageLabel = new JLabel(" ");
    private final JLabel trainingLabel = Theme.muted(new JLabel(" "));
    private final JComboBox<Character> symbolChoice = new JComboBox<>();
    private final JPanel countsPanel = new JPanel(new GridLayout(0, 13, 5, 5));
    private final JProgressBar overall = new JProgressBar(0, 100);
    private final JButton trainButton = Theme.secondary(new JButton("Train model"));
    private final Map<Character, JLabel> countLabels = new LinkedHashMap<>();
    private final Map<Character, RoundedPanel> cells = new LinkedHashMap<>();

    private final RoundedPanel tile =
            new RoundedPanel(new BorderLayout(), Theme.ACCENT_SOFT, null, 24);
    private final ExpressionPreview preview = new ExpressionPreview();
    private final JLabel askLabel = new JLabel("Please write this symbol");
    private final JLabel tipLabel =
            Theme.muted(new JLabel("All strokes count as one symbol · no limit"));
    private final JPanel chooseRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    private final JButton skipButton = Theme.secondary(new JButton("Skip this one"));
    private boolean expressionMode;

    private final CollectSampleController collectController;
    private final PracticeExpressionController practiceController;
    private final TrainModelController trainController;
    private final Executor background;
    private final Executor ui;
    private int seenTrainingRevision;

    /** Uses a background thread for training and the Swing event thread for updates. */
    public CollectorView(CollectSampleController collectController,
                         TrainModelController trainController, CollectorViewModel viewModel) {
        this(collectController, trainController, viewModel, daemonExecutor(),
                SwingUtilities::invokeLater);
    }

    /**
     * @param background runs the slow training off the event thread
     * @param ui runs view updates on the event thread
     */
    public CollectorView(CollectSampleController collectController,
                         TrainModelController trainController, CollectorViewModel viewModel,
                         Executor background, Executor ui) {
        this(collectController, null, null, trainController, viewModel, background, ui);
    }

    /**
     * @param practiceController hands out made-up expressions to write; null leaves out the
     *                           expression mode
     * @param practiceModel what the practice currently shows
     */
    public CollectorView(CollectSampleController collectController,
                         PracticeExpressionController practiceController,
                         PracticeExpressionViewModel practiceModel,
                         TrainModelController trainController, CollectorViewModel viewModel) {
        this(collectController, practiceController, practiceModel, trainController, viewModel,
                daemonExecutor(), SwingUtilities::invokeLater);
    }

    public CollectorView(CollectSampleController collectController,
                         PracticeExpressionController practiceController,
                         PracticeExpressionViewModel practiceModel,
                         TrainModelController trainController, CollectorViewModel viewModel,
                         Executor background, Executor ui) {
        this.collectController = collectController;
        this.practiceController = practiceController;
        this.trainController = trainController;
        this.background = background;
        this.ui = ui;
        setPreferredSize(new Dimension(1000, 680));

        canvas.setName("canvas");
        canvas.setFullBleed(true);
        setBackdrop(canvas);
        floatAt(buildPrompt(), Anchor.TOP_LEFT);
        if (practiceController != null) {
            floatAt(buildModeSwitch(), Anchor.TOP_LEFT);
        }
        floatAt(buildTraining(), Anchor.TOP_RIGHT, 440, 640);
        // bottom stack, lowest first: the sample counts, then the save bar above them
        floatAt(buildCounts(), Anchor.BOTTOM_CENTER);
        floatAt(buildActions(), Anchor.BOTTOM_CENTER, 560, 760);

        viewModel.addPropertyChangeListener(event -> {
            if (CollectorViewModel.STATE_PROPERTY.equals(event.getPropertyName())) {
                CollectorState state = (CollectorState) event.getNewValue();
                this.ui.execute(() -> render(state));
            }
        });
        if (practiceModel != null) {
            practiceModel.addPropertyChangeListener(event -> {
                if (PracticeExpressionViewModel.STATE_PROPERTY.equals(event.getPropertyName())) {
                    PracticeExpressionState state = (PracticeExpressionState) event.getNewValue();
                    this.ui.execute(() -> renderPractice(state));
                }
            });
        }
        collectController.refresh();
    }

    private static Executor daemonExecutor() {
        return Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "model-training");
            thread.setDaemon(true);
            return thread;
        });
    }

    private GlassPanel buildPrompt() {
        promptLabel.setName("promptLabel");
        promptLabel.setFont(Theme.font(Font.BOLD, 48f));
        promptLabel.setForeground(Theme.ACCENT);
        tile.setPreferredSize(new Dimension(88, 88));
        tile.add(promptLabel, BorderLayout.CENTER);
        preview.setName("expressionPreview");

        JLabel ask = askLabel;
        ask.setFont(Theme.font(Font.BOLD, 17f));
        progressLabel.setName("progressLabel");
        Theme.muted(progressLabel);

        symbolChoice.setName("symbolChoice");
        symbolChoice.setToolTipText("Write a different symbol instead");
        symbolChoice.addActionListener(e -> {
            Object selected = symbolChoice.getSelectedItem();
            if (selected != null) {
                promptLabel.setText(String.valueOf(selected));
            }
        });

        JPanel choose = chooseRow;
        choose.setOpaque(false);
        choose.add(Theme.muted(new JLabel("or practise")));
        choose.add(symbolChoice);

        skipButton.setName("skipButton");
        skipButton.setVisible(false);
        skipButton.addActionListener(e -> {
            canvas.clear();
            practiceController.next();
        });
        JPanel skipRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        skipRow.setOpaque(false);
        skipRow.add(skipButton);

        JLabel tip = tipLabel;
        tip.setFont(Theme.font(Font.PLAIN, 12f));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        for (JComponent c : new JComponent[] {ask, progressLabel, choose, skipRow, tip}) {
            c.setAlignmentX(LEFT_ALIGNMENT);
            text.add(c);
        }

        GlassPanel panel = new GlassPanel(new BorderLayout(16, 0), null, 14);
        panel.add(tile, BorderLayout.WEST);
        panel.add(text, BorderLayout.CENTER);
        return panel;
    }

    private GlassPanel buildTraining() {
        overall.setName("overallProgress");
        overall.setStringPainted(true);
        overall.putClientProperty("FlatLaf.style", "arc: 999; foreground: #4F46E5");
        trainingLabel.setName("trainingLabel");
        trainButton.setName("trainButton");
        trainButton.addActionListener(e -> startTraining());

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setOpaque(false);
        row.add(trainButton);
        trainingLabel.setFont(Theme.font(Font.BOLD, 14f));
        trainingLabel.setForeground(Theme.TEXT);

        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        overall.setAlignmentX(LEFT_ALIGNMENT);
        row.setAlignmentX(LEFT_ALIGNMENT);
        column.add(overall);
        column.add(Box.createVerticalStrut(10));
        column.add(row);
        column.add(Box.createVerticalStrut(8));
        trainingLabel.setAlignmentX(LEFT_ALIGNMENT);
        column.add(trainingLabel);

        GlassPanel panel = new GlassPanel(new BorderLayout(), null, 12);
        panel.add(column, BorderLayout.CENTER);
        return panel;
    }

    private GlassPanel buildActions() {
        messageLabel.setName("messageLabel");

        JButton save = Theme.primary(new JButton("Save & next"));
        save.setName("saveButton");
        save.setToolTipText("Enter");
        save.addActionListener(e -> saveSample());
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("ENTER"), "save");
        getActionMap().put("save", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                save.doClick();
            }
        });

        JButton clear = Theme.secondary(new JButton("Clear"));
        clear.setName("clearButton");
        clear.addActionListener(e -> canvas.clear());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        buttons.add(clear);
        buttons.add(save);

        GlassPanel panel = new GlassPanel(new BorderLayout(18, 0), null, 8);
        messageLabel.setPreferredSize(new Dimension(250, 20));
        panel.add(messageLabel, BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.EAST);
        return panel;
    }

    private GlassPanel buildCounts() {
        countsPanel.setName("countsPanel");
        countsPanel.setOpaque(false);
        GlassPanel panel = new GlassPanel(new BorderLayout(), null, 8);
        panel.add(countsPanel, BorderLayout.CENTER);
        return panel;
    }

    private GlassPanel buildModeSwitch() {
        JToggleButton symbols = modeButton("Single symbols", "symbolsModeButton");
        JToggleButton expressions = modeButton("Whole expressions", "expressionsModeButton");
        ButtonGroup group = new ButtonGroup();
        group.add(symbols);
        group.add(expressions);
        symbols.setSelected(true);
        symbols.addActionListener(e -> setMode(false));
        expressions.addActionListener(e -> setMode(true));

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row.setOpaque(false);
        row.add(symbols);
        row.add(expressions);
        GlassPanel panel = new GlassPanel(new BorderLayout(), null, 4);
        panel.add(row, BorderLayout.CENTER);
        return panel;
    }

    private static JToggleButton modeButton(String text, String name) {
        JToggleButton button = new JToggleButton(text);
        button.setName(name);
        button.setFont(Theme.font(Font.BOLD, 13f));
        button.putClientProperty("FlatLaf.style",
                "arc: 999; focusWidth: 0; margin: 6,16,6,16; borderWidth: 0;"
                        + "background: #FFFFFF; selectedBackground: #4F46E5;"
                        + "selectedForeground: #FFFFFF");
        return button;
    }

    /** Switches between writing single symbols and writing made-up expressions. */
    private void setMode(boolean expressions) {
        if (expressionMode == expressions) {
            return;
        }
        expressionMode = expressions;
        canvas.clear();
        tile.removeAll();
        if (expressions) {
            tile.setPreferredSize(new Dimension(240, 100));
            tile.add(preview, BorderLayout.CENTER);
            askLabel.setText("Please write this expression");
            progressLabel.setText("Write it as shown, symbols apart");
            tipLabel.setText("Every symbol becomes a training sample");
            messageLabel.setText(" ");
            practiceController.next();
        } else {
            tile.setPreferredSize(new Dimension(88, 88));
            tile.add(promptLabel, BorderLayout.CENTER);
            askLabel.setText("Please write this symbol");
            tipLabel.setText("All strokes count as one symbol · no limit");
            messageLabel.setText(" ");
            collectController.refresh();
        }
        chooseRow.setVisible(!expressions);
        skipButton.setVisible(expressions);
        tile.revalidate();
        tile.repaint();
        revalidate();
        repaint();
    }

    private void renderPractice(PracticeExpressionState state) {
        if (!expressionMode) {
            return;
        }
        preview.setExpression(state.getLayout(), state.getParts());
        messageLabel.setText(state.getMessage().isEmpty() ? " " : state.getMessage());
        messageLabel.setForeground(state.isError() ? Theme.ERROR : Theme.SUCCESS);
        if (state.isSaved()) {
            canvas.clear();
            collectController.refresh();
        }
    }

    private void saveSample() {
        if (expressionMode) {
            practiceController.submit(canvas.getStrokes());
            return;
        }
        Object selected = symbolChoice.getSelectedItem();
        if (selected != null) {
            collectController.execute(canvas.getStrokes(), (Character) selected);
        }
    }

    private void startTraining() {
        trainButton.setEnabled(false);
        trainingLabel.setText("Training…");
        background.execute(trainController::execute);
    }

    private void render(CollectorState state) {
        if (countLabels.size() != state.getCounts().size()) {
            rebuildCounts(state);
        }
        int target = state.getTargetPerSymbol();
        for (Map.Entry<Character, Integer> entry : state.getCounts().entrySet()) {
            char symbol = entry.getKey();
            int count = entry.getValue();
            JLabel label = countLabels.get(symbol);
            label.setText(String.valueOf(count));
            RoundedPanel cell = cells.get(symbol);
            if (symbol == state.getNextSymbol()) {
                cell.setFill(Theme.ACCENT_SOFT);
                label.setForeground(Theme.ACCENT);
            } else if (count >= target) {
                cell.setFill(Theme.SUCCESS_SOFT);
                label.setForeground(Theme.SUCCESS);
            } else {
                cell.setFill(CELL);
                label.setForeground(Theme.MUTED);
            }
        }
        int total = 0;
        int covered = 0;
        for (int count : state.getCounts().values()) {
            total += count;
            if (count >= target) {
                covered++;
            }
        }
        int symbols = state.getCounts().size();
        overall.setValue(symbols == 0 ? 0 : Math.round(100f * covered / symbols));
        overall.setString(total + " samples  ·  " + covered + " of " + symbols
                + " symbols have " + target + "+");

        Integer nextCount = state.getCounts().get(state.getNextSymbol());
        if (nextCount != null) {
            promptLabel.setText(String.valueOf(state.getNextSymbol()));
            if (!expressionMode) {
                progressLabel.setText(
                        nextCount + (nextCount == 1 ? " sample so far" : " samples so far"));
            }
            symbolChoice.setSelectedItem(state.getNextSymbol());
        }

        if (!expressionMode) {
            messageLabel.setText(state.getMessage().isEmpty() ? " " : state.getMessage());
            messageLabel.setForeground(state.isError() ? Theme.ERROR : Theme.SUCCESS);
            if (state.isSaved()) {
                canvas.clear();
            }
        }

        if (state.getTrainingRevision() != seenTrainingRevision) {
            seenTrainingRevision = state.getTrainingRevision();
            trainingLabel.setText(state.getTrainingStatus());
            trainingLabel.setToolTipText(state.getTrainingStatus());
            trainButton.setEnabled(true);
        }
    }

    private void rebuildCounts(CollectorState state) {
        countsPanel.removeAll();
        countLabels.clear();
        cells.clear();
        symbolChoice.removeAllItems();
        for (char symbol : state.getCounts().keySet()) {
            JLabel name = new JLabel(String.valueOf(symbol), SwingConstants.CENTER);
            name.setFont(Theme.font(Font.BOLD, 16f));
            JLabel count = new JLabel("0", SwingConstants.CENTER);
            count.setName("count_" + symbol);
            count.setFont(Theme.font(Font.PLAIN, 11f));
            RoundedPanel cell = new RoundedPanel(new BorderLayout(0, 0), CELL, null, 14);
            cell.setBorder(new EmptyBorder(4, 4, 4, 4));
            cell.add(name, BorderLayout.CENTER);
            cell.add(count, BorderLayout.SOUTH);
            countsPanel.add(cell);
            countLabels.put(symbol, count);
            cells.put(symbol, cell);
            symbolChoice.addItem(symbol);
        }
        countsPanel.revalidate();
        countsPanel.repaint();
    }
}
