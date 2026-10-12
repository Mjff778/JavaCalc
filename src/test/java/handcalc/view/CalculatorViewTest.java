package handcalc.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import handcalc.entity.Stroke;
import handcalc.interface_adapter.CalculatorState;
import handcalc.interface_adapter.CalculatorViewModel;
import handcalc.interface_adapter.ReadSymbol;
import handcalc.interface_adapter.RecognizeAndSolveController;
import handcalc.use_case.recognize_and_solve.RecognizeAndSolveInputData;
import handcalc.interface_adapter.LineView;
import handcalc.interface_adapter.WrittenAnswer;
import handcalc.entity.Formula;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JToggleButton;
import org.junit.jupiter.api.Test;

class CalculatorViewTest {

    private final CalculatorViewModel viewModel = new CalculatorViewModel();
    private final List<RecognizeAndSolveInputData> requests = new ArrayList<>();
    private int collectorOpened;
    private final CalculatorView view = new CalculatorView(
            new RecognizeAndSolveController(requests::add), viewModel, () -> collectorOpened++);

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

    private CanvasPanel canvas() {
        return find(view, "canvas", CanvasPanel.class);
    }

    private void drawDot(int x, int y) {
        CanvasPanel canvas = canvas();
        canvas.dispatchEvent(new MouseEvent(canvas, MouseEvent.MOUSE_PRESSED,
                System.currentTimeMillis(), 0, x, y, 1, false, MouseEvent.BUTTON1));
        canvas.dispatchEvent(new MouseEvent(canvas, MouseEvent.MOUSE_RELEASED,
                System.currentTimeMillis(), 0, x, y, 1, false, MouseEvent.BUTTON1));
    }

    /** "2" then "÷", written in boxes 40px tall; the "÷" is at fault. */
    private static List<ReadSymbol> readTwoDivided() {
        return Arrays.asList(
                new ReadSymbol('2', Arrays.asList(
                        new ReadSymbol.Alternative('2', 80), new ReadSymbol.Alternative('3', 15)),
                        100, 100, 130, 140, false, Collections.singletonList(
                                new Stroke(Arrays.asList(new handcalc.entity.Point(100, 100),
                                        new handcalc.entity.Point(130, 140))))),
                new ReadSymbol('÷', Arrays.asList(
                        new ReadSymbol.Alternative('÷', 60), new ReadSymbol.Alternative('+', 30)),
                        150, 100, 180, 140, true, Collections.singletonList(
                                new Stroke(Arrays.asList(new handcalc.entity.Point(150, 120),
                                        new handcalc.entity.Point(180, 120))))));
    }

    /** The symbols the user picked that are now on the page. */
    private String picked() {
        StringBuilder symbols = new StringBuilder();
        for (handcalc.entity.PlacedSymbol symbol : canvas().getPlacedSymbols()) {
            symbols.append(symbol.getSymbol());
        }
        return symbols.toString();
    }

    @Test
    void thereIsNoCalculateButtonOrAnswerPanel() {
        assertNull(find(view, "calculateButton", JButton.class));
        assertNull(find(view, "resultLabel", javax.swing.JLabel.class));
    }

    @Test
    void afterAPauseTheWritingIsSentToBeSolvedOnceItEndsInEquals() {
        drawDot(30, 40);

        view.readNow();

        assertEquals(1, requests.size());
        assertTrue(requests.get(0).isWaitForEquals());
        assertEquals(1, requests.get(0).getStrokes().size());
        assertEquals(30, requests.get(0).getStrokes().get(0).getMinX());
    }

    @Test
    void theWritingIsReadOnlyWhenThePenRests() {
        drawDot(30, 40);
        drawDot(60, 40);

        assertTrue(requests.isEmpty(), "nothing is read while the user is still writing");
    }

    /** The text of the notes written on the page, top to bottom. */
    private List<String> notes() {
        List<String> texts = new ArrayList<>();
        for (CanvasPanel.Note note : canvas().getNotes()) {
            texts.add(note.getText());
        }
        return texts;
    }

    /** A line of one stroke at the given height, on the canvas, answered with the text. */
    private static LineView answeredLine(Stroke stroke, String answer, boolean error) {
        return new LineView(Collections.emptyList(),
                new WrittenAnswer(answer, 300, stroke.getMinY() + 20, 40, error),
                Formula.row(Arrays.asList(Formula.text("1"), Formula.text("+"),
                        Formula.text("1"))),
                Collections.singletonList(stroke), true);
    }

    private static Stroke strokeAt(double y) {
        return new Stroke(Arrays.asList(
                new handcalc.entity.Point(100, y), new handcalc.entity.Point(140, y + 40)));
    }

