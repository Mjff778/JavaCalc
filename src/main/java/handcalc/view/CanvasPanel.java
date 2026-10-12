package handcalc.view;

import handcalc.entity.PlacedSymbol;
import handcalc.entity.Point;
import handcalc.entity.Stroke;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * A sheet of dotted paper the user writes on with the mouse, trackpad or a stylus, or wipes
 * whole strokes off with the eraser. Undo takes back the last edit: a stroke drawn, what one
 * sweep of the eraser wiped, or a clear.
 *
 * <p>Strokes are kept in page coordinates. A navigable sheet can be zoomed and moved in every
 * direction, so the page is as large as the user needs; the writing stays where it is on the
 * page.
 *
 * <p>A symbol the user picked can take the place of what they wrote; it is set in the
 * handwriting typeface in ink and is part of the writing, wiped and undone like a stroke.
 *
 * <p>Notes, such as the answer after a line's "=", are written on the page in a handwriting
 * typeface, and symbols at fault are underlined with a red wavy line. Neither is part of the
 * user's writing: the eraser wipes a note whole, and undo brings it back.
 */
public class CanvasPanel extends JPanel {

    /** Text written on the page that is not the user's writing. */
    public static final class Note {
        private final Object key;
        private final String text;
        private final double height;
        private final Color color;
        private final Shape shape;

        /**
         * @param key identifies the note to note listeners
         * @param x the left edge of the text on the page
         * @param centreY the height on the page the text's digits are centred on
         * @param height how tall the text's digits are on the page
         */
        public Note(Object key, String text, double x, double centreY, double height,
                    Color color) {
            this.key = key;
            this.text = text;
            this.height = height;
            this.color = color;
            this.shape = HandwritingFont.outline(text, x, centreY, height);
        }

        public Object getKey() {
            return key;
        }

        public String getText() {
            return text;
        }

        public Color getColor() {
            return color;
        }

        /** @return the box the written text covers on the page */
        public Rectangle2D getBounds() {
            return shape.getBounds2D();
        }
    }

    /** Told when the eraser wipes a note, and when undo brings it back. */
    public interface NoteListener {
        void erased(Object key);

        void restored(Object key);
    }

    private static final float INK_WIDTH = 4.5f;
    /** Strokes this close to the middle of the eraser are wiped, in screen pixels. */
    private static final double ERASER_RADIUS = 12;
    /** The answer's outline is inked a little, so its weight matches the pen. */
    private static final double NOTE_INK_RATIO = 0.025;
    private static final float WAVE_WIDTH = 2f;
    private static final double WAVE_LENGTH = 8;
    private static final double WAVE_HEIGHT = 2.5;
    private static final double WAVE_GAP = 7;
    private static final double MIN_ZOOM = 0.25;
    private static final double MAX_ZOOM = 4;
    /** How much one notch of the wheel zooms, while the zoom key is held. */
    private static final double WHEEL_ZOOM = 1.1;
    private static final int SCROLL_STEP = 16;
    private static final int ROUNDED_ARC = 28;
    private static final int GRID = 28;
    /** Below this many screen pixels apart, every other grid dot is left out. */
    private static final double MIN_DOT_SPACING = 14;
    private static final String HINT = "Write a sum ending in =, e.g.  12 + 3 × 4 =";

    private final List<Stroke> strokes = new ArrayList<>();
    private final Deque<Runnable> undoSteps = new ArrayDeque<>();
    private final List<ChangeListener> listeners = new ArrayList<>();
    private final List<Runnable> viewListeners = new ArrayList<>();
    private final List<NoteListener> noteListeners = new ArrayList<>();
    private final List<Rectangle2D> underlined = new ArrayList<>();
    private final List<Note> notes = new ArrayList<>();
    private final List<PlacedSymbol> placed = new ArrayList<>();
    private List<Point> currentPoints;
    private boolean erasing;
    /** The strokes and notes wiped in the current sweep of the eraser; null between. */
    private List<Erased> sweep;
    private List<Note> sweptNotes;
    private List<PlacedSymbol> sweptPlaced;
    /** Where on the page the eraser last wiped in the current sweep. */
    private Point2D sweptTo;
    private java.awt.Point mouseAt;
    private boolean fullBleed;
    private boolean navigable;
    private double zoom = 1;
    private double offsetX;
    private double offsetY;

