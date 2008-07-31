package org.limewire.core.impl.library;

import org.limewire.core.api.library.FileList;

public class FileListAdapter implements FileList {
    
    private final com.limegroup.gnutella.FileList fileList;
    
    public FileListAdapter(com.limegroup.gnutella.FileList fileList) {
        this.fileList = fileList;
    }
    
    @Override
    public long size() {
        if(fileList != null) {
            return fileList.size();
        } else {
            return 0;
        }
    }

}
