package org.limewire.ui.swing.images;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.api.library.FileItem;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.swing.EventListModel;

public class ImageListModel extends EventListModel<LocalFileItem>{

    private final EventList<LocalFileItem> fileItems;
    
    private final LocalFileList fileList;
    
    public ImageListModel(EventList<LocalFileItem> fileItems, LocalFileList fileList) {
        super(fileItems);
        this.fileItems = fileItems;
        this.fileList = fileList;
    }
    
    public void removeFile(int index) {
        LocalFileItem item = fileItems.get(index);
        fileList.removeFile(item.getFile());
    }
    
    public LocalFileList getFileList() {
        return fileList;
    }
    
    public LocalFileItem getFileItem(int index) {
        return fileItems.get(index);
    }

}
