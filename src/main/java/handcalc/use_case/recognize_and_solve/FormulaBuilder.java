package handcalc.use_case.recognize_and_solve;

import handcalc.entity.Formula;
import java.util.ArrayList;
import java.util.List;

/**
 * Typesets an expression line the way it was calculated, so the user can check what was
 * read: a division made from a fraction bar is stacked as a fraction, and the brackets and
 * signs the layout added to make one line of the writing are left out, except for a "×"
 * that keeps two numbers apart.
 *
 * <p>The line must be one that {@link handcalc.entity.Expression} can read.
 */
public final class FormulaBuilder {

    private static final char END = '\0';
    private static final char BAR = '-';

    private final MathLayout.Line line;
    private final List<SymbolReading> readings;
    private final int end;
    private int pos;

    private FormulaBuilder(MathLayout.Line line, List<SymbolReading> readings, int end) {
        this.line = line;
        this.readings = readings;
        this.end = end;
    }

    /**
     * @param line the laid-out writing
     * @param readings the readings the line was laid out from
     * @param end how much of the line is the expression (a trailing "=" is not)
     */
    public static Formula build(MathLayout.Line line, List<SymbolReading> readings, int end) {
        return new FormulaBuilder(line, readings, end).expression();
    }

    private Formula expression() {
        List<Formula> row = new ArrayList<>();
        row.add(term());
        while (peek() == '+' || peek() == '-') {
            row.add(Formula.text(peek() == '+' ? "+" : "−"));
            pos++;
            row.add(term());
        }
        return Formula.row(row);
    }

    private Formula term() {
        List<Formula> row = new ArrayList<>();
        row.add(unary());
        while (true) {
            char c = peek();
            if (c == '÷' && isFractionBar(pos)) {
                pos++;
                Formula numerator = row.remove(row.size() - 1);
                row.add(Formula.fraction(numerator, unary()));
            } else if (c == '×' || c == '*' || c == '÷' || c == '/') {
                row.add(Formula.text(c == '×' || c == '*' ? "×" : "÷"));
                pos++;
                row.add(unary());
            } else if (startsPrimary(c)) {
                row.add(unary());
            } else {
                return Formula.row(row);
            }
        }
    }

    private Formula unary() {
        if (peek() == '-') {
            pos++;
            List<Formula> row = new ArrayList<>();
            row.add(Formula.text("−"));
            row.add(unary());
            return Formula.row(row);
        }
        Formula base = primary();
        if (peek() == '^') {
            pos++;
            return Formula.power(base, unary());
        }
        return base;
    }

    private Formula primary() {
        char c = peek();
        if (c == '(') {
            boolean added = line.sourceAt(pos) < 0;
            pos++;
            Formula inside = expression();
            if (peek() == ')') {
                pos++;
            }
            return added ? inside : Formula.brackets(inside);
        }
        if (c == '√') {
            pos++;
            return Formula.root(unary());
        }
        if (Character.isDigit(c) || c == '.') {
            int start = pos;
            while (Character.isDigit(peek()) || peek() == '.') {
                pos++;
            }
            return Formula.text(line.getText().substring(start, pos));
        }
        if (c == 'π') {
            pos++;
            return Formula.text("π");
        }
        if (Character.isLetter(c)) {
            int start = pos;
            while (Character.isLetter(peek()) && peek() != 'π') {
                pos++;
            }
            List<Formula> row = new ArrayList<>();
            row.add(Formula.text(line.getText().substring(start, pos)));
            row.add(peek() == '(' ? primary() : unary());
            return Formula.row(row);
        }
        // not an expression after all: show what is there
        pos++;
        return Formula.text(String.valueOf(c));
    }

    private boolean isFractionBar(int index) {
        int source = line.sourceAt(index);
        return source >= 0 && readings.get(source).getSymbol() == BAR;
    }

    private char peek() {
        return pos < end ? line.getText().charAt(pos) : END;
    }

    private static boolean startsPrimary(char c) {
        return c == '(' || c == '√' || c == 'π' || Character.isLetter(c) || Character.isDigit(c)
                || c == '.';
    }
}
