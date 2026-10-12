package handcalc.use_case;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import handcalc.entity.Glyph;
import handcalc.entity.HistoryEntry;
import handcalc.entity.Point;
import handcalc.entity.Stroke;
import handcalc.use_case.history.HistoryOutputBoundary;
import handcalc.use_case.history.HistoryRecorder;
import handcalc.use_case.history.HistoryRepository;
import handcalc.use_case.history.ShowHistoryInteractor;
import handcalc.use_case.recognize_and_solve.Candidate;
import handcalc.use_case.recognize_and_solve.CalculationRecorder;
import handcalc.use_case.recognize_and_solve.GlyphSegmenter;
import handcalc.use_case.recognize_and_solve.RecognizeAndSolveInputData;
import handcalc.use_case.recognize_and_solve.RecognizeAndSolveInteractor;
import handcalc.use_case.recognize_and_solve.RecognizeAndSolveOutputBoundary;
import handcalc.use_case.recognize_and_solve.RecognizeAndSolveOutputData;
import handcalc.use_case.recognize_and_solve.SymbolReading;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class HistoryTest {

    private static class MemoryHistory implements HistoryRepository {
        final List<HistoryEntry> entries = new ArrayList<>();

        @Override
        public void append(HistoryEntry entry) {
            entries.add(entry);
        }

        @Override
        public List<HistoryEntry> loadAll() {
            return entries;
        }
    }

    private static final Stroke STROKE =
            new Stroke(Arrays.asList(new Point(1, 2), new Point(3, 4)));

    private final MemoryHistory history = new MemoryHistory();

    private HistoryEntry entry(String expression, double result, long epochSecond) {
        return new HistoryEntry(expression, result, Instant.ofEpochSecond(epochSecond),
                Collections.singletonList(STROKE));
    }

    @Test
    void calculationIsRecorded() {
        Clock clock = Clock.fixed(Instant.ofEpochSecond(1000), ZoneOffset.UTC);

        new HistoryRecorder(history, clock)
                .record("1+2", 3.0, Collections.singletonList(STROKE));

        HistoryEntry saved = history.entries.get(0);
        assertEquals("1+2", saved.getExpression());
        assertEquals(3.0, saved.getResult());
        assertEquals(Instant.ofEpochSecond(1000), saved.getTime());
        assertEquals(STROKE, saved.getStrokes().get(0));
    }

    @Test
    void showHistoryListsNewestFirst() {
        history.append(entry("1+1", 2, 100));
        history.append(entry("2+2", 4, 300));
        history.append(entry("3+3", 6, 200));
        List<String> shown = new ArrayList<>();
        String[] failure = new String[1];

        new ShowHistoryInteractor(history, new HistoryOutputBoundary() {
            @Override
            public void prepareHistoryView(List<HistoryEntry> entries) {
                for (HistoryEntry e : entries) {
                    shown.add(e.getExpression());
                }
            }

            @Override
            public void prepareFailView(String message) {
                failure[0] = message;
            }
        }).refresh();

        assertEquals(Arrays.asList("2+2", "3+3", "1+1"), shown);
        assertNull(failure[0]);
    }

    @Test
    void solverRecordsSuccessfulCalculationsWithTheirStrokes() {
        List<String> recorded = new ArrayList<>();
        List<Integer> strokeCounts = new ArrayList<>();
        CalculationRecorder recorder = (expression, result, strokes) -> {
            recorded.add(expression + "=" + result);
            strokeCounts.add(strokes.size());
        };
        RecognizeAndSolveOutputBoundary quiet = new RecognizeAndSolveOutputBoundary() {
            @Override
            public void prepareSuccessView(RecognizeAndSolveOutputData outputData) {
            }

            @Override
            public void prepareFailView(String expression, String message,
                                        List<SymbolReading> readings) {
            }
        };
        String[] script = {"2", "×", "3"};
        int[] call = {0};
        RecognizeAndSolveInteractor interactor = new RecognizeAndSolveInteractor(
                new GlyphSegmenter(), (Glyph g) -> Collections.singletonList(
                        new Candidate(script[call[0]++].charAt(0), 1.0)), quiet, recorder);

        interactor.execute(new RecognizeAndSolveInputData(Arrays.asList(
                new Stroke(Arrays.asList(new Point(0, 0), new Point(0, 50))),
                new Stroke(Arrays.asList(new Point(100, 0), new Point(100, 50))),
                new Stroke(Arrays.asList(new Point(200, 0), new Point(200, 50))))));

        assertEquals(Collections.singletonList("2×3=6.0"), recorded);
        assertEquals(Collections.singletonList(3), strokeCounts);
    }

    @Test
    void solverDoesNotRecordFailures() {
        List<String> recorded = new ArrayList<>();
        RecognizeAndSolveInteractor interactor = new RecognizeAndSolveInteractor(
                new GlyphSegmenter(),
                (Glyph g) -> Collections.singletonList(new Candidate('+', 1.0)),
                new RecognizeAndSolveOutputBoundary() {
                    @Override
                    public void prepareSuccessView(RecognizeAndSolveOutputData outputData) {
                    }

                    @Override
                    public void prepareFailView(String expression, String message,
                                                List<SymbolReading> readings) {
                    }
                }, (e, r, s) -> recorded.add(e));

        interactor.execute(new RecognizeAndSolveInputData(Collections.singletonList(
                new Stroke(Arrays.asList(new Point(0, 0), new Point(0, 50))))));

        assertTrue(recorded.isEmpty());
    }

    @Test
    void showHistoryReportsAnUnreadableHistoryInsteadOfFailing() {
        String[] failure = new String[1];
        HistoryRepository broken = new MemoryHistory() {
            @Override
            public List<HistoryEntry> loadAll() {
                throw new java.io.UncheckedIOException(new java.io.IOException("disk error"));
            }
        };

        new ShowHistoryInteractor(broken, new HistoryOutputBoundary() {
            @Override
            public void prepareHistoryView(List<HistoryEntry> entries) {
                throw new AssertionError();
            }

            @Override
            public void prepareFailView(String message) {
                failure[0] = message;
            }
        }).refresh();

        assertEquals("Could not read your history", failure[0]);
    }
}
