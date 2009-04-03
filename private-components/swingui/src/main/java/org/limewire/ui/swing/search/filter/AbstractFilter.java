package org.limewire.ui.swing.search.filter;

import java.util.ArrayList;
import java.util.List;

/**
 * Base implementation of Filter that provides listener support.
 */
abstract class AbstractFilter implements Filter {

    /** List of listeners notified when filter is changed. */
    private final List<FilterListener> listenerList = new ArrayList<FilterListener>();
    
    /**
     * Adds the specified listener to the list that is notified when the 
     * filter changes.
     */
    @Override
    public void addFilterListener(FilterListener listener) {
        if (!listenerList.contains(listener)) {
            listenerList.add(listener);
        }
    }

    /**
     * Removes the specified listener from the list that is notified when the 
     * filter changes.
     */
    @Override
    public void removeFilterListener(FilterListener listener) {
        listenerList.remove(listener);
    }
    
    /**
     * Notifies all registered listeners that the filter has changed for the
     * specified filter component.
     */
    protected void fireFilterChanged(Filter filter) {
        for (int i = 0, size = listenerList.size(); i < size; i++) {
            listenerList.get(i).filterChanged(filter);
        }
    }
}
