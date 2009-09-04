package org.limewire.ui.swing.search.resultpanel.list;

import org.limewire.ui.swing.search.model.SearchResultsModel;
import org.limewire.ui.swing.search.resultpanel.DownloadHandler;

public interface ListViewTableEditorRendererFactory {
    
    ListViewTableEditorRenderer create(
            DownloadHandler downloadHandler,
            ListViewRowHeightRule rowHeightRule,
            ListViewDisplayedRowsLimit limit,
            SearchResultsModel searchResultsModel);
    
}
