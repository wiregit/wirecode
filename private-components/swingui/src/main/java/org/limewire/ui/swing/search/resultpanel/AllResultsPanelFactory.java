package org.limewire.ui.swing.search.resultpanel;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.swing.EventSelectionModel;

import org.limewire.core.api.search.Search;
import org.limewire.ui.swing.search.model.VisualSearchResult;

public interface AllResultsPanelFactory {
    
    AllResultsPanel create(
        EventList<VisualSearchResult> eventList,
        EventSelectionModel<VisualSearchResult> selectionModel,
        Search search);
}
