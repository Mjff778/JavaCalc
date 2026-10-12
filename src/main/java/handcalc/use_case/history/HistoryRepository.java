package handcalc.use_case.history;

import handcalc.entity.HistoryEntry;
import java.util.List;

/**
 * Persistent calculation history.
 */
public interface HistoryRepository {

    void append(HistoryEntry entry);

    /** @return all entries, oldest first. */
    List<HistoryEntry> loadAll();
}
