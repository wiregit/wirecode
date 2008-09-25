package org.limewire.ui.swing.search.resultpanel;

import org.limewire.core.api.download.SearchResultDownloader;
import org.limewire.core.api.search.Search;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.search.model.VisualSearchResult;

import ca.odell.glazedlists.EventList;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class VideoResultsPanel extends BaseResultPanel {
    
    @AssistedInject
    public VideoResultsPanel(
        @Assisted EventList<VisualSearchResult> eventList,
        SearchResultDownloader searchResultDownloader,
        @Assisted Search search,
        Navigator navigator) {
        super("Video from Everyone", eventList, new VideoTableFormat(),
            searchResultDownloader, search, navigator);
    }
}