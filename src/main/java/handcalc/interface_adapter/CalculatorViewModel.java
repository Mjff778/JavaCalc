package handcalc.interface_adapter;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * Holds the {@link CalculatorState} and tells listeners (the view) when it changes.
 */
public class CalculatorViewModel {

    public static final String STATE_PROPERTY = "state";

    private final PropertyChangeSupport support = new PropertyChangeSupport(this);
    private CalculatorState state = CalculatorState.empty();

    public CalculatorState getState() {
        return state;
    }

    public void setState(CalculatorState newState) {
        CalculatorState old = state;
        state = newState;
        // always fire: a new state object means a new calculation, even with equal text
        support.firePropertyChange(new PropertyChangeEvent(this, STATE_PROPERTY, old, newState));
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }
}
