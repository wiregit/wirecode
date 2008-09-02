package org.limewire.ui.swing.sharing.table;

import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.FileList;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.swing.EventTableModel;

public class SharingTableModel extends EventTableModel<FileItem> {

    private EventList<FileItem> sharedItems;
    
    private FileList fileList;
    
    public SharingTableModel(EventList<FileItem> sharedItems, FileList fileList, TableFormat<FileItem> tableFormat) {
        super(sharedItems, tableFormat);
        this.sharedItems = sharedItems;
        this.fileList = fileList;
    }
    
    public void removeFile(int index) { 
        FileItem item = sharedItems.get(index);
        fileList.removeFile(item.getFile());
    }
    
    public FileList getFileList() {
        return fileList;
    }
}
