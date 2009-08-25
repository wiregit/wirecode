package org.limewire.ui.swing.search.store;

import org.limewire.ui.swing.search.model.SearchResultsModel;

/**
 * Defines a factory to create instances of StoreController.
 */
public interface StoreControllerFactory {

    /**
     * Creates a new StoreController object using the specified search results
     * model.
     */
    StoreController create(SearchResultsModel searchResultsModel);
}
