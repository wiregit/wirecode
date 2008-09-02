package org.limewire.core.impl.library;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.limewire.core.api.library.BuddyShareListListener;
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
    private final List<BuddyShareListListener> buddyListeners = new CopyOnWriteArrayList<BuddyShareListListener>();
    
    private LibraryFileList libraryFileList;
    private GnutellaFileList gnutellaFileList;
    private Map<String,FileList> buddyFileLists;
    
    private Map<String, FileList> buddyLibraryFileLists;
    
    @Inject
    LibraryManagerImpl(FileManager fileManager) {
        this.fileManager = fileManager;
        
        libraryFileList = new LibraryFileList(fileManager);
        gnutellaFileList = new GnutellaFileList(fileManager);
        buddyFileLists = new HashMap<String, FileList>();
        buddyLibraryFileLists = new HashMap<String, FileList>();
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
    public FileList getLibraryList() {
        return libraryFileList;
    }
    
    @Override
    public FileList getGnutellaList() {
        return gnutellaFileList;
    }

    @Override
    public Map<String, FileList> getAllBuddyLists() {
        return buddyFileLists;
    }
    
    @Override
    public FileList getBuddy(String name) {
        if(buddyFileLists.containsKey(name))
            return buddyFileLists.get(name);
        
        BuddyFileList newBuddyList = new BuddyFileList(fileManager, name);
        buddyFileLists.put(name, newBuddyList);
        return newBuddyList;
    }
    
    @Override
    public void addBuddy(String name) {
        fileManager.addBuddyFileList(name);
        for(BuddyShareListListener listener : buddyListeners) {
            listener.handleBuddyShareEvent(BuddyShareListListener.BuddyShareEvent.ADD, name);
        }
    }

    @Override
    public void removeBuddy(String name) {
        fileManager.removeBuddyFileList(name);
        for(BuddyShareListListener listener : buddyListeners) {
            listener.handleBuddyShareEvent(BuddyShareListListener.BuddyShareEvent.REMOVE, name);
        }
    }
    
    @Override
    public boolean containsBuddy(String name) {
        return fileManager.containsBuddyFileList(name);
    }
    

    @Override
    public void addBuddyShareListListener(BuddyShareListListener listener) {
        buddyListeners.add(listener);
    }

    @Override
    public void removeBuddyShareListListener(BuddyShareListListener listener) {
        buddyListeners.remove(listener);
    }
    
    //////////////////////////////////////////////////////
    //  Accessors for Buddy Libraries (Files being shared with you)
    /////////////////////////////////////////////////////

    @Override
    public Map<String, FileList> getAllBuddyLibraries() {
        return buddyLibraryFileLists;
    }

    @Override
    public FileList getBuddyLibrary(String name) {
        return buddyLibraryFileLists.get(name);
    }

    @Override
    public void addBuddyLibrary(String name) {
        if(!buddyLibraryFileLists.containsKey(name)) {
            buddyLibraryFileLists.put(name, new BuddyLibraryFileList(name));
        }
    }
    
    @Override
    public void removeBuddyLibrary(String name) {
        buddyLibraryFileLists.remove(name);
    }
    
    @Override
    public boolean containsBuddyLibrary(String name) {
        return buddyLibraryFileLists.containsKey(name);
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

        @Override
        public String getName() {
            return "Gnutella List";
        }
    }
    
    private class BuddyFileList extends FileListImpl implements FileListListener{

        private FileManager fileManager;
        private String name;
        
        private Map<File, FileItem> lookup;
        
        BuddyFileList(FileManager fileManager, String name) {
            this.fileManager = fileManager;
            this.name = name;
            
            this.fileManager.getBuddyFileList(name).addFileListListener(this);
            lookup = new HashMap<File, FileItem>();
        }
        
        @Override
        public void addFile(File file) {
            fileManager.addBuddyFile(name, file);
        }

        @Override
        public void removeFile(File file) {
            fileManager.getBuddyFileList(name).remove(fileManager.getFileDesc(file));
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public void addEvent(FileDesc fileDesc) {
            FileItem newItem = new CoreFileItem(fileDesc);  
            lookup.put(fileDesc.getFile(), newItem);
            eventList.add(newItem);
        }

        @Override
        public void changeEvent(FileDesc oldDesc, FileDesc newDesc) {
            FileItem oldItem = lookup.remove(oldDesc.getFile());
            FileItem newItem = new CoreFileItem(newDesc);
            lookup.put(newDesc.getFile(), newItem);

            eventList.remove(oldItem);
            eventList.add(newItem);
        }

        @Override
        public void removeEvent(FileDesc fileDesc) {
            FileItem old = lookup.remove(fileDesc.getFile());
            eventList.remove(old);
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

        @Override
        public String getName() {
            return "My Library";
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
        
        @Override
        public int size() {
            return eventList.size();
        }
    }
    
    private class BuddyLibraryFileList extends FileListImpl {

        private final String name;
        
        public BuddyLibraryFileList(String name) {
            this.name = name;
        }
        
        @Override
        public void addFile(File file) {
            
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public void removeFile(File file) {

        }
        //TODO: add new accessors appropriate for creating FileItems based on
        //      lookups. May also need to subclass CoreFileItem appropriate for
        //      buddy library info.
    }
}
