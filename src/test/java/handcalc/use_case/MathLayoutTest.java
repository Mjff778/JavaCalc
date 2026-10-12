package handcalc.use_case;

import static org.junit.jupiter.api.Assertions.assertEquals;

import handcalc.entity.Glyph;
import handcalc.entity.Point;
import handcalc.entity.Stroke;
import handcalc.use_case.recognize_and_solve.Candidate;
import handcalc.use_case.recognize_and_solve.MathLayout;
import handcalc.use_case.recognize_and_solve.SymbolReading;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

class MathLayoutTest {

    /** A symbol whose ink occupies the given box. */
    private static SymbolReading at(char symbol, double x1, double y1, double x2, double y2) {
        Stroke stroke = new Stroke(Arrays.asList(new Point(x1, y1), new Point(x2, y2)));
        return new SymbolReading(new Glyph(Collections.singletonList(stroke)), symbol,
                Collections.singletonList(new Candidate(symbol, 1.0)));
    }

    /** Digits and operators of a plain line: 40px tall, 25px wide, 15px apart. */
    private static List<SymbolReading> line(String text) {
        List<SymbolReading> items = new ArrayList<>();
        for (int i = 0; i < text.length(); i++) {
            double x = i * 40;
            char c = text.charAt(i);
            if (c == '-' || c == '=') {
                items.add(at(c, x, 20, x + 25, 20));
            } else if (c == '.') {
                items.add(at(c, x + 10, 38, x + 14, 40));
            } else {
                items.add(at(c, x, 0, x + 25, 40));
            }
        }
        return items;
    }

    private static String linear(List<SymbolReading> items) {
        return MathLayout.toLinear(items);
    }

    @Test
    void aPlainLineIsReadLeftToRight() {
        assertEquals("12+3×4", linear(line("12+3×4")));
    }

    @Test
    void decimalPointsStayInsideTheNumber() {
        assertEquals("1.5+2", linear(line("1.5+2")));
    }

    @Test
    void aMinusBetweenNumbersIsNotAFractionBar() {
        assertEquals("9-4", linear(line("9-4")));
    }

    @Test
    void barWithSymbolsAboveAndBelowIsAFraction() {
        List<SymbolReading> items = Arrays.asList(
                at('1', 40, 40, 55, 80),
                at('-', 0, 100, 100, 100),
                at('2', 40, 120, 58, 160));

        assertEquals("(1÷2)", linear(items));
    }

    @Test
    void fractionsCanHaveSeveralSymbolsAndSitInsideALine() {
        List<SymbolReading> items = Arrays.asList(
                at('3', 0, 60, 25, 100),
                at('+', 40, 70, 65, 90),
                at('1', 90, 40, 105, 80),
                at('2', 120, 40, 140, 80),
                at('-', 85, 100, 160, 100),
                at('5', 110, 120, 135, 160),
                at('×', 190, 60, 215, 100),
                at('2', 240, 60, 265, 100));

        assertEquals("3+(12÷5)×2", linear(items));
    }

    @Test
    void aSmallRaisedSymbolAfterANumberIsItsExponent() {
        List<SymbolReading> items = Arrays.asList(
                at('2', 0, 40, 25, 80),
                at('3', 28, 30, 40, 52),
                at('+', 70, 50, 95, 70),
                at('1', 120, 40, 145, 80));

        assertEquals("2^3+1", linear(items));
    }

    @Test
    void exponentsMayHaveSeveralSymbolsAndTheBaseSeveralDigits() {
        List<SymbolReading> items = Arrays.asList(
                at('1', 0, 40, 20, 80),
                at('2', 30, 40, 55, 80),
                at('1', 60, 28, 72, 50),
                at('0', 76, 28, 90, 50));

        assertEquals("12^10", linear(items));
    }

    @Test
    void aNearlyFullSizeDigitRaisedWellAboveTheBaseIsStillAPower() {
        List<SymbolReading> items = Arrays.asList(
                at('2', 0, 40, 25, 90),
                at('3', 30, 5, 52, 50)); // 90% of the base's height, centre well above its middle

        assertEquals("2^3", linear(items));
    }

