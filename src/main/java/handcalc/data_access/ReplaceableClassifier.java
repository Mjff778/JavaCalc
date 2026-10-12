package handcalc.data_access;

import handcalc.entity.Glyph;
import handcalc.use_case.recognize_and_solve.Candidate;
import handcalc.use_case.recognize_and_solve.TrainableSymbolClassifier;
import java.util.List;

/**
 * The classifier the whole application shares. A newly trained model is swapped in here, so
 * the calculator picks it up without being restarted.
 */
public class ReplaceableClassifier implements TrainableSymbolClassifier {

    private volatile NeuralSymbolClassifier current;

    public void replace(NeuralSymbolClassifier model) {
        this.current = model;
    }

    public int getCollectedSampleCount() {
        return model().getCollectedSampleCount();
    }

    @Override
    public List<Candidate> classify(Glyph glyph) {
        return model().classify(glyph);
    }

    @Override
    public void learn(Glyph glyph, char symbol) {
        model().learn(glyph, symbol);
    }

    @Override
    public boolean canLearn(char symbol) {
        return model().canLearn(symbol);
    }

    private NeuralSymbolClassifier model() {
        NeuralSymbolClassifier model = current;
        if (model == null) {
            throw new IllegalStateException("No recognition model has been loaded yet");
        }
        return model;
    }
}
