package org.limewire.ui.swing.search.resultpanel;

import org.limewire.core.api.download.SearchResultDownloader;
import org.limewire.core.api.search.Search;
import org.limewire.ui.swing.search.model.VisualSearchResult;

import ca.odell.glazedlists.EventList;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class OtherResultsPanel extends BaseResultPanel {
    
    @AssistedInject public OtherResultsPanel(
        @Assisted EventList<VisualSearchResult> eventList,
        OtherTableFormat tableFormat,
        SearchResultDownloader searchResultDownloader,
        @Assisted Search search) {
        super("Other from Everyone", eventList, tableFormat,
            searchResultDownloader, search);
    }
}