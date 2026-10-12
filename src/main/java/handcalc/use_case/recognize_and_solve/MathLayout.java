package handcalc.use_case.recognize_and_solve;

import handcalc.entity.Glyph;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Turns recognized symbols and where they sit on the page into a one-line expression:
 * <ul>
 *   <li>a bar with symbols above <em>and</em> below it is a fraction, written {@code (a÷b)};</li>
 *   <li>a root sign covering symbols is a square root, written {@code √a};</li>
 *   <li>a smaller symbol raised at the upper right of a number, bracket, fraction or root is
 *       an exponent, written {@code base^exp};</li>
 *   <li>everything else is read left to right.</li>
 * </ul>
 * Letters that almost spell sin, cos or tan are corrected, because "c" looks like "(",
 * "o" like "0" and "s" like "5".
 */
public final class MathLayout {

    private static final String[] FUNCTIONS = {"sin", "cos", "tan"};
    /** For each function, the characters each letter may have been mistaken for. */
    private static final String[][] LOOK_ALIKES = {
        {"s5", "i1", "n"},
        {"c(", "o0", "s5"},
        {"t+", "a", "n"},
    };

    private MathLayout() {
    }

    /** @param readings the recognized symbols with their position on the page */
    public static String toLinear(List<SymbolReading> readings) {
        List<Node> nodes = new ArrayList<>();
        for (SymbolReading reading : readings) {
            nodes.add(Node.symbol(reading.getSymbol(), reading.getGlyph()));
        }
        return repairFunctionNames(linear(group(nodes)));
    }

    // ---- structure: fractions and roots --------------------------------------------------

    /** Collapses fractions and roots into single nodes, widest (outermost) first. */
    private static List<Node> group(List<Node> items) {
        List<Node> remaining = new ArrayList<>(items);
        List<Node> containers = new ArrayList<>();
        for (Node node : items) {
            if (node.symbol == '-' || node.symbol == '√') {
                containers.add(node);
            }
        }
        containers.sort(Comparator.comparingDouble((Node n) -> n.maxX - n.minX).reversed());

        List<Node> composites = new ArrayList<>();
        for (Node container : containers) {
            if (!remaining.contains(container)) {
                continue;
            }
            Node composite = container.symbol == '-'
                    ? asFraction(container, remaining) : asRadical(container, remaining);
            if (composite != null) {
                composites.add(composite);
            }
        }
        remaining.addAll(composites);
        remaining.sort(Comparator.comparingDouble(n -> n.minX));
        return remaining;
    }

    private static Node asFraction(Node bar, List<Node> remaining) {
        List<Node> above = new ArrayList<>();
        List<Node> below = new ArrayList<>();
        for (Node n : remaining) {
            if (n == bar || n.centreX() < bar.minX || n.centreX() > bar.maxX) {
                continue;
            }
            (n.centreY() < bar.centreY() ? above : below).add(n);
        }
        if (above.isEmpty() || below.isEmpty()) {
            return null;
        }
        remaining.remove(bar);
        remaining.removeAll(above);
        remaining.removeAll(below);
        String text = "(" + wrap(linear(group(above))) + "÷" + wrap(linear(group(below))) + ")";
        return Node.composite(text, Node.Kind.FRACTION, bar, above, below);
    }

    private static Node asRadical(Node root, List<Node> remaining) {
        List<Node> inside = new ArrayList<>();
        for (Node n : remaining) {
            if (n != root && n.centreX() > root.minX && n.centreX() < root.maxX
                    && n.centreY() > root.minY && n.centreY() < root.maxY) {
                inside.add(n);
            }
        }
        if (inside.isEmpty()) {
            return null;
        }
        remaining.remove(root);
        remaining.removeAll(inside);
        String text = "√" + wrap(linear(group(inside)));
        return Node.composite(text, Node.Kind.RADICAL, root, inside, new ArrayList<>());
    }

    // ---- one line: numbers, exponents ----------------------------------------------------

