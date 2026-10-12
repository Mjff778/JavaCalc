package handcalc.view;

import handcalc.entity.PlacedSymbol;
import handcalc.entity.Point;
import handcalc.entity.Stroke;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Sets a symbol the user picked in the handwriting typeface, in the place of the writing it
 * replaces and about as big.
 */
final class TypesetSymbol {

    private static final double REFERENCE_HEIGHT = 100;
    /** A box flatter or thinner than this is made rounder, so a picked digit is not squashed. */
    private static final double MIN_SIDE_RATIO = 0.6;
    /** A decimal point is a dot, however big the writing it replaces. */
    private static final double POINT_RATIO = 0.18;
    private static final double FLATNESS = 0.5;

    private TypesetSymbol() {
    }

    /**
     * @param box where the replaced writing is on the page
     * @param alternatives what else the replaced writing might have been
     */
    static PlacedSymbol place(char symbol, String alternatives, Rectangle2D box) {
        double width = box.getWidth();
        double height = box.getHeight();
        if (symbol == '.') {
            width = POINT_RATIO * Math.max(width, height);
            height = width;
        } else {
            width = Math.max(width, MIN_SIDE_RATIO * height);
            height = Math.max(height, MIN_SIDE_RATIO * width);
        }
        Shape letter = HandwritingFont.outline(String.valueOf(symbol), 0, 0, REFERENCE_HEIGHT);
        Rectangle2D bounds = letter.getBounds2D();
        double scale = Math.min(width / Math.max(bounds.getWidth(), 1e-9),
                height / Math.max(bounds.getHeight(), 1e-9));
        AffineTransform fit = new AffineTransform();
        fit.translate(box.getCenterX(), box.getCenterY());
        fit.scale(scale, scale);
        fit.translate(-bounds.getCenterX(), -bounds.getCenterY());
        return new PlacedSymbol(symbol, alternatives, outline(letter, fit));
    }

    /** One closed stroke for each contour of the letter. */
    private static List<Stroke> outline(Shape letter, AffineTransform fit) {
        List<Stroke> strokes = new ArrayList<>();
        List<Point> contour = new ArrayList<>();
        double[] at = new double[6];
        for (PathIterator path = letter.getPathIterator(fit, FLATNESS); !path.isDone();
                path.next()) {
            int segment = path.currentSegment(at);
            if (segment == PathIterator.SEG_MOVETO && !contour.isEmpty()) {
                strokes.add(new Stroke(contour));
                contour = new ArrayList<>();
            }
            if (segment == PathIterator.SEG_CLOSE) {
                if (!contour.isEmpty()) {
                    contour.add(contour.get(0));
                }
            } else {
                contour.add(new Point(at[0], at[1]));
            }
        }
        if (!contour.isEmpty()) {
            strokes.add(new Stroke(contour));
        }
        return strokes;
    }
}
