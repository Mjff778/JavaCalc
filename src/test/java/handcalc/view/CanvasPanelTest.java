package handcalc.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import handcalc.entity.Stroke;
import java.awt.Color;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

    private CanvasPanel.Note note(Object key, double x) {
        return new CanvasPanel.Note(key, "42", x, 60, 40, Theme.ACCENT);
    }

    private static int count(BufferedImage image, Color colour) {
        int found = 0;
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                if (image.getRGB(x, y) == colour.getRGB()) {
                    found++;
                }
            }
        }
        return found;
    }

    private BufferedImage paintCanvas() {
        canvas.setSize(300, 120);
        BufferedImage image = new BufferedImage(300, 120, BufferedImage.TYPE_INT_RGB);
        canvas.paint(image.createGraphics());
        return image;
    }

    private void wheel(int x, int y, double turn, int modifiers) {
        canvas.dispatchEvent(new MouseWheelEvent(canvas, MouseEvent.MOUSE_WHEEL,
                System.currentTimeMillis(), modifiers, x, y, 0, 0, 0, false,
                MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, (int) Math.signum(turn), turn));
    }

    /** Records which notes the eraser wiped and which undo brought back. */
    private final List<String> noteEvents = new ArrayList<>();

    private void listenToNotes() {
        canvas.addNoteListener(new CanvasPanel.NoteListener() {
            @Override
            public void erased(Object key) {
                noteEvents.add("erased " + key);
            }

            @Override
            public void restored(Object key) {
                noteEvents.add("restored " + key);
            }
        });
    }

    @Test
    void notesAreShownButAreNotPartOfTheWriting() {
        drawLine(0, 0, 50, 50);
        int[] changes = {0};
        canvas.addChangeListener(e -> changes[0]++);

        canvas.setNotes(Collections.singletonList(note("a", 100)));

        assertEquals("42", canvas.getNotes().get(0).getText());
        assertEquals(1, canvas.getStrokes().size());
        assertEquals(0, changes[0]);
    }

    @Test
    void notesAreWrittenInTheirColour() {
        canvas.setNotes(Collections.singletonList(note("a", 100)));

        assertTrue(count(paintCanvas(), Theme.ACCENT) > 50);
    }

    @Test
    void faultySymbolsAreUnderlinedInRed() {
        canvas.setUnderlined(Collections.singletonList(
                new java.awt.geom.Rectangle2D.Double(40, 20, 60, 50)));

        BufferedImage image = paintCanvas();

        int red = 0;
        for (int x = 40; x <= 100; x++) {
            for (int y = 70; y < 90; y++) {
                Color c = new Color(image.getRGB(x, y));
                if (c.getRed() > 180 && c.getGreen() < 120) {
                    red++;
                }
            }
        }
        assertTrue(red > 30, red + " red pixels under the symbol");
    }

    @Test
    void writingDoesNotWipeTheNotes() {
        canvas.setNotes(Collections.singletonList(note("a", 100)));

        drawLine(0, 60, 50, 60);
        canvas.undo();

        assertEquals(1, canvas.getNotes().size());
    }

    @Test
    void theEraserWipesANoteItTouchesAndUndoBringsItBack() {
        listenToNotes();
        canvas.setNotes(Arrays.asList(note("a", 100), note("b", 300)));
        java.awt.geom.Rectangle2D first = canvas.getNotes().get(0).getBounds();
        int[] changes = {0};
        canvas.addChangeListener(e -> changes[0]++);
        canvas.setErasing(true);

        int x = (int) first.getCenterX();
        int y = (int) first.getCenterY();
        drawLine(x, y, x + 2, y + 2);

        assertEquals(1, canvas.getNotes().size());
        assertEquals("b", canvas.getNotes().get(0).getKey());
        assertEquals(Collections.singletonList("erased a"), noteEvents);
        assertEquals(0, changes[0], "wiping a note does not change the writing");

        canvas.undo();

        assertEquals(2, canvas.getNotes().size());
        assertEquals(Arrays.asList("erased a", "restored a"), noteEvents);
    }

    @Test
    void theEraserWipesWholeStrokesItTouches() {
        drawLine(10, 20, 110, 20);
        drawLine(10, 80, 110, 80);
        canvas.setErasing(true);

        // a short wipe across the middle of the first stroke only
        drawLine(55, 10, 65, 30);

        assertEquals(1, canvas.getStrokes().size());
        assertEquals(80, canvas.getStrokes().get(0).getMinY());
    }

    @Test
    void aQuickEraserSweepDoesNotJumpOverStrokes() {
        drawLine(100, 0, 100, 100);
        canvas.setErasing(true);

        mouse(MouseEvent.MOUSE_PRESSED, 0, 50);
        mouse(MouseEvent.MOUSE_DRAGGED, 200, 50);
        mouse(MouseEvent.MOUSE_RELEASED, 200, 50);

        assertTrue(canvas.getStrokes().isEmpty());
    }

    @Test
    void undoBringsBackEverythingOneEraserSweepWiped() {
        drawLine(10, 20, 110, 20);
        drawLine(10, 50, 110, 50);
        drawLine(10, 80, 110, 80);
        canvas.setErasing(true);
        mouse(MouseEvent.MOUSE_PRESSED, 60, 10);
        mouse(MouseEvent.MOUSE_DRAGGED, 60, 60);
        mouse(MouseEvent.MOUSE_RELEASED, 60, 60);
        assertEquals(1, canvas.getStrokes().size());

        canvas.undo();

        List<Stroke> back = canvas.getStrokes();
        assertEquals(3, back.size());
        assertEquals(20, back.get(0).getMinY());
        assertEquals(50, back.get(1).getMinY());
        assertEquals(80, back.get(2).getMinY());
    }

    @Test
    void undoTakesBackAClearWithItsNotes() {
        drawLine(10, 20, 110, 20);
        drawLine(10, 50, 110, 50);
        canvas.setNotes(Collections.singletonList(note("a", 200)));
        canvas.clear();
        assertTrue(canvas.getNotes().isEmpty());

        canvas.undo();

        assertEquals(2, canvas.getStrokes().size());
        assertEquals(1, canvas.getNotes().size());
    }

    @Test
    void anEraserSweepThatWipesNothingIsNotAnEdit() {
        drawLine(10, 20, 110, 20);
        int[] changes = {0};
        canvas.addChangeListener(e -> changes[0]++);
        canvas.setErasing(true);

        drawLine(200, 200, 210, 210);

        assertEquals(0, changes[0]);
        canvas.undo();
        assertTrue(canvas.getStrokes().isEmpty(), "undo still takes back the drawn stroke");
    }

    @Test
    void writingWhileZoomedInIsKeptInPageCoordinates() {
        canvas.setSize(400, 300);
        canvas.zoomAt(new java.awt.Point(0, 0), 2);

        drawLine(100, 100, 200, 100);

        Stroke stroke = canvas.getStrokes().get(0);
        assertEquals(50, stroke.getMinX(), 1e-9);
        assertEquals(100, stroke.getMaxX(), 1e-9);
        assertEquals(50, stroke.getMinY(), 1e-9);
    }

    @Test
    void zoomingKeepsThePageStillUnderThePointer() {
        canvas.setSize(400, 300);
        canvas.setNavigable(true);
        java.awt.Point pointer = new java.awt.Point(120, 80);
        java.awt.geom.Point2D before = canvas.toPage(pointer);

        wheel(pointer.x, pointer.y, -3, InputEvent.META_DOWN_MASK);

        assertTrue(canvas.getZoom() > 1, "zoomed in to " + canvas.getZoom());
        java.awt.geom.Point2D after = canvas.toPage(pointer);
        assertEquals(before.getX(), after.getX(), 1e-9);
        assertEquals(before.getY(), after.getY(), 1e-9);
    }

    @Test
    void zoomStaysWithinLimits() {
        canvas.setSize(400, 300);
        canvas.zoomBy(100);
        assertEquals(4, canvas.getZoom(), 1e-9);
        canvas.zoomBy(0.0001);
        assertEquals(0.25, canvas.getZoom(), 1e-9);
    }

    @Test
    void scrollingMovesThePageUpDownAndSideways() {
        canvas.setSize(400, 300);
        canvas.setNavigable(true);

        wheel(100, 100, 2, 0);
        assertEquals(32, canvas.toPage(new java.awt.Point(0, 0)).getY(), 1e-9);

        wheel(100, 100, 2, InputEvent.SHIFT_DOWN_MASK);
        assertEquals(32, canvas.toPage(new java.awt.Point(0, 0)).getX(), 1e-9);
    }

    @Test
    void aSheetThatIsNotNavigableIgnoresTheWheel() {
        canvas.setSize(400, 300);

        wheel(100, 100, 2, 0);

        assertEquals(0, canvas.toPage(new java.awt.Point(0, 0)).getY(), 1e-9);
    }

    @Test
    void resetViewGoesBackToTheStart() {
        canvas.setSize(400, 300);
        canvas.zoomBy(2);
        canvas.panBy(30, 40);

        canvas.resetView();

        assertEquals(1, canvas.getZoom(), 1e-9);
        assertEquals(0, canvas.toPage(new java.awt.Point(0, 0)).getX(), 1e-9);
    }

    private static handcalc.entity.PlacedSymbol picked(char symbol, double x, double y) {
        return TypesetSymbol.place(symbol, "",
                new java.awt.geom.Rectangle2D.Double(x, y, 40, 60));
    }

    @Test
    void aPickedSymbolTakesThePlaceOfTheWritingAndUndoPutsItBack() {
        drawLine(10, 20, 50, 80);
        drawLine(100, 20, 140, 80);
        Stroke replaced = canvas.getStrokes().get(1);
        int[] changes = {0};
        canvas.addChangeListener(e -> changes[0]++);

        canvas.place(picked('9', 100, 20), Collections.singletonList(replaced));

        assertEquals(1, canvas.getStrokes().size());
        assertEquals('9', canvas.getPlacedSymbols().get(0).getSymbol());
        assertEquals(1, changes[0], "it is a change to the writing");

        canvas.undo();

        assertEquals(2, canvas.getStrokes().size());
        assertTrue(canvas.getStrokes().get(1) == replaced);
        assertTrue(canvas.getPlacedSymbols().isEmpty());
    }

    @Test
    void pickingAgainReplacesTheSymbolPickedBefore() {
        canvas.place(picked('9', 100, 20), Collections.emptyList());
        handcalc.entity.PlacedSymbol first = canvas.getPlacedSymbols().get(0);

        canvas.place(picked('8', 100, 20), first.getOutline());

        assertEquals(1, canvas.getPlacedSymbols().size());
        assertEquals('8', canvas.getPlacedSymbols().get(0).getSymbol());
        canvas.undo();
        assertEquals('9', canvas.getPlacedSymbols().get(0).getSymbol());
    }

    @Test
    void theEraserWipesAPickedSymbolAndUndoBringsItBack() {
        canvas.place(picked('9', 100, 20), Collections.emptyList());
        canvas.setErasing(true);

        // a sweep across the middle of the 9, set in the box from (100, 20) to (140, 80)
        mouse(MouseEvent.MOUSE_PRESSED, 90, 50);
        mouse(MouseEvent.MOUSE_DRAGGED, 150, 50);
        mouse(MouseEvent.MOUSE_RELEASED, 150, 50);
        assertTrue(canvas.getPlacedSymbols().isEmpty());

        canvas.undo();
        assertEquals(1, canvas.getPlacedSymbols().size());
    }

    @Test
    void clearWipesPickedSymbolsAndUndoBringsThemBack() {
        canvas.place(picked('9', 100, 20), Collections.emptyList());

        canvas.clear();
        assertTrue(canvas.getPlacedSymbols().isEmpty());

        canvas.undo();
        assertEquals(1, canvas.getPlacedSymbols().size());
    }

    @Test
    void aPickedSymbolIsSetInInk() {
        canvas.place(picked('8', 100, 20), Collections.emptyList());

        BufferedImage image = paintCanvas();

        int ink = 0;
        for (int x = 100; x < 140; x++) {
            for (int y = 20; y < 80; y++) {
                Color c = new Color(image.getRGB(x, y));
                if (c.getRed() < 0x60 && c.getGreen() < 0x60 && c.getBlue() < 0x60) {
                    ink++;
                }
            }
        }
        assertTrue(ink > 80, ink + " dark pixels where the 8 is");
    }

    @Test
    void aPickedSymbolIsSetWhereTheWritingWasAndAboutAsBig() {
        handcalc.entity.Glyph nine = new handcalc.entity.Glyph(picked('9', 100, 20).getOutline());

        assertEquals(120, (nine.getMinX() + nine.getMaxX()) / 2, 1);
        assertEquals(50, (nine.getMinY() + nine.getMaxY()) / 2, 1);
        assertTrue(nine.getMaxY() - nine.getMinY() > 45 && nine.getMaxY() - nine.getMinY() <= 61,
                "height " + (nine.getMaxY() - nine.getMinY()));
    }
}
