package org.limewire.ui.swing.sharing.table;

import javax.swing.ListSelectionModel;

import org.jdesktop.swingx.JXTable;
import org.limewire.core.api.library.FileItem;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.gui.TableFormat;

public class SharingFancyTable extends JXTable {

    public SharingFancyTable(EventList<FileItem> sharedItems, TableFormat<FileItem> tableFormat) {
        super(new SharingTableModel(sharedItems, tableFormat));
        
        setColumnControlVisible(false);
        setShowGrid(false, false);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setFillsViewportHeight(false);
        getTableHeader().setFocusable(false);
        getTableHeader().setReorderingAllowed(false);
    }
    
    @Override
    public boolean isCellEditable(int row, int column) {
        // as long as an editor has been installed, assuming we want to use it
        if(getColumnModel().getColumn(column).getCellEditor() != null)
            return true;
        return false;        
    }
}
