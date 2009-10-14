package com.limegroup.gnutella.library;

import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.List;

import org.limewire.concurrent.ListeningFuture;
import org.limewire.core.api.library.FileProcessingEvent;
import org.limewire.listener.EventListener;

import com.google.inject.Singleton;
import com.limegroup.gnutella.xml.LimeXMLDocument;

@Singleton
public class LibraryStub extends AbstractFileCollectionStub implements Library {
    
    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void addManagedListStatusListener(EventListener<LibraryStatusEvent> listener) {
    }

    @Override
    public ListeningFuture<FileDesc> fileChanged(File file, List<? extends LimeXMLDocument> xmlDocs) {
        throw new UnsupportedOperationException();   
    }

    @Override
    public ListeningFuture<FileDesc> fileRenamed(File oldName, File newName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isLoadFinished() {
        return true;
    }

    @Override
    public void removeManagedListStatusListener(EventListener<LibraryStatusEvent> listener) {
    }
    
    @Override
    public boolean isDirectoryAllowed(File folder) {
        return folder.isDirectory();
    }

    @Override
    public boolean isProgramManagingAllowed() {
        return false;
    }
    
    @Override
    public boolean isFileAllowed(File file) {
        return true;
    }

    @Override
    public void addFileProcessingListener(EventListener<FileProcessingEvent> listener) {
        
    }

    @Override
    public void removeFileProcessingListener(EventListener<FileProcessingEvent> listener) {
        
    }

    @Override
    public void cancelPendingTasks() {
        
    }

    @Override
    public int peekPublicSharedListCount() {
        return 0;
    }
}
