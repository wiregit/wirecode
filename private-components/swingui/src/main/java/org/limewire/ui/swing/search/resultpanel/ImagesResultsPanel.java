package org.limewire.ui.swing.search.resultpanel;

import ca.odell.glazedlists.EventList;
import org.limewire.core.api.download.SearchResultDownloader;
import org.limewire.core.api.search.Search;
import org.limewire.ui.swing.search.model.VisualSearchResult;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class ImagesResultsPanel extends BaseResultPanel {
    
    @AssistedInject public ImagesResultsPanel(
        @Assisted EventList<VisualSearchResult> eventList,
        SearchResultDownloader searchResultDownloader,
        @Assisted Search search) {
        super("Images from Everyone", eventList, new ImageTableFormat(),
            searchResultDownloader, search);
    }
}