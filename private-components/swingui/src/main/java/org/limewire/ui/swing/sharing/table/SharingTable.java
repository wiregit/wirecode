package org.limewire.ui.swing.sharing.table;

import javax.swing.ListSelectionModel;

import org.jdesktop.swingx.decorator.HighlighterFactory;
import org.limewire.core.api.library.FileItem;
import org.limewire.ui.swing.table.StripedJXTable;

import ca.odell.glazedlists.EventList;

public class SharingTable extends StripedJXTable {

    public SharingTable(EventList<FileItem> sharedItems) {
        super(new SharingTableModel(sharedItems));
        
        setColumnControlVisible(true);
        setShowHorizontalLines(false);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setHighlighters(HighlighterFactory.createSimpleStriping());
    }
    
    @Override
    public boolean isCellEditable(int row, int column) {
        // as long as an editor has been installed, assuming we want to use it
        if(getColumnModel().getColumn(column).getCellEditor() != null)
            return true;
        return false;        
    }
}
