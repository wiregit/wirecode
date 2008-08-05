package org.limewire.core.impl.library;

import java.io.File;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.LibraryListEventType;
import org.limewire.core.api.library.LibraryListListener;
import org.limewire.core.api.library.LibraryManager;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;

import com.google.inject.Inject;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.FileEventListener;
import com.limegroup.gnutella.FileListListener;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.FileManagerEvent;

class LibraryManagerImpl implements LibraryManager, FileEventListener {
    
    private final FileManager fileManager;
    private final List<Listener> listeners = new CopyOnWriteArrayList<Listener>();
    
    private EventList<FileItem> allFileList;
    private EventList<FileItem> gnutellaList;
    private EventList<FileItem> buddyList;
    
    @Inject
    LibraryManagerImpl(FileManager fileManager) {
        this.fileManager = fileManager;
        
        allFileList = GlazedLists.threadSafeList(new BasicEventList<FileItem>());
        gnutellaList = GlazedLists.threadSafeList(new BasicEventList<FileItem>());
        buddyList = GlazedLists.threadSafeList(new BasicEventList<FileItem>());
        
        registerListeners();
    }
    
    private void registerListeners() {
        fileManager.addFileEventListener(this);
        
        fileManager.getSharedFileList().addFileListListener(new FileListener(gnutellaList));
        fileManager.getBuddyFileList().addFileListListener(new FileListener(buddyList));
    }

    @Override
    public void addLibraryLisListener(LibraryListListener libraryListener) {
        Listener listener = new Listener(libraryListener);
        listeners.add(listener);
        fileManager.addFileEventListener(listener);
    }
    
    @Override
    public void removeLibraryListener(LibraryListListener libraryListener) {
        for(Iterator<Listener> iter = listeners.iterator(); iter.hasNext(); ) {
            Listener next = iter.next();
            if(next.listener == libraryListener) {
                iter.remove();
                fileManager.removeFileEventListener(next);
                break;
            }
        }
    }

    @Override
    public EventList<FileItem> getAllFiles() {
        return allFileList;
    }
    
    @Override
    public EventList<FileItem> getAllBuddyList() {
        return buddyList;
    }

    @Override
    public EventList<FileItem> getGnutellaList() {
        return gnutellaList;
    }    

    @Override
    public Map<String, EventList<FileItem>> getUniqueLists() {
        return Collections.emptyMap();
    }
    
    @Override
    public void addBuddy(String name) {
        fileManager.addBuddyFileList(name);
    }

    @Override
    public void removeBuddy(String name) {
        fileManager.removeBuddyFileList(name);
    }
    

    @Override
    public void handleFileEvent(FileManagerEvent evt) {
        switch(evt.getType()) {
            case ADD_FILE:
                allFileList.add(new CoreFileItem(evt.getNewFileDesc()));
                break;
            case FILEMANAGER_LOAD_STARTED:
                allFileList.clear();
                break;
            case REMOVE_FILE:
                removeFileItem(allFileList, evt.getNewFileDesc());
                break;
            case RENAME_FILE:
            case CHANGE_FILE:
                removeFileItem(allFileList, evt.getOldFileDesc());
                allFileList.add(new CoreFileItem(evt.getNewFileDesc()));
        }
    }
    
    private class FileListener implements FileListListener {

        private final EventList<FileItem> list;
        
        public FileListener(EventList<FileItem> list) {
            this.list = list;
        }
        
        @Override
        public void addEvent(FileDesc fileDesc) { 
            list.add(new CoreFileItem(fileDesc));
        }

        @Override
        public void changeEvent(FileDesc oldDesc, FileDesc newDesc) {
            removeFileItem(list, oldDesc);
            list.add(new CoreFileItem(oldDesc));            
        }

        @Override
        public void removeEvent(FileDesc fileDesc) {
            removeFileItem(list, fileDesc);
        }
    }
    
    private static class Listener implements FileEventListener {
        private final LibraryListListener listener;
        
        Listener(LibraryListListener listener) {
            this.listener = listener;
        }
        
        @Override
        public void handleFileEvent(FileManagerEvent evt) {
            switch(evt.getType()) {
            case REMOVE_FILE: listener.handleLibraryListEvent(LibraryListEventType.FILE_REMOVED);
            case ADD_FILE: listener.handleLibraryListEvent(LibraryListEventType.FILE_ADDED);
            }
        }
    }

    
    private void removeFileItem(EventList<FileItem> list, FileDesc toRemove) {
        for(FileItem item : list) {
            if(item.getFile().equals(toRemove.getFile())) {
                list.remove(item);
                return;
            }
        }
    }

    @Override
    public void addGnutellaFile(File file) { 
        fileManager.addSharedFileAlways(file);
    }
}
