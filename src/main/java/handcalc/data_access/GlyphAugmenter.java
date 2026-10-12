package handcalc.data_access;

import handcalc.entity.Glyph;
import handcalc.entity.Point;
import handcalc.entity.Stroke;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Makes plausible variations of a written symbol (stretch, tilt, slant, wobble) around the
 * same centre and size, so a few real samples can stand for many.
 */
public final class GlyphAugmenter {

    private GlyphAugmenter() {
    }

    public static Glyph distort(Glyph glyph, Random random) {
        double width = glyph.getMaxX() - glyph.getMinX();
        double height = glyph.getMaxY() - glyph.getMinY();
        double size = Math.max(width, height);
        if (size < 1e-9) {
            return glyph;
        }
        double centreX = (glyph.getMinX() + glyph.getMaxX()) / 2;
        double centreY = (glyph.getMinY() + glyph.getMaxY()) / 2;

        double scaleX = 0.7 + 0.6 * random.nextDouble();
        double scaleY = 0.8 + 0.4 * random.nextDouble();
        double angle = Math.toRadians((random.nextDouble() * 2 - 1) * 12);
        double shear = (random.nextDouble() * 2 - 1) * 0.2;
        double wobble = 0.02;

        List<Stroke> strokes = new ArrayList<>();
        for (Stroke stroke : glyph.getStrokes()) {
            double strokeDx = random.nextGaussian() * 0.02;
            double strokeDy = random.nextGaussian() * 0.02;
            List<Point> points = new ArrayList<>();
            for (Point p : stroke.getPoints()) {
                double x = ((p.getX() - centreX) / size + strokeDx
                        + random.nextGaussian() * wobble) * scaleX;
                double y = ((p.getY() - centreY) / size + strokeDy
                        + random.nextGaussian() * wobble) * scaleY;
                x += shear * y;
                double rx = x * Math.cos(angle) - y * Math.sin(angle);
                double ry = x * Math.sin(angle) + y * Math.cos(angle);
                points.add(new Point(centreX + rx * size, centreY + ry * size));
            }
            strokes.add(new Stroke(points));
        }
        return new Glyph(strokes);
    }
}