    @Test
    void eachLinesAnswerIsWrittenOnThePage() {
        Stroke top = strokeAt(100);
        Stroke bottom = strokeAt(300);
        view.loadStrokes(Arrays.asList(top, bottom));

        viewModel.setState(CalculatorState.page(Arrays.asList(
                answeredLine(top, "2", false), answeredLine(bottom, "undefined", true))));

        assertEquals(Arrays.asList("2", "undefined"), notes());
        assertEquals(Theme.ERROR, canvas().getNotes().get(1).getColor());
    }

    @Test
    void writingAnotherLineLeavesTheAnswersAlone() {
        Stroke top = strokeAt(100);
        view.loadStrokes(Collections.singletonList(top));
        viewModel.setState(CalculatorState.page(
                Collections.singletonList(answeredLine(top, "2", false))));

        drawDot(100, 400);

        assertEquals(Collections.singletonList("2"), notes());
    }

    @Test
    void writingOnALineTakesItsAnswerAwayAtOnce() {
        Stroke top = strokeAt(100);
        view.loadStrokes(Collections.singletonList(top));
        viewModel.setState(CalculatorState.page(
                Collections.singletonList(answeredLine(top, "2", false))));

        drawDot(160, 120);

        assertTrue(notes().isEmpty());
    }

    @Test
    void aWipedAnswerStaysWipedWhileItsLineIsUnchanged() {
        Stroke top = strokeAt(100);
        view.loadStrokes(Collections.singletonList(top));
        CalculatorState page = CalculatorState.page(
                Collections.singletonList(answeredLine(top, "2", false)));
        viewModel.setState(page);
        canvas().setSize(800, 600);
        java.awt.geom.Rectangle2D answer = canvas().getNotes().get(0).getBounds();

        find(view, "eraserButton", JToggleButton.class).doClick();
        int x = (int) answer.getCenterX();
        int y = (int) answer.getCenterY();
        canvas().dispatchEvent(new MouseEvent(canvas(), MouseEvent.MOUSE_PRESSED,
                System.currentTimeMillis(), 0, x, y, 1, false, MouseEvent.BUTTON1));
        canvas().dispatchEvent(new MouseEvent(canvas(), MouseEvent.MOUSE_RELEASED,
                System.currentTimeMillis(), 0, x, y, 1, false, MouseEvent.BUTTON1));
        assertTrue(notes().isEmpty());

        viewModel.setState(page);
        assertTrue(notes().isEmpty(), "the same line read again stays without its answer");

        Stroke changed = strokeAt(100);
        viewModel.setState(CalculatorState.page(
                Collections.singletonList(answeredLine(changed, "3", false))));
        assertEquals(Collections.singletonList("3"), notes());
    }

    @Test
    void undoBringsAWipedAnswerBack() {
        Stroke top = strokeAt(100);
        view.loadStrokes(Collections.singletonList(top));
        CalculatorState page = CalculatorState.page(
                Collections.singletonList(answeredLine(top, "2", false)));
        viewModel.setState(page);
        canvas().setSize(800, 600);
        java.awt.geom.Rectangle2D answer = canvas().getNotes().get(0).getBounds();
        find(view, "eraserButton", JToggleButton.class).doClick();
        int x = (int) answer.getCenterX();
        int y = (int) answer.getCenterY();
        canvas().dispatchEvent(new MouseEvent(canvas(), MouseEvent.MOUSE_PRESSED,
                System.currentTimeMillis(), 0, x, y, 1, false, MouseEvent.BUTTON1));
        canvas().dispatchEvent(new MouseEvent(canvas(), MouseEvent.MOUSE_RELEASED,
                System.currentTimeMillis(), 0, x, y, 1, false, MouseEvent.BUTTON1));

        find(view, "undoButton", JButton.class).doClick();
        viewModel.setState(page);

        assertEquals(Collections.singletonList("2"), notes());
    }

    @Test
    void restingOnAnAnswerShowsTheCalculationTypeset() {
        Stroke top = strokeAt(100);
        view.loadStrokes(Collections.singletonList(top));
        viewModel.setState(CalculatorState.page(
                Collections.singletonList(answeredLine(top, "2", false))));
        canvas().setSize(800, 600);
        FormulaCard card = find(view, "formulaCard", FormulaCard.class);

        card.showFor(canvas().getNotes().get(0).getKey());

        assertTrue(card.isVisible());
        assertTrue(card.getWidth() > 0 && card.getHeight() > 0);
        assertFalse(card.contains(card.getWidth() / 2, card.getHeight() / 2),
                "the pen writes through the card");
    }

