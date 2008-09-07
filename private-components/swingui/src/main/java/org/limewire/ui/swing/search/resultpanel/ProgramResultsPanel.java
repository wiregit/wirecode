package org.limewire.ui.swing.search.resultpanel;

import ca.odell.glazedlists.EventList;
import org.limewire.core.api.download.SearchResultDownloader;
import org.limewire.core.api.search.Search;
import org.limewire.ui.swing.search.model.VisualSearchResult;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class ProgramResultsPanel extends BaseResultPanel {
    
    @AssistedInject public ProgramResultsPanel(
        @Assisted EventList<VisualSearchResult> eventList,
        ProgramTableFormat tableFormat,
        SearchResultDownloader searchResultDownloader,
        @Assisted Search search) {
        super("Programs from Everyone", eventList, tableFormat,
            searchResultDownloader, search);
    }
}