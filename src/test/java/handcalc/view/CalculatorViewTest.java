package handcalc.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import handcalc.entity.Stroke;
import handcalc.interface_adapter.CalculatorState;
import handcalc.interface_adapter.CalculatorViewModel;
import handcalc.interface_adapter.CorrectSymbolController;
import handcalc.interface_adapter.ReadSymbol;
import handcalc.interface_adapter.RecognizeAndSolveController;
import handcalc.use_case.correct_symbol.CorrectSymbolInputData;
import handcalc.use_case.recognize_and_solve.RecognizeAndSolveInputData;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import org.junit.jupiter.api.Test;

class CalculatorViewTest {

    private final CalculatorViewModel viewModel = new CalculatorViewModel();
    private final List<RecognizeAndSolveInputData> requests = new ArrayList<>();
    private final List<CorrectSymbolInputData> corrections = new ArrayList<>();
    private int collectorOpened;
    private final CalculatorView view = new CalculatorView(
            new RecognizeAndSolveController(requests::add),
            new CorrectSymbolController(corrections::add), viewModel,
            () -> collectorOpened++);

    @SuppressWarnings("unchecked")
    private <T extends Component> T find(Container root, String name, Class<T> type) {
        for (Component c : root.getComponents()) {
            if (name.equals(c.getName()) && type.isInstance(c)) {
                return (T) c;
            }
            if (c instanceof Container) {
                T found = find((Container) c, name, type);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private void drawDot(int x, int y) {
        CanvasPanel canvas = find(view, "canvas", CanvasPanel.class);
        canvas.dispatchEvent(new MouseEvent(canvas, MouseEvent.MOUSE_PRESSED,
                System.currentTimeMillis(), 0, x, y, 1, false, MouseEvent.BUTTON1));
        canvas.dispatchEvent(new MouseEvent(canvas, MouseEvent.MOUSE_RELEASED,
                System.currentTimeMillis(), 0, x, y, 1, false, MouseEvent.BUTTON1));
    }

    @Test
    void calculateSendsTheCanvasStrokesToTheController() {
        drawDot(30, 40);

        find(view, "calculateButton", JButton.class).doClick();

        assertEquals(1, requests.size());
        assertEquals(1, requests.get(0).getStrokes().size());
        assertEquals(30, requests.get(0).getStrokes().get(0).getMinX());
    }

    @Test
    void successfulResultIsShownAsEquation() {
        viewModel.setState(CalculatorState.solved("12+3", "15", Collections.emptyList()));

        assertEquals("12+3 = 15", find(view, "resultLabel", JLabel.class).getText());
    }

    @Test
    void errorIsShownTogetherWithWhatWasRead() {
        viewModel.setState(CalculatorState.failed("5÷0", "Division by zero", Collections.emptyList()));

        String text = find(view, "resultLabel", JLabel.class).getText();
        assertTrue(text.contains("5÷0"), text);
        assertTrue(text.contains("Division by zero"), text);
    }

    @Test
    void clearEmptiesTheCanvasAndTheResult() {
        drawDot(30, 40);
        viewModel.setState(CalculatorState.solved("1+1", "2", Collections.emptyList()));

        find(view, "clearButton", JButton.class).doClick();

        assertTrue(find(view, "canvas", CanvasPanel.class).getStrokes().isEmpty());
        assertEquals(" ", find(view, "resultLabel", JLabel.class).getText());
    }

    @Test
    void undoRemovesTheLastStroke() {
        drawDot(30, 40);
        drawDot(90, 40);

        find(view, "undoButton", JButton.class).doClick();

        List<Stroke> left = find(view, "canvas", CanvasPanel.class).getStrokes();
        assertEquals(1, left.size());
        assertEquals(30, left.get(0).getMinX());
    }

    private static List<ReadSymbol> readTwoPlusTwo() {
        return Arrays.asList(
                new ReadSymbol('2', Arrays.asList(
                        new ReadSymbol.Alternative('2', 80), new ReadSymbol.Alternative('3', 15))),
                new ReadSymbol('÷', Arrays.asList(
                        new ReadSymbol.Alternative('÷', 60), new ReadSymbol.Alternative('+', 30))));
    }

    @Test
    void readSymbolsAreListedAsButtons() {
        viewModel.setState(CalculatorState.failed("2÷", "Expression ended unexpectedly",
                readTwoPlusTwo()));

        assertEquals("2", find(view, "symbolButton0", JButton.class).getText());
        assertEquals("÷", find(view, "symbolButton1", JButton.class).getText());
    }

    @Test
    void choosingAnAlternativeSendsACorrectionForThatSymbol() {
        drawDot(30, 40);
        viewModel.setState(CalculatorState.failed("2÷", "Expression ended unexpectedly",
                readTwoPlusTwo()));

        JPopupMenu menu = find(view, "symbolButton1", JButton.class).getComponentPopupMenu();
        JMenuItem plus = null;
        for (Component item : menu.getComponents()) {
            if ("alternative_+".equals(item.getName())) {
                plus = (JMenuItem) item;
            }
        }
        assertTrue(plus != null && plus.getText().contains("30%"), "menu offers '+' with 30%");
        plus.doClick();

        assertEquals(1, corrections.size());
        assertEquals(1, corrections.get(0).getGlyphIndex());
        assertEquals('+', corrections.get(0).getSymbol());
        assertEquals(1, corrections.get(0).getStrokes().size());
    }

    @Test
    void editingTheCanvasHidesTheOutdatedSymbolButtons() {
        viewModel.setState(CalculatorState.failed("2÷", "x", readTwoPlusTwo()));

        drawDot(30, 40);

        assertTrue(find(view, "symbolButton0", JButton.class) == null);
    }

    @Test
    void collectSamplesButtonOpensTheCollector() {
        find(view, "collectButton", JButton.class).doClick();

        assertEquals(1, collectorOpened);
    }

    @Test
    void loadStrokesPutsEarlierWritingBackOnTheCanvas() {
        drawDot(30, 40);
        Stroke earlier = new Stroke(Arrays.asList(
                new handcalc.entity.Point(100, 10), new handcalc.entity.Point(120, 10)));

        view.loadStrokes(Collections.singletonList(earlier));

        List<Stroke> now = find(view, "canvas", CanvasPanel.class).getStrokes();
        assertEquals(1, now.size());
        assertEquals(100, now.get(0).getMinX());
    }
}