    public CanvasPanel() {
        super(null);
        setOpaque(false);
        setPreferredSize(new Dimension(800, 300));
        MouseAdapter pen = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Point2D at = toPage(e.getPoint());
                if (erasing) {
                    sweep = new ArrayList<>();
                    sweptNotes = new ArrayList<>();
                    sweptPlaced = new ArrayList<>();
                    sweptTo = at;
                    eraseAround(at.getX(), at.getY());
                } else {
                    currentPoints = new ArrayList<>();
                    currentPoints.add(new Point(at.getX(), at.getY()));
                }
                repaint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                Point2D at = toPage(e.getPoint());
                if (sweep != null) {
                    eraseAlong(at.getX(), at.getY());
                } else if (currentPoints != null) {
                    currentPoints.add(new Point(at.getX(), at.getY()));
                }
                mouseAt = e.getPoint();
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (sweep != null) {
                    finishSweep();
                } else if (currentPoints != null) {
                    Point2D at = toPage(e.getPoint());
                    Point last = currentPoints.get(currentPoints.size() - 1);
                    if (last.getX() != at.getX() || last.getY() != at.getY()) {
                        currentPoints.add(new Point(at.getX(), at.getY()));
                    }
                    Stroke stroke = new Stroke(currentPoints);
                    currentPoints = null;
                    strokes.add(stroke);
                    undoSteps.push(() -> {
                        strokes.remove(stroke);
                        edited();
                    });
                    edited();
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                mouseAt = e.getPoint();
                if (erasing) {
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (erasing) {
                    mouseAt = null;
                    repaint();
                }
            }
        };
        addMouseListener(pen);
        addMouseMotionListener(pen);
        addMouseWheelListener(this::wheelMoved);
    }

    /** A full-bleed sheet runs edge to edge: no rounded corners, no outline. */
    public void setFullBleed(boolean fullBleed) {
        this.fullBleed = fullBleed;
        repaint();
    }

    /**
     * A navigable sheet is moved with the scroll wheel or two fingers, and zoomed with the
     * zoom key (⌘ or Ctrl) and the wheel or by pinching.
     */
    public void setNavigable(boolean navigable) {
        this.navigable = navigable;
        if (navigable) {
            Pinch.addTo(this, magnification -> {
                java.awt.Point at = mouseAt != null ? mouseAt
                        : new java.awt.Point(getWidth() / 2, getHeight() / 2);
                zoomAt(at, 1 + magnification);
            });
        }
    }

    /** Switches between writing (false) and wiping whole strokes and notes off (true). */
    public void setErasing(boolean erasing) {
        this.erasing = erasing;
        setCursor(Cursor.getPredefinedCursor(
                erasing ? Cursor.CROSSHAIR_CURSOR : Cursor.DEFAULT_CURSOR));
        repaint();
    }

    public boolean isErasing() {
        return erasing;
    }

    /** @return true while the pen or eraser is down. */
    public boolean isBusy() {
        return currentPoints != null || sweep != null;
    }

    /** @return the finished strokes in the order they were drawn, in page coordinates. */
    public List<Stroke> getStrokes() {
        return Collections.unmodifiableList(new ArrayList<>(strokes));
    }

    /** Takes back the last edit: a stroke drawn, an eraser sweep, or a clear. */
    public void undo() {
        if (!undoSteps.isEmpty()) {
            undoSteps.pop().run();
        }
    }

    /** @return the symbols the user picked in place of their writing */
    public List<PlacedSymbol> getPlacedSymbols() {
        return Collections.unmodifiableList(new ArrayList<>(placed));
    }

    /**
     * Puts a symbol the user picked in place of some of the writing: strokes, or the outline
     * of a symbol picked before. Undo puts the writing back.
     */
    public void place(PlacedSymbol symbol, Collection<Stroke> replacing) {
        Set<Stroke> gone = Collections.newSetFromMap(new IdentityHashMap<>());
        gone.addAll(replacing);
        List<Erased> removed = new ArrayList<>();
        for (int i = strokes.size() - 1; i >= 0; i--) {
            if (gone.contains(strokes.get(i))) {
                removed.add(new Erased(i, strokes.remove(i)));
            }
        }
        List<PlacedSymbol> replaced = new ArrayList<>();
        for (PlacedSymbol earlier : new ArrayList<>(placed)) {
            if (earlier.getOutline().stream().anyMatch(gone::contains)) {
                placed.remove(earlier);
                replaced.add(earlier);
            }
        }
        placed.add(symbol);
        undoSteps.push(() -> {
            placed.remove(symbol);
            for (int k = removed.size() - 1; k >= 0; k--) {
                strokes.add(removed.get(k).index, removed.get(k).stroke);
            }
            placed.addAll(replaced);
            edited();
        });
        edited();
    }