    @Test
    void symbolsAtFaultAreUnderlinedAndNothingIsWritten() {
        viewModel.setState(CalculatorState.solved("1+1", "2", Collections.emptyList(),
                new WrittenAnswer("2", 200, 120, 40, false)));

        viewModel.setState(CalculatorState.failed("2÷", "Expression ended unexpectedly",
                readTwoDivided()));

        assertTrue(notes().isEmpty());
        assertEquals(1, canvas().getUnderlined().size());
        assertEquals(150, canvas().getUnderlined().get(0).getMinX());
    }

    @Test
    void clearEmptiesTheCanvas() {
        drawDot(30, 40);

        find(view, "clearButton", JButton.class).doClick();

        assertTrue(canvas().getStrokes().isEmpty());
    }

    @Test
    void undoRemovesTheLastStroke() {
        drawDot(30, 40);
        drawDot(90, 40);

        find(view, "undoButton", JButton.class).doClick();

        List<Stroke> left = canvas().getStrokes();
        assertEquals(1, left.size());
        assertEquals(30, left.get(0).getMinX());
    }

    @Test
    void theEraserButtonSwitchesTheCanvasToErasing() {
        JToggleButton eraser = find(view, "eraserButton", JToggleButton.class);

        eraser.doClick();
        assertTrue(canvas().isErasing());

        eraser.doClick();
        assertFalse(canvas().isErasing());
    }

    @Test
    void choosingAnotherReadingPutsThatSymbolInPlaceAndReadsThePageAgain() {
        viewModel.setState(CalculatorState.failed("2÷", "Expression ended unexpectedly",
                readTwoDivided()));
        requests.clear();

        view.showChoicesFor(1);
        JButton plus = find(view, "choice_+", JButton.class);
        assertTrue(plus != null, "'+' is offered");
        assertNull(find(view, "choice_÷", JButton.class), "the current reading is not a choice");
        plus.doClick();

        assertEquals("+", picked());
        assertEquals(1, requests.size());
        assertEquals(1, requests.get(0).getPlaced().size());
        assertFalse(find(view, "symbolChoices", SymbolChoices.class).isVisible());
    }

    @Test
    void noChoicesAreOfferedBeforeTheWritingEndsInEquals() {
        viewModel.setState(CalculatorState.read(readTwoDivided()));
        canvas().setSize(800, 400);

        view.showChoicesFor(0);

        assertFalse(find(view, "symbolChoices", SymbolChoices.class).isVisible());
    }

    @Test
    void theCurrentReadingIsShownFirstThenTheOtherChoices() {
        viewModel.setState(CalculatorState.failed("2÷", "x", readTwoDivided()));
        canvas().setSize(800, 400);

        view.showChoicesFor(1);

        SymbolChoices choices = find(view, "symbolChoices", SymbolChoices.class);
        assertEquals("current_÷", choices.getComponent(0).getName());
        assertEquals("choice_+", choices.getComponent(1).getName());
    }

    private static void click(java.awt.Component target) {
        for (int id : new int[] {MouseEvent.MOUSE_PRESSED, MouseEvent.MOUSE_RELEASED}) {
            target.dispatchEvent(new MouseEvent(target, id, System.currentTimeMillis(),
                    id == MouseEvent.MOUSE_PRESSED ? MouseEvent.BUTTON1_DOWN_MASK : 0,
                    5, 5, 1, false, MouseEvent.BUTTON1));
        }
    }

    /** Lets the event thread run what the timers queued. */
    private static void settle() throws Exception {
        javax.swing.SwingUtilities.invokeAndWait(() -> { });
    }

    @Test
    void aPressTheMomentTheChoicesAppearIsNotAChoice() {
        viewModel.setState(CalculatorState.failed("2÷", "x", readTwoDivided()));
        canvas().setSize(800, 400);
        view.showChoicesFor(1);
        find(view, "symbolChoices", SymbolChoices.class).doLayout();

        click(find(view, "choice_+", JButton.class));

        assertEquals("", picked(), "a pen coming down on a choice is not choosing it");
    }

    @Test
    void aClickOnAChoiceThatHasBeenShownAMomentIsAChoice() throws Exception {
        viewModel.setState(CalculatorState.failed("2÷", "x", readTwoDivided()));
        canvas().setSize(800, 400);
        view.showChoicesFor(1);
        find(view, "symbolChoices", SymbolChoices.class).doLayout();
        Thread.sleep(SymbolChoices.PRESS_GRACE_MS + 100);

        click(find(view, "choice_+", JButton.class));

        assertEquals("+", picked());
    }

