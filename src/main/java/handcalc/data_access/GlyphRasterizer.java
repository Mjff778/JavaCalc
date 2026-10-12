package handcalc.data_access;

import handcalc.entity.Glyph;
import handcalc.entity.Point;
import handcalc.entity.Stroke;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.util.List;

/**
 * Turns a {@link Glyph} into the fixed-size grayscale image the classifier works on.
 *
 * <p>Like MNIST, the symbol is scaled to fit a 20x20 box <em>keeping its aspect ratio</em> and
 * centred in a 28x28 image, so a "−" stays a flat bar and a "1" stays a thin line.
 */
public final class GlyphRasterizer {

    public static final int SIZE = 28;
    private static final double BOX = 20;
    private static final float PEN_WIDTH = 2.2f;

    private GlyphRasterizer() {
    }

    /** @return {@code SIZE*SIZE} values in [0,1], row-major, 1 = ink. */
    public static float[] rasterize(Glyph glyph) {
        double width = glyph.getMaxX() - glyph.getMinX();
        double height = glyph.getMaxY() - glyph.getMinY();
        double longest = Math.max(width, height);
        double scale = longest < 1e-9 ? 1 : BOX / longest;
        double offsetX = (SIZE - width * scale) / 2 - glyph.getMinX() * scale;
        double offsetY = (SIZE - height * scale) / 2 - glyph.getMinY() * scale;

        BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, SIZE, SIZE);
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(PEN_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (Stroke stroke : glyph.getStrokes()) {
                g.draw(toPath(stroke, scale, offsetX, offsetY));
            }
        } finally {
            g.dispose();
        }

        Raster raster = image.getRaster();
        float[] pixels = new float[SIZE * SIZE];
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                pixels[y * SIZE + x] = raster.getSample(x, y, 0) / 255f;
            }
        }
        return pixels;
    }

    private static Path2D toPath(Stroke stroke, double scale, double offsetX, double offsetY) {
        List<Point> points = stroke.getPoints();
        Path2D.Double path = new Path2D.Double();
        Point first = points.get(0);
        double startX = first.getX() * scale + offsetX;
        double startY = first.getY() * scale + offsetY;
        path.moveTo(startX, startY);
        if (points.size() == 1) {
            // a zero-length segment is drawn as a round dot
            path.lineTo(startX, startY);
        }
        for (int i = 1; i < points.size(); i++) {
            path.lineTo(points.get(i).getX() * scale + offsetX,
                    points.get(i).getY() * scale + offsetY);
        }
        return path;
    }
}
