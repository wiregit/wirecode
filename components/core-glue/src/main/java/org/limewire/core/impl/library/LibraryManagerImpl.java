package org.limewire.core.impl.library;

import java.awt.EventQueue;
import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.FriendRemoteLibraryEvent;
import org.limewire.core.api.library.LibraryListEventType;
import org.limewire.core.api.library.LibraryListListener;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.library.RemoteFileList;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.listener.EventListener;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.TransformedList;
import ca.odell.glazedlists.swing.GlazedListsSwing;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.FileEventListener;
import com.limegroup.gnutella.FileListListener;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.FileManagerEvent;
import com.limegroup.gnutella.LocalFileDetailsFactory;

@Singleton
class LibraryManagerImpl implements ShareListManager, LibraryManager, RemoteLibraryManager {
    
    private final FileManager fileManager;
    private final List<Listener> listeners = new CopyOnWriteArrayList<Listener>();
    
    private final LibraryFileList libraryFileList;
    private final GnutellaFileList gnutellaFileList;
    
    private final ConcurrentHashMap<String, LocalFileListImpl> friendLocalFileLists;
    
    private final EventListener<FriendRemoteLibraryEvent> friendLibraryEventListener; 
    private final ConcurrentHashMap<String, RemoteFileListImpl> friendLibraryFileLists;
    private final LocalFileDetailsFactory detailsFactory;

    @Inject
    LibraryManagerImpl(FileManager fileManager, LocalFileDetailsFactory detailsFactory, EventListener<FriendRemoteLibraryEvent> friendLibraryEventListener) {
        this.fileManager = fileManager;
        this.detailsFactory = detailsFactory;
        this.friendLibraryEventListener = friendLibraryEventListener;

        libraryFileList = new LibraryFileList(fileManager);
        gnutellaFileList = new GnutellaFileList(fileManager);
        friendLocalFileLists = new ConcurrentHashMap<String, LocalFileListImpl>();
        friendLibraryFileLists = new ConcurrentHashMap<String, RemoteFileListImpl>();
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
    public LocalFileList getLibraryManagedList() {
        return libraryFileList;
    }
    
    @Override
    public LocalFileList getGnutellaShareList() {
        return gnutellaFileList;
    }
    
    @Override
    public Collection<LocalFileList> getAllFriendShareLists() {
        // No clue why this has to be multiple lines -- but compile failure otherwise.
        Collection<? extends LocalFileList> list = friendLocalFileLists.values();
        return Collections.unmodifiableCollection(list);
    }
    
    @Override
    public LocalFileList getOrCreateFriendShareList(Friend friend) {
        FriendFileListImpl newList = new FriendFileListImpl(fileManager, friend.getId());        
        LocalFileList existing = friendLocalFileLists.putIfAbsent(friend.getId(), newList);        
        if(existing == null) {
            newList.loadSavedFiles();
            return newList;
        } else {
            return existing;
        }
    }
    
    @Override
    public LocalFileList getFriendShareList(Friend friend) {
        return friendLocalFileLists.get(friend.getId());
    }

    @Override
    public void removeFriendShareList(Friend friend) {
        fileManager.removeFriendFileList(friend.getId());
        // TODO: Should we clean up friendLocalFileLists?
    }
    
    //////////////////////////////////////////////////////
    //  Accessors for friend Libraries (Files being shared with you)
    /////////////////////////////////////////////////////

    @Override
    public RemoteFileList getOrCreateFriendLibrary(Friend friend) {
        RemoteFileListImpl newList = new FriendLibraryFileList();
        RemoteFileListImpl existing = friendLibraryFileLists.putIfAbsent(friend.getId(), newList);
        
        if(existing == null) {
            friendLibraryEventListener.handleEvent(new FriendRemoteLibraryEvent(FriendRemoteLibraryEvent.Type.FRIEND_LIBRARY_ADDED, newList, friend));
            return newList;
        } else {
            return existing;
        }
    }
    
    @Override
    public void removeFriendLibrary(Friend friend) {
        RemoteFileListImpl list = friendLibraryFileLists.remove(friend.getId());
        if(list != null) {
            friendLibraryEventListener.handleEvent(new FriendRemoteLibraryEvent(FriendRemoteLibraryEvent.Type.FRIEND_LIBRARY_REMOVED, list, friend));
            list.dispose();
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
            case REMOVE_FILE:
                listener.handleLibraryListEvent(LibraryListEventType.FILE_REMOVED);
                break;
            case ADD_FILE:
                listener.handleLibraryListEvent(LibraryListEventType.FILE_ADDED);
                break;
            }
        }
    }
    
    private class GnutellaFileList extends LocalFileListImpl implements FileListListener {

        private final FileManager fileManager;
        
