package handcalc.view;

import handcalc.entity.Formula;
import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Typesets a {@link Formula}: fractions stacked over a bar, powers raised and smaller, roots
 * under a sign with a bar over the radicand, brackets as tall as what they hold.
 */
final class FormulaPainter {

    /** How a piece is laid out: its size around the baseline, and how to draw it. */
    static final class Box {
        final double width;
        final double ascent;
        final double descent;
        private final Drawing drawing;

        Box(double width, double ascent, double descent, Drawing drawing) {
            this.width = width;
            this.ascent = ascent;
            this.descent = descent;
            this.drawing = drawing;
        }

        /** Draws the piece with its left edge at x and its baseline at y. */
        void draw(Graphics2D g, double x, double y) {
            drawing.draw(g, x, y);
        }
    }

    private interface Drawing {
        void draw(Graphics2D g, double x, double y);
    }

    private static final FontRenderContext CONTEXT =
            new FontRenderContext(new AffineTransform(), true, true);
    private static final String OPERATORS = "+−×÷";
    private static final double OPERATOR_SPACE = 0.22;
    private static final double SCRIPT_SCALE = 0.7;
    private static final double FRACTION_SCALE = 0.85;
    /** Where fraction bars sit above the baseline: the middle of the digits. */
    private static final double AXIS = 0.3;
    private static final double RISE = 0.45;
    private static final double GAP = 0.12;

    private FormulaPainter() {
    }

    /** @param size the font size of the outermost pieces */
    static Box layOut(Formula formula, float size) {
        switch (formula.getKind()) {
            case TEXT:
                return text(formula.getText(), size);
            case FRACTION:
                return fraction(layOut(formula.getChildren().get(0), size * (float) FRACTION_SCALE),
                        layOut(formula.getChildren().get(1), size * (float) FRACTION_SCALE), size);
            case ROOT:
                return root(layOut(formula.getChildren().get(0), size), size);
            case POWER:
                return power(layOut(formula.getChildren().get(0), size),
                        layOut(formula.getChildren().get(1), size * (float) SCRIPT_SCALE), size);
            case BRACKETS:
                return brackets(layOut(formula.getChildren().get(0), size), size);
            default:
                List<Box> pieces = new ArrayList<>();
                for (Formula child : formula.getChildren()) {
                    pieces.add(layOut(child, size));
                }
                return row(pieces);
        }
    }

    private static Box text(String text, float size) {
        Font font = Theme.font(Font.PLAIN, size);
        LineMetrics metrics = font.getLineMetrics(text, CONTEXT);
        double space = OPERATORS.contains(text) ? OPERATOR_SPACE * size : 0;
        double width = font.getStringBounds(text, CONTEXT).getWidth() + 2 * space;
        // digits do not reach the full ascent of the font
        return new Box(width, metrics.getAscent() * 0.8, metrics.getDescent(), (g, x, y) -> {
            g.setFont(font);
            g.drawString(text, (float) (x + space), (float) y);
        });
    }

    private static Box row(List<Box> pieces) {
        double width = 0;
        double ascent = 0;
        double descent = 0;
        for (Box piece : pieces) {
            width += piece.width;
            ascent = Math.max(ascent, piece.ascent);
            descent = Math.max(descent, piece.descent);
        }
        return new Box(width, ascent, descent, (g, x, y) -> {
            double at = x;
            for (Box piece : pieces) {
                piece.draw(g, at, y);
                at += piece.width;
            }
        });
    }

    private static Box fraction(Box numerator, Box denominator, float size) {
        double gap = GAP * size;
        double pad = 0.15 * size;
        double width = Math.max(numerator.width, denominator.width) + 2 * pad;
        double axis = AXIS * size;
        double ascent = axis + gap + numerator.descent + numerator.ascent;
        double descent = gap + denominator.ascent + denominator.descent - axis;
        return new Box(width, ascent, descent, (g, x, y) -> {
            double bar = y - axis;
            numerator.draw(g, x + (width - numerator.width) / 2, bar - gap - numerator.descent);
            denominator.draw(g, x + (width - denominator.width) / 2,
                    bar + gap + denominator.ascent);
            g.setStroke(new BasicStroke(size / 16f));
            g.draw(new Line2D.Double(x + pad / 2, bar, x + width - pad / 2, bar));
        });
    }

    private static Box root(Box radicand, float size) {
        double gap = GAP * size;
        double sign = 0.55 * size;
        double width = sign + radicand.width + 0.1 * size;
        double ascent = radicand.ascent + 2 * gap;
        return new Box(width, ascent, radicand.descent, (g, x, y) -> {
            double top = y - radicand.ascent - gap;
            Path2D.Double path = new Path2D.Double();
            path.moveTo(x, y - 0.35 * size);
            path.lineTo(x + 0.15 * size, y - 0.42 * size);
            path.lineTo(x + 0.3 * size, y + radicand.descent);
            path.lineTo(x + sign - 0.05 * size, top);
            path.lineTo(x + width, top);
            g.setStroke(new BasicStroke(size / 16f, BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND));
            g.draw(path);
            radicand.draw(g, x + sign, y);
        });
    }

    private static Box power(Box base, Box exponent, float size) {
        double rise = RISE * size;
        double width = base.width + exponent.width + 0.05 * size;
        double ascent = Math.max(base.ascent, rise + exponent.ascent);
        double descent = Math.max(base.descent, exponent.descent - rise);
        return new Box(width, ascent, descent, (g, x, y) -> {
            base.draw(g, x, y);
            exponent.draw(g, x + base.width + 0.05 * size, y - rise);
        });
    }

    private static Box brackets(Box inside, float size) {
        double side = 0.3 * size;
        double ascent = inside.ascent + 0.1 * size;
        double descent = inside.descent + 0.1 * size;
        return new Box(inside.width + 2 * side, ascent, descent, (g, x, y) -> {
            g.setStroke(new BasicStroke(size / 16f, BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND));
            double top = y - ascent;
            double bottom = y + descent;
            double middle = (top + bottom) / 2;
            Path2D.Double open = new Path2D.Double();
            open.moveTo(x + side * 0.8, top);
            open.quadTo(x - side * 0.1, middle, x + side * 0.8, bottom);
            Path2D.Double close = new Path2D.Double();
            double right = x + side + inside.width;
            close.moveTo(right + side * 0.2, top);
            close.quadTo(right + side * 1.1, middle, right + side * 0.2, bottom);
            g.draw(open);
            g.draw(close);
            inside.draw(g, x + side, y);
        });
    }
}
