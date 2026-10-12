package handcalc.view;

import handcalc.interface_adapter.PracticeExpressionState;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.geom.Path2D;
import java.util.Collections;
import java.util.List;
import javax.swing.JComponent;

/** Draws an expression the way it is to be written: stacked fractions, raised powers. */
public class ExpressionPreview extends JComponent {

    private PracticeExpressionState.Layout layout = PracticeExpressionState.Layout.LINE;
    private List<String> parts = Collections.singletonList("");

    public ExpressionPreview() {
        setPreferredSize(new Dimension(260, 96));
        setOpaque(false);
    }

    public void setExpression(PracticeExpressionState.Layout newLayout, List<String> newParts) {
        layout = newLayout;
        parts = newParts;
        repaint();
    }

    public List<String> getParts() {
        return parts;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(Theme.ACCENT);
            Font big = Theme.font(Font.BOLD, 40f);
            Font small = Theme.font(Font.BOLD, 24f);
            switch (layout) {
                case POWER:
                    paintPower(g, big, small);
                    break;
                case FRACTION:
                    paintFraction(g, Theme.font(Font.BOLD, 34f));
                    break;
                case ROOT:
                    paintRoot(g, big);
                    break;
                default:
                    paintLine(g, big);
                    break;
            }
        } finally {
            g.dispose();
        }
    }

    private void paintLine(Graphics2D g, Font font) {
        g.setFont(fit(g, font, parts.get(0)));
        FontMetrics fm = g.getFontMetrics();
        String text = parts.get(0);
        g.drawString(text, (getWidth() - fm.stringWidth(text)) / 2,
                (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
    }

    private void paintPower(Graphics2D g, Font big, Font small) {
        String base = parts.get(0);
        String exponent = parts.get(1);
        g.setFont(big);
        FontMetrics bigMetrics = g.getFontMetrics();
        g.setFont(small);
        FontMetrics smallMetrics = g.getFontMetrics();
        int width = bigMetrics.stringWidth(base) + smallMetrics.stringWidth(exponent) + 2;
        int x = (getWidth() - width) / 2;
        int baseline = (getHeight() - bigMetrics.getHeight()) / 2 + bigMetrics.getAscent() + 10;
        g.setFont(big);
        g.drawString(base, x, baseline);
        g.setFont(small);
        g.drawString(exponent, x + bigMetrics.stringWidth(base) + 2,
                baseline - bigMetrics.getAscent() + smallMetrics.getAscent() - 4);
    }

    private void paintFraction(Graphics2D g, Font font) {
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        String top = parts.get(0);
        String bottom = parts.get(1);
        int width = Math.max(fm.stringWidth(top), fm.stringWidth(bottom)) + 20;
        int barY = getHeight() / 2;
        int x = (getWidth() - width) / 2;
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(x, barY, x + width, barY);
        g.drawString(top, x + (width - fm.stringWidth(top)) / 2, barY - 8);
        g.drawString(bottom, x + (width - fm.stringWidth(bottom)) / 2,
                barY + 8 + fm.getAscent());
    }

    private void paintRoot(Graphics2D g, Font font) {
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        String text = parts.get(0);
        int textWidth = fm.stringWidth(text);
        int sign = 26;
        int total = sign + textWidth + 12;
        int x = (getWidth() - total) / 2;
        int top = (getHeight() - fm.getHeight()) / 2 + 2;
        int bottom = top + fm.getHeight();
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D.Double path = new Path2D.Double();
        path.moveTo(x, bottom - fm.getHeight() * 0.4);
        path.lineTo(x + 6, bottom - fm.getHeight() * 0.5);
        path.lineTo(x + 14, bottom);
        path.lineTo(x + sign, top);
        path.lineTo(x + total, top);
        g.draw(path);
        g.drawString(text, x + sign + 4, top + fm.getAscent());
    }

    private Font fit(Graphics2D g, Font font, String text) {
        Font current = font;
        while (current.getSize() > 16
                && g.getFontMetrics(current).stringWidth(text) > getWidth() - 12) {
            current = current.deriveFont(current.getSize() - 2f);
        }
        return current;
    }
}
