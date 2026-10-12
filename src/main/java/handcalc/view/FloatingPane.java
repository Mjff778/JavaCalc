package handcalc.view;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.swing.JPanel;

/**
 * A screen made of one full-window backdrop (the writing canvas or plain paper) with glass
 * panels floating above it, stacked inwards from the edge they are anchored to.
 */
public class FloatingPane extends JPanel {

    /** Where a floating panel sits. FILL takes whatever room the edge stacks leave. */
    public enum Anchor {
        TOP_LEFT, TOP_CENTER, TOP_RIGHT, TOP_STRETCH,
        BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT, BOTTOM_STRETCH,
        CENTER, FILL
    }

    private static final int GAP = 6;
    /** Glass panels carry a shadow margin, so neighbours may overlap their bounds a little. */
    private static final int OVERLAP = 18;

    private final List<Placed> floating = new ArrayList<>();
    private Component backdrop;

    public FloatingPane() {
        super(null);
        setLayout(new Layout());
        setBackground(Theme.CARD);
    }

    /** The component that fills the window; it is painted beneath everything else. */
    protected void setBackdrop(Component component) {
        if (backdrop != null) {
            remove(backdrop);
        }
        backdrop = component;
        add(component, getComponentCount());
    }

    public Component getBackdrop() {
        return backdrop;
    }

    public void floatAt(Component component, Anchor anchor) {
        floatAt(component, anchor, 0, Integer.MAX_VALUE);
    }

    /**
     * @param minWidth the narrowest the panel may be laid out
     * @param maxWidth the widest the panel may be laid out
     */
    public void floatAt(Component component, Anchor anchor, int minWidth, int maxWidth) {
        floating.add(new Placed(component, anchor, minWidth, maxWidth));
        add(component, 0);
    }

    /** Floating panels overlap the backdrop, so Swing must repaint them back to front. */
    @Override
    public boolean isOptimizedDrawingEnabled() {
        return false;
    }

    private static final class Placed {
        final Component component;
        final Anchor anchor;
        final int minWidth;
        final int maxWidth;

        Placed(Component component, Anchor anchor, int minWidth, int maxWidth) {
            this.component = component;
            this.anchor = anchor;
            this.minWidth = minWidth;
            this.maxWidth = maxWidth;
        }
    }

    private final class Layout implements LayoutManager {
        @Override
        public void addLayoutComponent(String name, Component comp) {
        }

        @Override
        public void removeLayoutComponent(Component comp) {
        }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            return new Dimension(900, 620);
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            return new Dimension(560, 420);
        }

        @Override
        public void layoutContainer(Container parent) {
            int w = parent.getWidth();
            int h = parent.getHeight();
            if (backdrop != null) {
                backdrop.setBounds(0, 0, w, h);
            }
            Map<Anchor, Integer> cursor = new EnumMap<>(Anchor.class);
            int topEdge = GAP;
            int bottomEdge = h - GAP;
            Placed fill = null;
            for (Placed item : floating) {
                if (item.anchor == Anchor.FILL) {
                    fill = item;
                    continue;
                }
                Dimension size = item.component.getPreferredSize();
                int width = Math.min(w - 2 * GAP,
                        Math.min(item.maxWidth, Math.max(size.width, item.minWidth)));
                boolean top = item.anchor.name().startsWith("TOP");
                boolean centered = item.anchor == Anchor.CENTER;
                int x;
                if (item.anchor.name().endsWith("STRETCH")) {
                    x = GAP;
                    width = w - 2 * GAP;
                } else if (item.anchor.name().endsWith("LEFT")) {
                    x = GAP;
                } else if (item.anchor.name().endsWith("RIGHT")) {
                    x = w - GAP - width;
                } else {
                    x = (w - width) / 2;
                }
                if (centered) {
                    item.component.setBounds(x, (h - size.height) / 2, width, size.height);
                    continue;
                }
                int y;
                if (top) {
                    y = cursor.getOrDefault(item.anchor, GAP);
                    cursor.put(item.anchor, y + size.height - OVERLAP);
                    topEdge = Math.max(topEdge, y + size.height - OVERLAP);
                } else {
                    int start = cursor.getOrDefault(item.anchor, h - GAP);
                    y = start - size.height;
                    cursor.put(item.anchor, y + OVERLAP);
                    bottomEdge = Math.min(bottomEdge, y + OVERLAP);
                }
                item.component.setBounds(x, y, width, size.height);
            }
            if (fill != null) {
                int topY = topEdge == GAP ? GAP : topEdge;
                fill.component.setBounds(GAP, topY, w - 2 * GAP, Math.max(0, bottomEdge - topY));
            }
        }
    }
}
