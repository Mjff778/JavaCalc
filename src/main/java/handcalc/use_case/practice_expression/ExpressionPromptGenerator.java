package handcalc.use_case.practice_expression;

import java.util.Map;
import java.util.Random;

/**
 * Makes up random expressions to write. Several are drawn each time and the one that uses
 * the least-collected symbols wins, so practice keeps the collection balanced.
 */
public class ExpressionPromptGenerator {

    private static final int CANDIDATES = 8;
    private static final String OPERATORS = "+-×÷";
    private static final String[] FUNCTIONS = {"sin", "cos", "tan"};

    private final Random random;
    private final boolean hasPi;

    /** @param symbols every symbol the application knows; letters and π are used if present */
    public ExpressionPromptGenerator(Random random, String symbols) {
        this.random = random;
        this.hasPi = symbols.indexOf('π') >= 0;
    }

    /** @param counts samples collected so far per symbol; missing symbols count as zero */
    public ExpressionPrompt generate(Map<Character, Integer> counts) {
        ExpressionPrompt best = null;
        double bestScore = -1;
        for (int i = 0; i < CANDIDATES; i++) {
            ExpressionPrompt candidate = candidate();
            // the average rarity, so that a long expression is not preferred just for length
            double score = 0;
            for (char symbol : candidate.getSymbols().toCharArray()) {
                score += 1.0 / (1 + counts.getOrDefault(symbol, 0));
            }
            score /= candidate.getSymbols().length();
            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    private ExpressionPrompt candidate() {
        int pick = random.nextInt(10);
        if (pick < 3) {
            return ExpressionPrompt.line(lineText());
        }
        if (pick < 5) {
            String base = random.nextInt(3) == 0
                    ? "(" + number() + OPERATORS.charAt(random.nextInt(2)) + digit() + ")"
                    : String.valueOf(digit());
            return ExpressionPrompt.power(base, String.valueOf(2 + random.nextInt(3)));
        }
        if (pick < 7) {
            return ExpressionPrompt.fraction(number(), number());
        }
        if (pick < 8) {
            return ExpressionPrompt.root(number());
        }
        return ExpressionPrompt.trig(FUNCTIONS[random.nextInt(FUNCTIONS.length)]
                + "(" + number() + ")");
    }

    private String lineText() {
        StringBuilder text = new StringBuilder();
        if (random.nextInt(4) == 0) {
            text.append('(').append(operand()).append(OPERATORS.charAt(random.nextInt(2)))
                    .append(operand()).append(')');
        } else {
            text.append(operand());
        }
        int more = 1 + random.nextInt(2);
        for (int i = 0; i < more; i++) {
            text.append(OPERATORS.charAt(random.nextInt(OPERATORS.length()))).append(operand());
        }
        if (random.nextInt(3) == 0) {
            text.append('=');
        }
        return text.toString();
    }

    private String operand() {
        if (hasPi && random.nextInt(8) == 0) {
            return "π";
        }
        return number();
    }

    private String number() {
        return random.nextInt(3) == 0 ? "" + (1 + random.nextInt(9)) + random.nextInt(10)
                : String.valueOf(1 + random.nextInt(9));
    }

    private int digit() {
        return 1 + random.nextInt(9);
    }
}
