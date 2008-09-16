package org.limewire.ui.swing.search.resultpanel;

import ca.odell.glazedlists.EventList;
import org.limewire.core.api.download.SearchResultDownloader;
import org.limewire.core.api.search.Search;
import org.limewire.ui.swing.search.model.VisualSearchResult;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.limewire.ui.swing.nav.NavigableTree;

public class ImagesResultsPanel extends BaseResultPanel {
    
    @AssistedInject
    public ImagesResultsPanel(
        @Assisted EventList<VisualSearchResult> eventList,
        SearchResultDownloader searchResultDownloader,
        @Assisted Search search,
        NavigableTree navTree) {
        super("Images from Everyone", eventList, new ImageTableFormat(),
            searchResultDownloader, search, navTree);
    }
}