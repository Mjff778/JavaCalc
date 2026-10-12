package handcalc.use_case.collect_sample;

import handcalc.entity.Glyph;
import handcalc.entity.LabeledGlyph;
import handcalc.use_case.correct_symbol.SampleRepository;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stores one deliberately written symbol (all strokes on the canvas, however many) as a
 * labeled sample and keeps the collection
 * balanced by always suggesting the symbol with the fewest samples next.
 */
public class CollectSampleInteractor implements CollectSampleInputBoundary {

    private final SampleRepository repository;
    private final String symbols;
    private final int targetPerSymbol;
    private final CollectSampleOutputBoundary presenter;

    public CollectSampleInteractor(SampleRepository repository, String symbols, int targetPerSymbol,
                                   CollectSampleOutputBoundary presenter) {
        this.repository = repository;
        this.symbols = symbols;
        this.targetPerSymbol = targetPerSymbol;
        this.presenter = presenter;
    }

    @Override
    public void execute(CollectSampleInputData inputData) {
        char symbol = inputData.getSymbol();
        if (symbols.indexOf(symbol) < 0) {
            presenter.prepareFailView("Unsupported symbol: " + symbol);
            return;
        }
        if (inputData.getStrokes().isEmpty()) {
            presenter.prepareFailView("Nothing written yet");
            return;
        }
        // The user said which symbol this is, so every stroke belongs to it - however far
        // apart the parts of a "÷" or "=" were drawn.
        repository.save(new LabeledGlyph(new Glyph(inputData.getStrokes()), symbol));
        report(symbol);
    }

    @Override
    public void refresh() {
        report(null);
    }

    private void report(Character savedSymbol) {
        Map<Character, Integer> counts = new LinkedHashMap<>();
        for (char symbol : symbols.toCharArray()) {
            counts.put(symbol, 0);
        }
        for (LabeledGlyph sample : repository.loadAll()) {
            counts.computeIfPresent(sample.getSymbol(), (c, n) -> n + 1);
        }
        char next = symbols.charAt(0);
        for (Map.Entry<Character, Integer> entry : counts.entrySet()) {
            if (entry.getValue() < counts.get(next)) {
                next = entry.getKey();
            }
        }
        presenter.prepareProgressView(
                new CollectSampleOutputData(counts, next, targetPerSymbol, savedSymbol));
    }
}
