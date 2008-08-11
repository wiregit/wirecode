package org.limewire.ui.swing.search.resultpanel;

import org.limewire.core.api.download.SearchResultDownloader;
import org.limewire.core.api.search.Search;
import org.limewire.ui.swing.search.model.VisualSearchResult;

import ca.odell.glazedlists.EventList;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class AllResultsPanel extends BaseResultPanel {
        
    @AssistedInject public AllResultsPanel(
        @Assisted EventList<VisualSearchResult> eventList,
        SearchResultDownloader searchResultDownloader,
        @Assisted Search search) {
        super("All Results from Everyone", eventList, 
            searchResultDownloader, search);
    }
}