package handcalc.use_case.correct_symbol;

import handcalc.entity.LabeledGlyph;
import java.util.List;

/**
 * Persistent store of handwriting samples whose true symbol is known.
 */
public interface SampleRepository {

    void save(LabeledGlyph sample);

    List<LabeledGlyph> loadAll();
}
