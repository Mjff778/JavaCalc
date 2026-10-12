package handcalc.use_case.recognize_and_solve;

import handcalc.entity.Glyph;

/**
 * A classifier that can be taught from corrections while the program runs.
 */
public interface TrainableSymbolClassifier extends SymbolClassifier {

    void learn(Glyph glyph, char symbol);

    /** @return whether {@link #learn} accepts this symbol. */
    default boolean canLearn(char symbol) {
        return true;
    }
}
