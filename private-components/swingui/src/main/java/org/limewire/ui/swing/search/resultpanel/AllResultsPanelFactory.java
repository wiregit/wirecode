package org.limewire.ui.swing.search.resultpanel;

import org.limewire.core.api.search.Search;
import org.limewire.ui.swing.search.model.VisualSearchResult;

import ca.odell.glazedlists.swing.EventListModel;
import ca.odell.glazedlists.swing.EventSelectionModel;

public interface AllResultsPanelFactory {
    
    AllResultsPanel create(
        EventListModel<VisualSearchResult> listModel,
        EventSelectionModel<VisualSearchResult> selectionModel,
        Search search);
}
