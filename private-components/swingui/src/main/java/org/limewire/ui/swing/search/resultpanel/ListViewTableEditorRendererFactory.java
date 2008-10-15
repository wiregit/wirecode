package org.limewire.ui.swing.search.resultpanel;

import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.search.RemoteHostActions;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.table.RowColorResolver;

public interface ListViewTableEditorRendererFactory {
    
    ListViewTableEditorRenderer create(
            ActionColumnTableCellEditor actionEditor, 
            String searchText, 
            RemoteHostActions fromActions, 
            Navigator navigator, 
            RowColorResolver<VisualSearchResult> colorResolver);
    
}
