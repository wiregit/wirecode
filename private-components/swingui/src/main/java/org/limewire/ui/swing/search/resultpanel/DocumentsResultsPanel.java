package org.limewire.ui.swing.search.resultpanel;

import ca.odell.glazedlists.EventList;
import org.limewire.core.api.download.SearchResultDownloader;
import org.limewire.core.api.search.Search;
import org.limewire.ui.swing.search.model.VisualSearchResult;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class DocumentsResultsPanel extends BaseResultPanel {
        
    @AssistedInject public DocumentsResultsPanel(
        @Assisted EventList<VisualSearchResult> eventList,
        DocumentTableFormat tableFormat,
        SearchResultDownloader searchResultDownloader,
        @Assisted Search search) {
        super("Documents from Everyone", eventList, tableFormat,
            searchResultDownloader, search);
    }
}