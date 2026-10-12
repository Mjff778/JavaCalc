package handcalc.use_case.recognize_and_solve;

import handcalc.entity.Glyph;
import handcalc.entity.Point;
import handcalc.entity.Stroke;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Groups the pen strokes of a written line into symbols.
 *
 * <p>Strokes belong together when they
 * <ul>
 *   <li>cross or almost touch (the two strokes of "+", "×", "4", "t"),</li>
 *   <li>are two flat bars stacked closely (the two bars of "="), or</li>
 *   <li>are a tiny dot sitting above or below a stroke: the dots of "÷" (a bar with a dot on
 *       <em>both</em> sides) and the dot of "i".</li>
 * </ul>
 * Everything else stays separate, so a fraction bar does not swallow its numerator and
 * denominator and a root sign does not swallow its radicand; the layout of such separate
 * symbols is worked out later. Symbols are returned from left to right.
 */
public class GlyphSegmenter {

    private static final double TOUCH_RATIO = 0.15;
    private static final double TOUCH_EXACT = 0.02;
    private static final double MIN_OVERLAP_RATIO = 0.3;
    private static final double DOT_RATIO = 0.25;
    private static final double DOT_SIDEWAYS_TOLERANCE = 0.1;
    private static final double FLAT_RATIO = 0.3;
    /** With this many ordinary strokes on a line, dots are judged against their median size. */
    private static final int MEDIAN_MIN_STROKES = 3;
    /** How far below its bar each of two or more legs may start, relative to the leg's size. */
    private static final double LEG_GAP_RATIO = 0.35;
    /** How close a root sign's tick must start to the left end of its bar. */
    private static final double TICK_REACH_RATIO = 0.2;
    /** How much higher than its lowest point both ends of a root sign's tick must be. */
    private static final double TICK_RISE_RATIO = 0.05;