    @Test
    void aSlightlyHighDigitOfTheSameSizeIsNotAPower() {
        List<SymbolReading> items = Arrays.asList(
                at('2', 0, 40, 25, 90),
                at('3', 30, 33, 55, 83)); // same size, only a little higher: ordinary drift

        assertEquals("23", linear(items));
    }

    @Test
    void piIsAConstantNextToNumbersAndCanBeSquared() {
        assertEquals("2π", linear(line("2π")));
        List<SymbolReading> squared = Arrays.asList(
                at('π', 0, 0, 40, 40),
                at('2', 46, 0, 58, 18));

        assertEquals("π^2", linear(squared));
    }

    @Test
    void aRaisedEqualsSignOrOperatorIsNeverAnExponent() {
        List<SymbolReading> items = Arrays.asList(
                at('4', 0, 0, 25, 40),
                at('=', 35, 2, 55, 12),
                at('+', 65, 0, 80, 10));

        assertEquals("4=+", linear(items));
    }

    @Test
    void sameSizedSymbolOnTheBaselineIsNotAnExponent() {
        assertEquals("23", linear(line("23")));
    }

    @Test
    void exponentOfAParenthesisedSum() {
        List<SymbolReading> items = Arrays.asList(
                at('(', 0, 35, 12, 85),
                at('1', 20, 40, 40, 80),
                at('+', 48, 50, 70, 70),
                at('2', 78, 40, 98, 80),
                at(')', 106, 35, 118, 85),
                at('2', 124, 25, 138, 50));

        assertEquals("(1+2)^2", linear(items));
    }

    @Test
    void radicalSignCoversTheNumbersUnderItsBar() {
        List<SymbolReading> items = Arrays.asList(
                at('√', 0, 40, 100, 100),
                at('9', 40, 55, 60, 95));

        assertEquals("√9", linear(items));
    }

    @Test
    void radicandWithSeveralDigitsStaysAWholeNumberAndTheLineContinues() {
        List<SymbolReading> items = Arrays.asList(
                at('√', 0, 40, 110, 100),
                at('1', 40, 55, 58, 95),
                at('6', 66, 55, 90, 95),
                at('+', 130, 60, 155, 80),
                at('1', 180, 50, 200, 90));

        assertEquals("√16+1", linear(items));
    }

    @Test
    void aRadicalCanSitInAFractionsNumerator() {
        List<SymbolReading> items = Arrays.asList(
                at('√', 0, 0, 60, 50),
                at('9', 25, 12, 45, 45),
                at('-', 0, 65, 70, 65),
                at('2', 25, 80, 45, 120));

        assertEquals("(√9÷2)", linear(items));
    }

    @Test
    void anExpressionInAnExponentOrRadicandIsParenthesised() {
        List<SymbolReading> power = Arrays.asList(
                at('2', 0, 40, 25, 80),
                at('1', 28, 20, 40, 42),
                at('+', 44, 26, 58, 36),
                at('1', 62, 20, 74, 42));
        assertEquals("2^(1+1)", linear(power));

        List<SymbolReading> root = Arrays.asList(
                at('√', 0, 40, 130, 100),
                at('9', 40, 55, 60, 95),
                at('+', 66, 65, 90, 85),
                at('1', 96, 55, 116, 95));
        assertEquals("√(9+1)", linear(root));
    }

    @Test
    void functionNamesAreAssembledFromLetters() {
        List<SymbolReading> items = new ArrayList<>(line("sin"));
        items.addAll(shift(line("(30)"), 130));

        assertEquals("sin(30)", linear(items));
    }

    @Test
    void lookAlikesInsideAFunctionNameAreRepaired() {
        List<SymbolReading> cos = new ArrayList<>(line("(os"));
        cos.addAll(shift(line("(60)"), 130));
        assertEquals("cos(60)", linear(cos));

        List<SymbolReading> sin = new ArrayList<>(line("5in"));
        sin.addAll(shift(line("(30)"), 130));
        assertEquals("sin(30)", linear(sin));

        List<SymbolReading> tan = new ArrayList<>(line("+an"));
        tan.addAll(shift(line("(45)"), 130));
        assertEquals("tan(45)", linear(tan));
    }

