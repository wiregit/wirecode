package org.limewire.ui.swing.search.resultpanel;

import java.awt.Color;

import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.search.RemoteHostActions;

public interface ListViewTableEditorRendererFactory {
    
    ListViewTableEditorRenderer create(
            ActionColumnTableCellEditor actionEditor, 
            String searchText, 
            RemoteHostActions fromActions, 
            Navigator navigator, 
            Color rowSelectionColor);
    
}