    public List<Glyph> segment(List<Stroke> strokes) {
        int n = strokes.size();
        int[] parent = new int[n];
        for (int i = 0; i < n; i++) {
            parent[i] = i;
        }
        double reference = referenceSize(strokes);

        boolean[] dot = new boolean[n];
        for (int i = 0; i < n; i++) {
            dot[i] = reference > 0 && size(strokes.get(i)) < DOT_RATIO * reference;
        }

        // small strokes may join each other (a "+" or "=" written small), but join a large
        // stroke only as a dot, below - unless the small stroke is a short bar, such as the top
        // of a "5" made tiny by tall brackets on the same line
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                boolean alike = dot[i] == dot[j]
                        || isFlat(dot[i] ? strokes.get(i) : strokes.get(j));
                if (alike && belongTogether(strokes.get(i), strokes.get(j))) {
                    union(parent, i, j);
                }
            }
        }
        attachLegsToBars(strokes, dot, parent);
        attachDots(strokes, dot, parent, reference);

        List<List<Stroke>> groups = new ArrayList<>();
        int[] groupOf = new int[n];
        java.util.Arrays.fill(groupOf, -1);
        for (int i = 0; i < n; i++) {
            int root = find(parent, i);
            if (groupOf[root] < 0) {
                groupOf[root] = groups.size();
                groups.add(new ArrayList<>());
            }
            groups.get(groupOf[root]).add(strokes.get(i));
        }
        List<Glyph> glyphs = new ArrayList<>();
        for (List<Stroke> group : groups) {
            glyphs.add(new Glyph(group));
        }
        glyphs.sort(Comparator.comparingDouble(Glyph::getMinX));
        return glyphs;
    }

    /**
     * Size of a typical full-height stroke; the yardstick for what counts as a dot. On a line
     * with several strokes this is their median size, so that tall brackets or a root sign do
     * not make the legs of a "π" or the top of a "5" look like dots.
     */
    private static double referenceSize(List<Stroke> strokes) {
        double tallest = 0;
        double longest = 0;
        List<Double> sizes = new ArrayList<>();
        for (Stroke stroke : strokes) {
            double size = size(stroke);
            longest = Math.max(longest, size);
            if (size > 0 && !isFlat(stroke)) {
                tallest = Math.max(tallest, size);
                sizes.add(size);
            }
        }
        if (sizes.size() >= MEDIAN_MIN_STROKES) {
            Collections.sort(sizes);
            return sizes.get(sizes.size() / 2);
        }
        return tallest > 0 ? tallest : longest;
    }

    private static boolean belongTogether(Stroke a, Stroke b) {
        if (isFlat(a) && isFlat(b) && stackedBars(a, b)) {
            return true;
        }
        if ((isFlat(a) && entirelyAboveOrBelow(b, a)) || (isFlat(b) && entirelyAboveOrBelow(a, b))) {
            // a numerator or denominator, however close to the fraction bar
            return false;
        }
        double smaller = Math.min(size(a), size(b));
        double tolerance = TOUCH_RATIO * smaller;
        if (encloses(a, b) || encloses(b, a)) {
            // e.g. a number under a root sign: it may only join by really crossing
            tolerance = TOUCH_EXACT * smaller;
        }
        double gap = distance(a, b);
        if (gap > tolerance) {
            return false;
        }
        // Really crossing strokes are one symbol. Merely close ones are only one symbol when
        // they sit on top of each other (the stem of a "4" or "t", the parts of an "a");
        // strokes side by side are neighbouring symbols or a power next to its base.
        return gap <= TOUCH_EXACT * smaller || overlapsHorizontally(a, b);
    }

    private static boolean overlapsHorizontally(Stroke a, Stroke b) {
        double overlap = Math.min(a.getMaxX(), b.getMaxX()) - Math.max(a.getMinX(), b.getMinX());
        double narrower = Math.min(a.getMaxX() - a.getMinX(), b.getMaxX() - b.getMinX());
        return overlap >= 0 && overlap >= MIN_OVERLAP_RATIO * narrower;
    }

    /**
     * Does {@code other} lie wholly on one side of the line the {@code bar} was drawn along,
     * and is the bar clearly wider than it? (The top bar of a "5" is about as wide as the rest
     * of the digit, a fraction bar is wider than its numerator and denominator.) The bar may
     * be tilted, so the comparison follows its slope rather than its bounding box.
     */
    private static boolean entirelyAboveOrBelow(Stroke other, Stroke bar) {
        double barWidth = bar.getMaxX() - bar.getMinX();
        double otherWidth = other.getMaxX() - other.getMinX();
        if (barWidth < 1.4 * otherWidth) {
            return false;
        }
        double[] fit = fitLine(bar);
        double margin = 0.03 * size(other);
        boolean above = true;
        boolean below = true;
        for (Point p : other.getPoints()) {
            double lineY = fit[0] * p.getX() + fit[1];
            above &= p.getY() < lineY - margin;
            below &= p.getY() > lineY + margin;
        }
        return above || below;
    }

    /** Least-squares line y = slope * x + intercept through the stroke's points. */
    private static double[] fitLine(Stroke stroke) {
        List<Point> points = stroke.getPoints();
        double n = points.size();
        double sumX = 0;
        double sumY = 0;
        double sumXY = 0;
        double sumXX = 0;
        for (Point p : points) {
            sumX += p.getX();
            sumY += p.getY();
            sumXY += p.getX() * p.getY();
            sumXX += p.getX() * p.getX();
        }
        double denominator = n * sumXX - sumX * sumX;
        double slope = denominator == 0 ? 0 : (n * sumXY - sumX * sumY) / denominator;
        return new double[] {slope, (sumY - slope * sumX) / n};
    }

    /**
     * Is {@code inner} well inside the bounding box of the much larger {@code outer}, clear of
     * its top, left and right edges? (A bar along the top of a "5" is not enclosed; a number
     * under the bar of a root sign is.)
     */
    private static boolean encloses(Stroke outer, Stroke inner) {
        double margin = 0.08 * size(outer);
        boolean inside = inner.getMinX() >= outer.getMinX() + margin
                && inner.getMaxX() <= outer.getMaxX() - margin
                && inner.getMinY() >= outer.getMinY() + margin
                && inner.getMaxY() <= outer.getMaxY() + 0.02 * size(outer);
        double outerArea = (outer.getMaxX() - outer.getMinX()) * (outer.getMaxY() - outer.getMinY());
        double innerArea = (inner.getMaxX() - inner.getMinX()) * (inner.getMaxY() - inner.getMinY());
        return inside && outerArea >= 2.5 * innerArea && size(outer) >= 1.5 * size(inner);
    }

    private static boolean stackedBars(Stroke a, Stroke b) {
        double widthA = a.getMaxX() - a.getMinX();
        double widthB = b.getMaxX() - b.getMinX();
        double wider = Math.max(widthA, widthB);
        double narrower = Math.min(widthA, widthB);
        if (narrower < 0.5 * wider) {
            return false;
        }
        double overlap = Math.min(a.getMaxX(), b.getMaxX()) - Math.max(a.getMinX(), b.getMinX());
        if (overlap < 0.6 * narrower) {
            return false;
        }
        double gap = Math.max(a.getMinY(), b.getMinY()) - Math.min(a.getMaxY(), b.getMaxY());
        return gap <= 1.2 * wider;
    }

    /**
     * The legs of a "π" (and the stroke of a two-stroke "7") hang from a wide bar. Anything
     * under a bar normally belongs to a fraction, but a fraction needs something above its bar
     * as well: with nothing above, strokes that touch the bar from below belong to it. A pen
     * moved by mouse often leaves a gap between the bar and the legs, so when two or more
     * strokes hang from the bar, as the legs of a "π" do, they may also start a little below it.
     */
    private static void attachLegsToBars(List<Stroke> strokes, boolean[] dot, int[] parent) {
        int n = strokes.size();
        for (int b = 0; b < n; b++) {
            Stroke bar = strokes.get(b);
            if (dot[b] || !isFlat(bar)) {
                continue;
            }
            int tick = rootTick(strokes, dot, b);
            if (tick >= 0) {
                // the bar of a root sign: what hangs below it is the radicand, not legs
                union(parent, b, tick);
                continue;
            }
            double barWidth = bar.getMaxX() - bar.getMinX();
            double barY = (bar.getMinY() + bar.getMaxY()) / 2;
            List<Integer> legs = new ArrayList<>();
            List<Integer> nearLegs = new ArrayList<>();
            int crossingLegs = 0;
            boolean somethingAbove = false;
            for (int o = 0; o < n; o++) {
                Stroke other = strokes.get(o);
                if (o == b || dot[o]) {
                    continue;
                }
                double cx = (other.getMinX() + other.getMaxX()) / 2;
                double cy = (other.getMinY() + other.getMaxY()) / 2;
                boolean underBar = cx >= bar.getMinX() - 0.1 * barWidth
                        && cx <= bar.getMaxX() + 0.1 * barWidth;
                if (!underBar || isFlat(other)) {
                    continue;
                }
                if (cy < barY) {
                    somethingAbove |= barY - cy <= 1.5 * barWidth;
                } else if (!entirelyAboveOrBelow(other, bar)) {
                    crossingLegs++;
                } else if (distance(bar, other) <= 0.1 * size(other)) {
                    legs.add(o);
                } else if (distance(bar, other) <= LEG_GAP_RATIO * size(other)) {
                    nearLegs.add(o);
                }
            }
            if (!somethingAbove) {
                if (crossingLegs + legs.size() + nearLegs.size() >= 2) {
                    legs.addAll(nearLegs);
                }
                for (int leg : legs) {
                    union(parent, b, leg);
                }
            }
        }
    }

    private static Point leftmost(Stroke stroke) {
        Point best = stroke.getPoints().get(0);
        for (Point p : stroke.getPoints()) {
            if (p.getX() < best.getX()) {
                best = p;
            }
        }
        return best;
    }

    /**
     * Finds the tick of a root sign drawn apart from its bar: a stroke that leaves the bar's
     * left end, dips down to the left and comes back up a little. Its lowest point lies inside
     * the stroke, which tells it from a neighbour such as a "2", a "(" or the slash of a "7".
     *
     * @return the index of the tick, or -1 if the bar has none
     */
    private static int rootTick(List<Stroke> strokes, boolean[] dot, int b) {
        Stroke bar = strokes.get(b);
        Point barStart = leftmost(bar);
        for (int t = 0; t < strokes.size(); t++) {
            Stroke tick = strokes.get(t);
            if (t == b || dot[t] || isFlat(tick)) {
                continue;
            }
            List<Point> points = tick.getPoints();
            double reach = TICK_REACH_RATIO * size(tick);
            boolean leavesBarStart = distance(points.get(0), barStart) <= reach
                    || distance(points.get(points.size() - 1), barStart) <= reach;
            boolean leftOfBar = (tick.getMinX() + tick.getMaxX()) / 2 < barStart.getX();
            if (leavesBarStart && leftOfBar && dipsInTheMiddle(tick)) {
                return t;
            }
        }
        return -1;
    }

    /** Is the stroke's lowest point between its ends, with both ends clearly higher? */
    private static boolean dipsInTheMiddle(Stroke stroke) {
        List<Point> points = stroke.getPoints();
        double height = stroke.getMaxY() - stroke.getMinY();
        double rise = TICK_RISE_RATIO * height;
        return height > 0
                && points.get(0).getY() <= stroke.getMaxY() - rise
                && points.get(points.size() - 1).getY() <= stroke.getMaxY() - rise;
    }

    private static double distance(Point a, Point b) {
        return Math.hypot(a.getX() - b.getX(), a.getY() - b.getY());
    }

    /** Joins each tiny dot to the stroke it belongs to, if any. */
    private static void attachDots(List<Stroke> strokes, boolean[] dot, int[] parent,
                                   double reference) {
        int n = strokes.size();
        int[] aboveDot = new int[n];
        int[] belowDot = new int[n];
        java.util.Arrays.fill(aboveDot, -1);
        java.util.Arrays.fill(belowDot, -1);

        for (int d = 0; d < n; d++) {
            if (!dot[d]) {
                continue;
            }
            double cx = (strokes.get(d).getMinX() + strokes.get(d).getMaxX()) / 2;
            double cy = (strokes.get(d).getMinY() + strokes.get(d).getMaxY()) / 2;
            int host = -1;
            double best = Double.POSITIVE_INFINITY;
            for (int h = 0; h < n; h++) {
                if (dot[h]) {
                    continue;
                }
                double distance = distance(strokes.get(d), strokes.get(h));
                if (distance < best) {
                    best = distance;
                    host = h;
                }
            }
            if (host < 0 || best > reference) {
                continue;
            }
            Stroke hostStroke = strokes.get(host);
            double slack = DOT_SIDEWAYS_TOLERANCE * reference;
            if (cx < hostStroke.getMinX() - slack || cx > hostStroke.getMaxX() + slack) {
                continue;
            }
            boolean above = cy < (hostStroke.getMinY() + hostStroke.getMaxY()) / 2;
            if (isFlat(hostStroke)) {
                // a division sign has a dot on both sides of its bar
                if (above) {
                    aboveDot[host] = d;
                } else {
                    belowDot[host] = d;
                }
            } else if (above && cy < hostStroke.getMinY()) {
                union(parent, d, host);
            }
        }
        for (int h = 0; h < n; h++) {
            if (aboveDot[h] >= 0 && belowDot[h] >= 0) {
                union(parent, aboveDot[h], h);
                union(parent, belowDot[h], h);
            }
        }
    }

    /** A horizontal bar: much wider than it is tall. */
    private static boolean isFlat(Stroke stroke) {
        double width = stroke.getMaxX() - stroke.getMinX();
        double height = stroke.getMaxY() - stroke.getMinY();
        return width > 0 && height <= FLAT_RATIO * width;
    }

    private static double size(Stroke stroke) {
        return Math.max(stroke.getMaxX() - stroke.getMinX(), stroke.getMaxY() - stroke.getMinY());
    }

    /** Smallest distance between any two points of the two pen traces. */
    private static double distance(Stroke a, Stroke b) {
        List<Point> pa = a.getPoints();
        List<Point> pb = b.getPoints();
        double best = Double.POSITIVE_INFINITY;
        for (int i = 0; i < pa.size(); i++) {
            Point a1 = pa.get(i);
            Point a2 = pa.get(Math.min(i + 1, pa.size() - 1));
            for (int j = 0; j < pb.size(); j++) {
                Point b1 = pb.get(j);
                Point b2 = pb.get(Math.min(j + 1, pb.size() - 1));
                best = Math.min(best, segmentDistance(a1, a2, b1, b2));
                if (best == 0) {
                    return 0;
                }
            }
        }
        return best;
    }

    private static double segmentDistance(Point a1, Point a2, Point b1, Point b2) {
        if (segmentsIntersect(a1, a2, b1, b2)) {
            return 0;
        }
        return Math.min(Math.min(pointToSegment(a1, b1, b2), pointToSegment(a2, b1, b2)),
                Math.min(pointToSegment(b1, a1, a2), pointToSegment(b2, a1, a2)));
    }

    private static boolean segmentsIntersect(Point p1, Point p2, Point p3, Point p4) {
        double d1 = cross(p3, p4, p1);
        double d2 = cross(p3, p4, p2);
        double d3 = cross(p1, p2, p3);
        double d4 = cross(p1, p2, p4);
        return ((d1 > 0 && d2 < 0) || (d1 < 0 && d2 > 0))
                && ((d3 > 0 && d4 < 0) || (d3 < 0 && d4 > 0));
    }

    private static double cross(Point a, Point b, Point c) {
        return (b.getX() - a.getX()) * (c.getY() - a.getY())
                - (b.getY() - a.getY()) * (c.getX() - a.getX());
    }

    private static double pointToSegment(Point p, Point a, Point b) {
        double dx = b.getX() - a.getX();
        double dy = b.getY() - a.getY();
        double lengthSquared = dx * dx + dy * dy;
        double t = lengthSquared == 0 ? 0
                : ((p.getX() - a.getX()) * dx + (p.getY() - a.getY()) * dy) / lengthSquared;
        t = Math.max(0, Math.min(1, t));
        return Math.hypot(p.getX() - (a.getX() + t * dx), p.getY() - (a.getY() + t * dy));
    }

    private static int find(int[] parent, int i) {
        while (parent[i] != i) {
            parent[i] = parent[parent[i]];
            i = parent[i];
        }
        return i;
    }

    private static void union(int[] parent, int a, int b) {
        parent[find(parent, a)] = find(parent, b);
    }
}
