package handcalc.use_case.history;

import handcalc.entity.HistoryEntry;
import java.util.List;

/**
 * Implemented by the history presenter.
 */
public interface HistoryOutputBoundary {

    /** @param entries the calculations, newest first */
    void prepareHistoryView(List<HistoryEntry> entries);

    void prepareFailView(String message);
}
