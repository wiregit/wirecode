package org.limewire.ui.swing.search.filter;

import javax.swing.JComponent;

import org.limewire.ui.swing.components.Disposable;
import org.limewire.ui.swing.search.model.VisualSearchResult;

import ca.odell.glazedlists.matchers.MatcherEditor;

/**
 * Defines a component used to filter search results.
 */
public interface Filter extends Disposable {

    /**
     * Adds the specified listener to the list that is notified when the 
     * filter changes.
     */
    void addFilterListener(FilterListener listener);
    
    /**
     * Removes the specified listener from the list that is notified when the 
     * filter changes.
     */
    void removeFilterListener(FilterListener listener);
    
    /**
     * Returns an indicator that determines whether the filter is in use.
     */
    boolean isActive();
    
    /**
     * Returns the display text for an active filter.
     */
    String getActiveText();
    
    /**
     * Returns the Swing component that displays the filter controls.
     */
    JComponent getComponent();
    
    /**
     * Returns the matcher/editor used to filter search results.
     */
    MatcherEditor<VisualSearchResult> getMatcherEditor();
    
    /**
     * Resets the filter.
     */
    void reset();
    
}
