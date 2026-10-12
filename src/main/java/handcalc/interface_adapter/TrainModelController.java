package handcalc.interface_adapter;

import handcalc.use_case.train_model.TrainModelInputBoundary;

/**
 * Turns the "train" button into a use case call.
 */
public class TrainModelController {

    private final TrainModelInputBoundary useCase;

    public TrainModelController(TrainModelInputBoundary useCase) {
        this.useCase = useCase;
    }

    public void execute() {
        useCase.execute();
    }
}
