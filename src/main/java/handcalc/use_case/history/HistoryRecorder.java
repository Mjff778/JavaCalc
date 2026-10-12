package handcalc.use_case.history;

import handcalc.entity.HistoryEntry;
import handcalc.entity.Stroke;
import handcalc.use_case.recognize_and_solve.CalculationRecorder;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Keeps every solved calculation in the history.
 */
public class HistoryRecorder implements CalculationRecorder {

    private final HistoryRepository repository;
    private final Clock clock;

    public HistoryRecorder(HistoryRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public void record(String expression, double result, List<Stroke> strokes) {
        repository.append(new HistoryEntry(expression, result, Instant.now(clock), strokes));
    }
}
