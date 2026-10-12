package handcalc.use_case.practice_expression;

import handcalc.entity.Glyph;
import handcalc.entity.LabeledGlyph;
import handcalc.entity.Stroke;
import handcalc.use_case.correct_symbol.SampleRepository;
import handcalc.use_case.recognize_and_solve.GlyphSegmenter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Practice with whole expressions: the user writes a made-up expression, the written symbols
 * are matched to the symbols it contains, and each becomes a labelled training sample.
 */
public class PracticeExpressionInteractor implements PracticeExpressionInputBoundary {

    private final GlyphSegmenter segmenter;
    private final SampleRepository repository;
    private final ExpressionPromptGenerator generator;
    private final PracticeExpressionOutputBoundary presenter;
    private ExpressionPrompt current;

    public PracticeExpressionInteractor(GlyphSegmenter segmenter, SampleRepository repository,
                                        ExpressionPromptGenerator generator,
                                        PracticeExpressionOutputBoundary presenter) {
        this.segmenter = segmenter;
        this.repository = repository;
        this.generator = generator;
        this.presenter = presenter;
    }

    @Override
    public void next() {
        current = generator.generate(countSamples());
        presenter.prepareView(current, 0);
    }

    @Override
    public void submit(List<Stroke> strokes) {
        if (current == null) {
            next();
        }
        if (strokes.isEmpty()) {
            presenter.prepareFailView("Nothing written yet");
            return;
        }
        List<Glyph> glyphs = segmenter.segment(strokes);
        List<LabeledGlyph> labelled = current.label(glyphs);
        if (labelled == null) {
            // The strokes did not fall into the expected number of symbols by themselves:
            // someone drew a symbol with more or fewer strokes than the segmenter expects.
            // The expression says how many symbols there are, so divide the strokes that way.
            List<Glyph> divided = StrokePartitioner.partition(strokes, current);
            labelled = divided == null ? null : current.label(divided);
        }
        if (labelled == null) {
            int symbols = current.getSymbols().length();
            presenter.prepareFailView(strokes.size() < symbols
                    ? "This expression has " + symbols + " symbols but only " + strokes.size()
                            + " strokes were written."
                    : "What was written does not match the " + symbols
                            + " symbols of this expression. Write each symbol apart, as shown.");
            return;
        }
        for (LabeledGlyph sample : labelled) {
            repository.save(sample);
        }
        int saved = labelled.size();
        current = generator.generate(countSamples());
        presenter.prepareView(current, saved);
    }

    private Map<Character, Integer> countSamples() {
        Map<Character, Integer> counts = new HashMap<>();
        for (LabeledGlyph sample : repository.loadAll()) {
            counts.merge(sample.getSymbol(), 1, Integer::sum);
        }
        return counts;
    }
}
