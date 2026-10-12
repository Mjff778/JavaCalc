package handcalc.data_access;

import handcalc.entity.HistoryEntry;
import handcalc.entity.Point;
import handcalc.entity.Stroke;
import handcalc.use_case.history.HistoryRepository;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Stores the history on this computer as one JSON object per line in a single file.
 */
public class FileHistoryRepository implements HistoryRepository {

    private final Path file;

    public FileHistoryRepository(Path file) {
        this.file = file;
    }

    @Override
    public void append(HistoryEntry entry) {
        JSONArray strokes = new JSONArray();
        for (Stroke stroke : entry.getStrokes()) {
            JSONArray points = new JSONArray();
            for (Point p : stroke.getPoints()) {
                points.put(new JSONArray().put(p.getX()).put(p.getY()));
            }
            strokes.put(points);
        }
        JSONObject json = new JSONObject()
                .put("expression", entry.getExpression())
                .put("result", entry.getResult())
                .put("time", entry.getTime().toString())
                .put("strokes", strokes);
        try {
            Path folder = file.toAbsolutePath().getParent();
            if (folder != null) {
                Files.createDirectories(folder);
            }
            Files.write(file,
                    (json + "\n").getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save history to " + file, e);
        }
    }

    @Override
    public List<HistoryEntry> loadAll() {
        if (!Files.exists(file)) {
            return Collections.emptyList();
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read history from " + file, e);
        }
        List<HistoryEntry> entries = new ArrayList<>();
        for (String line : lines) {
            try {
                entries.add(parse(new JSONObject(line)));
            } catch (RuntimeException e) {
                // skip lines that are not valid entries
            }
        }
        return entries;
    }

    private static HistoryEntry parse(JSONObject json) {
        List<Stroke> strokes = new ArrayList<>();
        JSONArray strokeArray = json.getJSONArray("strokes");
        for (int s = 0; s < strokeArray.length(); s++) {
            JSONArray pointArray = strokeArray.getJSONArray(s);
            List<Point> points = new ArrayList<>();
            for (int p = 0; p < pointArray.length(); p++) {
                JSONArray xy = pointArray.getJSONArray(p);
                points.add(new Point(xy.getDouble(0), xy.getDouble(1)));
            }
            strokes.add(new Stroke(points));
        }
        return new HistoryEntry(json.getString("expression"), json.getDouble("result"),
                Instant.parse(json.getString("time")), strokes);
    }
}
