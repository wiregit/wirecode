package org.limewire.ui.swing.search.resultpanel.list;

import org.limewire.ui.swing.search.resultpanel.DownloadHandler;
import org.limewire.ui.swing.search.resultpanel.StoreController;

public interface ListViewTableEditorRendererFactory {
    
    ListViewTableEditorRenderer create(
            DownloadHandler downloadHandler,
            ListViewRowHeightRule rowHeightRule,
            ListViewDisplayedRowsLimit limit,
            StoreController storeController);
    
}
