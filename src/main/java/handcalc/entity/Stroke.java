package handcalc.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One pen-down to pen-up movement: an ordered list of points.
 */
public final class Stroke {

    private final List<Point> points;
    private final double minX;
    private final double maxX;
    private final double minY;
    private final double maxY;

    public Stroke(List<Point> points) {
        if (points.isEmpty()) {
            throw new IllegalArgumentException("A stroke needs at least one point");
        }
        this.points = Collections.unmodifiableList(new ArrayList<>(points));
        double lowX = Double.POSITIVE_INFINITY;
        double highX = Double.NEGATIVE_INFINITY;
        double lowY = Double.POSITIVE_INFINITY;
        double highY = Double.NEGATIVE_INFINITY;
        for (Point p : points) {
            lowX = Math.min(lowX, p.getX());
            highX = Math.max(highX, p.getX());
            lowY = Math.min(lowY, p.getY());
            highY = Math.max(highY, p.getY());
        }
        this.minX = lowX;
        this.maxX = highX;
        this.minY = lowY;
        this.maxY = highY;
    }

    public List<Point> getPoints() {
        return points;
    }

    public double getMinX() {
        return minX;
    }

    public double getMaxX() {
        return maxX;
    }

    public double getMinY() {
        return minY;
    }

    public double getMaxY() {
        return maxY;
    }
}
