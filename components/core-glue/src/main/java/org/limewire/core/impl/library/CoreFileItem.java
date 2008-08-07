package org.limewire.core.impl.library;

import java.io.File;

import org.limewire.core.api.library.FileItem;

import com.limegroup.gnutella.FileDesc;

public class CoreFileItem implements FileItem {

    private final File file;
    private final String name;
    private final long creationTime;
    private final long modifiedTime;
    private final long size;
    private final int numHits;
    private final int numUploads;
    
    public CoreFileItem(FileDesc fileDesc) { 
        this.file = fileDesc.getFile();
        this.name = fileDesc.getFileName();
        this.creationTime = fileDesc.getCreationTime();
        this.modifiedTime = fileDesc.lastModified();
        this.size = fileDesc.getFileSize();
        this.numHits = fileDesc.getHitCount();
        this.numUploads = fileDesc.getCompletedUploads();
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
