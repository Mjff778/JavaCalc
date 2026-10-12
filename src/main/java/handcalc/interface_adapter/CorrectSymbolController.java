package handcalc.interface_adapter;

import handcalc.entity.PlacedSymbol;
import handcalc.entity.Stroke;
import handcalc.use_case.correct_symbol.CorrectSymbolInputBoundary;
import handcalc.use_case.correct_symbol.CorrectSymbolInputData;
import java.util.List;

/**
 * Turns "this symbol is really X" into a use case call.
 */
public class CorrectSymbolController {

    private final CorrectSymbolInputBoundary useCase;

    public CorrectSymbolController(CorrectSymbolInputBoundary useCase) {
        this.useCase = useCase;
    }

    public void execute(List<Stroke> strokes, int glyphIndex, char symbol) {
        useCase.execute(new CorrectSymbolInputData(strokes, glyphIndex, symbol));
    }

    /** Applies the correction, and solves the writing if it ends in "=". */
    public void executeWhenFinished(List<Stroke> strokes, int glyphIndex, char symbol) {
        useCase.execute(new CorrectSymbolInputData(strokes, glyphIndex, symbol, true));
    }

    /** Applies the correction to a page with symbols the user picked earlier. */
    public void executeWhenFinished(List<Stroke> strokes, List<PlacedSymbol> placed,
                                    int glyphIndex, char symbol) {
        useCase.execute(new CorrectSymbolInputData(strokes, placed, glyphIndex, symbol, true));
    }
}
