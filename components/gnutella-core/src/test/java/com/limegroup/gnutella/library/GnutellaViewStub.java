package com.limegroup.gnutella.library;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.Lock;

import org.limewire.concurrent.ListeningFuture;
import org.limewire.listener.EventListener;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.xml.LimeXMLDocument;

class GnutellaViewStub implements GnutellaFileView {
    
    private final GnutellaCollectionStub gnutellaCollectionStub;

    public GnutellaViewStub(GnutellaCollectionStub gnutellaCollectionStub) {
        super();
        this.gnutellaCollectionStub = gnutellaCollectionStub;
    }

    public ListeningFuture<FileDesc> add(File file, List<? extends LimeXMLDocument> documents) {
        return gnutellaCollectionStub.add(file, documents);
    }

    public ListeningFuture<FileDesc> add(File file) {
        return gnutellaCollectionStub.add(file);
    }

    public boolean add(FileDesc fileDesc) {
        return gnutellaCollectionStub.add(fileDesc);
    }

    public void addFileViewListener(EventListener<FileViewChangeEvent> listener) {
        gnutellaCollectionStub.addFileViewListener(listener);
    }

    public boolean contains(File file) {
        return gnutellaCollectionStub.contains(file);
    }

    public boolean contains(FileDesc fileDesc) {
        return gnutellaCollectionStub.contains(fileDesc);
    }

    public FileDesc getFileDesc(File f) {
        return gnutellaCollectionStub.getFileDesc(f);
    }

    public FileDesc getFileDesc(URN urn) {
        return gnutellaCollectionStub.getFileDesc(urn);
    }

    public FileDesc getFileDescForIndex(int index) {
        return gnutellaCollectionStub.getFileDescForIndex(index);
    }

    public List<FileDesc> getFileDescsMatching(URN urn) {
        return gnutellaCollectionStub.getFileDescsMatching(urn);
    }

    public List<FileDesc> getFilesInDirectory(File directory) {
        return gnutellaCollectionStub.getFilesInDirectory(directory);
    }

    public long getNumBytes() {
        return gnutellaCollectionStub.getNumBytes();
    }

    public Lock getReadLock() {
        return gnutellaCollectionStub.getReadLock();
    }

    public boolean hasApplicationSharedFiles() {
        return gnutellaCollectionStub.hasApplicationSharedFiles();
    }

    public boolean isFileApplicationShare(String filename) {
        return gnutellaCollectionStub.isFileApplicationShare(filename);
    }

    public Iterator<FileDesc> iterator() {
        return gnutellaCollectionStub.iterator();
    }

    public Iterable<FileDesc> pausableIterable() {
        return gnutellaCollectionStub.pausableIterable();
    }

    public void removeDocuments() {
        gnutellaCollectionStub.removeDocuments();
    }

    public void removeFileViewListener(EventListener<FileViewChangeEvent> listener) {
        gnutellaCollectionStub.removeFileViewListener(listener);
    }

    public int size() {
        return gnutellaCollectionStub.size();
    }

}
