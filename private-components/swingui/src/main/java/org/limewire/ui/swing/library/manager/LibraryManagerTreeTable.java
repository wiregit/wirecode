package org.limewire.ui.swing.library.manager;

import javax.swing.ListSelectionModel;
import javax.swing.UIManager;

import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.treetable.TreeTableModel;

public class LibraryManagerTreeTable extends JXTreeTable {

    public LibraryManagerTreeTable() {        
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setEditable(true);
    }
    
    @Override
    public void setTreeTableModel(TreeTableModel model) {
        super.setTreeTableModel(model);

        // all the nodes are always folders, set the leaf node to the folder icon
        setLeafIcon(UIManager.getIcon("Tree.closedIcon"));
  
        getColumn(LibraryManagerModel.SCAN_INDEX).setCellRenderer(new ScanButtonRenderer());
        getColumn(LibraryManagerModel.SCAN_INDEX).setCellEditor(new ScanButtonEditor(this));
        getColumn(LibraryManagerModel.DONT_SCAN_INDEX).setCellEditor(new DontScanButtonEditor(this));
        getColumn(LibraryManagerModel.DONT_SCAN_INDEX).setCellRenderer(new DontScanButtonRenderer());
        
        
        getColumn(LibraryManagerModel.SCAN_INDEX).setMinWidth(80);
        getColumn(LibraryManagerModel.SCAN_INDEX).setMaxWidth(80);
        getColumn(LibraryManagerModel.DONT_SCAN_INDEX).setMinWidth(80);
        getColumn(LibraryManagerModel.DONT_SCAN_INDEX).setMaxWidth(80);
        
        //position the columns
        moveColumn(LibraryManagerModel.SCAN_INDEX, LibraryManagerModel.FOLDER);
        moveColumn(LibraryManagerModel.DONT_SCAN_INDEX, LibraryManagerModel.SCAN_INDEX);
    }

    
    @Override
    public boolean isCellEditable(int row, int col) {
        if (row >= getRowCount() || col >= getColumnCount() || row < 0 || col < 0) {
            return false;
        }
        return getColumnModel().getColumn(col).getCellEditor() != null;
    }
}