        private final Map<File, FileItem> lookup;
        
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
            LocalFileItem newItem = new CoreLocalFileItem(fileDesc, detailsFactory);  
            lookup.put(fileDesc.getFile(), newItem);
            eventList.add(newItem);
        }

        @Override
        public void changeEvent(FileDesc oldDesc, FileDesc newDesc) {
            FileItem old = lookup.remove(oldDesc.getFile());
            LocalFileItem newItem = new CoreLocalFileItem(newDesc, detailsFactory);
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
    }

    
    private class FriendFileListImpl extends LocalFileListImpl implements FileListListener {

        private final FileManager fileManager;
        private final String name;
        
        private final Map<File, FileItem> lookup;
        
        FriendFileListImpl(FileManager fileManager, String name) {
            this.fileManager = fileManager;
            this.name = name;
                     
            this.fileManager.getOrCreateFriendFileList(name).addFileListListener(this);
            lookup = new ConcurrentHashMap<File, FileItem>();
        }
        
        //TODO: reexamine this. Needs to be loaded on a seperate thread and maybe cleaned up somehow
        void loadSavedFiles() {
            com.limegroup.gnutella.FileList fileList =
                fileManager.getFriendFileList(name);  

              synchronized (fileList.getLock()) {
                  Iterator<FileDesc> iter = fileList.iterator();
                  while(iter.hasNext()) {
                      FileDesc fileDesc = iter.next();
                      LocalFileItem newItem = new CoreLocalFileItem(fileDesc, detailsFactory);  
                      lookup.put(fileDesc.getFile(), newItem);
                      eventList.add(newItem);
                  }
              }
        }
        
        @Override
        public void addFile(File file) {
            fileManager.addFriendFile(name, file);
        }

        @Override
        public void removeFile(File file) {
            fileManager.getFriendFileList(name).remove(fileManager.getFileDesc(file));
        }
        
        @Override
        public void clear() {
            fileManager.getFriendFileList(name).clear();
        }

        @Override
        public void addEvent(FileDesc fileDesc) {
            LocalFileItem newItem = new CoreLocalFileItem(fileDesc, detailsFactory);  
            lookup.put(fileDesc.getFile(), newItem);
            eventList.add(newItem);
        }

        @Override
        public void changeEvent(FileDesc oldDesc, FileDesc newDesc) {
            FileItem oldItem = lookup.remove(oldDesc.getFile());
            LocalFileItem newItem = new CoreLocalFileItem(newDesc, detailsFactory);
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

        private final FileManager fileManager;
        
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
    }
    
    private abstract class LocalFileListImpl implements LocalFileList {
        protected final EventList<LocalFileItem> eventList;
        private EventList<LocalFileItem> swingEventList;
        
        LocalFileListImpl() {
            eventList = GlazedLists.threadSafeList(new BasicEventList<LocalFileItem>());
        }
        
        @Override
        public EventList<LocalFileItem> getModel() {
            return eventList;
        }
        
        @Override
        public EventList<LocalFileItem> getSwingModel() {
            assert EventQueue.isDispatchThread();
            if(swingEventList == null) {
                swingEventList = GlazedListsSwing.swingThreadProxyList(eventList);
            }
            return swingEventList;
        }
        
        
        void remove(File file) {
            
        }
        
        @Override
        public int size() {
            return eventList.size();
        }
    }
    
    private abstract class RemoteFileListImpl implements RemoteFileList {
        protected final EventList<RemoteFileItem> eventList;
        protected volatile TransformedList<RemoteFileItem, RemoteFileItem> swingEventList;
        
        RemoteFileListImpl() {
            eventList = GlazedLists.threadSafeList(new BasicEventList<RemoteFileItem>());
        }
        
        @Override
        public EventList<RemoteFileItem> getModel() {
            return eventList;
        }
        
        @Override
        public EventList<RemoteFileItem> getSwingModel() {
            assert EventQueue.isDispatchThread();
            if(swingEventList == null) {
                swingEventList = GlazedListsSwing.swingThreadProxyList(eventList);
            }
            return swingEventList;
        }
        
        void dispose() {
            if(swingEventList != null) {
                swingEventList.dispose();
            }
        }
        
        
        @Override
        public int size() {
            return eventList.size();
        }
    }
    
    private class FriendLibraryFileList extends RemoteFileListImpl {


        public void addFile(RemoteFileItem file) {
            eventList.add(file);
        }

        public void removeFile(RemoteFileItem file) {
            eventList.remove(file);
        }
        
        public void clear() {
            
        }
        //TODO: add new accessors appropriate for creating FileItems based on
        //      lookups. May also need to subclass CoreFileItem appropriate for
        //      friend library info.
    }
}
