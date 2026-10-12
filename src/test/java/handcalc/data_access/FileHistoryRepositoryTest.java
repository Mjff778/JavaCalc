package handcalc.data_access;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import handcalc.entity.HistoryEntry;
import handcalc.entity.Point;
import handcalc.entity.Stroke;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileHistoryRepositoryTest {

    @TempDir
    Path dir;

    private static HistoryEntry entry(String expression, double result) {
        return new HistoryEntry(expression, result, Instant.parse("2026-10-11T20:15:30Z"),
                Arrays.asList(
                        new Stroke(Arrays.asList(new Point(1.5, 2), new Point(3, 4.25))),
                        new Stroke(Arrays.asList(new Point(9, 9)))));
    }

    @Test
    void missingFileMeansNoHistory() {
        assertTrue(new FileHistoryRepository(dir.resolve("history.jsonl")).loadAll().isEmpty());
    }

    @Test
    void entriesComeBackInOrderWithTimeAndStrokes() {
        FileHistoryRepository repo =
                new FileHistoryRepository(dir.resolve("data").resolve("history.jsonl"));
        repo.append(entry("1+2", 3));
        repo.append(entry("4÷8", 0.5));

        List<HistoryEntry> loaded = repo.loadAll();

        assertEquals(2, loaded.size());
        assertEquals("1+2", loaded.get(0).getExpression());
        assertEquals(0.5, loaded.get(1).getResult());
        assertEquals(Instant.parse("2026-10-11T20:15:30Z"), loaded.get(0).getTime());
        assertEquals(2, loaded.get(0).getStrokes().size());
        assertEquals(4.25, loaded.get(0).getStrokes().get(0).getPoints().get(1).getY());
        assertEquals(1, loaded.get(0).getStrokes().get(1).getPoints().size());
    }

    @Test
    void historySurvivesANewRepository() {
        Path file = dir.resolve("history.jsonl");
        new FileHistoryRepository(file).append(entry("1+1", 2));

        assertEquals("1+1", new FileHistoryRepository(file).loadAll().get(0).getExpression());
    }

    @Test
    void corruptLinesAreSkipped() throws Exception {
        Path file = dir.resolve("history.jsonl");
        FileHistoryRepository repo = new FileHistoryRepository(file);
        repo.append(entry("1+1", 2));
        Files.write(file, Arrays.asList("not json", "{\"expression\":1}"),
                java.nio.file.StandardOpenOption.APPEND);
        repo.append(entry("2+2", 4));

        assertEquals(2, repo.loadAll().size());
    }
}
