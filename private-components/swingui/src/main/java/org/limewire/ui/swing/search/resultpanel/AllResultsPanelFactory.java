package org.limewire.ui.swing.search.resultpanel;

import ca.odell.glazedlists.EventList;
import org.limewire.core.api.search.Search;
import org.limewire.ui.swing.search.model.VisualSearchResult;

public interface AllResultsPanelFactory {
    
    AllResultsPanel create(
        EventList<VisualSearchResult> eventList,
        Search search);
}