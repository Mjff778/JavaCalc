package handcalc.use_case;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import handcalc.entity.Glyph;
import handcalc.entity.Point;
import handcalc.entity.Stroke;
import handcalc.use_case.recognize_and_solve.GlyphSegmenter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class GlyphSegmenterTest {

    private final GlyphSegmenter segmenter = new GlyphSegmenter();

    private static Stroke line(double x1, double y1, double x2, double y2) {
        return new Stroke(Arrays.asList(new Point(x1, y1), new Point(x2, y2)));
    }

    private static Stroke dot(double x, double y) {
        return new Stroke(Collections.singletonList(new Point(x, y)));
    }

    @Test
    void noStrokesGiveNoGlyphs() {
        assertTrue(segmenter.segment(Collections.emptyList()).isEmpty());
    }

    @Test
    void crossingStrokesFormOneGlyph() {
        Stroke horizontal = line(0, 50, 100, 50);
        Stroke vertical = line(50, 0, 50, 100);
        List<Glyph> glyphs = segmenter.segment(Arrays.asList(vertical, horizontal));
        assertEquals(1, glyphs.size());
        assertEquals(2, glyphs.get(0).getStrokes().size());
    }

    @Test
    void divisionSignWithTwoDotsIsOneGlyph() {
        List<Glyph> glyphs = segmenter.segment(
                Arrays.asList(line(0, 50, 100, 50), dot(50, 20), dot(50, 80)));
        assertEquals(1, glyphs.size());
        assertEquals(3, glyphs.get(0).getStrokes().size());
    }

    @Test
    void separatedStrokesAreOrderedLeftToRightRegardlessOfDrawingOrder() {
        Stroke right = line(200, 0, 200, 100);
        Stroke left = line(0, 0, 0, 100);
        Stroke middle = line(100, 0, 100, 100);
        List<Glyph> glyphs = segmenter.segment(Arrays.asList(right, left, middle));
        assertEquals(3, glyphs.size());
        assertEquals(0, glyphs.get(0).getMinX());
        assertEquals(100, glyphs.get(1).getMinX());
        assertEquals(200, glyphs.get(2).getMinX());
    }

    @Test
    void slightlyOverlappingNeighboursStaySeparate() {
        Stroke first = new Stroke(Arrays.asList(new Point(0, 0), new Point(40, 100)));
        Stroke second = new Stroke(Arrays.asList(new Point(38, 0), new Point(80, 100)));
        assertEquals(2, segmenter.segment(Arrays.asList(first, second)).size());
    }

    private static Stroke polyline(double... xy) {
        java.util.List<Point> points = new java.util.ArrayList<>();
        for (int i = 0; i < xy.length; i += 2) {
            points.add(new Point(xy[i], xy[i + 1]));
        }
        return new Stroke(points);
    }

    @Test
    void fractionBarStaysSeparateFromNumeratorAndDenominator() {
        Stroke bar = line(0, 100, 100, 100);
        Stroke numerator = line(50, 40, 50, 80);
        Stroke denominator = polyline(40, 120, 60, 120, 40, 160, 60, 160);

        List<Glyph> glyphs = segmenter.segment(Arrays.asList(numerator, bar, denominator));

        assertEquals(3, glyphs.size());
    }

    @Test
    void radicalSignDoesNotSwallowTheNumberUnderItsBar() {
        Stroke radical = polyline(0, 60, 10, 50, 25, 100, 40, 0, 150, 0);
        Stroke digit = polyline(70, 30, 70, 80);

        List<Glyph> glyphs = segmenter.segment(Arrays.asList(radical, digit));

        assertEquals(2, glyphs.size());
    }

    @Test
    void twoStackedBarsFormAnEqualsSign() {
        List<Glyph> glyphs = segmenter.segment(
                Arrays.asList(line(0, 0, 60, 0), line(5, 25, 55, 25)));

        assertEquals(1, glyphs.size());
    }

    @Test
    void aSmallDotBesideADigitIsADecimalPointNotPartOfTheDigit() {
        Stroke three = polyline(0, 0, 20, 0, 5, 20, 20, 40, 0, 40);
        Stroke point = dot(32, 38);
        Stroke five = polyline(50, 0, 70, 0, 50, 40);

        List<Glyph> glyphs = segmenter.segment(Arrays.asList(three, point, five));

        assertEquals(3, glyphs.size());
    }

    @Test
    void theDotOfALowercaseIBelongsToItsStem() {
        List<Glyph> glyphs = segmenter.segment(
                Arrays.asList(line(50, 20, 50, 60), dot(50, 5)));

        assertEquals(1, glyphs.size());
        assertEquals(2, glyphs.get(0).getStrokes().size());
    }

    @Test
    void strokesThatAlmostTouchBelongTogether() {
        Stroke top = line(0, 0, 30, 0);
        Stroke body = polyline(2, 3, 0, 20, 25, 25);

        assertEquals(1, segmenter.segment(Arrays.asList(top, body)).size());
    }

    @Test
    void aSingleDotAboveABarIsNotADivisionSign() {
        List<Glyph> glyphs = segmenter.segment(
                Arrays.asList(line(0, 50, 100, 50), dot(50, 20)));

        assertEquals(2, glyphs.size());
    }

    @Test
    void numeratorWrittenCloseAboveTheBarStaysSeparate() {
        Stroke bar = line(0, 100, 100, 100);
        Stroke numerator = polyline(40, 60, 60, 60, 45, 78, 60, 96);
        Stroke denominator = polyline(40, 104, 60, 104, 40, 140, 60, 140);

        assertEquals(3, segmenter.segment(Arrays.asList(bar, numerator, denominator)).size());
    }

    @Test
    void aTopBarTouchingTheStemOfAFiveStillBelongsToIt() {
        Stroke topBar = line(0, 0, 30, 0);
        Stroke body = polyline(0, 0, 0, 14, 20, 16, 28, 30, 10, 40, 0, 36);

        assertEquals(1, segmenter.segment(Arrays.asList(topBar, body)).size());
    }

    @Test
    void aDigitCrampedAgainstTheRadicalSignStaysSeparate() {
        Stroke radical = polyline(0, 60, 10, 50, 25, 100, 40, 0, 150, 0);
        Stroke digit = polyline(40, 30, 40, 80);

        assertEquals(2, segmenter.segment(Arrays.asList(radical, digit)).size());
    }

    @Test
    void aTiltedFractionBarStillSeparatesFromADenominatorBelowIt() {
        Stroke bar = line(0, 70, 90, 87);
        Stroke denominator = polyline(30, 80, 30, 112, 45, 100, 60, 100);

        assertEquals(2, segmenter.segment(Arrays.asList(bar, denominator)).size());
    }

    @Test
    void aTopBarLyingAlongTheTopEdgeOfAFiveBelongsToItEvenWithASmallGap() {
        Stroke topBar = line(3, 1, 27, 4);
        Stroke body = polyline(0, 1, 0, 20, 20, 22, 30, 38, 10, 48, 0, 44);

        assertEquals(1, segmenter.segment(Arrays.asList(topBar, body)).size());
    }

    @Test
    void shortStackedBarsStillFormAnEqualsSign() {
        List<Glyph> glyphs = segmenter.segment(
                Arrays.asList(line(0, 0, 17, 3), line(2, 18, 18, 20)));

        assertEquals(1, glyphs.size());
    }

    @Test
    void closeButNotOverlappingNeighboursAreSeparateSymbols() {
        // two digits 3px apart: closer than the "almost touching" distance, but side by side
        List<Glyph> glyphs = new GlyphSegmenter().segment(Arrays.asList(
                line(0, 0, 0, 50), line(3, 0, 3, 50)));

        assertEquals(2, glyphs.size());
    }

    @Test
    void aSmallRaisedPowerCloseToItsBaseIsNotMerged() {
        List<Glyph> glyphs = new GlyphSegmenter().segment(Arrays.asList(
                line(0, 50, 30, 100), line(33, 20, 50, 50)));

        assertEquals(2, glyphs.size());
    }

    @Test
    void closeStrokesThatSitOnTopOfEachOtherStillFormOneSymbol() {
        // a stem a hair away from a slanted stroke, but inside its width
        List<Glyph> glyphs = segmenter.segment(Arrays.asList(
                line(0, 40, 25, 0), line(20, 12, 20, 50)));

        assertEquals(1, glyphs.size());
    }

    @Test
    void legsHangingFromABarWithNothingAboveFormOneSymbol() {
        // a pi: wide bar with two legs touching it
        List<Glyph> glyphs = segmenter.segment(Arrays.asList(
                line(0, 0, 50, 0), line(14, 0, 12, 50), line(36, 0, 38, 50)));

        assertEquals(1, glyphs.size());
    }

    @Test
    void aNumberUnderABarWithANumeratorAboveStaysAFraction() {
        List<Glyph> glyphs = segmenter.segment(Arrays.asList(
                line(20, 0, 20, 30), line(0, 40, 50, 40), line(20, 50, 20, 90)));

        assertEquals(3, glyphs.size());
    }

    @Test
    void smallPlusBetweenTallDigitsIsOneSymbol() {
        List<Glyph> glyphs = segmenter.segment(Arrays.asList(
                line(10, 0, 10, 100),
                line(40, 50, 60, 50), line(50, 40, 50, 60),
                line(90, 0, 90, 100)));

        assertEquals(3, glyphs.size());
        assertEquals(2, glyphs.get(1).getStrokes().size());
    }

    @Test
    void smallEqualsBetweenTallDigitsIsOneSymbol() {
        List<Glyph> glyphs = segmenter.segment(Arrays.asList(
                line(10, 0, 10, 100),
                line(40, 45, 60, 45), line(40, 55, 60, 55),
                line(90, 0, 90, 100)));

        assertEquals(3, glyphs.size());
        assertEquals(2, glyphs.get(1).getStrokes().size());
    }

    @Test
    void piWithLegsStartingAGapBelowTheBarIsOneSymbol() {
        // drawn with a mouse, the legs rarely touch the bar
        List<Glyph> glyphs = segmenter.segment(Arrays.asList(
                line(0, 0, 70, 0), line(18, 6, 16, 32), line(52, 5, 54, 30)));

        assertEquals(1, glyphs.size());
    }

    @Test
    void rootDrawnAsBarThenTickIsOneSymbolAndKeepsItsRadicandSeparate() {
        // the tick starts at the bar's left end, dips and rises again; the digits under the
        // bar are its radicand, not the legs of a pi
        List<Glyph> glyphs = segmenter.segment(Arrays.asList(
                line(40, 0, 160, 0),
                polyline(37, 4, 25, 60, 10, 40),
                line(80, 10, 80, 50),
                line(120, 10, 120, 50)));

        assertEquals(3, glyphs.size());
        assertEquals(2, glyphs.get(0).getStrokes().size());
    }

    @Test
    void aTwoWrittenTightlyBeforeAPiIsNotTakenForARootTick() {
        List<Glyph> glyphs = segmenter.segment(Arrays.asList(
                polyline(-35, 4, -20, 0, -8, 8, -10, 18, -34, 40, -6, 40),
                line(0, 0, 70, 0), line(18, 6, 16, 32), line(52, 5, 54, 30)));

        assertEquals(2, glyphs.size());
        assertEquals(3, glyphs.get(1).getStrokes().size());
    }

    @Test
    void aTwoStrokeSevenWrittenTightlyBeforeAPiIsNotTakenForARootTick() {
        List<Glyph> glyphs = segmenter.segment(Arrays.asList(
                line(-40, 0, -8, 0), line(-8, 1, -28, 40),
                line(0, 0, 70, 0), line(18, 6, 16, 32), line(52, 5, 54, 30)));

        assertEquals(2, glyphs.size());
        assertEquals(2, glyphs.get(0).getStrokes().size());
        assertEquals(3, glyphs.get(1).getStrokes().size());
    }

    @Test
    void aShortTopBarOfAFiveBesideATallBracketStillBelongsToIt() {
        // the bracket makes the line tall, so the top bar is small enough to pass for a dot
        Stroke bracket = polyline(30, 0, 5, 50, 30, 105);
        // the stem starts a little above the bar and crosses it, so the bar does not sit on top
        // of it as the dot of an "i" would
        Stroke body = polyline(60, 22, 58, 50, 80, 48, 92, 62, 85, 80, 62, 82);
        Stroke topBar = line(60, 26, 79, 26);

        List<Glyph> glyphs = segmenter.segment(Arrays.asList(bracket, body, topBar));

        assertEquals(2, glyphs.size());
        assertEquals(2, glyphs.get(1).getStrokes().size());
    }

    @Test
    void aFractionBarDoesNotTakeTheDigitBelowItsLeftEndForAFive() {
        Stroke numerator = line(50, 0, 50, 30);
        Stroke bar = line(0, 40, 100, 40);
        Stroke denominator = polyline(2, 44, 20, 44, 25, 55, 2, 85, 26, 85);

        assertEquals(3, segmenter.segment(Arrays.asList(numerator, bar, denominator)).size());
    }

    @Test
    void aPiBetweenTallBracketsKeepsItsShortLegs() {
        // "(2π)": brackets twice the height of the digit must not make the legs count as dots
        List<Glyph> glyphs = segmenter.segment(Arrays.asList(
                polyline(30, 0, 5, 60, 30, 120),
                line(45, 30, 45, 90),
                line(60, 40, 100, 40), line(70, 40, 68, 65), line(90, 40, 92, 65),
                polyline(115, 0, 140, 60, 115, 120)));

        assertEquals(4, glyphs.size());
        assertEquals(3, glyphs.get(2).getStrokes().size());
    }

    @Test
    void aPiOverAFractionBarIsNotAnEqualsSign() {
        // as written: π (bar and two legs), the fraction bar, and a 4 below it
        Stroke piBar = line(335, 309, 393, 309);
        Stroke fractionBar = line(333, 353, 395, 355);
        List<Glyph> glyphs = new GlyphSegmenter().segment(Arrays.asList(
                piBar, line(347, 310, 347, 329), line(377, 310, 377, 341),
                fractionBar, line(344, 364, 381, 387), line(367, 364, 368, 402)));

        Glyph bar = null;
        for (Glyph glyph : glyphs) {
            if (glyph.getStrokes().contains(fractionBar)) {
                bar = glyph;
            }
        }
        assertEquals(1, bar.getStrokes().size(), "the fraction bar stands alone");
    }

    @Test
    void twoBarsWithNothingBetweenThemAreStillAnEqualsSign() {
        List<Glyph> glyphs = new GlyphSegmenter().segment(Arrays.asList(
                line(614, 345, 639, 346), line(621, 360, 646, 361)));

        assertEquals(1, glyphs.size());
    }
}
