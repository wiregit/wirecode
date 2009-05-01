package com.limegroup.gnutella.library;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.limewire.collection.IntSet;

import com.limegroup.gnutella.URN;

abstract class AbstractFileView implements FileView {
    
    private final IntSet indexes = new IntSet();
    protected final LibraryImpl library;
    
    public AbstractFileView(LibraryImpl library) {
        this.library = library;
    }
    
    public IntSet getIndexes() {
        return indexes;
    }
    
    @Override
    public boolean contains(File file) {
        return getFileDesc(file) != null;
    }

    @Override
    public boolean contains(FileDesc fileDesc) {
        getReadLock().lock();
        try {
            return indexes.contains(fileDesc.getIndex());
        } finally {
            getReadLock().unlock();
        }
    }

    @Override
    public FileDesc getFileDesc(URN urn) {
        List<FileDesc> descs = getFileDescsMatching(urn);
        if(descs.isEmpty()) {
            return null;
        } else {
            return descs.get(0);
        }
    }

    @Override
    public FileDesc getFileDesc(File f) {
        FileDesc fd = library.getFileDesc(f);
        if(fd != null && contains(fd)) {
            return fd;
        } else {
            return null;
        }
    }

    @Override
    public FileDesc getFileDescForIndex(int index) {
        return library.getFileDescForIndex(index);
    }

    @Override
    public List<FileDesc> getFileDescsMatching(URN urn) {
        List<FileDesc> fds = null;
        List<FileDesc> matching = library.getFileDescsMatching(urn);
        
        // Optimal case.
        if(matching.size() == 1 && contains(matching.get(0))) {
            return matching;
        } else {
            for(FileDesc fd : matching) {
                if(contains(fd)) {
                    if(fds == null) {
                        fds = new ArrayList<FileDesc>(matching.size());
                    }
                    fds.add(fd);
                }
            }
            
            if(fds == null) {
                return Collections.emptyList();
            } else {
                return fds;
            }
        }
    }

    @Override
    public int size() {
        getReadLock().lock();
        try {
            return indexes.size();
        } finally {
            getReadLock().unlock();
        }
    }

}
