package handcalc.use_case.history;

import handcalc.entity.HistoryEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Shows the earlier calculations, newest first.
 */
public class ShowHistoryInteractor implements HistoryInputBoundary {

    private final HistoryRepository repository;
    private final HistoryOutputBoundary presenter;

    public ShowHistoryInteractor(HistoryRepository repository, HistoryOutputBoundary presenter) {
        this.repository = repository;
        this.presenter = presenter;
    }

    @Override
    public void refresh() {
        List<HistoryEntry> entries;
        try {
            entries = new ArrayList<>(repository.loadAll());
        } catch (RuntimeException e) {
            presenter.prepareFailView("Could not read your history");
            return;
        }
        entries.sort((a, b) -> b.getTime().compareTo(a.getTime()));
        presenter.prepareHistoryView(Collections.unmodifiableList(entries));
    }
}
