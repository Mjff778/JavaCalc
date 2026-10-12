package handcalc.use_case.collect_sample;

/**
 * Entry point of the "collect a handwriting sample" use case.
 */
public interface CollectSampleInputBoundary {

    /** Stores the writing as a sample of the given symbol and reports the new progress. */
    void execute(CollectSampleInputData inputData);

    /** Reports the current progress without storing anything. */
    void refresh();
}
