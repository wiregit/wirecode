package org.limewire.ui.swing.search.resultpanel.list;

import org.limewire.ui.swing.listener.MousePopupListener;
import org.limewire.ui.swing.search.model.SearchResultsModel;
import org.limewire.ui.swing.search.resultpanel.DownloadHandler;
import org.limewire.ui.swing.search.store.StoreController;

public interface ListViewTableEditorRendererFactory {
    
    ListViewTableEditorRenderer create(
            DownloadHandler downloadHandler,
            ListViewRowHeightRule rowHeightRule,
            ListViewDisplayedRowsLimit limit,
            SearchResultsModel searchResultsModel,
            MousePopupListener storePopupListener,
            StoreController storeController);
    
}
