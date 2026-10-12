package handcalc.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import handcalc.entity.Stroke;
import handcalc.interface_adapter.CollectSampleController;
import handcalc.interface_adapter.CollectorState;
import handcalc.interface_adapter.CollectorViewModel;
import handcalc.interface_adapter.TrainModelController;
import handcalc.use_case.collect_sample.CollectSampleInputBoundary;
import handcalc.use_case.collect_sample.CollectSampleInputData;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JToggleButton;
import handcalc.interface_adapter.PracticeExpressionController;
import handcalc.interface_adapter.PracticeExpressionState;
import handcalc.interface_adapter.PracticeExpressionViewModel;
import handcalc.use_case.practice_expression.PracticeExpressionInputBoundary;
import org.junit.jupiter.api.Test;

class CollectorViewTest {

    private final CollectorViewModel viewModel = new CollectorViewModel();
    private final List<CollectSampleInputData> saves = new ArrayList<>();
    private int refreshes;
    private int trains;
    private final CollectorView view = new CollectorView(
            new CollectSampleController(new CollectSampleInputBoundary() {
                @Override
                public void execute(CollectSampleInputData inputData) {
                    saves.add(inputData);
                }

                @Override
                public void refresh() {
                    refreshes++;
                }
            }),
            new TrainModelController(() -> trains++),
            viewModel, Runnable::run, Runnable::run);

    @SuppressWarnings("unchecked")
    private <T extends Component> T find(String name, Class<T> type) {
        return (T) findIn(view, name, type);
    }

