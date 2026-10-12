package handcalc.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import handcalc.data_access.SyntheticSamples;
import handcalc.entity.Glyph;
import handcalc.entity.Point;
import handcalc.entity.Stroke;
import handcalc.interface_adapter.CalculatorState;
import handcalc.interface_adapter.LineView;
import handcalc.interface_adapter.WrittenAnswer;
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

    private static double right(List<Stroke> strokes) {
        return strokes.stream().mapToDouble(Stroke::getMaxX).max().getAsDouble();
    }

    private static List<Stroke> below(List<Stroke> strokes, double dy) {
        List<Stroke> moved = new ArrayList<>();
        for (Stroke stroke : strokes) {
            List<Point> points = new ArrayList<>();
            for (Point p : stroke.getPoints()) {
                points.add(new Point(p.getX(), p.getY() + dy));
            }
            moved.add(new Stroke(points));
        }
        return moved;
    }

    private static LineView onlyLine(CalculatorApp app) {
        List<LineView> lines = app.getViewModel().getState().getLines();
        assertEquals(1, lines.size());
        return lines.get(0);
    }

    @Test
    void theAnswerIsWrittenAfterTheEqualsSign() {
        List<Stroke> strokes = write("7-2=", 21);
        CalculatorApp app = new CalculatorApp(samples);

        app.getController().executeWhenFinished(strokes);

        WrittenAnswer answer = onlyLine(app).getAnswer();
        assertEquals("5", answer.getText());
        assertTrue(answer.getX() > right(strokes), "the answer comes after the '='");
        assertEquals("7−2", onlyLine(app).getFormula().toString());
    }

    @Test
    void writingThatDoesNotEndInEqualsIsReadButNotSolved() {
        CalculatorApp app = new CalculatorApp(samples);

        app.getController().executeWhenFinished(write("7-2", 21));

        LineView line = onlyLine(app);
        assertFalse(line.isFinished());
        assertNull(line.getAnswer());
        assertEquals(3, line.getSymbols().size());
    }

    @Test
    void theSymbolThatLeavesTheExpressionUnfinishedIsMarked() {
        CalculatorApp app = new CalculatorApp(samples);

        app.getController().executeWhenFinished(write("5+=", 3));

        LineView line = onlyLine(app);
        assertNull(line.getAnswer());
        assertFalse(line.getSymbols().get(0).isFaulty());
        assertTrue(line.getSymbols().get(1).isFaulty());
        assertFalse(line.getSymbols().get(2).isFaulty());
    }

    @Test
    void aCalculationWithNoValueIsWrittenAsUndefined() {
        CalculatorApp app = new CalculatorApp(samples);

        app.getController().executeWhenFinished(write("5÷0=", 9));

        LineView line = onlyLine(app);
        assertEquals("undefined", line.getAnswer().getText());
        assertTrue(line.getAnswer().isError());
        assertTrue(line.getSymbols().stream().noneMatch(s -> s.isFaulty()));
        assertEquals("5÷0", line.getFormula().toString());
    }

    @Test
    void aCorrectionOfWritingThatEndsInEqualsRewritesTheAnswer() {
        CalculatorApp app = new CalculatorApp(samples);

        app.getCorrectController().executeWhenFinished(write("2+2=", 51), 1, '×');

        assertEquals("4", onlyLine(app).getAnswer().getText());
        assertEquals("2×2", onlyLine(app).getFormula().toString());
    }

    @Test
    void eachLineOfThePageGetsItsOwnAnswer() {
        List<Stroke> page = new ArrayList<>(write("7-2=", 21));
        page.addAll(below(write("3×4=", 22), 300));
        CalculatorApp app = new CalculatorApp(samples);

        app.getController().executeWhenFinished(page);

        List<LineView> lines = app.getViewModel().getState().getLines();
        assertEquals(2, lines.size());
        assertEquals("5", lines.get(0).getAnswer().getText());
        assertEquals("12", lines.get(1).getAnswer().getText());
    }

    @Test
    void aCorrectionOnTheSecondLineCountsSymbolsFromTheTop() {
        List<Stroke> page = new ArrayList<>(write("7-2=", 21));
        page.addAll(below(write("2+2=", 51), 300));
        CalculatorApp app = new CalculatorApp(samples);

        // the "+" of the second line comes after the four symbols of the first
        app.getCorrectController().executeWhenFinished(page, 5, '×');

        List<LineView> lines = app.getViewModel().getState().getLines();
        assertEquals("5", lines.get(0).getAnswer().getText());
        assertEquals("4", lines.get(1).getAnswer().getText());
        assertEquals("2×2", lines.get(1).getFormula().toString());
    }

    @Test
    void readingAPageAgainDoesNotRepeatItsHistory() {
        List<Stroke> page = new ArrayList<>(write("7-2=", 21));
        page.addAll(below(write("3×4=", 22), 300));
        CalculatorApp app = new CalculatorApp(samples);

        app.getController().executeWhenFinished(page);
        app.getController().executeWhenFinished(page);
        app.getHistoryController().refresh();

        assertEquals(2, app.getHistoryViewModel().getState().getRows().size());
    }
}
