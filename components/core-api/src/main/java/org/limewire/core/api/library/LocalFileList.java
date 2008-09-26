package org.limewire.core.api.library;

import java.io.File;

public interface LocalFileList extends FileList<LocalFileItem> {

    public void addFile(File file);
    
    public void removeFile(File file);

}
