package org.limewire.ui.swing.sharing.table;

import org.limewire.core.api.library.FileItem;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.swing.EventTableModel;

public class SharingTableModel extends EventTableModel<FileItem> {

    private final EventList<FileItem> sharedItems;
    
    public SharingTableModel(EventList<FileItem> sharedItems, TableFormat<FileItem> tableFormat) {
        super(sharedItems, tableFormat);
        this.sharedItems = sharedItems;
    }
    
    public FileItem getFileItem(int index) {
        return sharedItems.get(index);
    }
}
