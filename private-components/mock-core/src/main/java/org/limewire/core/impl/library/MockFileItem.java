package org.limewire.core.impl.library;

import java.io.File;

import org.limewire.core.api.library.FileItem;

public class MockFileItem implements FileItem {

    private final String name;
    private final long size;
    private final long creationTime;
    private final long lastModifiedTime;
    private final int numHits;
    private final int numUploads;
    
    public MockFileItem(String name, long size, long creationTime, long lastModified,
            int numHits, int numUploads) {
        this.name = name;
        this.size = size;
        this.creationTime = creationTime;
        this.lastModifiedTime = lastModified;
        this.numHits = numHits;
        this.numUploads = numUploads;
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

    @Override
    public int getNumHits() {
        return numHits;
    }

    @Override
    public int getNumUploads() {
        return numUploads;
    }
}