    private Component findIn(Container root, String name, Class<?> type) {
        for (Component c : root.getComponents()) {
            if (name.equals(c.getName()) && type.isInstance(c)) {
                return c;
            }
            if (c instanceof Container) {
                Component found = findIn((Container) c, name, type);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static Map<Character, Integer> counts(int one, int two, int three) {
        Map<Character, Integer> counts = new LinkedHashMap<>();
        counts.put('1', one);
        counts.put('2', two);
        counts.put('3', three);
        return counts;
    }

    private void showProgress(int one, int two, int three, char next, boolean saved) {
        viewModel.setState(viewModel.getState().withProgress(
                counts(one, two, three), next, 20, saved ? "Saved a sample of '1'" : "", saved,
                false));
    }

    private void write(int x1, int x2) {
        CanvasPanel canvas = find("canvas", CanvasPanel.class);
        for (int[] e : new int[][] {{MouseEvent.MOUSE_PRESSED, x1}, {MouseEvent.MOUSE_DRAGGED, x2},
                {MouseEvent.MOUSE_RELEASED, x2}}) {
            canvas.dispatchEvent(new MouseEvent(canvas, e[0], System.currentTimeMillis(), 0,
                    e[1], 40, 1, false, MouseEvent.BUTTON1));
        }
    }

    @Test
    void asksTheUseCaseForTheCurrentProgressWhenOpened() {
        assertEquals(1, refreshes);
    }

    @Test
    void suggestedSymbolIsPromptedAndProgressShown() {
        showProgress(5, 1, 7, '2', false);

        assertEquals("2", find("promptLabel", JLabel.class).getText());
        assertEquals("1 sample so far", find("progressLabel", JLabel.class).getText());
        assertEquals("5", find("count_1", JLabel.class).getText());
        assertEquals('2', find("symbolChoice", JComboBox.class).getSelectedItem());
    }

    @Test
    void saveSendsTheWritingWithTheChosenSymbol() {
        showProgress(0, 0, 0, '1', false);
        write(30, 30);
        find("symbolChoice", JComboBox.class).setSelectedItem('3');

        find("saveButton", JButton.class).doClick();

        assertEquals(1, saves.size());
        assertEquals('3', saves.get(0).getSymbol());
        assertEquals(1, saves.get(0).getStrokes().size());
    }

    @Test
    void canvasIsClearedAfterASuccessfulSaveOnly() {
        showProgress(0, 0, 0, '1', false);
        write(30, 90);

        viewModel.setState(viewModel.getState().withProgress(
                counts(0, 0, 0), '1', 20, "Nothing written yet", false, true));
        assertEquals(1, find("canvas", CanvasPanel.class).getStrokes().size());
        assertEquals("Nothing written yet", find("messageLabel", JLabel.class).getText());

        showProgress(1, 0, 0, '2', true);
        assertTrue(find("canvas", CanvasPanel.class).getStrokes().isEmpty());
    }

    @Test
    void trainButtonStartsTrainingAndShowsProgressText() {
        find("trainButton", JButton.class).doClick();

        assertEquals(1, trains);
        assertEquals("Training…", find("trainingLabel", JLabel.class).getText());
    }

    @Test
    void trainingResultReplacesTheTrainingText() {
        viewModel.setState(viewModel.getState().withTrainingStatus("Trained on 15 samples."));

        assertEquals("Trained on 15 samples.", find("trainingLabel", JLabel.class).getText());
        assertTrue(find("trainButton", JButton.class).isEnabled());
    }

    @Test
    void clearEmptiesTheCanvas() {
        write(10, 80);

        find("clearButton", JButton.class).doClick();

        assertTrue(find("canvas", CanvasPanel.class).getStrokes().isEmpty());
        assertNotNull(find("saveButton", JButton.class));
        assertFalse(find("canvas", CanvasPanel.class).getStrokes().iterator().hasNext());
    }

    // ---- expression practice mode ----

    private final PracticeExpressionViewModel practiceModel = new PracticeExpressionViewModel();
    private final List<List<Stroke>> submitted = new ArrayList<>();
    private int nexts;
    private int practiceRefreshes;
    private final CollectorView practiceView = new CollectorView(
            new CollectSampleController(new CollectSampleInputBoundary() {
                @Override
                public void execute(CollectSampleInputData inputData) {
                    saves.add(inputData);
                }

                @Override
                public void refresh() {
                    practiceRefreshes++;
                }
            }),
            new PracticeExpressionController(new PracticeExpressionInputBoundary() {
                @Override
                public void next() {
                    nexts++;
                }

                @Override
                public void submit(List<Stroke> strokes) {
                    submitted.add(strokes);
                }
            }),
            practiceModel, new TrainModelController(() -> trains++), viewModel,
            Runnable::run, Runnable::run);

    @SuppressWarnings("unchecked")
    private <T extends Component> T findInPractice(String name, Class<T> type) {
        return (T) findIn(practiceView, name, type);
    }

    @Test
    void withoutPracticeTheModeSwitchIsAbsent() {
        assertEquals(null, find("expressionsModeButton", JToggleButton.class));
    }

    @Test
    void choosingExpressionsAsksForAnExpressionAndShowsIt() {
        findInPractice("expressionsModeButton", JToggleButton.class).doClick();

        assertEquals(1, nexts);
        practiceModel.setState(practiceModel.getState().withPrompt(
                PracticeExpressionState.Layout.FRACTION, java.util.Arrays.asList("3", "4"), "",
                false));
        assertEquals(java.util.Arrays.asList("3", "4"),
                findInPractice("expressionPreview", ExpressionPreview.class).getParts());
    }

    @Test
    void savingInExpressionModeSubmitsTheWholeCanvas() {
        findInPractice("expressionsModeButton", JToggleButton.class).doClick();
        CanvasPanel canvas = findInPractice("canvas", CanvasPanel.class);
        canvas.setStrokes(java.util.Collections.singletonList(new Stroke(
                java.util.Arrays.asList(new handcalc.entity.Point(1, 1),
                        new handcalc.entity.Point(5, 40)))));

        findInPractice("saveButton", JButton.class).doClick();

        assertEquals(1, submitted.size());
        assertTrue(saves.isEmpty());
    }

    @Test
    void aSavedExpressionClearsTheCanvasAndRefreshesTheCounts() {
        findInPractice("expressionsModeButton", JToggleButton.class).doClick();
        CanvasPanel canvas = findInPractice("canvas", CanvasPanel.class);
        canvas.setStrokes(java.util.Collections.singletonList(new Stroke(
                java.util.Arrays.asList(new handcalc.entity.Point(1, 1),
                        new handcalc.entity.Point(5, 40)))));
        int before = practiceRefreshes;

        practiceModel.setState(practiceModel.getState().withPrompt(
                PracticeExpressionState.Layout.LINE, java.util.Collections.singletonList("1+2"),
                "Saved 3 samples", true));

        assertTrue(canvas.getStrokes().isEmpty());
        assertEquals(before + 1, practiceRefreshes);
        assertEquals("Saved 3 samples", findInPractice("messageLabel", JLabel.class).getText());
    }

    @Test
    void goingBackToSingleSymbolsRestoresThePrompt() {
        findInPractice("expressionsModeButton", JToggleButton.class).doClick();
        findInPractice("symbolsModeButton", JToggleButton.class).doClick();

        assertTrue(findInPractice("promptLabel", JLabel.class).isShowing()
                || findInPractice("promptLabel", JLabel.class).getParent() != null);
        assertFalse(findInPractice("skipButton", JButton.class).isVisible());
    }
}