    /** Reads grouped nodes (sorted left to right) as a single line. */
    private static String linear(List<Node> nodes) {
        List<Token> tokens = new ArrayList<>();
        int i = 0;
        while (i < nodes.size()) {
            Node node = nodes.get(i);
            Token last = tokens.isEmpty() ? null : tokens.get(tokens.size() - 1);
            // "=", "+", "×", "÷" and ")" can sit inside an exponent but never begin one
            boolean mayBegin = !(node.kind == Node.Kind.SYMBOL
                    && "=+×÷)".indexOf(node.symbol) >= 0);
            if (last != null && mayBegin && last.canTakeExponent() && last.isBaseFor(node)) {
                List<Node> exponent = new ArrayList<>();
                int j = i;
                while (j < nodes.size() && last.isBaseFor(nodes.get(j))) {
                    exponent.add(nodes.get(j));
                    j++;
                }
                last.attachExponent(wrap(linear(exponent)), exponent);
                i = j;
                continue;
            }
            if (last != null && last.kind == Token.Kind.NUMBER && !last.hasExponent
                    && node.isDigitOrPoint()) {
                last.extend(node);
            } else {
                tokens.add(Token.of(node));
            }
            i++;
        }
        StringBuilder text = new StringBuilder();
        for (Token token : tokens) {
            // "2³" then "4", or "√4" then "5": written side by side they would read as one
            // number ("2^34", "√45"), so the multiplication is made explicit
            if (text.length() > 0 && isDigitLike(text.charAt(text.length() - 1))
                    && isDigitLike(token.text.charAt(0))) {
                text.append('×');
            }
            text.append(token.text);
        }
        return text.toString();
    }

    // ---- helpers -------------------------------------------------------------------------

    /** Puts brackets around anything that is more than a single number, symbol or group. */
    private static String wrap(String text) {
        return isAtomic(text) ? text : "(" + text + ")";
    }

