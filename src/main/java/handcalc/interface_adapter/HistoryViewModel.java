package handcalc.interface_adapter;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * Holds the {@link HistoryState} and notifies the history window.
 */
public class HistoryViewModel {

    public static final String STATE_PROPERTY = "historyState";

    private final PropertyChangeSupport support = new PropertyChangeSupport(this);
    private volatile HistoryState state = HistoryState.empty();

    public HistoryState getState() {
        return state;
    }

    public void setState(HistoryState newState) {
        HistoryState old = state;
        state = newState;
        support.firePropertyChange(new PropertyChangeEvent(this, STATE_PROPERTY, old, newState));
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }
}
