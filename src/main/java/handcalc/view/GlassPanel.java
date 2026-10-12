package handcalc.view;

import java.awt.Color;
import java.awt.Component;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/**
 * A frosted-glass panel that floats over a backdrop: the backdrop behind it is blurred and
 * lightly tinted, with a soft shadow and a bright rim.
 */
public class GlassPanel extends JPanel {

    private static final int SHADOW = 14;
    private static final int ARC = 30;

    private final Component explicitBackdrop;

    /**
     * @param backdrop the component painted underneath, which shows blurred through the glass;
     *                 null takes the backdrop of the enclosing {@link FloatingPane}
     * @param padding space between the glass edge and the content
     */
    public GlassPanel(LayoutManager layout, Component backdrop, int padding) {
        super(layout);
        this.explicitBackdrop = backdrop;
        setOpaque(false);
        setBorder(new EmptyBorder(SHADOW + padding, SHADOW + padding + 4, SHADOW + padding,
                SHADOW + padding + 4));
        // the glass is solid to the pen: writing never starts underneath a control
        addMouseListener(new MouseAdapter() { });
        addMouseMotionListener(new MouseAdapter() { });
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth() - 2 * SHADOW;
            int h = getHeight() - 2 * SHADOW;
            if (w <= 0 || h <= 0) {
                return;
            }
            // soft shadow: nested translucent outlines, fading out well inside the panel bounds
            for (int i = SHADOW - 3; i > 0; i--) {
                g.setColor(new Color(40, 40, 90, 3));
                g.fill(new RoundRectangle2D.Double(SHADOW - i, SHADOW - i + 3,
                        w + 2 * i, h + 2 * i, ARC + 2 * i, ARC + 2 * i));
            }
            RoundRectangle2D.Double shape = new RoundRectangle2D.Double(SHADOW, SHADOW, w, h,
                    ARC, ARC);

            java.awt.Shape oldClip = g.getClip();
            g.clip(shape);
            BufferedImage blurred = blurredBackdrop(w, h);
            if (blurred != null) {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(blurred, SHADOW, SHADOW, w, h, null);
            }
            g.setColor(new Color(255, 255, 255, 150));
            g.fill(shape);
            g.setPaint(new GradientPaint(0, SHADOW, new Color(255, 255, 255, 190),
                    0, SHADOW + h * 0.55f, new Color(255, 255, 255, 0)));
            g.fill(shape);
            g.setClip(oldClip);

            g.setColor(new Color(60, 60, 110, 38));
            g.draw(shape);
            g.setColor(new Color(255, 255, 255, 230));
            g.draw(new RoundRectangle2D.Double(SHADOW + 1, SHADOW + 1, w - 2, h - 2,
                    ARC - 2, ARC - 2));
        } finally {
            g.dispose();
        }
        super.paintComponent(graphics);
    }

    /** What lies under the glass, shrunk and scaled back up so it looks out of focus. */
    private BufferedImage blurredBackdrop(int w, int h) {
        Component backdrop = explicitBackdrop;
        if (backdrop == null && getParent() instanceof FloatingPane) {
            backdrop = ((FloatingPane) getParent()).getBackdrop();
        }
        if (backdrop == null || backdrop.getWidth() == 0) {
            return null;
        }
        BufferedImage sharp = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D sg = sharp.createGraphics();
        try {
            sg.setColor(Color.WHITE);
            sg.fillRect(0, 0, w, h);
            sg.translate(backdrop.getX() - getX() - SHADOW, backdrop.getY() - getY() - SHADOW);
            // print, not paint: we are already inside a double-buffered paint of this window
            backdrop.print(sg);
        } catch (RuntimeException e) {
            return null;
        } finally {
            sg.dispose();
        }
        BufferedImage current = sharp;
        for (int step = 0; step < 2; step++) {
            int sw = Math.max(1, current.getWidth() / 3);
            int sh = Math.max(1, current.getHeight() / 3);
            BufferedImage small = new BufferedImage(sw, sh, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = small.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(current, 0, 0, sw, sh, null);
            g.dispose();
            current = small;
        }
        return current;
    }
}
