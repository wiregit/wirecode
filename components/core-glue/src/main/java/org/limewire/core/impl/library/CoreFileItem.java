package org.limewire.core.impl.library;

import java.io.File;

import org.limewire.core.api.library.FileItem;

import com.limegroup.gnutella.FileDesc;

public class CoreFileItem implements FileItem {

    private final File file;
    private final long creationTime;
    private final long modifiedTime;
    private final long size;
    
    public CoreFileItem(FileDesc fileDesc) {
        this.file = fileDesc.getFile();
        this.creationTime = fileDesc.getCreationTime();
        this.modifiedTime = -1;
        this.size = fileDesc.getFileSize();
    }
    
    @Override
    public long getCreationTime() {
        return creationTime;
    }

    @Override
    public File getFile() {
        return file;
    }

    @Override
    public long getLastModifiedTime() {
        return modifiedTime;
    }

    @Override
    public String getName() {
        return file.getName();
    }

    @Override
    public long getSize() {
        return size;
    }

}
