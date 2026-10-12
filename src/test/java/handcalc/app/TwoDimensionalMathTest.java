package handcalc.app;

import static org.junit.jupiter.api.Assertions.assertTrue;

import handcalc.data_access.SyntheticSamples;
import handcalc.entity.Glyph;
import handcalc.entity.Point;
import handcalc.entity.Stroke;
import handcalc.interface_adapter.CalculatorState;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Whole-pipeline checks of fractions, powers, roots and trigonometry written in 2D.
 *
 * <p>The handwriting is synthetic (distorted templates), so each scene is tried with ten
 * different random "hands" and must come out right for at least eight of them.
 */
class TwoDimensionalMathTest {

    private static final int HANDS = 10;
    private static final int REQUIRED = 8;
    private static final String SMALL_LETTERS = "scnoa";

    @TempDir
    static Path dir;

    private static CalculatorApp app;

    @BeforeAll
    static void startApp() {
        app = new CalculatorApp(dir.resolve("samples.txt"));
    }

    /** Places hand-like symbols left to right, bottoms aligned, with a gap between them. */
    private static final class Line {
        final List<Stroke> strokes = new ArrayList<>();
        final Random random;
        final double bottom;
        final double height;
        double x;

        Line(double x, double bottom, double height, Random random) {
            this.x = x;
            this.bottom = bottom;
            this.height = height;
            this.random = random;
        }

        /** @return the right edge of the symbol */
        double put(char symbol, double symbolHeight, Double width) {
            return put(symbol, symbolHeight, width, bottom);
        }

        double put(char symbol, double symbolHeight, Double width, double symbolBottom) {
            Glyph glyph = SyntheticSamples.randomGlyph(symbol, random);
            double glyphWidth = glyph.getMaxX() - glyph.getMinX();
            double glyphHeight = glyph.getMaxY() - glyph.getMinY();
            double scaleY = glyphHeight > 0 ? symbolHeight / glyphHeight : 1;
            double scaleX = width != null && glyphWidth > 0 ? width / glyphWidth : scaleY;
            double top = symbolBottom - glyphHeight * scaleY;
            for (Stroke stroke : glyph.getStrokes()) {
                List<Point> points = new ArrayList<>();
                for (Point p : stroke.getPoints()) {
                    points.add(new Point(x + (p.getX() - glyph.getMinX()) * scaleX,
                            top + (p.getY() - glyph.getMinY()) * scaleY));
                }
                strokes.add(new Stroke(points));
            }
            double right = x + glyphWidth * scaleX;
            x = right + 16;
            return right;
        }

        void text(String text) {
            for (char c : text.toCharArray()) {
                if (c == '=') {
                    // an equals sign is short and sits in the middle of the line
                    double short_ = height * 0.4;
                    put(c, short_, null, bottom - (height - short_) / 2);
                } else {
                    put(c, SMALL_LETTERS.indexOf(c) >= 0 ? height * 0.6 : height, null);
                }
            }
        }
    }

    private static List<Stroke> concat(List<Stroke>... parts) {
        List<Stroke> all = new ArrayList<>();
        for (List<Stroke> part : parts) {
            all.addAll(part);
        }
        return all;
    }

    /** A nearly level bar from x to x+width at height y. */
    private static List<Stroke> bar(double x, double y, double width, Random random) {
        double tilt = Math.toRadians(random.nextDouble() * 4 - 2);
        List<Point> points = new ArrayList<>();
        for (int k = 0; k <= 20; k++) {
            double px = x + width * k / 20;
            points.add(new Point(px, y + Math.tan(tilt) * (px - x) + random.nextGaussian() * 0.6));
        }
        List<Stroke> strokes = new ArrayList<>();
        strokes.add(new Stroke(points));
        return strokes;
    }

