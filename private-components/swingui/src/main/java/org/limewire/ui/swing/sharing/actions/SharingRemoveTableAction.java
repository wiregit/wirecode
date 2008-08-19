package org.limewire.ui.swing.sharing.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Icon;

import org.jdesktop.swingx.JXTable;
import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.FileList;
import org.limewire.ui.swing.sharing.table.SharingTableModel;

/**
 * Creates a button in a table cell and removes this item from the table and the
 * fileManager when the button is clicked.
 */
public class SharingRemoveTableAction extends AbstractAction {

    private FileList fileList;
    private JXTable table;
    
    public SharingRemoveTableAction(FileList fileList, JXTable table, Icon icon) {
        super("", icon);
        
        this.fileList = fileList;
        this.table = table;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        int index = table.getEditingRow();
        table.getCellEditor().cancelCellEditing();

        if(index > -1) {
            SharingTableModel model = (SharingTableModel) table.getModel();
            FileItem item = model.getFileItem(index);
            fileList.removeFile(item.getFile());
        }
    }
}
