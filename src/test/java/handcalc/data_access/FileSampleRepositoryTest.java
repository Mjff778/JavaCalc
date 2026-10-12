package handcalc.data_access;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import handcalc.entity.Glyph;
import handcalc.entity.LabeledGlyph;
import handcalc.entity.Point;
import handcalc.entity.Stroke;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileSampleRepositoryTest {

    @TempDir
    Path dir;

    private static LabeledGlyph sample(char symbol, double offset) {
        Stroke bar = new Stroke(Arrays.asList(new Point(offset, 5.5), new Point(offset + 40, 5.5)));
        Stroke dot = new Stroke(Collections.singletonList(new Point(offset + 20, 1.25)));
        return new LabeledGlyph(new Glyph(Arrays.asList(bar, dot)), symbol);
    }

    @Test
    void missingFileMeansNoSamples() {
        assertTrue(new FileSampleRepository(dir.resolve("none.txt")).loadAll().isEmpty());
    }

    @Test
    void savedSamplesComeBackWithLabelAndExactStrokes() {
        FileSampleRepository repo = new FileSampleRepository(dir.resolve("data/samples.txt"));
        repo.save(sample('÷', 10));
        repo.save(sample('×', 100));

        List<LabeledGlyph> loaded = repo.loadAll();

        assertEquals(2, loaded.size());
        assertEquals('÷', loaded.get(0).getSymbol());
        assertEquals('×', loaded.get(1).getSymbol());
        Glyph glyph = loaded.get(0).getGlyph();
        assertEquals(2, glyph.getStrokes().size());
        assertEquals(2, glyph.getStrokes().get(0).getPoints().size());
        assertEquals(50, glyph.getStrokes().get(0).getMaxX());
        assertEquals(1.25, glyph.getStrokes().get(1).getPoints().get(0).getY());
    }

    @Test
    void samplesAreAppendedAcrossInstances() {
        Path file = dir.resolve("samples.txt");
        new FileSampleRepository(file).save(sample('1', 0));
        new FileSampleRepository(file).save(sample('2', 0));

        assertEquals(2, new FileSampleRepository(file).loadAll().size());
    }

    @Test
    void corruptLinesAreSkipped() throws Exception {
        Path file = dir.resolve("samples.txt");
        new FileSampleRepository(file).save(sample('1', 0));
        Files.write(file, Arrays.asList("garbage", "x\tnot,numbers"),
                java.nio.file.StandardOpenOption.APPEND);
        new FileSampleRepository(file).save(sample('2', 0));

        assertEquals(2, new FileSampleRepository(file).loadAll().size());
    }
}
