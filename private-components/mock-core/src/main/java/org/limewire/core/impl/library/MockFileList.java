package org.limewire.core.impl.library;

import org.limewire.core.api.library.FileList;

public class MockFileList implements FileList {
    
    private final long size;
    
    public MockFileList(long size) {
        this.size = size;
    }

    @Override
    public long size() {
        return size;
    }

}
