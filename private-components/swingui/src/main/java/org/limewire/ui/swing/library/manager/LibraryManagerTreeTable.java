package org.limewire.ui.swing.library.manager;

import javax.swing.ListSelectionModel;

import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.treetable.TreeTableModel;

public class LibraryManagerTreeTable extends JXTreeTable {

    public LibraryManagerTreeTable() {        
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }
    
    @Override
    public void setTreeTableModel(TreeTableModel model) {
        super.setTreeTableModel(model);
        
        getColumn(1).setCellRenderer(new ScanButtonRenderer());
        getColumn(2).setCellRenderer(new DontScanButtonRenderer());
        getColumn(1).setMinWidth(80);
        getColumn(1).setMaxWidth(80);
        getColumn(2).setMinWidth(80);
        getColumn(2).setMaxWidth(80);
    }
}
