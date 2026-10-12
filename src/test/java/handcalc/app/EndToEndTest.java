package handcalc.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import handcalc.data_access.SyntheticSamples;
import handcalc.entity.Glyph;
import handcalc.entity.Point;
import handcalc.entity.Stroke;
import handcalc.interface_adapter.CalculatorState;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EndToEndTest {

    @TempDir
    Path dir;

    private Path samples;

    @BeforeEach
    void samplesFile() {
        samples = dir.resolve("samples.txt");
    }

    /** Writes the text as hand-like symbols, left to right with a clear gap between them. */
    private static List<Stroke> write(String text, long seed) {
        Random random = new Random(seed);
        List<Stroke> strokes = new ArrayList<>();
        double cursor = 0;
        for (int i = 0; i < text.length(); i++) {
            Glyph glyph = SyntheticSamples.randomGlyph(text.charAt(i), random);
            double dx = cursor - glyph.getMinX();
            for (Stroke s : glyph.getStrokes()) {
                List<Point> moved = new ArrayList<>();
                for (Point p : s.getPoints()) {
                    moved.add(new Point(p.getX() + dx, p.getY()));
                }
                strokes.add(new Stroke(moved));
            }
            cursor += glyph.getMaxX() - glyph.getMinX() + 45;
        }
        return strokes;
    }

    @Test
    void handwrittenSumIsReadAndSolved() {
        CalculatorApp app = new CalculatorApp(samples);

        app.getController().execute(write("12+3×4", 7));

        CalculatorState state = app.getViewModel().getState();
        assertEquals("12+3×4", state.getExpression());
        assertEquals("24", state.getResult());
        assertNull(state.getError());
    }

    @Test
    void handwrittenDivisionWithDecimalAnswer() {
        CalculatorApp app = new CalculatorApp(samples);

        app.getController().execute(write("9÷2", 11));

        assertEquals("9÷2", app.getViewModel().getState().getExpression());
        assertEquals("4.5", app.getViewModel().getState().getResult());
    }

    @Test
    void unfinishedExpressionReportsAnError() {
        CalculatorApp app = new CalculatorApp(samples);

        app.getController().execute(write("5+", 3));

        assertEquals("5+", app.getViewModel().getState().getExpression());
        assertNull(app.getViewModel().getState().getResult());
        assertEquals("Expression ended unexpectedly", app.getViewModel().getState().getError());
    }

    @Test
    void correctionFixesTheAnswerAndIsRememberedAfterARestart() throws Exception {
        List<Stroke> strokes = write("1+1", 5);
        CalculatorApp app = new CalculatorApp(samples);

        app.getCorrectController().execute(strokes, 1, '×');

        assertEquals("1×1", app.getViewModel().getState().getExpression());
        assertEquals("1", app.getViewModel().getState().getResult());
        assertEquals(1, Files.readAllLines(samples).size());

        CalculatorApp restarted = new CalculatorApp(samples);
        restarted.getController().execute(strokes);
        assertEquals("1×1", restarted.getViewModel().getState().getExpression());
    }

    @Test
    void trailingEqualsSignIsReadAndIgnored() {
        CalculatorApp app = new CalculatorApp(samples);

        app.getController().execute(write("7-2=", 21));

        assertEquals("7-2", app.getViewModel().getState().getExpression());
        assertEquals("5", app.getViewModel().getState().getResult());
    }

    @Test
    void calculationsAreKeptInTheHistoryWithTheirHandwriting() {
        CalculatorApp app = new CalculatorApp(samples);

        app.getController().execute(write("6÷3", 31));
        app.getController().execute(write("9-4", 32));

        app.getHistoryController().refresh();
        handcalc.interface_adapter.HistoryState history = app.getHistoryViewModel().getState();
        assertEquals(2, history.getRows().size());
        assertEquals("9-4 = 5", history.getRows().get(0).getText());
        assertEquals("6÷3 = 2", history.getRows().get(1).getText());
        assertTrue(history.getRows().get(0).getStrokes().size() >= 3);

        CalculatorApp restarted = new CalculatorApp(samples);
        restarted.getHistoryController().refresh();
        assertEquals(2, restarted.getHistoryViewModel().getState().getRows().size());
        assertTrue(Files.exists(dir.resolve("history.jsonl")));
    }

    @Test
    void emptyHistoryHasAMessage() {
        CalculatorApp app = new CalculatorApp(samples);

        app.getHistoryController().refresh();

        assertEquals("No calculations yet", app.getHistoryViewModel().getState().getMessage());
    }

    @Test
    void correctionsAreAlsoKeptInTheHistory() {
        CalculatorApp app = new CalculatorApp(samples);

        app.getCorrectController().execute(write("2+2", 51), 1, '×');
        app.getHistoryController().refresh();

        assertEquals("2×2 = 4",
                app.getHistoryViewModel().getState().getRows().get(0).getText());
    }
}
