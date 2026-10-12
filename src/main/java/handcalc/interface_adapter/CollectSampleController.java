package handcalc.interface_adapter;

import handcalc.entity.Stroke;
import handcalc.use_case.collect_sample.CollectSampleInputBoundary;
import handcalc.use_case.collect_sample.CollectSampleInputData;
import java.util.List;

/**
 * Turns the collection window's actions into use case calls.
 */
public class CollectSampleController {

    private final CollectSampleInputBoundary useCase;

    public CollectSampleController(CollectSampleInputBoundary useCase) {
        this.useCase = useCase;
    }

    public void execute(List<Stroke> strokes, char symbol) {
        useCase.execute(new CollectSampleInputData(strokes, symbol));
    }

    public void refresh() {
        useCase.refresh();
    }
}
