package org.mongopipe.core.notifier;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * Used to update PipelineStore, but can be used in for others also.
 */
public class GenericChangeNotifier {
  private PropertyChangeSupport publisher;

  public GenericChangeNotifier() {
    publisher = new PropertyChangeSupport(this);
  }

  public void addListener(PropertyChangeListener listener) {
    publisher.addPropertyChangeListener(listener);
  }

  /**
   * Trigger an generic event.
   */
  public void fire() {
    publisher.firePropertyChange("change", null, null);
  }
}
