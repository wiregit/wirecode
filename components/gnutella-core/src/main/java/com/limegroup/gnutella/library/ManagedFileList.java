package com.limegroup.gnutella.library;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executor;

import org.limewire.listener.EventListener;
import org.limewire.listener.EventMulticaster;
import org.limewire.listener.EventMulticasterImpl;

import com.limegroup.gnutella.URN;

class ManagedFileList implements FileList, FileListPackage, EventListener<FileManagerEvent> {
    
    private final FileManagerImpl fileManager;
    private final Executor eventThread;
    private final EventMulticaster<FileListChangedEvent> listenerSupport;

    public ManagedFileList(Executor eventThread, FileManagerImpl fileManager) {
        this.fileManager = fileManager;
        this.eventThread = eventThread;        
        this.listenerSupport = new EventMulticasterImpl<FileListChangedEvent>();
        fileManager.addFileEventListener(this);
    }
    
    @Override
    public void handleEvent(FileManagerEvent event) {
        switch(event.getType()) {
        case ADD_FILE:
            dispatch(new FileListChangedEvent(ManagedFileList.this, FileListChangedEvent.Type.ADDED, event.getNewFileDesc()));
            break;
        case REMOVE_FILE:
            dispatch(new FileListChangedEvent(ManagedFileList.this, FileListChangedEvent.Type.REMOVED, event.getNewFileDesc()));
            break;
        case CHANGE_FILE:
        case RENAME_FILE:
            dispatch(new FileListChangedEvent(ManagedFileList.this, FileListChangedEvent.Type.CHANGED, event.getOldFileDesc(), event.getNewFileDesc()));
            break;            
        }
    }
    
    private void dispatch(final FileListChangedEvent event) {
        eventThread.execute(new Runnable() {
            @Override
            public void run() {
                listenerSupport.broadcast(event);
            }
        });
    }
    
    @Override
    public boolean contains(File file) {
        return contains(fileManager.getFileDesc(file));
    }
    
    @Override
    public boolean contains(FileDesc fileDesc) {
        return fileDesc != null;
    }
    
    @Override
    public void add(File file) {
        fileManager.addFile(file);
    }

    @Override
    public boolean add(FileDesc fileDesc) {
        throw new UnsupportedOperationException("cannot add FDs directly.");
    }

    @Override
    public void addFileListListener(EventListener<FileListChangedEvent> listener) {
        listenerSupport.addListener(listener);
    }

    @Override
    public void cleanupListeners() {        
        fileManager.removeFileEventListener(this);
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("cannot clear managed list");
    }

    @Override
    public List<FileDesc> getAllFileDescs() {
        throw new UnsupportedOperationException("cannot get all from managed list");
    }

    @Override
    public FileDesc getFileDesc(File f) {
        return fileManager.getFileDesc(f);
    }

    @Override
    public FileDesc getFileDesc(URN urn) {
        return fileManager.getFileDesc(urn);
    }

    @Override
    public List<FileDesc> getFilesInDirectory(File directory) {
        throw new UnsupportedOperationException("cannot iterate on files in directory");
    }

    @Override
    public File[] getIndividualFiles() {
        throw new UnsupportedClassVersionError("no indiv files for managed list");
    }

    @Override
    public Object getLock() {
        return this;
    }

    @Override
    public int getNumBytes() {
        throw new UnsupportedOperationException("numbytes not supported");
    }

    @Override
    public int getNumForcedFiles() {
        throw new UnsupportedOperationException("numForcedFiles not supported");
    }

    @Override
    public int getNumIndividualFiles() {
        throw new UnsupportedOperationException("num indiv not supported");
    }

    @Override
    public boolean isAddNewAudioAlways() {
        throw new UnsupportedOperationException("not supported");
    }

    @Override
    public boolean isAddNewImageAlways() {
        throw new UnsupportedOperationException("not supported");
    }

    @Override
    public boolean isAddNewVideoAlways() {
        throw new UnsupportedOperationException("not supported");
    }

    @Override
    public boolean isFileAddable(File file) {
        throw new UnsupportedOperationException("not supported");
    }

    @Override
    public boolean isIndividualFile(File file) {
        throw new UnsupportedOperationException("not supported");
    }

    @Override
    public Iterator<FileDesc> iterator() {
        throw new UnsupportedOperationException("not supported");
    }
    
    @Override
    public boolean remove(File file) {
        return fileManager.removeFile(file) != null;
    }

    @Override
    public boolean remove(FileDesc fileDesc) {
        return remove(fileDesc.getFile());
    }

    @Override
    public void removeFileListListener(EventListener<FileListChangedEvent> listener) {
        listenerSupport.removeListener(listener);
    }

    @Override
    public void setAddNewAudioAlways(boolean value) {
        throw new UnsupportedOperationException("not supported");
    }

    @Override
    public void setAddNewImageAlways(boolean value) {
        throw new UnsupportedOperationException("not supported");
    }

    @Override
    public void setAddNewVideoAlways(boolean value) {
        throw new UnsupportedOperationException("not supported");
    }

    @Override
    public int size() {
        return fileManager.size();
    }

    @Override
    public void addPendingFile(File file) {
        throw new UnsupportedOperationException("not supported");
    }

    @Override
    public void addPendingFileAlways(File file) {
        throw new UnsupportedOperationException("not supported");
    }

    @Override
    public void addPendingFileForSession(File file) {
        throw new UnsupportedOperationException("not supported");
    }
    
    

}
