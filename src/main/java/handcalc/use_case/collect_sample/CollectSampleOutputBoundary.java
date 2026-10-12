package handcalc.use_case.collect_sample;

/**
 * Implemented by the presenter of the collection window.
 */
public interface CollectSampleOutputBoundary {

    void prepareProgressView(CollectSampleOutputData outputData);

    void prepareFailView(String message);
}
