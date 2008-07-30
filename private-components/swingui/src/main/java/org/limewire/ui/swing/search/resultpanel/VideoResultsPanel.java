package org.limewire.ui.swing.search.resultpanel;

import org.limewire.core.api.download.SearchResultDownloader;
import org.limewire.core.api.search.Search;
import org.limewire.ui.swing.search.model.VisualSearchResult;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import ca.odell.glazedlists.swing.EventListModel;
import ca.odell.glazedlists.swing.EventSelectionModel;

public class VideoResultsPanel extends BaseResultPanel {
    
    @AssistedInject public VideoResultsPanel(
        @Assisted EventListModel<VisualSearchResult> listModel,
        @Assisted EventSelectionModel<VisualSearchResult> selectionModel,
        SearchResultDownloader searchResultDownloader,
        @Assisted Search search) {
        super("Video from Everyone", listModel, selectionModel,
            searchResultDownloader, search);
    }
}