    /** Replaces the writing, e.g. with a calculation taken from the history. */
    public void setStrokes(List<Stroke> newStrokes) {
        strokes.clear();
        placed.clear();
        strokes.addAll(newStrokes);
        currentPoints = null;
        undoSteps.clear();
        notes.clear();
        edited();
    }

    /** Wipes the writing and the notes; undo brings them back. */
    public void clear() {
        List<Stroke> wiped = new ArrayList<>(strokes);
        List<Note> wipedNotes = new ArrayList<>(notes);
        List<PlacedSymbol> wipedPlaced = new ArrayList<>(placed);
        strokes.clear();
        notes.clear();
        placed.clear();
        currentPoints = null;
        if (!wiped.isEmpty() || !wipedNotes.isEmpty() || !wipedPlaced.isEmpty()) {
            undoSteps.push(() -> {
                strokes.addAll(wiped);
                placed.addAll(wipedPlaced);
                restore(wipedNotes);
                edited();
            });
        }
        edited();
    }

    /** Replaces the notes on the page. This is not a change to the writing. */
    public void setNotes(List<Note> newNotes) {
        notes.clear();
        notes.addAll(newNotes);
        repaint();
    }

    /** @return the notes on the page */
    public List<Note> getNotes() {
        return Collections.unmodifiableList(new ArrayList<>(notes));
    }

    /** @return the note written at this point on the screen, or null if there is none */
    public Note noteAt(java.awt.Point screen) {
        Point2D at = toPage(screen);
        for (Note note : notes) {
            if (note.getBounds().contains(at)) {
                return note;
            }
        }
        return null;
    }

    public void addNoteListener(NoteListener listener) {
        noteListeners.add(listener);
    }

    /** Underlines these boxes (in page coordinates) with a red wavy line. */
    public void setUnderlined(List<Rectangle2D> boxes) {
        underlined.clear();
        underlined.addAll(boxes);
        repaint();
    }

    /** @return the boxes underlined with a red wavy line. */
    public List<Rectangle2D> getUnderlined() {
        return Collections.unmodifiableList(new ArrayList<>(underlined));
    }

    /** Notified whenever the writing changes: a stroke drawn or wiped, an undo, a clear. */
    public void addChangeListener(ChangeListener listener) {
        listeners.add(listener);
    }

    /** Notified whenever the page is zoomed or moved. */
    public void addViewListener(Runnable listener) {
        viewListeners.add(listener);
    }

    private void edited() {
        repaint();
        ChangeEvent event = new ChangeEvent(this);
        for (ChangeListener listener : new ArrayList<>(listeners)) {
            listener.stateChanged(event);
        }
    }

    // ---- page and screen -----------------------------------------------------------------

    public double getZoom() {
        return zoom;
    }

    /** @return where a point on the screen is on the page */
    public Point2D toPage(java.awt.Point screen) {
        return new Point2D.Double((screen.x - offsetX) / zoom, (screen.y - offsetY) / zoom);
    }

    /** @return where a box on the page is on the screen */
    public Rectangle toScreen(Rectangle2D page) {
        double x = page.getMinX() * zoom + offsetX;
        double y = page.getMinY() * zoom + offsetY;
        int left = (int) Math.floor(x);
        int top = (int) Math.floor(y);
        return new Rectangle(left, top, (int) Math.ceil(x + page.getWidth() * zoom) - left,
                (int) Math.ceil(y + page.getHeight() * zoom) - top);
    }

