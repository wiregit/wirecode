package org.limewire.core.impl.library;

import java.io.File;

import org.limewire.core.api.library.FileItem;

public class MockFileItem implements FileItem {

    private final String name;
    private final long size;
    private final long creationTime;
    private final long lastModifiedTime;
    
    public MockFileItem(String name, long size, long creationTime, long lastModified ) {
        this.name = name;
        this.size = size;
        this.creationTime = creationTime;
        this.lastModifiedTime = lastModified;
    }
    
    @Override
    public long getCreationTime() {
        return creationTime;
    }

    @Override
    public File getFile() {
        return null;
    }

    @Override
    public long getLastModifiedTime() {
        return lastModifiedTime;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public long getSize() {
        return size;
    }
}
