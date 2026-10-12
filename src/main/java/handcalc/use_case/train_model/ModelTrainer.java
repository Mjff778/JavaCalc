package handcalc.use_case.train_model;

import handcalc.entity.LabeledGlyph;
import java.util.List;

/**
 * Trains the recognition model from collected samples and puts it into use.
 */
public interface ModelTrainer {

    TrainingReport train(List<LabeledGlyph> samples);
}
