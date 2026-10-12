package handcalc.interface_adapter;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.function.UnaryOperator;

/**
 * Holds the {@link CollectorState} and tells listeners (the collection window) about changes.
 */
public class CollectorViewModel {

    public static final String STATE_PROPERTY = "collectorState";

    private final PropertyChangeSupport support = new PropertyChangeSupport(this);
    private volatile CollectorState state = CollectorState.empty();

    public CollectorState getState() {
        return state;
    }

    public synchronized void setState(CollectorState newState) {
        CollectorState old = state;
        state = newState;
        support.firePropertyChange(new PropertyChangeEvent(this, STATE_PROPERTY, old, newState));
    }

    /**
     * Changes the state in one step. Training reports arrive on a background thread while
     * sample counts arrive on the event thread, so a separate get and set could lose one.
     */
    public synchronized void update(UnaryOperator<CollectorState> change) {
        setState(change.apply(state));
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }
}
