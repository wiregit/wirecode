package org.limewire.ui.swing.search.resultpanel;

import ca.odell.glazedlists.EventList;
import org.limewire.core.api.download.SearchResultDownloader;
import org.limewire.core.api.search.Search;
import org.limewire.ui.swing.search.model.VisualSearchResult;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.limewire.ui.swing.nav.NavigableTree;

public class VideoResultsPanel extends BaseResultPanel {
    
    @AssistedInject
    public VideoResultsPanel(
        @Assisted EventList<VisualSearchResult> eventList,
        SearchResultDownloader searchResultDownloader,
        @Assisted Search search,
        NavigableTree navTree) {
        super("Video from Everyone", eventList, new VideoTableFormat(),
            searchResultDownloader, search, navTree);
    }
}