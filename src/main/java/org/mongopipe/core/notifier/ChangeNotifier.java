package org.mongopipe.core.notifier;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public class ChangeNotifier {
    private PropertyChangeSupport publisher;

    public ChangeNotifier() {
        publisher = new PropertyChangeSupport(this);
    }

    public void addListener(PropertyChangeListener listener) { publisher.addPropertyChangeListener(listener);}

    /**
     * Trigger an generic event
     */
    public void fire(){ publisher.firePropertyChange("change", null, null); }
}
