package handcalc.interface_adapter;

import handcalc.use_case.recognize_and_solve.Candidate;
import handcalc.use_case.recognize_and_solve.RecognizeAndSolveOutputBoundary;
import handcalc.use_case.recognize_and_solve.RecognizeAndSolveOutputData;
import handcalc.use_case.recognize_and_solve.SymbolReading;
import java.util.ArrayList;
import java.util.List;

/**
 * Formats the use case output for the screen and stores it in the view model.
 */
public class RecognizeAndSolvePresenter implements RecognizeAndSolveOutputBoundary {

    private final CalculatorViewModel viewModel;

    public RecognizeAndSolvePresenter(CalculatorViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @Override
    public void prepareSuccessView(RecognizeAndSolveOutputData outputData) {
        viewModel.setState(CalculatorState.solved(outputData.getExpression(),
                ResultFormat.format(outputData.getResult()), toSymbols(outputData.getReadings())));
    }

    @Override
    public void prepareFailView(String recognizedExpression, String errorMessage,
                                List<SymbolReading> readings) {
        viewModel.setState(CalculatorState.failed(
                recognizedExpression, errorMessage, toSymbols(readings)));
    }

    private static List<ReadSymbol> toSymbols(List<SymbolReading> readings) {
        List<ReadSymbol> symbols = new ArrayList<>();
        for (SymbolReading reading : readings) {
            List<ReadSymbol.Alternative> alternatives = new ArrayList<>();
            for (Candidate candidate : reading.getAlternatives()) {
                alternatives.add(new ReadSymbol.Alternative(candidate.getSymbol(),
                        (int) Math.round(candidate.getConfidence() * 100)));
            }
            symbols.add(new ReadSymbol(reading.getSymbol(), alternatives));
        }
        return symbols;
    }
}
