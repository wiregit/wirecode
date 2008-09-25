package org.limewire.ui.swing.search.resultpanel;

import org.limewire.core.api.download.SearchResultDownloader;
import org.limewire.core.api.search.Search;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.search.model.VisualSearchResult;

import ca.odell.glazedlists.EventList;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class AudioResultsPanel extends BaseResultPanel {
    
    @AssistedInject
    public AudioResultsPanel(
        @Assisted EventList<VisualSearchResult> eventList,
        SearchResultDownloader searchResultDownloader,
        @Assisted Search search,
        Navigator navigator) {
        super("Audio from Everyone", eventList, new MusicTableFormat(),
            searchResultDownloader, search, navigator);
    }
}