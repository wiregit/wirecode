package org.limewire.ui.swing.search.resultpanel;

import org.limewire.ui.swing.search.RowSelectionPreserver;
import org.limewire.ui.swing.search.model.SearchResultsModel;

/**
 * Defines a factory class to create a BaseResultPanel.
 */
public interface BaseResultPanelFactory {

    /**
     * Creates a BaseResultPanel with the specified search model and row
     * selection preserver.
     */
    BaseResultPanel create(SearchResultsModel searchResultsModel,
            RowSelectionPreserver preserver);
}