    private static boolean isAtomic(String text) {
        if (text.length() <= 1) {
            return true;
        }
        String body = text.startsWith("√") ? text.substring(1) : text;
        if (body.matches("[0-9.]+")) {
            return true;
        }
        if (!text.startsWith("(") || !text.endsWith(")")) {
            return false;
        }
        int depth = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            depth += c == '(' ? 1 : c == ')' ? -1 : 0;
            if (depth == 0 && i < text.length() - 1) {
                return false;
            }
        }
        return true;
    }

    /**
     * Replaces runs that nearly spell sin, cos or tan by the function name, then fixes what
     * the context gives away: a "c" right after a function name is its opening bracket, and
     * an "o", "i" or "s" touching a digit is a 0, 1 or 5.
     */
    private static String repairFunctionNames(String text) {
        StringBuilder result = new StringBuilder();
        List<Boolean> inName = new ArrayList<>();
        int i = 0;
        while (i < text.length()) {
            String name = null;
            for (int f = 0; f < FUNCTIONS.length && name == null; f++) {
                if (i + 3 <= text.length() && matches(text, i, LOOK_ALIKES[f], FUNCTIONS[f])) {
                    name = FUNCTIONS[f];
                }
            }
            if (name != null) {
                result.append(name);
                for (int k = 0; k < 3; k++) {
                    inName.add(true);
                }
                i += 3;
                if (i < text.length() && text.charAt(i) == 'c') {
                    result.append('(');
                    inName.add(false);
                    i++;
                }
            } else {
                result.append(text.charAt(i));
                inName.add(false);
                i++;
            }
        }

        for (int k = 0; k < result.length(); k++) {
            if (inName.get(k)) {
                continue;
            }
            int digit = "ois".indexOf(result.charAt(k));
            boolean nextToDigit = (k > 0 && isDigitLike(result.charAt(k - 1)))
                    || (k + 1 < result.length() && isDigitLike(result.charAt(k + 1)));
            if (digit >= 0 && nextToDigit) {
                result.setCharAt(k, "015".charAt(digit));
            }
        }
        return result.toString();
    }

    private static boolean isDigitLike(char c) {
        return Character.isDigit(c) || c == '.';
    }

    /**
     * Is the text at {@code start} the function {@code name} with some letters misread as
     * their look-alikes? A letter can only be part of a function name, so a single correctly
     * read letter is enough to recognise the name ("c05" is cos); digits and brackets alone
     * ("(05") are taken as written.
     */
    private static boolean matches(String text, int start, String[] allowed, String name) {
        int exactLetters = 0;
        for (int k = 0; k < 3; k++) {
            char c = text.charAt(start + k);
            if (allowed[k].indexOf(c) < 0) {
                return false;
            }
            if (c == name.charAt(k)) {
                exactLetters++;
            }
        }
        return exactLetters >= 1;
    }

    // ---- value types ---------------------------------------------------------------------

    /** A symbol or a collapsed fraction/root, with the box it covers. */
    private static final class Node {
        enum Kind { SYMBOL, FRACTION, RADICAL }

        final Kind kind;
        final char symbol;
        final String text;
        final double minX;
        final double maxX;
        final double minY;
        final double maxY;

        private Node(Kind kind, char symbol, String text, double minX, double maxX,
                     double minY, double maxY) {
            this.kind = kind;
            this.symbol = symbol;
            this.text = text;
            this.minX = minX;
            this.maxX = maxX;
            this.minY = minY;
            this.maxY = maxY;
        }

        static Node symbol(char symbol, Glyph glyph) {
            return new Node(Kind.SYMBOL, symbol, String.valueOf(symbol), glyph.getMinX(),
                    glyph.getMaxX(), glyph.getMinY(), glyph.getMaxY());
        }

        static Node composite(String text, Kind kind, Node head, List<Node> first,
                              List<Node> second) {
            double minX = head.minX;
            double maxX = head.maxX;
            double minY = head.minY;
            double maxY = head.maxY;
            List<Node> all = new ArrayList<>(first);
            all.addAll(second);
            for (Node n : all) {
                minX = Math.min(minX, n.minX);
                maxX = Math.max(maxX, n.maxX);
                minY = Math.min(minY, n.minY);
                maxY = Math.max(maxY, n.maxY);
            }
            return new Node(kind, '\0', text, minX, maxX, minY, maxY);
        }

        double centreX() {
            return (minX + maxX) / 2;
        }

        double centreY() {
            return (minY + maxY) / 2;
        }

        boolean isDigitOrPoint() {
            return kind == Kind.SYMBOL && (Character.isDigit(symbol) || symbol == '.');
        }
    }

    /** A piece of the output line that remembers where it was written. */
    private static final class Token {
        enum Kind { NUMBER, CONSTANT, FRACTION, RADICAL, CLOSE, OTHER }

        Kind kind;
        String text;
        double minX;
        double maxX;
        double minY;
        double maxY;
        boolean hasExponent;

        static Token of(Node node) {
            Token t = new Token();
            t.text = node.text;
            t.minX = node.minX;
            t.maxX = node.maxX;
            t.minY = node.minY;
            t.maxY = node.maxY;
            if (node.kind == Node.Kind.FRACTION) {
                t.kind = Kind.FRACTION;
            } else if (node.kind == Node.Kind.RADICAL) {
                t.kind = Kind.RADICAL;
            } else if (node.isDigitOrPoint()) {
                t.kind = Kind.NUMBER;
            } else if (node.kind == Node.Kind.SYMBOL && node.symbol == 'π') {
                t.kind = Kind.CONSTANT;
            } else if (node.symbol == ')') {
                t.kind = Kind.CLOSE;
            } else {
                t.kind = Kind.OTHER;
            }
            return t;
        }

        void extend(Node node) {
            text += node.text;
            minY = Math.min(minY, node.minY);
            maxY = Math.max(maxY, node.maxY);
            maxX = Math.max(maxX, node.maxX);
        }

        boolean canTakeExponent() {
            return !hasExponent
                    && (kind == Kind.NUMBER || kind == Kind.CONSTANT || kind == Kind.FRACTION
                    || kind == Kind.RADICAL || kind == Kind.CLOSE);
        }

        /** Is {@code node} written as a superscript of this token? */
        boolean isBaseFor(Node node) {
            double height = maxY - minY;
            if (height <= 0) {
                return false;
            }
            double nodeHeight = node.maxY - node.minY;
            boolean toTheRight = node.minX >= maxX - 0.15 * height
                    && node.minX - maxX <= 1.5 * height;
            // How far the symbol's centre sits above the base's centre, and how big it is
            // next to the base: the more clearly raised it is, the less small it must be.
            double rise = ((minY + maxY) / 2 - node.centreY()) / height;
            double size = nodeHeight / height;
            boolean exponent = (rise >= 0.25 && size <= 0.95)
                    || (rise >= 0.15 && size <= 0.85)
                    || (rise >= 0.1 && size <= 0.7);
            return toTheRight && exponent;
        }

        void attachExponent(String exponent, List<Node> nodes) {
            String base = kind == Kind.RADICAL ? "(" + text + ")" : text;
            text = base + "^" + exponent;
            for (Node n : nodes) {
                maxX = Math.max(maxX, n.maxX);
            }
            hasExponent = true;
        }
    }
}
