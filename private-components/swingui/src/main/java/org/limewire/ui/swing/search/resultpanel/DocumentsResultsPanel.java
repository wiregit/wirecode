package org.limewire.ui.swing.search.resultpanel;

import org.limewire.core.api.download.SearchResultDownloader;
import org.limewire.core.api.search.Search;
import org.limewire.ui.swing.search.model.VisualSearchResult;

import ca.odell.glazedlists.swing.EventListModel;
import ca.odell.glazedlists.swing.EventSelectionModel;

public class DocumentsResultsPanel extends BaseResultPanel {
        
    public DocumentsResultsPanel(EventListModel<VisualSearchResult> listModel,
            EventSelectionModel<VisualSearchResult> selectionModel,
            SearchResultDownloader searchResultDownloader,
            Search search) {
        super("Documents from Everyone", listModel, selectionModel, searchResultDownloader, null);
    }
}