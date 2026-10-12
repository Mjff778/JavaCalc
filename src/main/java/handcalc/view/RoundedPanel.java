package handcalc.view;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import javax.swing.JPanel;

/**
 * A panel drawn as a rounded card with an optional hairline border.
 */
public class RoundedPanel extends JPanel {

    private Color fill;
    private final Color border;
    private final int arc;

    public RoundedPanel(LayoutManager layout, Color fill, Color border, int arc) {
        super(layout);
        this.fill = fill;
        this.border = border;
        this.arc = arc;
        setOpaque(false);
    }

    public void setFill(Color newFill) {
        this.fill = newFill;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(fill);
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            if (border != null) {
                g.setColor(border);
                g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            }
        } finally {
            g.dispose();
        }
        super.paintComponent(graphics);
    }
}
