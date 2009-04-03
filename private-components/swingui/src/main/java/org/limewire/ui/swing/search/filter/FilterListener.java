package org.limewire.ui.swing.search.filter;

/**
 * Defines a listener to handle filter changes.
 */
public interface FilterListener {

    /**
     * Invoked when the filter changes in the specified filter component.
     */
    void filterChanged(Filter filter);
    
}
