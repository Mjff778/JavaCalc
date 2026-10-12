package handcalc.use_case.recognize_and_solve;

import handcalc.entity.Glyph;
import handcalc.entity.Stroke;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Reads the written strokes symbol by symbol, left to right.
 *
 * <p>A decimal point is recognized by its size and place rather than by its shape: a mark
 * far smaller than the line's symbols, down at the baseline.
 */
public class HandwritingReader {

    private static final int MAX_ALTERNATIVES = 4;
    private static final double POINT_SIZE_RATIO = 0.3;
    private static final double BASELINE_ZONE_RATIO = 0.35;

    private final GlyphSegmenter segmenter;
    private final SymbolClassifier classifier;

    public HandwritingReader(GlyphSegmenter segmenter, SymbolClassifier classifier) {
        this.segmenter = segmenter;
        this.classifier = classifier;
    }

    public List<SymbolReading> read(List<Stroke> strokes) {
        List<Glyph> glyphs = segmenter.segment(strokes);
        double tallest = 0;
        for (Glyph glyph : glyphs) {
            tallest = Math.max(tallest, glyph.getMaxY() - glyph.getMinY());
        }
        double baseline = Double.NEGATIVE_INFINITY;
        for (Glyph glyph : glyphs) {
            if (glyph.getMaxY() - glyph.getMinY() >= 0.5 * tallest) {
                baseline = Math.max(baseline, glyph.getMaxY());
            }
        }

        List<SymbolReading> readings = new ArrayList<>();
        for (Glyph glyph : glyphs) {
            if (glyphs.size() > 1 && isDecimalPoint(glyph, tallest, baseline)) {
                readings.add(new SymbolReading(glyph, '.',
                        Collections.singletonList(new Candidate('.', 1.0))));
                continue;
            }
            List<Candidate> candidates = classifier.classify(glyph);
            List<Candidate> top =
                    candidates.subList(0, Math.min(MAX_ALTERNATIVES, candidates.size()));
            readings.add(new SymbolReading(glyph, candidates.get(0).getSymbol(), top));
        }
        return readings;
    }

    private static boolean isDecimalPoint(Glyph glyph, double tallest, double baseline) {
        double width = glyph.getMaxX() - glyph.getMinX();
        double height = glyph.getMaxY() - glyph.getMinY();
        double centreY = (glyph.getMinY() + glyph.getMaxY()) / 2;
        return tallest > 0
                && width < POINT_SIZE_RATIO * tallest
                && height < POINT_SIZE_RATIO * tallest
                && centreY >= baseline - BASELINE_ZONE_RATIO * tallest;
    }
}
