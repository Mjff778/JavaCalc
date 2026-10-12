package handcalc.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ExpressionTest {

    private static double eval(String text) throws InvalidExpressionException {
        return new Expression(text).evaluate();
    }

    @Test
    void singleNumber() throws Exception {
        assertEquals(42, eval("42"));
    }

    @Test
    void multiplicationBindsTighterThanAddition() throws Exception {
        assertEquals(14, eval("2+3×4"));
    }

    @Test
    void divisionAndSubtractionAreLeftAssociative() throws Exception {
        assertEquals(2, eval("8÷2÷2"));
        assertEquals(1, eval("8-4-3"));
    }

    @Test
    void parenthesesOverridePrecedence() throws Exception {
        assertEquals(20, eval("(2+3)×4"));
    }

    @Test
    void decimalNumbers() throws Exception {
        assertEquals(3.75, eval("1.5×2.5"));
    }

    @Test
    void unaryMinus() throws Exception {
        assertEquals(-3, eval("-5+2"));
        assertEquals(-6, eval("2×(-3)"));
    }

    @Test
    void asciiOperatorsAreAccepted() throws Exception {
        assertEquals(7, eval("1+2*3"));
        assertEquals(2.5, eval("5/2"));
    }

    @Test
    void divisionByZeroIsRejected() {
        InvalidExpressionException e =
                assertThrows(InvalidExpressionException.class, () -> eval("5÷0"));
        assertEquals("Division by zero", e.getMessage());
    }

    @Test
    void emptyExpressionIsRejected() {
        assertThrows(InvalidExpressionException.class, () -> eval(""));
    }

    @Test
    void danglingOperatorIsRejected() {
        assertThrows(InvalidExpressionException.class, () -> eval("3+"));
        assertThrows(InvalidExpressionException.class, () -> eval("×3"));
    }

    @Test
    void unbalancedParenthesesAreRejected() {
        assertThrows(InvalidExpressionException.class, () -> eval("(2+3"));
        assertThrows(InvalidExpressionException.class, () -> eval("2+3)"));
    }

    @Test
    void unknownCharacterIsRejected() {
        assertThrows(InvalidExpressionException.class, () -> eval("2a3"));
    }

    private static void assertValue(double expected, String text) throws Exception {
        assertEquals(expected, eval(text), 1e-9, text);
    }

    @Test
    void powersAreRightAssociativeAndBindTighterThanMultiplication() throws Exception {
        assertValue(8, "2^3");
        assertValue(512, "2^3^2");
        assertValue(17, "1+2^3×2");
        assertValue(0.5, "2^(0-1)");
    }

    @Test
    void unaryMinusBindsLooserThanPower() throws Exception {
        assertValue(-4, "-2^2");
        assertValue(4, "(-2)^2");
    }

    @Test
    void squareRoots() throws Exception {
        assertValue(3, "√9");
        assertValue(4, "√(16)");
        assertValue(5, "√(9+16)");
        assertValue(6, "2√9");
        assertValue(2, "√(√(16))");
    }

    @Test
    void squareRootOfANegativeNumberIsRejected() {
        InvalidExpressionException e =
                assertThrows(InvalidExpressionException.class, () -> eval("√(0-4)"));
        assertEquals("Square root of a negative number", e.getMessage());
    }

    @Test
    void trigonometryWorksInDegrees() throws Exception {
        assertValue(0.5, "sin(30)");
        assertValue(0.5, "cos(60)");
        assertValue(1, "tan(45)");
        assertValue(0.5, "sin30");
        assertValue(1, "2sin(30)");
        assertValue(0.25, "sin(30)^2");
        assertValue(1.5, "1+sin(30)");
    }

    @Test
    void tangentOfNinetyDegreesIsUndefined() {
        InvalidExpressionException e =
                assertThrows(InvalidExpressionException.class, () -> eval("tan(90)"));
        assertEquals("tan(90) is undefined", e.getMessage());
    }

    @Test
    void unknownFunctionNamesAreRejected() {
        InvalidExpressionException e =
                assertThrows(InvalidExpressionException.class, () -> eval("abc(1)"));
        assertEquals("Unknown function 'abc'", e.getMessage());
    }

    @Test
    void implicitMultiplication() throws Exception {
        assertValue(14, "2(3+4)");
        assertValue(8, "(1+1)(2+2)");
        assertValue(6, "3(2)");
    }

    @Test
    void resultsThatAreNotFiniteNumbersAreRejected() {
        assertThrows(InvalidExpressionException.class, () -> eval("10^400"));
        assertThrows(InvalidExpressionException.class, () -> eval("(0-8)^0.5"));
        assertThrows(InvalidExpressionException.class, () -> eval("0^(0-1)"));
    }

    @Test
    void piIsAConstantThatMultipliesImplicitly() throws Exception {
        assertEquals(Math.PI, eval("π"), 1e-12);
        assertEquals(2 * Math.PI, eval("2π"), 1e-12);
        assertEquals(2 * Math.PI, eval("π2"), 1e-12);
        assertEquals(Math.PI * Math.PI, eval("π^2"), 1e-12);
        assertEquals(Math.PI / 2, eval("π÷2"), 1e-12);
    }

    @Test
    void anArgumentContainingPiIsInRadians() throws Exception {
        assertEquals(1, eval("sin(π÷2)"), 1e-12);
        assertEquals(-1, eval("cos(π)"), 1e-12);
        assertEquals(0.5, eval("sin(30)"), 1e-12);
    }

    @Test
    void bracketedNumbersMultiplyImplicitly() throws Exception {
        assertEquals(9, eval("(1+2)3"), 1e-12);
    }

    @Test
    void piMayFollowAFunctionNameWithoutBrackets() throws Exception {
        assertValue(0, "sinπ");
        assertValue(-1, "cosπ");
        assertValue(-2, "2cosπ");
    }
}
