package handcalc.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import handcalc.entity.Stroke;
import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import org.junit.jupiter.api.Test;

class CanvasPanelTest {

    private final CanvasPanel canvas = new CanvasPanel();

    private void mouse(int id, int x, int y) {
        canvas.dispatchEvent(new MouseEvent(canvas, id, System.currentTimeMillis(),
                id == MouseEvent.MOUSE_DRAGGED ? MouseEvent.BUTTON1_DOWN_MASK : 0,
                x, y, 1, false, MouseEvent.BUTTON1));
    }

    private void drawLine(int x1, int y1, int x2, int y2) {
        mouse(MouseEvent.MOUSE_PRESSED, x1, y1);
        mouse(MouseEvent.MOUSE_DRAGGED, (x1 + x2) / 2, (y1 + y2) / 2);
        mouse(MouseEvent.MOUSE_DRAGGED, x2, y2);
        mouse(MouseEvent.MOUSE_RELEASED, x2, y2);
    }

    @Test
    void pressDragReleaseRecordsOneStrokeWithAllPoints() {
        drawLine(10, 20, 110, 20);

        List<Stroke> strokes = canvas.getStrokes();
        assertEquals(1, strokes.size());
        assertEquals(3, strokes.get(0).getPoints().size());
        assertEquals(10, strokes.get(0).getMinX());
        assertEquals(110, strokes.get(0).getMaxX());
    }

    @Test
    void releasePositionIsKeptWhenThePenMovedSinceTheLastDrag() {
        mouse(MouseEvent.MOUSE_PRESSED, 10, 20);
        mouse(MouseEvent.MOUSE_DRAGGED, 60, 20);
        mouse(MouseEvent.MOUSE_RELEASED, 120, 20);

        Stroke stroke = canvas.getStrokes().get(0);
        assertEquals(3, stroke.getPoints().size());
        assertEquals(120, stroke.getMaxX());
    }

    @Test
    void aClickRecordsADot() {
        mouse(MouseEvent.MOUSE_PRESSED, 50, 50);
        mouse(MouseEvent.MOUSE_RELEASED, 50, 50);

        assertEquals(1, canvas.getStrokes().size());
        assertEquals(1, canvas.getStrokes().get(0).getPoints().size());
    }

    @Test
    void undoRemovesTheLastStrokeOnly() {
        drawLine(0, 0, 50, 50);
        drawLine(100, 0, 150, 50);

        canvas.undo();

        assertEquals(1, canvas.getStrokes().size());
        assertEquals(0, canvas.getStrokes().get(0).getMinX());
    }

    @Test
    void undoOnEmptyCanvasDoesNothing() {
        canvas.undo();
        assertTrue(canvas.getStrokes().isEmpty());
    }

    @Test
    void clearRemovesEverything() {
        drawLine(0, 0, 50, 50);
        drawLine(100, 0, 150, 50);

        canvas.clear();

        assertTrue(canvas.getStrokes().isEmpty());
    }

    private static boolean hasDarkPixelInRow(BufferedImage image, int y) {
        for (int x = 0; x < image.getWidth(); x++) {
            Color c = new Color(image.getRGB(x, y));
            if (c.getRed() < 0x60 && c.getGreen() < 0x60 && c.getBlue() < 0x60) {
                return true;
            }
        }
        return false;
    }

    @Test
    void strokesArePaintedAsInk() {
        canvas.setSize(300, 100);
        BufferedImage blank = new BufferedImage(300, 100, BufferedImage.TYPE_INT_RGB);
        canvas.paint(blank.createGraphics());

        drawLine(10, 50, 110, 50);
        BufferedImage inked = new BufferedImage(300, 100, BufferedImage.TYPE_INT_RGB);
        canvas.paint(inked.createGraphics());

        assertFalse(hasDarkPixelInRow(blank, 50), "an empty canvas has no ink");
        assertTrue(hasDarkPixelInRow(inked, 50), "the stroke is painted dark");
    }

    @Test
    void setStrokesReplacesTheWritingAndNotifiesListeners() {
        drawLine(0, 0, 50, 50);
        int[] changes = {0};
        canvas.addChangeListener(e -> changes[0]++);
        Stroke replacement = new Stroke(java.util.Arrays.asList(
                new handcalc.entity.Point(5, 6), new handcalc.entity.Point(7, 8)));

        canvas.setStrokes(java.util.Collections.singletonList(replacement));

        assertEquals(1, canvas.getStrokes().size());
        assertEquals(5, canvas.getStrokes().get(0).getMinX());
        assertEquals(1, changes[0]);
    }
}
