package org.limewire.ui.swing.search.resultpanel;

import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.search.FromActions;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.table.RowColorResolver;

public interface ListViewTableEditorRendererFactory {
    
    ListViewTableEditorRenderer create(
            ActionColumnTableCellEditor actionEditor, 
            String searchText, 
            FromActions fromActions, 
            Navigator navigator, 
            RowColorResolver<VisualSearchResult> colorResolver);
    
}
