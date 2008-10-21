package com.limegroup.gnutella.library;

import java.util.Iterator;

import org.limewire.collection.IntSet;
import org.limewire.collection.IntSet.IntSetIterator;

class FileListIndexedIterator implements Iterator<FileDesc> {
    
    private final FileList fileList;
    private final IntSetIterator iter;
    
    public FileListIndexedIterator(FileList fileList, IntSet intSet) {
        this.fileList = fileList;
        this.iter = intSet.iterator();
    }

    @Override
    public boolean hasNext() {
        return iter.hasNext();
    }

    @Override
    public FileDesc next() {
        return fileList.getFileDescForIndex(iter.next());
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

}
