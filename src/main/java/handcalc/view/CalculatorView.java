package handcalc.view;

import handcalc.entity.Formula;
import handcalc.entity.PlacedSymbol;
import handcalc.entity.Stroke;
import handcalc.interface_adapter.CalculatorState;
import handcalc.interface_adapter.CalculatorViewModel;
import handcalc.interface_adapter.LineView;
import handcalc.interface_adapter.ReadSymbol;
import handcalc.interface_adapter.RecognizeAndSolveController;
import handcalc.interface_adapter.WrittenAnswer;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.Timer;

/**
 * The calculator screen: a sheet of paper and its tools. The paper is a page of lines of
 * working: shortly after the user stops writing, every line is read, and each line that ends
 * in "=" gets its answer written after it. An answer stays until its line is changed or the
 * user wipes it off. Symbols that make a line wrong are underlined in red; resting the mouse
 * on a symbol of a finished line shows how it was read and offers what else it might be, and
 * resting it on an answer shows the calculation that was done. A symbol the user picks there
 * takes the place of what they wrote, set in a handwriting typeface.
 */
public class CalculatorView extends FloatingPane {

    /** How long the pen must rest before the writing is read. */
    static final int PAUSE_MS = 500;
    private static final double ERROR_SCALE = 0.75;
    /** How far above and below its ink a line reaches when telling if new writing is on it. */
    private static final double LINE_REACH = 0.35;
    private static final double ZOOM_STEP = 1.25;

    private final CanvasPanel canvas = new CanvasPanel();
    private final SymbolChoices choices;
    private final FormulaCard formulaCard;
    private final Timer pause;
    private final RecognizeAndSolveController solveController;
    /** The lines as last read, from the top down. */
    private List<LineView> lines = new ArrayList<>();
    /** Lines whose answer the user wiped off; it stays off until the line changes. */
    private final Set<LineKey> wiped = new HashSet<>();

    public CalculatorView(RecognizeAndSolveController solveController,
                          CalculatorViewModel viewModel) {
        this(solveController, viewModel, null);
    }

    /**
     * @param openCollector opens the sample collection window; null hides the button
     */
    public CalculatorView(RecognizeAndSolveController solveController,
                          CalculatorViewModel viewModel, Runnable openCollector) {
        this.solveController = solveController;
        setPreferredSize(new Dimension(900, 640));
        canvas.setFullBleed(true);
        canvas.setNavigable(true);
        canvas.setName("canvas");
        setBackdrop(canvas);

        // a pick only changes the symbol: it may be an edit of a correct reading, so it is
        // not taught to the recogniser, which learns from the practice window instead
        choices = new SymbolChoices(canvas, (index, symbol) -> {
            ReadSymbol read = symbolAt(index);
            if (read != null && !read.getStrokes().isEmpty()) {
                canvas.place(TypesetSymbol.place(symbol, otherReadings(read, symbol), box(read)),
                        read.getStrokes());
                // read the changed writing at once, so its answer does not blink
                readNow();
            }
        });
        formulaCard = new FormulaCard(canvas);
        pause = new Timer(PAUSE_MS, e -> read());
        pause.setRepeats(false);
        canvas.addChangeListener(e -> {
            withdrawEditedLines();
            pause.restart();
        });
        canvas.addNoteListener(new CanvasPanel.NoteListener() {
            @Override
            public void erased(Object key) {
                wiped.add((LineKey) key);
                formulaCard.hideCard();
            }

            @Override
            public void restored(Object key) {
                wiped.remove((LineKey) key);
            }
        });

        JToggleButton eraser = Theme.toggle(new JToggleButton("Eraser"));
        eraser.setName("eraserButton");
        eraser.addActionListener(e -> {
            canvas.setErasing(eraser.isSelected());
            choices.hideChoices();
            formulaCard.hideCard();
        });

        JButton undo = Theme.secondary(new JButton("Undo"));
        undo.setName("undoButton");
        undo.addActionListener(e -> canvas.undo());

        JButton clear = Theme.secondary(new JButton("Clear"));
        clear.setName("clearButton");
        clear.addActionListener(e -> canvas.clear());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        buttons.add(eraser);
        buttons.add(undo);
        buttons.add(clear);

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
        floatAt(buttonRow, Anchor.BOTTOM_CENTER, 600, 900);

        bindZoomKeys();

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
    }

