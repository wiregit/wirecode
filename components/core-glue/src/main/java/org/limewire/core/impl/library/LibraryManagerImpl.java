package org.limewire.core.impl.library;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.FileList;
import org.limewire.core.api.library.LibraryListEventType;
import org.limewire.core.api.library.LibraryListListener;
import org.limewire.core.api.library.LibraryManager;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.FileEventListener;
import com.limegroup.gnutella.FileListListener;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.FileManagerEvent;

@Singleton
class LibraryManagerImpl implements LibraryManager, FileEventListener {
    
    private final FileManager fileManager;
    private final List<Listener> listeners = new CopyOnWriteArrayList<Listener>();
    
    private LibraryFileList libraryFileList;
    private GnutellaFileList gnutellaFileList;
    private BuddyFileList buddyFileList;
    
    @Inject
    LibraryManagerImpl(FileManager fileManager) {
        this.fileManager = fileManager;
        
        libraryFileList = new LibraryFileList(fileManager);
        gnutellaFileList = new GnutellaFileList(fileManager);
        buddyFileList = new BuddyFileList();
        
//        registerListeners();
    }
    
//    private void registerListeners() {
//        fileManager.addFileEventListener(this);
//        
//        fileManager.getSharedFileList().addFileListListener(new FileListener(gnutellaList));
//        fileManager.getBuddyFileList().addFileListListener(new FileListener(buddyList));
//    }

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
    public FileList getLibraryList() {
        return libraryFileList;
    }
    
    @Override
    public FileList getGnutellaList() {
        return gnutellaFileList;
    }
    
    @Override
    public FileList getAllBuddyList() {
        return buddyFileList;
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
//        switch(evt.getType()) {
//            case ADD_FILE:
//                allFileList.add(new CoreFileItem(evt.getNewFileDesc()));
//                break;
//            case FILEMANAGER_LOAD_STARTED:
//                allFileList.clear();
//                break;
//            case REMOVE_FILE:
//                removeFileItem(allFileList, evt.getNewFileDesc());
//                break;
//            case RENAME_FILE:
//            case CHANGE_FILE:
//                removeFileItem(allFileList, evt.getOldFileDesc());
//                allFileList.add(new CoreFileItem(evt.getNewFileDesc()));
//        }
    }
    
//    private class FileListener implements FileListListener {
//
//        private final EventList<FileItem> list;
//        
//        public FileListener(EventList<FileItem> list) {
//            this.list = list;
//        }
//        
//        @Override
//        public void addEvent(FileDesc fileDesc) { 
//            list.add(new CoreFileItem(fileDesc));
//        }
//
//        @Override
//        public void changeEvent(FileDesc oldDesc, FileDesc newDesc) {
//            removeFileItem(list, oldDesc);
//            list.add(new CoreFileItem(oldDesc));            
//        }
//
//        @Override
//        public void removeEvent(FileDesc fileDesc) {
//            removeFileItem(list, fileDesc);
//        }
//    }
    
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

    
//    private void removeFileItem(EventList<FileItem> list, FileDesc toRemove) {
//        for(FileItem item : list) {
//            if(item.getFile().equals(toRemove.getFile())) {
//                list.remove(item);
//                return;
//            }
//        }
//    }
    
    private class GnutellaFileList extends FileListImpl implements FileListListener {

        private FileManager fileManager;
        
        private Map<File, FileItem> lookup;
        
        public GnutellaFileList(FileManager fileManager) {
            this.fileManager = fileManager;
            this.fileManager.getSharedFileList().addFileListListener(this);
            
            lookup = new HashMap<File, FileItem>();
        }
        
        @Override
        public void addFile(File file) {
            //TODO: check this
            fileManager.addSharedFileAlways(file);
        }

        @Override
        public void removeFile(File file) {
            fileManager.getSharedFileList().remove(fileManager.getFileDesc(file));
        }

        @Override
        public void addEvent(FileDesc fileDesc) {
            FileItem newItem = new CoreFileItem(fileDesc);  
            lookup.put(fileDesc.getFile(), newItem);
            eventList.add(newItem);
        }

        @Override
        public void changeEvent(FileDesc oldDesc, FileDesc newDesc) {
            FileItem old = lookup.remove(oldDesc.getFile());
            FileItem newItem = new CoreFileItem(newDesc);
            lookup.put(newDesc.getFile(), newItem);
            
            eventList.remove(old);
            eventList.add(newItem);
        }

        @Override
        public void removeEvent(FileDesc fileDesc) {
            FileItem old = lookup.remove(fileDesc.getFile());
            eventList.remove(old);
        }
        
    }
    
    private class BuddyFileList extends FileListImpl {

        @Override
        public void addFile(File file) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void removeFile(File file) {
            // TODO Auto-generated method stub
            
        }
        
    }
    
    private class LibraryFileList extends FileListImpl implements FileEventListener {

        private FileManager fileManager;
        
        LibraryFileList(FileManager fileManager) {
            super();
            
            this.fileManager = fileManager;
            this.fileManager.addFileEventListener(this);
        }
        
        @Override
        public void addFile(File file) {
            fileManager.addFile(file);
        }

        @Override
        public void removeFile(File file) {
            fileManager.removeFile(file);
        }

        @Override
        public void handleFileEvent(FileManagerEvent evt) {
            switch(evt.getType()) {
            case ADD_FILE:
                eventList.add(new CoreFileItem(evt.getNewFileDesc()));
                break;
            case REMOVE_FILE:
                remove(evt.getNewFile());
                break;
            case FILEMANAGER_LOAD_STARTED:
                eventList.clear();
                break;
            }
        }
    }
    
    private abstract class FileListImpl implements FileList {
        final EventList<FileItem> eventList;
        
        FileListImpl() {
            eventList = GlazedLists.threadSafeList(new BasicEventList<FileItem>());
        }
        
        @Override
        public EventList<FileItem> getModel() {
            return eventList;
        }
        
        void remove(File file) {
            
        }
    }
}
