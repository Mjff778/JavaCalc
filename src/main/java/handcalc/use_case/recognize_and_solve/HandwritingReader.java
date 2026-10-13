package handcalc.use_case.recognize_and_solve;

import handcalc.entity.Glyph;
import handcalc.entity.PlacedSymbol;
import handcalc.entity.Stroke;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.List;

/**
 * Reads the written strokes symbol by symbol, left to right, or a whole page line by line.
 *
 * <p>A decimal point is recognized by its size and place rather than by its shape: a mark
 * far smaller than the line's symbols, down at the baseline. Place also settles a root sign,
 * which is easily read as an "n" or a "c": a symbol that might be a root and has other
 * symbols written under it is a root, as nothing is ever written under an "n". Likewise a
 * symbol whose strokes cross is never an "i", "=" or "÷", whose strokes stay apart.
 */
public class HandwritingReader {

    private static final int MAX_ALTERNATIVES = 4;
    private static final char ROOT = '√';
    private static final double POINT_SIZE_RATIO = 0.3;
    private static final double BASELINE_ZONE_RATIO = 0.35;
    /** Symbols made of strokes that never touch one another. */
    private static final String APART = "i=÷";

    private final GlyphSegmenter segmenter;
    private final SymbolClassifier classifier;

    public HandwritingReader(GlyphSegmenter segmenter, SymbolClassifier classifier) {
        this.segmenter = segmenter;
        this.classifier = classifier;
    }

    public List<SymbolReading> read(List<Stroke> strokes) {
        return readLine(segmenter.segment(strokes), Collections.emptyMap());
    }

    /** @return the readings of each line on the page, from the top line down */
    public List<List<SymbolReading>> readPage(List<Stroke> strokes) {
        return readPage(strokes, Collections.emptyList());
    }

    /**
     * @param placed symbols the user picked, which are read as picked
     * @return the readings of each line on the page, from the top line down
     */
    public List<List<SymbolReading>> readPage(List<Stroke> strokes, List<PlacedSymbol> placed) {
        List<Glyph> glyphs = new ArrayList<>(segmenter.segment(strokes));
        Map<Glyph, PlacedSymbol> picked = new IdentityHashMap<>();
        for (PlacedSymbol symbol : placed) {
            Glyph glyph = new Glyph(symbol.getOutline());
            picked.put(glyph, symbol);
            glyphs.add(glyph);
        }
        List<List<SymbolReading>> lines = new ArrayList<>();
        for (List<Glyph> line : LineSplitter.split(glyphs)) {
            lines.add(readLine(line, picked));
        }
        return lines;
    }

    /** Reads the symbols of one line; a decimal point is judged against its own line. */
    private List<SymbolReading> readLine(List<Glyph> glyphs, Map<Glyph, PlacedSymbol> picked) {
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
            PlacedSymbol pick = picked.get(glyph);
            if (pick != null) {
                readings.add(new SymbolReading(glyph, pick.getSymbol(), choices(pick), true));
                continue;
            }
            if (glyphs.size() > 1 && isDecimalPoint(glyph, tallest, baseline)) {
                readings.add(new SymbolReading(glyph, '.',
                        Collections.singletonList(new Candidate('.', 1.0))));
                continue;
            }
            List<Candidate> candidates = classifier.classify(glyph);
            List<Candidate> top =
                    candidates.subList(0, Math.min(MAX_ALTERNATIVES, candidates.size()));
            readings.add(new SymbolReading(glyph, mostLikely(glyph, candidates), top));
        }
        for (int i = 0; i < readings.size(); i++) {
            SymbolReading reading = readings.get(i);
            if (!reading.isPlaced() && reading.getSymbol() != ROOT && mightBe(reading, ROOT)
                    && coversAnother(reading.getGlyph(), glyphs)) {
                readings.set(i, new SymbolReading(reading.getGlyph(), ROOT,
                        reading.getAlternatives()));
            }
        }
        return readings;
    }

    /** The best candidate that the way the strokes meet allows. */
    private static char mostLikely(Glyph glyph, List<Candidate> candidates) {
        if (GlyphSegmenter.strokesTouch(glyph)) {
            for (Candidate candidate : candidates) {
                if (APART.indexOf(candidate.getSymbol()) < 0) {
                    return candidate.getSymbol();
                }
            }
        }
        return candidates.get(0).getSymbol();
    }

    /** A picked symbol is certain; what else its writing might have been is kept as choices. */
    private static List<Candidate> choices(PlacedSymbol placed) {
        List<Candidate> choices = new ArrayList<>();
        choices.add(new Candidate(placed.getSymbol(), 1.0));
        for (char other : placed.getAlternatives().toCharArray()) {
            if (other != placed.getSymbol()) {
                choices.add(new Candidate(other, 0.0));
            }
        }
        return choices;
    }

    private static boolean mightBe(SymbolReading reading, char symbol) {
        for (Candidate candidate : reading.getAlternatives()) {
            if (candidate.getSymbol() == symbol) {
                return true;
            }
        }
        return false;
    }

    /** Is the middle of another symbol inside this one's box, as under a root's roof? */
    private static boolean coversAnother(Glyph cover, List<Glyph> glyphs) {
        for (Glyph glyph : glyphs) {
            double x = (glyph.getMinX() + glyph.getMaxX()) / 2;
            double y = (glyph.getMinY() + glyph.getMaxY()) / 2;
            if (glyph != cover && x > cover.getMinX() && x < cover.getMaxX()
                    && y > cover.getMinY() && y < cover.getMaxY()) {
                return true;
            }
        }
        return false;
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