    /** Reads the writing now instead of after the pause; for tests. */
    void readNow() {
        pause.stop();
        read();
    }

    /** Shows the choices for the symbol at {@code index} at once; for tests. */
    void showChoicesFor(int index) {
        choices.showFor(index);
    }

    private void read() {
        if (canvas.isBusy()) {
            // still writing: the stroke being drawn will restart the pause when it is done
            return;
        }
        solveController.executeWhenFinished(canvas.getStrokes(), canvas.getPlacedSymbols());
    }

    /** @return the symbol at this place from the top of the page, or null */
    private ReadSymbol symbolAt(int index) {
        int at = index;
        for (LineView line : lines) {
            if (at < line.getSymbols().size()) {
                return line.getSymbols().get(at);
            }
            at -= line.getSymbols().size();
        }
        return null;
    }

    /** What else the writing might be, the reading it had first, for choosing again later. */
    private static String otherReadings(ReadSymbol read, char picked) {
        StringBuilder others = new StringBuilder();
        for (ReadSymbol.Alternative alternative : read.getAlternatives()) {
            if (alternative.getSymbol() != picked) {
                others.append(alternative.getSymbol());
            }
        }
        if (read.getSymbol() != picked && others.indexOf(String.valueOf(read.getSymbol())) < 0) {
            others.insert(0, read.getSymbol());
        }
        return others.toString();
    }