    private static void assertReads(String expectedExpression, String expectedResult,
                                    Function<Random, List<Stroke>> scene) {
        int correct = 0;
        StringBuilder seen = new StringBuilder();
        for (int seed = 0; seed < HANDS; seed++) {
            app.getController().execute(scene.apply(new Random(seed)));
            CalculatorState state = app.getViewModel().getState();
            if (expectedExpression.equals(state.getExpression())
                    && expectedResult.equals(state.getResult())) {
                correct++;
            } else {
                seen.append(state.getExpression()).append(" | ");
            }
        }
        assertTrue(correct >= REQUIRED, "only " + correct + "/" + HANDS + " read as "
                + expectedExpression + " = " + expectedResult + "; instead: " + seen);
    }

    @Test
    void fractionIsReadFromTheBarWithNumbersAboveAndBelow() {
        assertReads("(3÷4)+1", "1.75", random -> {
            Line numerator = new Line(30, 60, 36, random);
            numerator.put('3', 36, null);
            Line denominator = new Line(30, 112, 36, random);
            denominator.put('4', 36, null);
            Line rest = new Line(115, 100, 50, random);
            rest.text("+1");
            return concat(numerator.strokes, bar(10, 70, 80, random), denominator.strokes,
                    rest.strokes);
        });
    }

    @Test
    void raisedSmallNumberIsAPower() {
        assertReads("2^3", "8", random -> {
            Line base = new Line(10, 100, 50, random);
            double right = base.put('2', 50, null);
            Line exponent = new Line(right + 4, 58, 24, random);
            exponent.put('3', 24, null);
            return concat(base.strokes, exponent.strokes);
        });
    }

    @Test
    void exponentAppliesToTheWholeNumber() {
        assertReads("12^2", "144", random -> {
            Line base = new Line(10, 100, 50, random);
            base.put('1', 50, null);
            double right = base.put('2', 50, null);
            Line exponent = new Line(right + 4, 58, 24, random);
            exponent.put('2', 24, null);
            return concat(base.strokes, exponent.strokes);
        });
    }

    @Test
    void squareOfABracket() {
        assertReads("(1+2)^2", "9", random -> {
            Line base = new Line(10, 100, 50, random);
            base.text("(1+2");
            double right = base.put(')', 50, null);
            Line exponent = new Line(right + 4, 58, 24, random);
            exponent.put('2', 24, null);
            return concat(base.strokes, exponent.strokes);
        });
    }

    @Test
    void rootSignCoversItsNumber() {
        assertReads("√81", "9", random -> {
            Line digits = new Line(60, 100, 45, random);
            digits.text("81");
            double end = digits.x - 6;
            Line root = new Line(10, 100, 70, random);
            root.put('√', 70, end - 10);
            return concat(digits.strokes, root.strokes);
        });
    }

    @Test
    void piMultipliedByTwo() {
        assertReads("2π", "6.28318531", random -> {
            Line line = new Line(10, 100, 50, random);
            line.text("2π");
            return line.strokes;
        });
    }

    @Test
    void sineInDegrees() {
        assertReads("sin(30)", "0.5", random -> {
            Line line = new Line(10, 100, 50, random);
            line.text("sin(30)");
            return line.strokes;
        });
    }

    @Test
    void cosineInDegrees() {
        assertReads("cos(60)", "0.5", random -> {
            Line line = new Line(10, 100, 50, random);
            line.text("cos(60)");
            return line.strokes;
        });
    }

    @Test
    void decimalPoint() {
        assertReads("1.5+2", "3.5", random -> {
            Line line = new Line(10, 100, 50, random);
            line.put('1', 50, null);
            line.x -= 8;
            line.put('0', 6, 6.0);
            line.x += 4;
            line.text("5+2");
            return line.strokes;
        });
    }

    @Test
    void bracketsAndATrailingEqualsSign() {
        assertReads("3×(4+5)", "27", random -> {
            Line line = new Line(10, 100, 50, random);
            line.text("3×(4+5)=");
            return line.strokes;
        });
    }
}
