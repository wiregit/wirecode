package org.limewire.ui.swing.search.resultpanel.list;

import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.search.resultpanel.ActionColumnTableCellEditor;
import org.limewire.ui.swing.search.resultpanel.DownloadHandler;

public interface ListViewTableEditorRendererFactory {
    
    ListViewTableEditorRenderer create(
            ActionColumnTableCellEditor actionEditor, 
            String searchText, 
            Navigator navigator, 
            DownloadHandler downloadHandler, ListViewDisplayedRowsLimit limit);
    
}