    /** ⌘ (or Ctrl) with +, - and 0 zoom in, out and back. */
    private void bindZoomKeys() {
        int menu = GraphicsEnvironment.isHeadless()
                ? (System.getProperty("os.name", "").startsWith("Mac")
                        ? InputEvent.META_DOWN_MASK : InputEvent.CTRL_DOWN_MASK)
                : Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        InputMap keys = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        keys.put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, menu), "zoomIn");
        keys.put(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, menu), "zoomIn");
        keys.put(KeyStroke.getKeyStroke(KeyEvent.VK_ADD, menu), "zoomIn");
        keys.put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, menu), "zoomOut");
        keys.put(KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, menu), "zoomOut");
        keys.put(KeyStroke.getKeyStroke(KeyEvent.VK_0, menu), "zoomReset");
        getActionMap().put("zoomIn", action(() -> canvas.zoomBy(ZOOM_STEP)));
        getActionMap().put("zoomOut", action(() -> canvas.zoomBy(1 / ZOOM_STEP)));
        getActionMap().put("zoomReset", action(canvas::resetView));
    }

    private static AbstractAction action(Runnable run) {
        return new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                run.run();
            }
        };
    }

    private void render(CalculatorState state) {
        lines = new ArrayList<>(state.getLines());
        if (lines.isEmpty() && (!state.getSymbols().isEmpty() || state.getAnswer() != null)) {
            // a single expression rather than a page: shown as one line
            boolean finished = state.getResult() != null || state.getError() != null;
            lines.add(new LineView(state.getSymbols(), state.getAnswer(), null,
                    Collections.emptyList(), finished));
        }
        Set<LineKey> keys = new HashSet<>();
        for (LineView line : lines) {
            keys.add(new LineKey(line.getStrokes()));
        }
        wiped.retainAll(keys);
        show(Collections.emptySet(), true);
    }

    /**
     * Writing changed: until it is read again, lines that lost a stroke, or gained one, lose
     * their answer and underlines, and no symbol offers choices, as their places may change.
     */
    private void withdrawEditedLines() {
        List<Stroke> ink = new ArrayList<>(canvas.getStrokes());
        for (PlacedSymbol symbol : canvas.getPlacedSymbols()) {
            ink.addAll(symbol.getOutline());
        }
        Set<Stroke> present = identitySet(ink);
        Set<Stroke> known = Collections.newSetFromMap(new IdentityHashMap<>());
        for (LineView line : lines) {
            known.addAll(line.getStrokes());
        }
        List<Stroke> added = new ArrayList<>();
        for (Stroke stroke : ink) {
            if (!known.contains(stroke)) {
                added.add(stroke);
            }
        }
        Set<LineKey> edited = new HashSet<>();
        for (LineView line : lines) {
            LineKey key = new LineKey(line.getStrokes());
            if (!key.isWithin(present) || reachesLine(added, line)) {
                edited.add(key);
            }
        }
        show(edited, false);
    }

    /**
     * Puts the lines' answers, underlines, typeset calculations and choices on the page.
     *
     * @param withdrawn lines to show nothing for
     * @param offerChoices false while the symbols' places may have changed
     */
    private void show(Set<LineKey> withdrawn, boolean offerChoices) {
        List<CanvasPanel.Note> notes = new ArrayList<>();
        Map<Object, Formula> formulas = new HashMap<>();
        List<Rectangle2D> faulty = new ArrayList<>();
        List<ReadSymbol> offered = new ArrayList<>();
        for (LineView line : lines) {
            LineKey key = new LineKey(line.getStrokes());
            boolean shown = !withdrawn.contains(key);
            for (ReadSymbol symbol : line.getSymbols()) {
                offered.add(offerChoices && shown && line.isFinished() ? symbol : null);
                if (shown && symbol.isFaulty()) {
                    faulty.add(box(symbol));
                }
            }
            WrittenAnswer answer = line.getAnswer();
            if (!shown || answer == null || wiped.contains(key)) {
                continue;
            }
            // words are written a little smaller than digits, as their tall letters reach higher
            double height = answer.isError() ? ERROR_SCALE * answer.getHeight() : answer.getHeight();
            CanvasPanel.Note note = new CanvasPanel.Note(key, answer.getText(), answer.getX(),
                    answer.getCentreY(), height, answer.isError() ? Theme.ERROR : Theme.ACCENT);
            notes.add(note);
            if (line.getFormula() != null) {
                formulas.put(key, line.getFormula());
            }
        }
        canvas.setNotes(notes);
        canvas.setUnderlined(faulty);
        choices.setSymbols(offered);
        formulaCard.setFormulas(formulas);
    }

    /** @return true if any of the strokes is written at the height of the line */
    private static boolean reachesLine(List<Stroke> strokes, LineView line) {
        if (strokes.isEmpty() || line.getStrokes().isEmpty()) {
            return false;
        }
        double top = Double.POSITIVE_INFINITY;
        double bottom = Double.NEGATIVE_INFINITY;
        for (Stroke stroke : line.getStrokes()) {
            top = Math.min(top, stroke.getMinY());
            bottom = Math.max(bottom, stroke.getMaxY());
        }
        double reach = LINE_REACH * (bottom - top);
        for (Stroke stroke : strokes) {
            if (stroke.getMaxY() >= top - reach && stroke.getMinY() <= bottom + reach) {
                return true;
            }
        }
        return false;
    }

    private static Set<Stroke> identitySet(List<Stroke> strokes) {
        Set<Stroke> set = Collections.newSetFromMap(new IdentityHashMap<>());
        set.addAll(strokes);
        return set;
    }

    private static Rectangle2D box(ReadSymbol symbol) {
        return new Rectangle2D.Double(symbol.getMinX(), symbol.getMinY(),
                symbol.getMaxX() - symbol.getMinX(), symbol.getMaxY() - symbol.getMinY());
    }
}
