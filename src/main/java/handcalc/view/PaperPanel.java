package handcalc.view;

import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JPanel;

/** Plain dotted paper: the backdrop under floating panels on screens without a canvas. */
public class PaperPanel extends JPanel {

    private static final int GRID = 28;

    public PaperPanel() {
        setOpaque(true);
        setBackground(Theme.CARD);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setColor(Theme.GRID_DOT);
            for (int x = GRID / 2; x < getWidth(); x += GRID) {
                for (int y = GRID / 2; y < getHeight(); y += GRID) {
                    g.fillOval(x - 1, y - 1, 3, 3);
                }
            }
        } finally {
            g.dispose();
        }
    }
}
