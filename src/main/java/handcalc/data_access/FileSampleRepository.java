package handcalc.data_access;

import handcalc.entity.Glyph;
import handcalc.entity.LabeledGlyph;
import handcalc.entity.Point;
import handcalc.entity.Stroke;
import handcalc.use_case.correct_symbol.SampleRepository;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stores samples in a plain text file, one per line:
 * {@code <symbol> TAB <stroke>;<stroke>;...} where a stroke is {@code x,y x,y ...}.
 *
 * <p>Appending one line per sample keeps the file easy to merge between team members.
 */
public class FileSampleRepository implements SampleRepository {

    private final Path file;

    public FileSampleRepository(Path file) {
        this.file = file;
    }

    @Override
    public void save(LabeledGlyph sample) {
        StringBuilder line = new StringBuilder();
        line.append(sample.getSymbol()).append('\t');
        List<Stroke> strokes = sample.getGlyph().getStrokes();
        for (int s = 0; s < strokes.size(); s++) {
            if (s > 0) {
                line.append(';');
            }
            List<Point> points = strokes.get(s).getPoints();
            for (int p = 0; p < points.size(); p++) {
                if (p > 0) {
                    line.append(' ');
                }
                line.append(points.get(p).getX()).append(',').append(points.get(p).getY());
            }
        }
        line.append('\n');
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(file, line.toString().getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save sample to " + file, e);
        }
    }

    @Override
    public List<LabeledGlyph> loadAll() {
        if (!Files.exists(file)) {
            return Collections.emptyList();
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read samples from " + file, e);
        }
        List<LabeledGlyph> samples = new ArrayList<>();
        for (String line : lines) {
            LabeledGlyph sample = parse(line);
            if (sample != null) {
                samples.add(sample);
            }
        }
        return samples;
    }

    /** @return the sample, or null if the line is not a valid sample. */
    private static LabeledGlyph parse(String line) {
        try {
            int tab = line.indexOf('\t');
            if (tab != 1) {
                return null;
            }
            List<Stroke> strokes = new ArrayList<>();
            for (String strokeText : line.substring(tab + 1).split(";")) {
                List<Point> points = new ArrayList<>();
                for (String pointText : strokeText.trim().split(" ")) {
                    String[] xy = pointText.split(",");
                    points.add(new Point(Double.parseDouble(xy[0]), Double.parseDouble(xy[1])));
                }
                strokes.add(new Stroke(points));
            }
            return new LabeledGlyph(new Glyph(strokes), line.charAt(0));
        } catch (RuntimeException e) {
            return null;
        }
    }
}
