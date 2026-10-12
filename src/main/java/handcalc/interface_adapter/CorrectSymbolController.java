package handcalc.interface_adapter;

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
}
