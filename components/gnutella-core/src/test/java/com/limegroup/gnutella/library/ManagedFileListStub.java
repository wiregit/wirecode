package com.limegroup.gnutella.library;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.limewire.concurrent.ListeningFuture;
import org.limewire.listener.EventListener;

import com.limegroup.gnutella.xml.LimeXMLDocument;

public class ManagedFileListStub extends AbstractFileListStub implements ManagedFileList {


    @Override
    public void addManagedListStatusListener(EventListener<ManagedListStatusEvent> listener) {
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
    public Collection<String> getDefaultManagedExtensions() {
        return Collections.emptySet();
    }

    @Override
    public List<File> getDirectoriesToManageRecursively() {
        return Collections.emptyList();
    }

    @Override
    public Collection<String> getManagedExtensions() {
        return Collections.emptySet();
    }

    @Override
    public boolean isLoadFinished() {
        return true;
    }

    @Override
    public void removeManagedListStatusListener(EventListener<ManagedListStatusEvent> listener) {
    }

    @Override
    public ListeningFuture<List<ListeningFuture<FileDesc>>> setManagedExtensions(Collection<String> extensions) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public ListeningFuture<List<ListeningFuture<FileDesc>>> setManagedFolders(
            Collection<File> recursiveFoldersToManage, Collection<File> foldersToExclude) {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public boolean isDirectoryAllowed(File folder) {
        return folder.isDirectory();
    }
    
    @Override
    public boolean isDirectoryExcluded(File folder) {
        return false;
    }
}
