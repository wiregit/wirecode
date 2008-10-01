package org.limewire.ui.swing.search.resultpanel;

import org.limewire.core.api.download.SearchResultDownloader;
import org.limewire.core.api.search.Search;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.search.RowSelectionPreserver;
import org.limewire.ui.swing.search.model.VisualSearchResult;

import ca.odell.glazedlists.EventList;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class ProgramResultsPanel extends BaseResultPanel {
    
    @AssistedInject
    public ProgramResultsPanel(
        @Assisted EventList<VisualSearchResult> eventList,
        ProgramTableFormat tableFormat,
        SearchResultDownloader searchResultDownloader,
        @Assisted Search search,
        @Assisted RowSelectionPreserver preserver,
        Navigator navigator) {
        super("Programs from Everyone", eventList, tableFormat,
            searchResultDownloader, search, preserver, navigator);
    }
}