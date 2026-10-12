package handcalc.interface_adapter;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/** Holds the {@link PracticeExpressionState} and tells listeners about changes. */
public class PracticeExpressionViewModel {

    public static final String STATE_PROPERTY = "practiceState";

    private final PropertyChangeSupport support = new PropertyChangeSupport(this);
    private volatile PracticeExpressionState state = PracticeExpressionState.empty();

    public PracticeExpressionState getState() {
        return state;
    }

    public void setState(PracticeExpressionState newState) {
        PracticeExpressionState old = state;
        state = newState;
        support.firePropertyChange(new PropertyChangeEvent(this, STATE_PROPERTY, old, newState));
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }
}
