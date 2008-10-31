package com.limegroup.gnutella.library;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;

import org.limewire.listener.EventListener;

import com.limegroup.gnutella.xml.LimeXMLDocument;

public class ManagedFileListStub extends AbstractFileListStub implements ManagedFileList {

    @Override
    public void addDirectoryToExcludeFromManaging(File folder) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addDirectoryToManageRecursively(File folder) {
        throw new UnsupportedOperationException();        
    }

    @Override
    public void addManagedListStatusListener(EventListener<ManagedListStatusEvent> listener) {
    }

    @Override
    public Future<FileDesc> fileChanged(File file, List<? extends LimeXMLDocument> xmlDocs) {
        throw new UnsupportedOperationException();   
    }

    @Override
    public Future<FileDesc> fileRenamed(File oldName, File newName) {
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
    public void setManagedExtensions(Collection<String> extensions) {
        throw new UnsupportedOperationException();
    }
}
