package handcalc.view;

import handcalc.entity.Stroke;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * Names a line of writing by the very strokes it is made of: the same line read again is
 * the same key, and any stroke added or wiped makes it another line.
 */
final class LineKey {

    private final Set<Stroke> strokes = Collections.newSetFromMap(new IdentityHashMap<>());
    private final int hash;

    LineKey(Collection<Stroke> lineStrokes) {
        strokes.addAll(lineStrokes);
        int sum = 0;
        for (Stroke stroke : strokes) {
            sum += System.identityHashCode(stroke);
        }
        hash = sum;
    }

    /** @return true if every stroke of the line is among these */
    boolean isWithin(Set<Stroke> present) {
        return present.containsAll(strokes);
    }

    Set<Stroke> getStrokes() {
        return Collections.unmodifiableSet(strokes);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof LineKey)) {
            return false;
        }
        LineKey that = (LineKey) other;
        return hash == that.hash && strokes.size() == that.strokes.size()
                && strokes.containsAll(that.strokes);
    }

    @Override
    public int hashCode() {
        return hash;
    }
}
