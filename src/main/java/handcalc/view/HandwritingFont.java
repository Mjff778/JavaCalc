package handcalc.view;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The handwriting-style typeface the calculator writes its answers in: Caveat, bundled under
 * the SIL Open Font License (see {@code fonts/Caveat-OFL.txt}).
 */
final class HandwritingFont {

    private static final String RESOURCE = "/fonts/Caveat.ttf";
    private static final float REFERENCE_SIZE = 100f;
    private static final FontRenderContext CONTEXT =
            new FontRenderContext(new AffineTransform(), true, true);
    private static final Logger LOGGER = Logger.getLogger(HandwritingFont.class.getName());

    private static Font base;

    private HandwritingFont() {
    }

    /**
     * @param text what to write
     * @param x the left edge of the text
     * @param centreY the height the text's digits are centred on
     * @param digitHeight how tall the text's digits are
     * @return the outline of the written text
     */
    static Shape outline(String text, double x, double centreY, double digitHeight) {
        Font reference = font().deriveFont(REFERENCE_SIZE);
        // digits sit on the baseline: measure one to size the text and centre it
        Rectangle2D digit = reference.createGlyphVector(CONTEXT, "0").getVisualBounds();
        float size = (float) (REFERENCE_SIZE * digitHeight / digit.getHeight());
        double scale = size / REFERENCE_SIZE;
        double baseline = centreY - (digit.getMinY() + digit.getMaxY()) / 2 * scale;
        return reference.deriveFont(size).createGlyphVector(CONTEXT, text)
                .getOutline((float) x, (float) baseline);
    }

    private static synchronized Font font() {
        if (base == null) {
            try (InputStream in = HandwritingFont.class.getResourceAsStream(RESOURCE)) {
                if (in == null) {
                    throw new IOException(RESOURCE + " is missing");
                }
                base = Font.createFont(Font.TRUETYPE_FONT, in);
            } catch (IOException | FontFormatException e) {
                LOGGER.log(Level.WARNING, "Could not load the handwriting font", e);
                base = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
            }
        }
        return base;
    }
}
