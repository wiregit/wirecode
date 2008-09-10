package org.limewire.core.impl.library;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.limewire.core.api.library.BuddyFileList;
import org.limewire.core.api.library.BuddyShareListListener;
import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.LibraryListEventType;
import org.limewire.core.api.library.LibraryListListener;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.library.RemoteFileList;
import org.limewire.core.api.search.SearchResult;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.FileEventListener;
import com.limegroup.gnutella.FileListListener;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.FileManagerEvent;
import com.limegroup.gnutella.LocalFileDetailsFactory;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;

@Singleton
class LibraryManagerImpl implements LibraryManager, FileEventListener {
    
    private final FileManager fileManager;
    private final List<Listener> listeners = new CopyOnWriteArrayList<Listener>();
    private final List<BuddyShareListListener> buddyListeners = new CopyOnWriteArrayList<BuddyShareListListener>();
    
    private LibraryFileList libraryFileList;
    private GnutellaFileList gnutellaFileList;
    private Map<String, LocalFileList> buddyFileLists;
    
    private Map<String, RemoteFileList> buddyLibraryFileLists;
    private final LocalFileDetailsFactory detailsFactory;

    @Inject
    LibraryManagerImpl(FileManager fileManager, LocalFileDetailsFactory detailsFactory) {
        this.fileManager = fileManager;
        this.detailsFactory = detailsFactory;

        libraryFileList = new LibraryFileList(fileManager);
        gnutellaFileList = new GnutellaFileList(fileManager);
        buddyFileLists = new HashMap<String, LocalFileList>();
        buddyLibraryFileLists = new HashMap<String, RemoteFileList>();
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
    public LocalFileList getLibraryList() {
        return libraryFileList;
    }
    
    @Override
    public LocalFileList getGnutellaList() {
        return gnutellaFileList;
    }

    @Override
    public Map<String, LocalFileList> getAllBuddyLists() {
        return buddyFileLists;
    }
    
    @Override
    public LocalFileList getBuddy(String name) {
        if(buddyFileLists.containsKey(name))
            return buddyFileLists.get(name);

        BuddyFileListImpl newBuddyList = new BuddyFileListImpl(fileManager, name);
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
    public Map<String, RemoteFileList> getAllBuddyLibraries() {
        return buddyLibraryFileLists;
    }

    @Override
    public RemoteFileList getBuddyLibrary(String name) {
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
    
    private class GnutellaFileList extends LocalFileListImpl implements FileListListener {

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
            FileItem newItem = new CoreLocalFileItem(fileDesc, detailsFactory);  
            lookup.put(fileDesc.getFile(), newItem);
            eventList.add(newItem);
        }

        @Override
        public void changeEvent(FileDesc oldDesc, FileDesc newDesc) {
            FileItem old = lookup.remove(oldDesc.getFile());
            FileItem newItem = new CoreLocalFileItem(newDesc, detailsFactory);
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
        public void clear() {
            fileManager.getSharedFileList().clear();
        }

        @Override
        public String getName() {
            return "Gnutella List";
        }
    }

    
    private class BuddyFileListImpl extends LocalFileListImpl implements FileListListener, BuddyFileList {

        private FileManager fileManager;
        private String name;
        private EventList<LocalFileItem> filteredEventList;
        
        private Map<File, FileItem> lookup;
        
        BuddyFileListImpl(FileManager fileManager, String name) {
            this.fileManager = fileManager;
            this.name = name;
                     
            this.fileManager.getBuddyFileList(name).addFileListListener(this);
            lookup = new HashMap<File, FileItem>();
            loadSavedFiles();
        }
        
        //TODO: reexamine this. Needs to be loaded on a seperate thread and maybe cleaned up somehow
        private void loadSavedFiles() {
            com.limegroup.gnutella.FileList fileList = this.fileManager.getBuddyFileList(name);  

              synchronized (fileList.getLock()) {
                  Iterator<FileDesc> iter = fileList.iterator();
                  while(iter.hasNext()) {
                      FileDesc fileDesc = iter.next();
                      FileItem newItem = new CoreLocalFileItem(fileDesc, detailsFactory);  
                      lookup.put(fileDesc.getFile(), newItem);
                      eventList.add(newItem);
                  }
              }
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
        public void clear() {
            fileManager.getBuddyFileList(name).clear();
        }
        
        @Override
        public void setFilteredModel(EventList<LocalFileItem> filteredList) {
            this.filteredEventList = filteredList;
        }
        
        @Override
        public EventList<LocalFileItem> getFilteredModel() {
            return this.filteredEventList;
        }
        
        @Override
        public int getFilteredSize() {
            if(filteredEventList == null)
                return 0;
            else
                return filteredEventList.size();
        }

        @Override
        public void addEvent(FileDesc fileDesc) {
            FileItem newItem = new CoreLocalFileItem(fileDesc, detailsFactory);  
            lookup.put(fileDesc.getFile(), newItem);
            eventList.add(newItem);
        }

        @Override
        public void changeEvent(FileDesc oldDesc, FileDesc newDesc) {
            FileItem oldItem = lookup.remove(oldDesc.getFile());
            FileItem newItem = new CoreLocalFileItem(newDesc, detailsFactory);
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
    
    private class LibraryFileList extends LocalFileListImpl implements FileEventListener {

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
                eventList.add(new CoreLocalFileItem(evt.getNewFileDesc(), detailsFactory));
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
        public void clear() {
            
        }

        @Override
        public String getName() {
            return "My Library";
        }
    }
    
    private abstract class LocalFileListImpl implements LocalFileList {
        final EventList<FileItem> eventList;
        
        LocalFileListImpl() {
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
    
    private abstract class RemoteFileListImpl implements RemoteFileList {
        final EventList<RemoteFileItem> eventList;
        
        RemoteFileListImpl() {
            eventList = GlazedLists.threadSafeList(new BasicEventList<RemoteFileItem>());
        }
        
        @Override
        public EventList<RemoteFileItem> getModel() {
            return eventList;
        }
        
        @Override
        public int size() {
            return eventList.size();
        }
    }
    
    private class BuddyLibraryFileList extends RemoteFileListImpl {

        private final String name;
        
        public BuddyLibraryFileList(String name) {
            this.name = name;
        }

        public void addFile(SearchResult file) {
            eventList.add(new CoreRemoteFileItem(file));
        }

        public void removeFile(SearchResult file) {
            eventList.remove(new CoreRemoteFileItem(file));
        }

        @Override
        public String getName() {
            return name;
        }
        
        public void clear() {
            
        }
        //TODO: add new accessors appropriate for creating FileItems based on
        //      lookups. May also need to subclass CoreFileItem appropriate for
        //      buddy library info.
    }
}
