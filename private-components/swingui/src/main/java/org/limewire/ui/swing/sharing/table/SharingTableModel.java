package org.limewire.ui.swing.sharing.table;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.TransformedList;
import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.swing.EventTableModel;

public class SharingTableModel extends EventTableModel<LocalFileItem> {

    private EventList<LocalFileItem> sharedItems;
    
    private LocalFileList fileList;
    
    public SharingTableModel(EventList<LocalFileItem> sharedItems, LocalFileList fileList, TableFormat<LocalFileItem> tableFormat) {
        super(sharedItems, tableFormat);
        this.sharedItems = sharedItems;
        this.fileList = fileList;
    }
    
    public void dispose() {
        if(sharedItems instanceof TransformedList) {
            ((TransformedList)sharedItems).dispose();
        }
    }
    
    public void removeFile(int index) { 
        LocalFileItem item = sharedItems.get(index);
        fileList.removeFile(item.getFile());
    }
    
    public LocalFileList getFileList() {
        return fileList;
    }
    
    public LocalFileItem getFileItem(int index) {
        return sharedItems.get(index);
    }
}
