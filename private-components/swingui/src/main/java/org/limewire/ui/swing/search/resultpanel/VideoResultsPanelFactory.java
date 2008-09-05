package org.limewire.ui.swing.search.resultpanel;

import ca.odell.glazedlists.EventList;
import org.limewire.core.api.search.Search;
import org.limewire.ui.swing.search.model.VisualSearchResult;

public interface VideoResultsPanelFactory {
    
    VideoResultsPanel create(
        EventList<VisualSearchResult> eventList,
        Search search);
}