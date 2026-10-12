package handcalc.view;

import handcalc.entity.Point;
import handcalc.entity.Stroke;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * A sheet of dotted paper the user writes on with the mouse, trackpad or a stylus.
 */
public class CanvasPanel extends JPanel {

    private static final float INK_WIDTH = 4.5f;
    private static final int ROUNDED_ARC = 28;
    private static final int GRID = 28;
    private static final String HINT = "Write here, e.g.  12 + 3 × 4";

    private final List<Stroke> strokes = new ArrayList<>();
    private final List<ChangeListener> listeners = new ArrayList<>();
    private List<Point> currentPoints;
    private boolean fullBleed;

    public CanvasPanel() {
        setOpaque(false);
        setPreferredSize(new Dimension(800, 300));
        MouseAdapter pen = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                currentPoints = new ArrayList<>();
                currentPoints.add(new Point(e.getX(), e.getY()));
                repaint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (currentPoints != null) {
                    currentPoints.add(new Point(e.getX(), e.getY()));
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (currentPoints != null) {
                    Point last = currentPoints.get(currentPoints.size() - 1);
                    if (last.getX() != e.getX() || last.getY() != e.getY()) {
                        currentPoints.add(new Point(e.getX(), e.getY()));
                    }
                    strokes.add(new Stroke(currentPoints));
                    currentPoints = null;
                    repaint();
                    fireChanged();
                }
            }
        };
        addMouseListener(pen);
        addMouseMotionListener(pen);
    }

    /** A full-bleed sheet runs edge to edge: no rounded corners, no outline. */
    public void setFullBleed(boolean fullBleed) {
        this.fullBleed = fullBleed;
        repaint();
    }

    /** @return the finished strokes in the order they were drawn. */
    public List<Stroke> getStrokes() {
        return Collections.unmodifiableList(new ArrayList<>(strokes));
    }

    public void undo() {
        if (!strokes.isEmpty()) {
            strokes.remove(strokes.size() - 1);
            repaint();
            fireChanged();
        }
    }

    /** Replaces the writing, e.g. with a calculation taken from the history. */
    public void setStrokes(List<Stroke> newStrokes) {
        strokes.clear();
        strokes.addAll(newStrokes);
        currentPoints = null;
        repaint();
        fireChanged();
    }

    public void clear() {
        strokes.clear();
        currentPoints = null;
        repaint();
        fireChanged();
    }

    /** Notified whenever a stroke is finished, undone, or the canvas is cleared. */
    public void addChangeListener(ChangeListener listener) {
        listeners.add(listener);
    }

    private void fireChanged() {
        ChangeEvent event = new ChangeEvent(this);
        for (ChangeListener listener : new ArrayList<>(listeners)) {
            listener.stateChanged(event);
        }
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            int arc = fullBleed ? 0 : ROUNDED_ARC;
            RoundRectangle2D.Double paper =
                    new RoundRectangle2D.Double(0, 0, fullBleed ? w : w - 1,
                            fullBleed ? h : h - 1, arc, arc);

            g.setColor(Theme.CARD);
            g.fill(paper);

            java.awt.Shape oldClip = g.getClip();
            g.clip(paper);
            g.setColor(Theme.GRID_DOT);
            for (int x = GRID / 2; x < w; x += GRID) {
                for (int y = GRID / 2; y < h; y += GRID) {
                    g.fillOval(x - 1, y - 1, 3, 3);
                }
            }
            if (strokes.isEmpty() && currentPoints == null) {
                g.setFont(Theme.font(java.awt.Font.PLAIN, 22f));
                g.setColor(new Color(0xB4B4BC));
                FontMetrics fm = g.getFontMetrics();
                g.drawString(HINT, (w - fm.stringWidth(HINT)) / 2,
                        (h - fm.getHeight()) / 2 + fm.getAscent());
            }
            g.setClip(oldClip);

            if (!fullBleed) {
                g.setColor(Theme.BORDER);
                g.draw(paper);
            }

            g.setColor(Theme.TEXT);
            g.setStroke(new BasicStroke(INK_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (Stroke stroke : strokes) {
                g.draw(toPath(stroke.getPoints()));
            }
            if (currentPoints != null) {
                g.draw(toPath(currentPoints));
            }
        } finally {
            g.dispose();
        }
    }

    private static Path2D toPath(List<Point> points) {
        Path2D.Double path = new Path2D.Double();
        Point first = points.get(0);
        path.moveTo(first.getX(), first.getY());
        if (points.size() == 1) {
            path.lineTo(first.getX(), first.getY());
        }
        for (int i = 1; i < points.size(); i++) {
            path.lineTo(points.get(i).getX(), points.get(i).getY());
        }
        return path;
    }
}
