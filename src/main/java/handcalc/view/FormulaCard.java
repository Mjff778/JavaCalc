package handcalc.view;

import handcalc.entity.Formula;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.Timer;

/**
 * Floats beside an answer when the mouse rests on it, showing the calculation typeset the way
 * it was done, so the user can check the writing was read right. It never takes the mouse:
 * the pen writes straight through it.
 */
final class FormulaCard extends GlassPanel {

    private static final int DWELL_MS = 250;
    private static final float SIZE = 22f;
    private static final int PAD = 10;
    /** The panel's shadow margin may overlap the answer a little. */
    private static final int OVERLAP = 8;

    private final CanvasPanel canvas;
    private final FormulaView view = new FormulaView();
    private final Timer dwell;
    private Map<Object, Formula> formulas = Collections.emptyMap();
    private Object pending;
    private Object shown;

    FormulaCard(CanvasPanel canvas) {
        super(new java.awt.BorderLayout(), null, 0);
        this.canvas = canvas;
        setName("formulaCard");
        add(view);
        setVisible(false);
        canvas.add(this);

        dwell = new Timer(DWELL_MS, e -> showFor(pending));
        dwell.setRepeats(false);
        MouseAdapter hover = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                hoverAt(e.getPoint());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hideCard();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                hideCard();
            }
        };
        canvas.addMouseListener(hover);
        canvas.addMouseMotionListener(hover);
        canvas.addViewListener(this::hideCard);
    }

    /** The calculations behind the notes on the page, by the notes' keys. */
    void setFormulas(Map<Object, Formula> byNote) {
        formulas = new HashMap<>(byNote);
        hideCard();
    }

    /** The card is only to look at: the mouse goes through to the paper. */
    @Override
    public boolean contains(int x, int y) {
        return false;
    }

    /** Shows the calculation behind the note with this key at once, if there is one. */
    void showFor(Object key) {
        dwell.stop();
        pending = null;
        Formula formula = key == null ? null : formulas.get(key);
        CanvasPanel.Note note = key == null ? null : find(key);
        if (formula == null || note == null || canvas.isBusy() || canvas.isErasing()) {
            return;
        }
        shown = key;
        view.setFormula(formula);
        Dimension size = getPreferredSize();
        Rectangle answer = canvas.toScreen(note.getBounds());
        int x = Math.max(0, Math.min(canvas.getWidth() - size.width, answer.x));
        int y = answer.y + answer.height - OVERLAP;
        if (y + size.height > canvas.getHeight()) {
            y = answer.y - size.height + OVERLAP;
        }
        setBounds(x, y, size.width, size.height);
        validate();
        setVisible(true);
        canvas.repaint();
    }

    void hideCard() {
        dwell.stop();
        pending = null;
        shown = null;
        if (isVisible()) {
            setVisible(false);
            canvas.repaint();
        }
    }

    private void hoverAt(Point p) {
        CanvasPanel.Note note = canvas.isBusy() ? null : canvas.noteAt(p);
        Object key = note == null ? null : note.getKey();
        if (key == null || !formulas.containsKey(key)) {
            hideCard();
        } else if (!key.equals(shown) && !key.equals(pending)) {
            pending = key;
            dwell.restart();
        }
    }

    private CanvasPanel.Note find(Object key) {
        for (CanvasPanel.Note note : canvas.getNotes()) {
            if (note.getKey().equals(key)) {
                return note;
            }
        }
        return null;
    }

    /** The typeset formula itself. */
    private static final class FormulaView extends JComponent {
        private FormulaPainter.Box box;

        void setFormula(Formula formula) {
            box = FormulaPainter.layOut(formula, SIZE);
            setPreferredSize(new Dimension((int) Math.ceil(box.width) + 2 * PAD,
                    (int) Math.ceil(box.ascent + box.descent) + 2 * PAD));
            revalidate();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            if (box == null) {
                return;
            }
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g.setColor(Theme.TEXT);
                box.draw(g, PAD, PAD + box.ascent);
            } finally {
                g.dispose();
            }
        }
    }
}