    @Test
    void lettersBesideDigitsInsideAnArgumentAreReadAsDigits() {
        List<SymbolReading> items = new ArrayList<>(line("sin"));
        items.addAll(shift(line("(3o)"), 130));
        assertEquals("sin(30)", linear(items));

        List<SymbolReading> cos = new ArrayList<>(line("cos"));
        cos.addAll(shift(line("(6o)"), 130));
        assertEquals("cos(60)", linear(cos));
    }

    @Test
    void anOpeningBracketReadAsCAfterAFunctionNameIsRepaired() {
        List<SymbolReading> items = new ArrayList<>(line("cos"));
        items.addAll(shift(line("c60)"), 130));

        assertEquals("cos(60)", linear(items));
    }

    @Test
    void lettersAwayFromDigitsAreLeftAlone() {
        assertEquals("oo", linear(line("oo")));
    }

    @Test
    void ordinaryNumbersAreNotMistakenForFunctionNames() {
        assertEquals("5+10", linear(line("5+10")));
    }

    @Test
    void trailingEqualsSignIsKept() {
        assertEquals("1+2=", linear(line("1+2=")));
    }

    private static List<SymbolReading> shift(List<SymbolReading> items, double dx) {
        List<SymbolReading> moved = new ArrayList<>();
        for (SymbolReading r : items) {
            Glyph g = r.getGlyph();
            moved.add(at(r.getSymbol(), g.getMinX() + dx, g.getMinY(), g.getMaxX() + dx,
                    g.getMaxY()));
        }
        return moved;
    }

    @Test
    void aNumberAfterAPowerIsMultipliedNotAddedToTheExponent() {
        List<SymbolReading> items = Arrays.asList(
                at('2', 0, 0, 25, 40),
                at('3', 28, -15, 38, 5),
                at('4', 60, 0, 85, 40));

        assertEquals("2^3×4", linear(items));
    }

    @Test
    void aNumberAfterARootIsMultipliedNotPulledUnderIt() {
        List<SymbolReading> items = Arrays.asList(
                at('√', 0, 0, 60, 50),
                at('4', 25, 10, 45, 45),
                at('5', 80, 5, 100, 45));

        assertEquals("√4×5", linear(items));
    }

    private static String function(String name, String argument) {
        List<SymbolReading> items = new ArrayList<>(line(name));
        items.addAll(shift(line(argument), 130));
        return linear(items);
    }

    @Test
    void aFunctionNameWithTwoMisreadLettersIsStillRepaired() {
        assertEquals("cos(60)", function("c05", "(60)"));
        assertEquals("cos(60)", function("(0s", "(60)"));
        assertEquals("cos(60)", function("(o5", "(60)"));
        assertEquals("sin(30)", function("51n", "(30)"));
        assertEquals("tan(45)", function("+an", "(45)"));
    }

    @Test
    void digitsAndBracketsWithoutAnyLetterAreNeverTakenForAFunction() {
        assertEquals("(05(30))", linear(line("(05(30))")));
        assertEquals("51+2", linear(line("51+2")));
    }

    private static Set<Integer> indices(Integer... values) {
        return new TreeSet<>(Arrays.asList(values));
    }

    @Test
    void eachCharacterOfTheLineKnowsTheSymbolItCameFrom() {
        MathLayout.Line line = MathLayout.layOut(line("12+3"));

        assertEquals("12+3", line.getText());
        assertEquals(indices(2), line.readingsIn(2, 3));
        assertEquals(indices(0, 1), line.readingsIn(0, 2));
    }

    @Test
    void aFractionBarIsTracedThroughItsDivisionSign() {
        List<SymbolReading> items = Arrays.asList(
                at('1', 40, 40, 55, 80),
                at('-', 0, 100, 100, 100),
                at('2', 40, 120, 58, 160));
        MathLayout.Line line = MathLayout.layOut(items);

        assertEquals("(1÷2)", line.getText());
        assertEquals(indices(1), line.readingsIn(2, 3));
        // the closing bracket was added by the layout: the nearest written symbol is the "2"
        assertEquals(indices(2), line.readingsIn(4, 5));
    }

    @Test
    void repairedFunctionNamesKeepTheirSymbols() {
        MathLayout.Line line = MathLayout.layOut(line("5in(3)"));

        assertEquals("sin(3)", line.getText());
        assertEquals(indices(0, 1, 2), line.readingsIn(0, 3));
    }
}
