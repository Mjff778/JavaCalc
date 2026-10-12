package handcalc.interface_adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import handcalc.entity.Glyph;
import handcalc.entity.Point;
import handcalc.entity.Stroke;
import handcalc.use_case.recognize_and_solve.Candidate;
import handcalc.use_case.recognize_and_solve.RecognizeAndSolveInputBoundary;
import handcalc.use_case.recognize_and_solve.RecognizeAndSolveInputData;
import handcalc.use_case.recognize_and_solve.RecognizeAndSolveOutputData;
import handcalc.use_case.correct_symbol.CorrectSymbolInputBoundary;
import handcalc.use_case.correct_symbol.CorrectSymbolInputData;
import handcalc.use_case.recognize_and_solve.SymbolReading;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class CalculatorAdapterTest {

    private final CalculatorViewModel viewModel = new CalculatorViewModel();
    private final RecognizeAndSolvePresenter presenter = new RecognizeAndSolvePresenter(viewModel);

    @Test
    void wholeNumbersAreShownWithoutDecimalPoint() {
        presenter.prepareSuccessView(new RecognizeAndSolveOutputData("12+3", 15.0, Collections.emptyList()));

        assertEquals("12+3", viewModel.getState().getExpression());
        assertEquals("15", viewModel.getState().getResult());
        assertNull(viewModel.getState().getError());
    }

    @Test
    void fractionsAreRoundedToEightDecimals() {
        presenter.prepareSuccessView(new RecognizeAndSolveOutputData("1÷3", 1.0 / 3, Collections.emptyList()));
        assertEquals("0.33333333", viewModel.getState().getResult());

        presenter.prepareSuccessView(new RecognizeAndSolveOutputData("5÷2", 2.5, Collections.emptyList()));
        assertEquals("2.5", viewModel.getState().getResult());
    }

    @Test
    void negativeResultsKeepTheirSign() {
        presenter.prepareSuccessView(new RecognizeAndSolveOutputData("2-5", -3.0, Collections.emptyList()));
        assertEquals("-3", viewModel.getState().getResult());
    }

    @Test
    void failureShowsWhatWasReadAndTheError() {
        presenter.prepareSuccessView(new RecognizeAndSolveOutputData("1+1", 2, Collections.emptyList()));
        presenter.prepareFailView("5÷0", "Division by zero", Collections.emptyList());

        assertEquals("5÷0", viewModel.getState().getExpression());
        assertEquals("Division by zero", viewModel.getState().getError());
        assertNull(viewModel.getState().getResult());
    }

    @Test
    void listenersAreNotifiedWhenTheStateChanges() {
        List<Object> seen = new ArrayList<>();
        viewModel.addPropertyChangeListener(e -> seen.add(e.getNewValue()));

        presenter.prepareSuccessView(new RecognizeAndSolveOutputData("1+1", 2, Collections.emptyList()));

        assertEquals(1, seen.size());
        assertSame(viewModel.getState(), seen.get(0));
    }

    @Test
    void controllerPassesTheStrokesToTheUseCase() {
        List<RecognizeAndSolveInputData> received = new ArrayList<>();
        RecognizeAndSolveInputBoundary useCase = received::add;
        Stroke stroke = new Stroke(Collections.singletonList(new Point(1, 2)));

        new RecognizeAndSolveController(useCase).execute(Collections.singletonList(stroke));

        assertEquals(1, received.size());
        assertSame(stroke, received.get(0).getStrokes().get(0));
    }

    @Test
    void readingsBecomeSymbolsWithPercentAlternatives() {
        Glyph glyph = new Glyph(Collections.singletonList(
                new Stroke(Collections.singletonList(new Point(0, 0)))));
        SymbolReading reading = new SymbolReading(glyph, '2',
                Arrays.asList(new Candidate('2', 0.874), new Candidate('3', 0.061)));

        presenter.prepareFailView("2+", "Expression ended unexpectedly",
                Collections.singletonList(reading));

        ReadSymbol symbol = viewModel.getState().getSymbols().get(0);
        assertEquals('2', symbol.getSymbol());
        assertEquals(2, symbol.getAlternatives().size());
        assertEquals('3', symbol.getAlternatives().get(1).getSymbol());
        assertEquals(87, symbol.getAlternatives().get(0).getPercent());
        assertEquals(6, symbol.getAlternatives().get(1).getPercent());
    }

    @Test
    void correctionControllerPassesStrokeIndexAndSymbol() {
        List<CorrectSymbolInputData> received = new ArrayList<>();
        CorrectSymbolInputBoundary useCase = received::add;
        Stroke stroke = new Stroke(Collections.singletonList(new Point(1, 2)));

        new CorrectSymbolController(useCase).execute(Collections.singletonList(stroke), 2, '×');

        assertEquals(2, received.get(0).getGlyphIndex());
        assertEquals('×', received.get(0).getSymbol());
        assertSame(stroke, received.get(0).getStrokes().get(0));
    }
}
