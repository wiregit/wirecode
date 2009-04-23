package com.limegroup.gnutella.library;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.Lock;

import org.limewire.listener.EventListener;

import com.limegroup.gnutella.URN;

class GnutellaFileViewImpl implements GnutellaFileView {
    
    private final GnutellaFileCollectionImpl gnutellaCollection;
    
    public GnutellaFileViewImpl(GnutellaFileCollectionImpl gnutCollection) {
        this.gnutellaCollection = gnutCollection;
    }

    @Override
    public List<FileDesc> getFilesInDirectory(File directory) {
        return gnutellaCollection.getFilesInDirectory(directory);
    }

    @Override
    public long getNumBytes() {
        return gnutellaCollection.getNumBytes();
    }

    @Override
    public boolean hasApplicationSharedFiles() {
        return gnutellaCollection.hasApplicationSharedFiles();
    }

    @Override
    public boolean isFileApplicationShare(String filename) {
        return gnutellaCollection.isFileApplicationShare(filename);
    }

    @Override
    public void removeDocuments() {
        gnutellaCollection.removeDocuments();
    }

    @Override
    public void addFileViewListener(EventListener<FileViewChangeEvent> listener) {
        gnutellaCollection.addFileViewListener(listener);
    }

    @Override
    public boolean contains(File file) {
        return gnutellaCollection.contains(file);
    }

    @Override
    public boolean contains(FileDesc fileDesc) {
        return gnutellaCollection.contains(fileDesc);
    }

    @Override
    public FileDesc getFileDesc(URN urn) {
        return gnutellaCollection.getFileDesc(urn);
    }

    @Override
    public FileDesc getFileDesc(File f) {
        return gnutellaCollection.getFileDesc(f);
    }

    @Override
    public FileDesc getFileDescForIndex(int index) {
        return gnutellaCollection.getFileDescForIndex(index);
    }

    @Override
    public List<FileDesc> getFileDescsMatching(URN urn) {
        return gnutellaCollection.getFileDescsMatching(urn);
    }

    @Override
    public Lock getReadLock() {
        return gnutellaCollection.getReadLock();
    }

    @Override
    public Iterator<FileDesc> iterator() {
        return gnutellaCollection.iterator();
    }

    @Override
    public Iterable<FileDesc> pausableIterable() {
        return gnutellaCollection.pausableIterable();
    }

    @Override
    public void removeFileViewListener(EventListener<FileViewChangeEvent> listener) {
        gnutellaCollection.removeFileViewListener(listener);
    }

    @Override
    public int size() {
        return gnutellaCollection.size();
    }

}
