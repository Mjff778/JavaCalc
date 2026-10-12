package handcalc.use_case.recognize_and_solve;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * What became of one line of writing: it is solved, it could not be solved, or it does not
 * end in "=" yet and was only read.
 */
public final class LineResult {

    private final List<SymbolReading> readings;
    private final RecognizeAndSolveOutputData solved;
    private final RecognizeAndSolveFailureData failure;

    private LineResult(List<SymbolReading> readings, RecognizeAndSolveOutputData solved,
                       RecognizeAndSolveFailureData failure) {
        this.readings = Collections.unmodifiableList(new ArrayList<>(readings));
        this.solved = solved;
        this.failure = failure;
    }

    public static LineResult unfinished(List<SymbolReading> readings) {
        return new LineResult(readings, null, null);
    }

    public static LineResult solved(RecognizeAndSolveOutputData solved) {
        return new LineResult(solved.getReadings(), solved, null);
    }

    public static LineResult failed(RecognizeAndSolveFailureData failure) {
        return new LineResult(failure.getReadings(), null, failure);
    }

    /** @return how each symbol of the line was read */
    public List<SymbolReading> getReadings() {
        return readings;
    }

    /** @return the solution, or null if the line was not solved */
    public RecognizeAndSolveOutputData getSolved() {
        return solved;
    }

    /** @return why the line could not be solved, or null if it did not fail */
    public RecognizeAndSolveFailureData getFailure() {
        return failure;
    }

    /** @return true if the line ends in "=" and so was solved or failed */
    public boolean isFinished() {
        return solved != null || failure != null;
    }
}
