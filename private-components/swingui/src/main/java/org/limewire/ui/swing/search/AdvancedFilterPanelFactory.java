package org.limewire.ui.swing.search;

import org.limewire.ui.swing.search.model.SearchResultsModel;

/**
 * Defines a factory for creating the filter panel for the search results 
 * display.
 */
public interface AdvancedFilterPanelFactory {

    /**
     * Creates a new FilterPanel using the specified search results data model.
     */
    public AdvancedFilterPanel create(SearchResultsModel searchResultsModel);
    
}
