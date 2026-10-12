package handcalc.entity;

/**
 * An arithmetic expression such as {@code 12+3×(4-1)} or {@code 2sin(30)+√(9)^2}.
 *
 * <p>Supports {@code + - × ÷ ^}, parentheses, decimal numbers, unary minus, {@code √},
 * the constant {@code π}, the functions {@code sin cos tan} and implicit multiplication such
 * as {@code 2(3+4)} or {@code 2π}. Angles are in degrees, except that an argument which
 * contains {@code π} is taken as radians ({@code sin(π÷2)} is 1). The ASCII spellings {@code *} and {@code /} are accepted as well.
 */
public final class Expression {

    private final String text;

    public Expression(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public double evaluate() throws InvalidExpressionException {
        Parser parser = new Parser(text);
        double value = parser.parseExpression();
        parser.expectEnd();
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new InvalidExpressionException("Result is not a finite number");
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
     *               | name ( '(' expression ')' | power )
     * </pre>
     */
    private static final class Parser {
        private static final char END = '\0';

        private final String input;
        private int pos;
        private int piCount;

        Parser(String input) {
            this.input = input;
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
                        throw new InvalidExpressionException("Division by zero");
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
                pos++;
                double value = parseExpression();
                if (peek() != ')') {
                    throw new InvalidExpressionException("Missing closing parenthesis");
                }
                pos++;
                return value;
            }
            if (c == '√') {
                pos++;
                double radicand = parseUnary();
                if (radicand < 0) {
                    throw new InvalidExpressionException("Square root of a negative number");
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
                throw new InvalidExpressionException("Expression ended unexpectedly");
            }
            throw new InvalidExpressionException("Unexpected symbol '" + c + "'");
        }

        private double parseFunction() throws InvalidExpressionException {
            int start = pos;
            // π is a letter to Java, but never part of a name: "sinπ" is sin(π)
            while (Character.isLetter(peek()) && peek() != 'π') {
                pos++;
            }
            String name = input.substring(start, pos);
            if (!name.equals("sin") && !name.equals("cos") && !name.equals("tan")) {
                throw new InvalidExpressionException("Unknown function '" + name + "'");
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
                        throw new InvalidExpressionException(
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
                throw new InvalidExpressionException("Invalid number '" + number + "'");
            }
        }

        void expectEnd() throws InvalidExpressionException {
            if (pos < input.length()) {
                char c = input.charAt(pos);
                if (c == ')') {
                    throw new InvalidExpressionException("Unmatched closing parenthesis");
                }
                throw new InvalidExpressionException("Unexpected symbol '" + c + "'");
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
