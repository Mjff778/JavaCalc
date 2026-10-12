package handcalc.interface_adapter;

import handcalc.entity.Stroke;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One line of the history list.
 */
public final class HistoryRow {

    private final String text;
    private final String time;
    private final List<Stroke> strokes;

    public HistoryRow(String text, String time, List<Stroke> strokes) {
        this.text = text;
        this.time = time;
        this.strokes = Collections.unmodifiableList(new ArrayList<>(strokes));
    }

    public String getText() {
        return text;
    }

    public String getTime() {
        return time;
    }

    public List<Stroke> getStrokes() {
        return strokes;
    }

    @Override
    public String toString() {
        return text + "  " + time;
    }
}