    /** Zooms by {@code factor}, keeping the page still under the given point on the screen. */
    public void zoomAt(java.awt.Point screen, double factor) {
        double newZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom * factor));
        Point2D page = toPage(screen);
        offsetX = screen.x - page.getX() * newZoom;
        offsetY = screen.y - page.getY() * newZoom;
        zoom = newZoom;
        viewChanged();
    }

    /** Zooms by {@code factor} about the middle of the sheet. */
    public void zoomBy(double factor) {
        zoomAt(new java.awt.Point(getWidth() / 2, getHeight() / 2), factor);
    }

    /** Back to the start: no zoom, the top left of the page in the top left corner. */
    public void resetView() {
        zoom = 1;
        offsetX = 0;
        offsetY = 0;
        viewChanged();
    }

    /** Moves the page by this many screen pixels. */
    public void panBy(double dx, double dy) {
        offsetX += dx;
        offsetY += dy;
        viewChanged();
    }

    private void viewChanged() {
        repaint();
        for (Runnable listener : new ArrayList<>(viewListeners)) {
            listener.run();
        }
    }

    private void wheelMoved(MouseWheelEvent e) {
        if (!navigable) {
            return;
        }
        double turn = e.getPreciseWheelRotation();
        mouseAt = e.getPoint();
        if (e.isMetaDown() || e.isControlDown()) {
            zoomAt(e.getPoint(), Math.pow(WHEEL_ZOOM, -turn));
        } else if (e.isShiftDown()) {
            panBy(-turn * SCROLL_STEP, 0);
        } else {
            panBy(0, -turn * SCROLL_STEP);
        }
    }

    // ---- eraser --------------------------------------------------------------------------

    /** A stroke wiped by the eraser, and where it was in the writing. */
    private static final class Erased {
        final int index;
        final Stroke stroke;

        Erased(int index, Stroke stroke) {
            this.index = index;
            this.stroke = stroke;
        }
    }

    private void eraseAlong(double x, double y) {
        // check points along the way, so a quick sweep does not jump over a stroke
        double fromX = sweptTo.getX();
        double fromY = sweptTo.getY();
        double step = ERASER_RADIUS / 2 / zoom;
        int steps = Math.max(1, (int) Math.ceil(Math.hypot(x - fromX, y - fromY) / step));
        for (int k = 1; k <= steps; k++) {
            eraseAround(fromX + (x - fromX) * k / steps, fromY + (y - fromY) * k / steps);
        }
        sweptTo = new Point2D.Double(x, y);
    }

    private void eraseAround(double x, double y) {
        double radius = ERASER_RADIUS / zoom;
        double reach = radius + INK_WIDTH / 2;
        for (int i = strokes.size() - 1; i >= 0; i--) {
            if (distance(strokes.get(i), x, y) <= reach) {
                sweep.add(new Erased(i, strokes.remove(i)));
            }
        }
        for (int i = placed.size() - 1; i >= 0; i--) {
            if (shapeOf(placed.get(i)).intersects(x - radius, y - radius, 2 * radius,
                    2 * radius)) {
                sweptPlaced.add(placed.remove(i));
            }
        }
        for (int i = notes.size() - 1; i >= 0; i--) {
            if (notes.get(i).shape.intersects(x - radius, y - radius, 2 * radius, 2 * radius)) {
                Note note = notes.remove(i);
                sweptNotes.add(note);
                for (NoteListener listener : new ArrayList<>(noteListeners)) {
                    listener.erased(note.getKey());
                }
            }
        }
    }

    private void finishSweep() {
        List<Erased> wiped = sweep;
        List<Note> wipedNotes = sweptNotes;
        List<PlacedSymbol> wipedPlaced = sweptPlaced;
        sweep = null;
        sweptNotes = null;
        sweptPlaced = null;
        sweptTo = null;
        if (wiped.isEmpty() && wipedNotes.isEmpty() && wipedPlaced.isEmpty()) {
            repaint();
            return;
        }
        boolean writingChanged = !wiped.isEmpty() || !wipedPlaced.isEmpty();
        // put the strokes back latest first, so each returns to where it was
        undoSteps.push(() -> {
            for (int k = wiped.size() - 1; k >= 0; k--) {
                strokes.add(wiped.get(k).index, wiped.get(k).stroke);
            }
            placed.addAll(wipedPlaced);
            restore(wipedNotes);
            if (writingChanged) {
                edited();
            }
        });
        if (writingChanged) {
            edited();
        } else {
            repaint();
        }
    }

    private void restore(List<Note> restored) {
        notes.addAll(restored);
        for (Note note : restored) {
            for (NoteListener listener : new ArrayList<>(noteListeners)) {
                listener.restored(note.getKey());
            }
        }
        repaint();
    }

    private static double distance(Stroke stroke, double x, double y) {
        List<Point> points = stroke.getPoints();
        Point first = points.get(0);
        double best = Math.hypot(first.getX() - x, first.getY() - y);
        for (int i = 1; i < points.size(); i++) {
            Point a = points.get(i - 1);
            Point b = points.get(i);
            best = Math.min(best,
                    Line2D.ptSegDist(a.getX(), a.getY(), b.getX(), b.getY(), x, y));
        }
        return best;
    }

    // ---- painting ------------------------------------------------------------------------

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
            paintGrid(g, w, h);
            if (strokes.isEmpty() && placed.isEmpty() && currentPoints == null) {
                g.setFont(Theme.font(java.awt.Font.PLAIN, 22f));
                g.setColor(new Color(0xB4B4BC));
                FontMetrics fm = g.getFontMetrics();
                g.drawString(HINT, (w - fm.stringWidth(HINT)) / 2,
                        (h - fm.getHeight()) / 2 + fm.getAscent());
            }

            Graphics2D page = (Graphics2D) g.create();
            try {
                page.translate(offsetX, offsetY);
                page.scale(zoom, zoom);
                page.setColor(Theme.TEXT);
                page.setStroke(new BasicStroke(INK_WIDTH, BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND));
                for (Stroke stroke : strokes) {
                    page.draw(toPath(stroke.getPoints()));
                }
                if (currentPoints != null) {
                    page.draw(toPath(currentPoints));
                }
                paintPlaced(page);
                paintNotes(page);
                paintUnderlines(page);
            } finally {
                page.dispose();
            }
            g.setClip(oldClip);

            if (!fullBleed) {
                g.setColor(Theme.BORDER);
                g.draw(paper);
            }
            paintEraser(g);
        } finally {
            g.dispose();
        }
    }

    /** Dots fixed to the page, thinned out when zoomed far out. */
    private void paintGrid(Graphics2D g, int w, int h) {
        double spacing = GRID * zoom;
        while (spacing < MIN_DOT_SPACING) {
            spacing *= 2;
        }
        double startX = mod(offsetX + GRID / 2.0 * zoom, spacing);
        double startY = mod(offsetY + GRID / 2.0 * zoom, spacing);
        g.setColor(Theme.GRID_DOT);
        for (double x = startX; x < w; x += spacing) {
            for (double y = startY; y < h; y += spacing) {
                g.fillOval((int) x - 1, (int) y - 1, 3, 3);
            }
        }
    }

    private static double mod(double value, double modulus) {
        double rest = value % modulus;
        return rest < 0 ? rest + modulus : rest;
    }

    /** Picked symbols are set in ink, inked a little like the notes. */
    private void paintPlaced(Graphics2D g) {
        g.setColor(Theme.TEXT);
        for (PlacedSymbol symbol : placed) {
            Shape shape = shapeOf(symbol);
            g.fill(shape);
            g.setStroke(new BasicStroke((float) Math.max(1,
                    NOTE_INK_RATIO * shape.getBounds2D().getHeight()),
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(shape);
        }
    }

    /** The filled letter whose contours the symbol's outline strokes are. */
    private static Shape shapeOf(PlacedSymbol symbol) {
        Path2D.Double shape = new Path2D.Double(Path2D.WIND_NON_ZERO);
        for (Stroke contour : symbol.getOutline()) {
            shape.append(toPath(contour.getPoints()), false);
            shape.closePath();
        }
        return shape;
    }

    private void paintNotes(Graphics2D g) {
        for (Note note : notes) {
            g.setColor(note.color);
            g.fill(note.shape);
            g.setStroke(new BasicStroke((float) Math.max(1, NOTE_INK_RATIO * note.height),
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(note.shape);
        }
    }

    private void paintUnderlines(Graphics2D g) {
        g.setColor(Theme.ERROR);
        g.setStroke(new BasicStroke(WAVE_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (Rectangle2D box : underlined) {
            double y = box.getMaxY() + WAVE_GAP;
            Path2D.Double wave = new Path2D.Double();
            wave.moveTo(box.getMinX(), y);
            int halves = Math.max(2, (int) Math.round(box.getWidth() / (WAVE_LENGTH / 2)));
            double step = box.getWidth() / halves;
            for (int k = 1; k <= halves; k++) {
                double peak = k % 2 == 1 ? WAVE_HEIGHT : -WAVE_HEIGHT;
                wave.quadTo(box.getMinX() + (k - 0.5) * step, y + 2 * peak,
                        box.getMinX() + k * step, y);
            }
            g.draw(wave);
        }
    }

    private void paintEraser(Graphics2D g) {
        if (!erasing || mouseAt == null) {
            return;
        }
        Ellipse2D ring = new Ellipse2D.Double(mouseAt.x - ERASER_RADIUS,
                mouseAt.y - ERASER_RADIUS, 2 * ERASER_RADIUS, 2 * ERASER_RADIUS);
        g.setColor(new Color(255, 255, 255, 170));
        g.fill(ring);
        g.setColor(Theme.MUTED);
        g.setStroke(new BasicStroke(1.5f));
        g.draw(ring);
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