    @Test
    void choicesWaitUntilTheUserHasStoppedWriting() throws Exception {
        drawDot(400, 300);
        viewModel.setState(CalculatorState.failed("2÷", "x", readTwoDivided()));
        canvas().setSize(800, 400);
        SymbolChoices choices = find(view, "symbolChoices", SymbolChoices.class);

        canvas().dispatchEvent(new MouseEvent(canvas(), MouseEvent.MOUSE_MOVED,
                System.currentTimeMillis(), 0, 115, 120, 0, false, MouseEvent.NOBUTTON));
        Thread.sleep(600);
        settle();
        assertFalse(choices.isVisible(), "the user has only just written");

        Thread.sleep(SymbolChoices.QUIET_MS);
        settle();
        assertTrue(choices.isVisible(), "once the writing has rested, the choices come");
    }

    @Test
    void aPickedSymbolTakesThePlaceOfTheWritingAndIsReadAtOnce() {
        Stroke written = new Stroke(Arrays.asList(
                new handcalc.entity.Point(100, 100), new handcalc.entity.Point(130, 140)));
        view.loadStrokes(Collections.singletonList(written));
        ReadSymbol four = new ReadSymbol('4', Arrays.asList(
                new ReadSymbol.Alternative('4', 60), new ReadSymbol.Alternative('9', 30)),
                100, 100, 130, 140, false, Collections.singletonList(written));
        viewModel.setState(CalculatorState.page(Collections.singletonList(new LineView(
                Collections.singletonList(four), null, null,
                Collections.singletonList(written), true))));
        canvas().setSize(800, 400);
        requests.clear();
        view.showChoicesFor(0);

        find(view, "choice_9", JButton.class).doClick();

        assertTrue(canvas().getStrokes().isEmpty(), "the handwritten 4 is gone");
        assertEquals('9', canvas().getPlacedSymbols().get(0).getSymbol());
        assertEquals("4", canvas().getPlacedSymbols().get(0).getAlternatives(),
                "the 4 it was read as can be chosen again");
        assertEquals(1, requests.size(), "the page is read again at once");
        assertEquals(1, requests.get(0).getPlaced().size());
    }

    @Test
    void choosingTheCurrentReadingChangesNothing() {
        viewModel.setState(CalculatorState.failed("2÷", "x", readTwoDivided()));
        canvas().setSize(800, 400);
        view.showChoicesFor(1);

        find(view, "current_÷", JButton.class).doClick();

        assertEquals("", picked());
        assertFalse(find(view, "symbolChoices", SymbolChoices.class).isVisible());
    }

    @Test
    void thePenWritesThroughTheGapsBetweenTheChoices() {
        viewModel.setState(CalculatorState.failed("2÷", "x", readTwoDivided()));
        canvas().setSize(800, 400);
        view.showChoicesFor(1);
        SymbolChoices choices = find(view, "symbolChoices", SymbolChoices.class);
        choices.doLayout();
        Component chip = choices.getComponent(0);

        // the panel's margin, outside every choice, belongs to the paper
        Component atMargin = javax.swing.SwingUtilities.getDeepestComponentAt(
                canvas(), choices.getX() + 2, choices.getY() + 2);
        Component atChip = javax.swing.SwingUtilities.getDeepestComponentAt(canvas(),
                choices.getX() + chip.getX() + chip.getWidth() / 2,
                choices.getY() + chip.getY() + chip.getHeight() / 2);

        assertEquals(canvas(), atMargin);
        assertEquals(chip, atChip);
    }

    @Test
    void choicesFloatAboveTheirSymbol() {
        viewModel.setState(CalculatorState.failed("2÷", "x", readTwoDivided()));
        canvas().setSize(800, 400);

        view.showChoicesFor(0);

        SymbolChoices choices = find(view, "symbolChoices", SymbolChoices.class);
        assertTrue(choices.isVisible());
        assertTrue(choices.getY() < 100, "above the symbol's top at y=100");
        assertTrue(choices.getX() < 130 && choices.getX() + choices.getWidth() > 100);
    }

    @Test
    void editingTheCanvasWithdrawsTheChoices() {
        viewModel.setState(CalculatorState.failed("2÷", "x", readTwoDivided()));
        canvas().setSize(800, 400);
        view.showChoicesFor(0);

        drawDot(30, 40);

        assertFalse(find(view, "symbolChoices", SymbolChoices.class).isVisible());
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

        List<Stroke> now = canvas().getStrokes();
        assertEquals(1, now.size());
        assertEquals(100, now.get(0).getMinX());
    }
}
