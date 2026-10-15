package handcalc.entity;

/**
 * An arithmetic expression such as {@code 12+3×(4-1)} or {@code 2sin(30)+√(9)^2}.
 *
 * <p>Supports {@code + - × ÷ ^}, parentheses, decimal numbers, unary minus, {@code √},
 * the constant {@code π}, the functions {@code sin cos tan}, {@code ans} for an earlier answer,
 * and implicit multiplication such as {@code 2(3+4)}, {@code 2π} or {@code 2ans}. Angles are in degrees, except that an argument which
 * contains {@code π} is taken as radians ({@code sin(π÷2)} is 1). The ASCII spellings {@code *} and {@code /} are accepted as well.
 */
public final class Expression {

    private static final String ANS = "ans";
    private static final String[] NAMES = {ANS, "sin", "cos", "tan"};

    private final String text;
    private final Double ans;

    public Expression(String text) {
        this(text, null);
    }

    /** @param ans what {@code ans} stands for, or null if there is no answer to use */
    public Expression(String text, Double ans) {
        this.text = text;
        this.ans = ans;
    }

    public String getText() {
        return text;
    }

    /** @return the text with each {@code ans} replaced by {@code number} */
    public String withAnsWrittenAs(String number) {
        StringBuilder written = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            String name = nameAt(text, i);
            if (name == null) {
                written.append(text.charAt(i++));
                continue;
            }
            i += name.length();
            if (!name.equals(ANS)) {
                written.append(name);
            } else if (number.startsWith("-")
                    || (written.length() > 0 && isNumeral(written.charAt(written.length() - 1)))
                    || (i < text.length() && isNumeral(text.charAt(i)))) {
                // so that "2ans" is not written "23", nor "ans^2" "-3^2"
                written.append('(').append(number).append(')');
            } else {
                written.append(number);
            }
        }
        return written.toString();
    }

    /** @return the known name the letters at {@code start} begin with, as in "2anssin30" */
    private static String nameAt(String input, int start) {
        for (String name : NAMES) {
            if (input.startsWith(name, start)) {
                return name;
            }
        }
        return null;
    }

    private static boolean isNumeral(char c) {
        return Character.isDigit(c) || c == '.';
    }

    public double evaluate() throws InvalidExpressionException {
        Parser parser = new Parser(text, ans);
        double value = parser.parseExpression();
        parser.expectEnd();
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw InvalidExpressionException.undefined("Result is not a finite number");
        }
        return value;
    }

    /**
     * Recursive-descent parser:
     * <pre>
     * expression := term (('+' | '-') term)*
     * term       := unary ( ('×' | '÷') unary | primaryStart-unary )*   // implicit ×
     * unary      := '-' unary | power
     * power      := primary ('^' unary)?
     * primary    := number | 'π' | '(' expression ')' | '√' unary
     *               | 'ans' | name ( '(' expression ')' | power )
     * </pre>
     */
    private static final class Parser {
        private static final char END = '\0';
        private final String input;
        private final Double ans;
        private int pos;
        private int piCount;

        Parser(String input, Double ans) {
            this.input = input;
            this.ans = ans;
        }

        double parseExpression() throws InvalidExpressionException {
            double value = parseTerm();
            while (peek() == '+' || peek() == '-') {
                char op = input.charAt(pos++);
                double right = parseTerm();
                value = op == '+' ? value + right : value - right;
            }
            return value;
        }

        private double parseTerm() throws InvalidExpressionException {
            double value = parseUnary();
            while (true) {
                char c = peek();
                if (isMultiply(c)) {
                    pos++;
                    value *= parseUnary();
                } else if (isDivide(c)) {
                    pos++;
                    double right = parseUnary();
                    if (right == 0) {
                        throw InvalidExpressionException.undefined("Division by zero");
                    }
                    value /= right;
                } else if (startsPrimary(c)) {
                    value *= parseUnary();
                } else {
                    return value;
                }
            }
        }

        private double parseUnary() throws InvalidExpressionException {
            if (peek() == '-') {
                pos++;
                return -parseUnary();
            }
            return parsePower();
        }

        private double parsePower() throws InvalidExpressionException {
            double base = parsePrimary();
            if (peek() == '^') {
                pos++;
                return Math.pow(base, parseUnary());
            }
            return base;
        }

        private double parsePrimary() throws InvalidExpressionException {
            char c = peek();
            if (c == '(') {
                int open = pos++;
                double value = parseExpression();
                if (peek() != ')') {
                    throw new InvalidExpressionException("Missing closing parenthesis",
                            open, open + 1);
                }
                pos++;
                return value;
            }
            if (c == '√') {
                pos++;
                double radicand = parseUnary();
                if (radicand < 0) {
                    throw InvalidExpressionException.undefined("Square root of a negative number");
                }
                return Math.sqrt(radicand);
            }
            if (c == 'π') {
                pos++;
                piCount++;
                return Math.PI;
            }
            if (Character.isDigit(c) || c == '.') {
                return parseNumber();
            }
            if (Character.isLetter(c)) {
                return parseFunction();
            }
            if (c == END) {
                // the last symbol is the one left without what should follow it
                int last = input.length() - 1;
                throw new InvalidExpressionException("Expression ended unexpectedly",
                        last, last < 0 ? -1 : input.length());
            }
            throw new InvalidExpressionException("Unexpected symbol '" + c + "'", pos, pos + 1);
        }

        private double parseFunction() throws InvalidExpressionException {
            int start = pos;
            String name = nameAt(input, start);
            if (name == null) {
                // π is a letter to Java, but never part of a name: "sinπ" is sin(π)
                while (Character.isLetter(peek()) && peek() != 'π') {
                    pos++;
                }
                throw new InvalidExpressionException(
                        "Unknown function '" + input.substring(start, pos) + "'", start, pos);
            }
            pos += name.length();
            if (name.equals(ANS)) {
                if (ans == null) {
                    throw new InvalidExpressionException("ans needs a solved line above it",
                            start, pos);
                }
                return ans;
            }
            int piBefore = piCount;
            double argument = peek() == '(' ? parsePrimary() : parsePower();
            boolean inRadians = piCount > piBefore;
            double radians = inRadians ? argument : Math.toRadians(argument);
            switch (name) {
                case "sin":
                    return Math.sin(radians);
                case "cos":
                    return Math.cos(radians);
                default:
                    if (Math.abs(Math.cos(radians)) < 1e-12) {
                        throw InvalidExpressionException.undefined(
                                "tan(" + format(argument) + ") is undefined");
                    }
                    return Math.tan(radians);
            }
        }

        private double parseNumber() throws InvalidExpressionException {
            int start = pos;
            while (Character.isDigit(peek()) || peek() == '.') {
                pos++;
            }
            String number = input.substring(start, pos);
            try {
                return Double.parseDouble(number);
            } catch (NumberFormatException e) {
                throw new InvalidExpressionException("Invalid number '" + number + "'",
                        start, pos);
            }
        }

        void expectEnd() throws InvalidExpressionException {
            if (pos < input.length()) {
                char c = input.charAt(pos);
                if (c == ')') {
                    throw new InvalidExpressionException("Unmatched closing parenthesis",
                            pos, pos + 1);
                }
                throw new InvalidExpressionException("Unexpected symbol '" + c + "'",
                        pos, pos + 1);
            }
        }

        private char peek() {
            return pos < input.length() ? input.charAt(pos) : END;
        }

        private static boolean startsPrimary(char c) {
            return c == '(' || c == '√' || c == 'π' || Character.isLetter(c)
                    || Character.isDigit(c);
        }

        private static boolean isMultiply(char c) {
            return c == '×' || c == '*';
        }

        private static boolean isDivide(char c) {
            return c == '÷' || c == '/';
        }

        private static String format(double value) {
            return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
        }
    }
}
