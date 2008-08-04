package org.limewire.ui.swing.search.resultpanel;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.swing.EventSelectionModel;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import org.limewire.core.api.download.SearchResultDownloader;
import org.limewire.core.api.search.Search;
import org.limewire.ui.swing.search.model.VisualSearchResult;

public class ImagesResultsPanel extends BaseResultPanel {
    
    @AssistedInject public ImagesResultsPanel(
        @Assisted EventList<VisualSearchResult> eventList,
        @Assisted EventSelectionModel<VisualSearchResult> selectionModel,
        SearchResultDownloader searchResultDownloader,
        @Assisted Search search) {
        super("Images from Everyone", eventList, selectionModel,
            searchResultDownloader, search);
    }
}