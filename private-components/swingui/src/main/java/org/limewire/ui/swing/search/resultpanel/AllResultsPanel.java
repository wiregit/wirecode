package org.limewire.ui.swing.search.resultpanel;

import org.limewire.core.api.download.SearchResultDownloader;
import org.limewire.core.api.search.Search;
import org.limewire.ui.swing.search.model.VisualSearchResult;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import ca.odell.glazedlists.swing.EventListModel;
import ca.odell.glazedlists.swing.EventSelectionModel;

public class AllResultsPanel extends BaseResultPanel {
        
    @AssistedInject public AllResultsPanel(@Assisted EventListModel<VisualSearchResult> listModel,
            @Assisted EventSelectionModel<VisualSearchResult> selectionModel,
            SearchResultDownloader searchResultDownloader,
            @Assisted Search search) {
        super("All Results from Everyone", listModel, selectionModel, searchResultDownloader, search);
    }
}