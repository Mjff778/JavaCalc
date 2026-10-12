package handcalc.use_case;

import static org.junit.jupiter.api.Assertions.assertEquals;

import handcalc.entity.Glyph;
import handcalc.entity.Point;
import handcalc.entity.Stroke;
import handcalc.use_case.recognize_and_solve.Candidate;
import handcalc.use_case.recognize_and_solve.FormulaBuilder;
import handcalc.use_case.recognize_and_solve.MathLayout;
import handcalc.use_case.recognize_and_solve.SymbolReading;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The typeset formula shows what was calculated: written structure is kept, and the brackets
 * the layout adds to make a line of it are not shown.
 */
class FormulaBuilderTest {

    private static SymbolReading at(char symbol, double x1, double y1, double x2, double y2) {
        Stroke stroke = new Stroke(Arrays.asList(new Point(x1, y1), new Point(x2, y2)));
        return new SymbolReading(new Glyph(Collections.singletonList(stroke)), symbol,
                Collections.singletonList(new Candidate(symbol, 1.0)));
    }

    /** A plain line: symbols 40px tall, one every 40px. */
    private static List<SymbolReading> line(String text) {
        List<SymbolReading> items = new ArrayList<>();
        for (int i = 0; i < text.length(); i++) {
            double x = i * 40;
            char c = text.charAt(i);
            if (c == '-' || c == '=') {
                items.add(at(c, x, 20, x + 25, 20));
            } else {
                items.add(at(c, x, 0, x + 25, 40));
            }
        }
        return items;
    }

    private static String typeset(List<SymbolReading> readings) {
        MathLayout.Line line = MathLayout.layOut(readings);
        String text = line.getText();
        int end = text.endsWith("=") ? text.length() - 1 : text.length();
        return FormulaBuilder.build(line, readings, end).toString();
    }

    @Test
    void aPlainLineIsSetAsWritten() {
        assertEquals("12+3×4", typeset(line("12+3×4=")));
    }

    @Test
    void aWrittenDivisionSignStaysADivisionSign() {
        assertEquals("9÷2", typeset(line("9÷2=")));
    }

    @Test
    void writtenBracketsAreKept() {
        assertEquals("2(3+4)", typeset(line("2(3+4)=")));
    }

    @Test
    void aFractionIsStackedWithoutTheBracketsTheLayoutAdds() {
        List<SymbolReading> items = Arrays.asList(
                at('1', 40, 40, 55, 80),
                at('2', 60, 40, 75, 80),
                at('-', 0, 100, 100, 100),
                at('3', 40, 120, 58, 160));

        assertEquals("{12/3}", typeset(items));
    }

    @Test
    void aRaisedNumberIsAPower() {
        List<SymbolReading> items = Arrays.asList(
                at('7', 0, 40, 30, 100),
                at('2', 35, 10, 50, 40),
                at('+', 70, 55, 100, 85),
                at('1', 120, 40, 140, 100));

        assertEquals("7^{2}+1", typeset(items));
    }

    @Test
    void aRootCoversItsRadicand() {
        List<SymbolReading> items = Arrays.asList(
                at('√', 0, 0, 120, 60),
                at('1', 40, 15, 60, 55),
                at('6', 70, 15, 95, 55));

        assertEquals("√{16}", typeset(items));
    }

    @Test
    void aFunctionKeepsItsName() {
        assertEquals("sin(30)", typeset(line("sin(30)=")));
    }

    @Test
    void aMinusSignIsAMinusSign() {
        assertEquals("−5+2", typeset(line("-5+2=")));
    }
}
