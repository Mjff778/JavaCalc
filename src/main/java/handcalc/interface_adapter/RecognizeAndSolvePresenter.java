package handcalc.interface_adapter;

import handcalc.entity.Glyph;
import handcalc.entity.Stroke;
import handcalc.use_case.recognize_and_solve.AnswerPlacement;
import handcalc.use_case.recognize_and_solve.Candidate;
import handcalc.use_case.recognize_and_solve.LineResult;
import handcalc.use_case.recognize_and_solve.RecognizeAndSolveFailureData;
import handcalc.use_case.recognize_and_solve.RecognizeAndSolveOutputBoundary;
import handcalc.use_case.recognize_and_solve.RecognizeAndSolveOutputData;
import handcalc.use_case.recognize_and_solve.ResultFormat;
import handcalc.use_case.recognize_and_solve.SymbolReading;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Formats the use case output for the screen and stores it in the view model.
 */
public class RecognizeAndSolvePresenter implements RecognizeAndSolveOutputBoundary {

    /** Written after the "=" when the expression has no value, such as 5÷0. */
    static final String UNDEFINED = "undefined";

    private final CalculatorViewModel viewModel;

    public RecognizeAndSolvePresenter(CalculatorViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @Override
    public void prepareSuccessView(RecognizeAndSolveOutputData outputData) {
        String result = ResultFormat.format(outputData.getResult());
        viewModel.setState(CalculatorState.solved(outputData.getExpression(), result,
                toSymbols(outputData.getReadings(), Collections.emptySet()),
                written(result, outputData.getPlacement(), false)));
    }

    @Override
    public void prepareFailView(String recognizedExpression, String errorMessage,
                                List<SymbolReading> readings) {
        viewModel.setState(CalculatorState.failed(
                recognizedExpression, errorMessage, toSymbols(readings, Collections.emptySet())));
    }

    @Override
    public void prepareFailView(RecognizeAndSolveFailureData failure) {
        WrittenAnswer answer = failure.isUndefined()
                ? written(UNDEFINED, failure.getPlacement(), true) : null;
        viewModel.setState(CalculatorState.failed(failure.getExpression(), failure.getMessage(),
                toSymbols(failure.getReadings(), failure.getFaultySymbols()), answer));
    }

    @Override
    public void prepareUnfinishedView(List<SymbolReading> readings) {
        viewModel.setState(CalculatorState.read(toSymbols(readings, Collections.emptySet())));
    }

    @Override
    public void preparePageView(List<LineResult> lines) {
        List<LineView> views = new ArrayList<>();
        for (LineResult line : lines) {
            List<Stroke> strokes = new ArrayList<>();
            for (SymbolReading reading : line.getReadings()) {
                strokes.addAll(reading.getGlyph().getStrokes());
            }
            RecognizeAndSolveOutputData solved = line.getSolved();
            RecognizeAndSolveFailureData failure = line.getFailure();
            if (solved != null) {
                String result = ResultFormat.format(solved.getResult());
                views.add(new LineView(toSymbols(solved.getReadings(), Collections.emptySet()),
                        written(result, solved.getPlacement(), false), solved.getFormula(),
                        strokes, true));
            } else if (failure != null) {
                WrittenAnswer answer = failure.isUndefined()
                        ? written(UNDEFINED, failure.getPlacement(), true) : null;
                views.add(new LineView(toSymbols(failure.getReadings(),
                        failure.getFaultySymbols()), answer, failure.getFormula(), strokes, true));
            } else {
                views.add(new LineView(toSymbols(line.getReadings(), Collections.emptySet()),
                        null, null, strokes, false));
            }
        }
        viewModel.setState(CalculatorState.page(views));
    }

    private static WrittenAnswer written(String text, AnswerPlacement placement, boolean error) {
        if (placement == null) {
            return null;
        }
        return new WrittenAnswer(text, placement.getX(), placement.getCentreY(),
                placement.getHeight(), error);
    }

    private static List<ReadSymbol> toSymbols(List<SymbolReading> readings, Set<Integer> faulty) {
        List<ReadSymbol> symbols = new ArrayList<>();
        for (int i = 0; i < readings.size(); i++) {
            SymbolReading reading = readings.get(i);
            List<ReadSymbol.Alternative> alternatives = new ArrayList<>();
            for (Candidate candidate : reading.getAlternatives()) {
                alternatives.add(new ReadSymbol.Alternative(candidate.getSymbol(),
                        (int) Math.round(candidate.getConfidence() * 100)));
            }
            Glyph glyph = reading.getGlyph();
            symbols.add(new ReadSymbol(reading.getSymbol(), alternatives, glyph.getMinX(),
                    glyph.getMinY(), glyph.getMaxX(), glyph.getMaxY(), faulty.contains(i),
                    glyph.getStrokes()));
        }
        return symbols;
    }
}
