package org.limewire.ui.swing.search.resultpanel;

import org.limewire.core.api.search.Search;
import org.limewire.ui.swing.search.model.VisualSearchResult;

import ca.odell.glazedlists.EventList;

public interface ImagesResultsPanelFactory {
    
    ImagesResultsPanel create(
        EventList<VisualSearchResult> eventList,
        Search search);
}
