package handcalc.interface_adapter;

import handcalc.use_case.history.HistoryInputBoundary;

/**
 * Turns "show my history" into a use case call.
 */
public class HistoryController {

    private final HistoryInputBoundary useCase;

    public HistoryController(HistoryInputBoundary useCase) {
        this.useCase = useCase;
    }

    public void refresh() {
        useCase.refresh();
    }
}
