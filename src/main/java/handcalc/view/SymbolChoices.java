package handcalc.view;

import handcalc.interface_adapter.ReadSymbol;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.JButton;
import javax.swing.Timer;

/**
 * Floats over a written symbol when the mouse rests on it, showing how the symbol was read
 * (first, highlighted) and what else it might be; choosing another reading corrects it.
 *
 * <p>Only the choices themselves take the mouse: the pen writes straight through the rest of
 * the panel, so it never blocks the next stroke. Choosing must be deliberate, as it changes
 * the writing: the choices only come once the user has stopped writing for a moment, and a
 * press the moment they appear is taken as the pen coming down, not a choice.
 */
final class SymbolChoices extends GlassPanel {

    /** Told which symbol the user chose for the symbol at {@code index}. */
    interface Listener {
        void chosen(int index, char symbol);
    }

    private static final int DWELL_MS = 300;
    /** How long the writing must have rested before choices are offered. */
    static final int QUIET_MS = 1500;
    /** A press this soon after the choices appear is not taken as choosing. */
    static final int PRESS_GRACE_MS = 400;
    /** How far around a symbol's ink the mouse still counts as resting on it. */
    private static final int REACH = 8;
    /** The panel's shadow margin may overlap the symbol a little. */
    private static final int OVERLAP = 8;

    private final CanvasPanel canvas;
    private final Listener listener;
    private final Timer dwell;
    private List<ReadSymbol> symbols = Collections.emptyList();
    private int pending = -1;
    private int shown = -1;
    private long lastEdit;
    private long shownAt;
    private boolean pressedTooSoon;

    SymbolChoices(CanvasPanel canvas, Listener listener) {
        super(new FlowLayout(FlowLayout.CENTER, 6, 0), null, 0);
        this.canvas = canvas;
        this.listener = listener;
        setName("symbolChoices");
        setVisible(false);
        canvas.add(this);

        dwell = new Timer(DWELL_MS, e -> showOnceQuiet());
        dwell.setRepeats(false);
        canvas.addChangeListener(e -> lastEdit = System.currentTimeMillis());

        MouseAdapter hover = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                hoverAt(e.getPoint());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hoverAt(e.getPoint());
            }

            @Override
            public void mousePressed(MouseEvent e) {
                hideChoices();
            }
        };
        canvas.addMouseListener(hover);
        canvas.addMouseMotionListener(hover);
        canvas.addViewListener(this::hideChoices);
    }

    /** Only the choices are part of the panel for the mouse; elsewhere is the paper's. */
    @Override
    public boolean contains(int x, int y) {
        for (java.awt.Component choice : getComponents()) {
            if (choice.getBounds().contains(x, y)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The symbols now on the page; the choices for any earlier ones are withdrawn. A null
     * keeps a symbol's place without offering choices for it.
     */
    void setSymbols(List<ReadSymbol> newSymbols) {
        symbols = new ArrayList<>(newSymbols);
        hideChoices();
    }

    /** Shows the choices for the symbol at {@code index}, if it has any. */
    void showFor(int index) {
        dwell.stop();
        pending = -1;
        if (index < 0 || index >= symbols.size() || symbols.get(index) == null
                || canvas.isErasing() || canvas.isBusy()) {
            return;
        }
        ReadSymbol read = symbols.get(index);
        removeAll();
        add(current(read));
        for (ReadSymbol.Alternative alternative : read.getAlternatives()) {
            if (alternative.getSymbol() != read.getSymbol()) {
                add(choice(index, alternative));
            }
        }
        if (getComponentCount() == 1) {
            hideChoices();
            return;
        }
        shown = index;
        shownAt = System.currentTimeMillis();
        Rectangle box = canvas.toScreen(box(read));
        java.awt.Dimension size = getPreferredSize();
        int x = (int) Math.round(box.getCenterX() - size.width / 2.0);
        x = Math.max(0, Math.min(canvas.getWidth() - size.width, x));
        int y = box.y - size.height + OVERLAP;
        if (y < 0) {
            y = box.y + box.height - OVERLAP;
        }
        setBounds(x, y, size.width, size.height);
        validate();
        setVisible(true);
        canvas.repaint();
    }

    void hideChoices() {
        dwell.stop();
        pending = -1;
        shown = -1;
        if (isVisible()) {
            setVisible(false);
            canvas.repaint();
        }
    }

    private void hoverAt(Point p) {
        if (canvas.isErasing() || canvas.isBusy()) {
            hideChoices();
            return;
        }
        int index = symbolAt(p);
        if (index >= 0) {
            if (index != shown && index != pending) {
                pending = index;
                dwell.setInitialDelay(DWELL_MS);
                dwell.restart();
            }
            return;
        }
        dwell.stop();
        pending = -1;
        if (shown >= 0) {
            // keep the choices while the mouse crosses from the symbol up to them
            Rectangle keep = getBounds().union(grown(canvas.toScreen(box(symbols.get(shown)))));
            if (!keep.contains(p)) {
                hideChoices();
            }
        }
    }

    /** Shows the pending choices, or waits until the writing has rested long enough. */
    private void showOnceQuiet() {
        long wait = lastEdit + QUIET_MS - System.currentTimeMillis();
        if (wait > 0) {
            dwell.setInitialDelay((int) wait);
            dwell.restart();
        } else {
            showFor(pending);
        }
    }

    private int symbolAt(Point p) {
        for (int i = 0; i < symbols.size(); i++) {
            if (symbols.get(i) != null && grown(canvas.toScreen(box(symbols.get(i)))).contains(p)) {
                return i;
            }
        }
        return -1;
    }

    /** How the symbol is read now; choosing it keeps the reading. */
    private JButton current(ReadSymbol read) {
        JButton button = Theme.currentChip(new JButton(String.valueOf(read.getSymbol())));
        button.setName("current_" + read.getSymbol());
        button.setToolTipText("Read as this now");
        button.addActionListener(e -> hideChoices());
        return deliberate(button);
    }

    private JButton choice(int index, ReadSymbol.Alternative alternative) {
        JButton button = Theme.chip(new JButton(String.valueOf(alternative.getSymbol())));
        button.setName("choice_" + alternative.getSymbol());
        if (alternative.getPercent() > 0) {
            button.setToolTipText(alternative.getPercent() + "% likely");
        }
        button.addActionListener(e -> {
            hideChoices();
            if (pressedTooSoon) {
                pressedTooSoon = false;
                return;
            }
            listener.chosen(index, alternative.getSymbol());
        });
        return deliberate(button);
    }

    /** Notes whether the button was pressed too soon after the choices appeared. */
    private JButton deliberate(JButton button) {
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                pressedTooSoon = System.currentTimeMillis() - shownAt < PRESS_GRACE_MS;
            }
        });
        return button;
    }

    /** @return where the symbol's ink is on the page */
    private static Rectangle2D box(ReadSymbol read) {
        return new Rectangle2D.Double(read.getMinX(), read.getMinY(),
                read.getMaxX() - read.getMinX(), read.getMaxY() - read.getMinY());
    }

    private static Rectangle grown(Rectangle box) {
        Rectangle grown = new Rectangle(box);
        grown.grow(REACH, REACH);
        return grown;
    }
}
