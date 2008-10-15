package org.limewire.ui.swing.sharing.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.jdesktop.swingx.JXTable;
import org.limewire.ui.swing.sharing.table.SharingTableModel;

/**
 * Creates a button in a table cell and removes this item from the table and the
 * fileManager when the button is clicked.
 */
public class SharingRemoveTableAction extends AbstractAction {

    private JXTable table;
    
    public SharingRemoveTableAction(JXTable table) {
        super("");
        
        this.table = table;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        final int index = table.getEditingRow();
        table.getCellEditor().cancelCellEditing();

        if(index > -1) { 
            SharingTableModel model = (SharingTableModel) table.getModel(); 
            model.removeFile(index);
        }
    }
}
