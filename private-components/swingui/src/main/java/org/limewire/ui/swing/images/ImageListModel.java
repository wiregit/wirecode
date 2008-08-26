package org.limewire.ui.swing.images;

import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.FileList;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.swing.EventListModel;

public class ImageListModel extends EventListModel<FileItem>{

    private final EventList<FileItem> fileItems;
    
    private final FileList fileList;
    
    public ImageListModel(EventList<FileItem> fileItems, FileList fileList) {
        super(fileItems);
        this.fileItems = fileItems;
        this.fileList = fileList;
    }
    
    public FileItem getFileItem(int index) {
        return fileItems.get(index);
    }
    
    public FileList getFileList() {
        return fileList;
    }

}
