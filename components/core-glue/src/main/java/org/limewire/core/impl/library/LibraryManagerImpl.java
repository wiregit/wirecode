package org.limewire.core.impl.library;

import java.awt.EventQueue;
import java.io.File;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.FileList;
import org.limewire.core.api.library.FriendRemoteLibraryEvent;
import org.limewire.core.api.library.FriendShareListEvent;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.library.RemoteFileList;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.listener.EventListener;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.CompositeList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.TransformedList;
import ca.odell.glazedlists.UniqueList;
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
    
    private final LibraryFileList libraryFileList;
    private final GnutellaFileList gnutellaFileList;
    
    private final CombinedFriendShareList combinedFriendShareLists;
    
    private final ConcurrentHashMap<String, LocalFileListImpl> friendLocalFileLists;
    
    private final EventListener<FriendRemoteLibraryEvent> friendLibraryEventListener; 
    private final EventListener<FriendShareListEvent> friendShareListEventListener;
    private final ConcurrentHashMap<String, RemoteFileListImpl> friendLibraryFileLists;
    private final LocalFileDetailsFactory detailsFactory;

    @Inject
    LibraryManagerImpl(FileManager fileManager, LocalFileDetailsFactory detailsFactory,
            EventListener<FriendRemoteLibraryEvent> friendLibraryEventListener,
            EventListener<FriendShareListEvent> friendShareListEventListener) {
        this.fileManager = fileManager;
        this.detailsFactory = detailsFactory;
        this.friendLibraryEventListener = friendLibraryEventListener;
        this.friendShareListEventListener = friendShareListEventListener;

        combinedFriendShareLists = new CombinedFriendShareList();
        
        libraryFileList = new LibraryFileList(fileManager);
        gnutellaFileList = new GnutellaFileList(fileManager);
        friendLocalFileLists = new ConcurrentHashMap<String, LocalFileListImpl>();
        friendLibraryFileLists = new ConcurrentHashMap<String, RemoteFileListImpl>();
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
    public FileList<LocalFileItem> getCombinedFriendShareLists() {
        return combinedFriendShareLists;
    }
    
    @Override
    public LocalFileList getOrCreateFriendShareList(Friend friend) {
        FriendFileListImpl newList = new FriendFileListImpl(fileManager, friend.getId());        
        LocalFileList existing = friendLocalFileLists.putIfAbsent(friend.getId(), newList);        
        if(existing == null) {
            newList.loadSavedFiles();
            friendShareListEventListener.handleEvent(new FriendShareListEvent(FriendShareListEvent.Type.FRIEND_SHARE_LIST_ADDED, newList, friend));
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
        //       (is there a reason the old code didn't do this?)        
        LocalFileListImpl list = friendLocalFileLists.remove(friend.getId());
        if(list != null) {
            friendShareListEventListener.handleEvent(new FriendShareListEvent(FriendShareListEvent.Type.FRIEND_SHARE_LIST_REMOVED, list, friend));
            list.dispose();
        }
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
    public boolean hasFriendLibrary(Friend friend) {
        return friendLibraryFileLists.get(friend.getId()) != null;
    }
    
    @Override
    public void removeFriendLibrary(Friend friend) {
        RemoteFileListImpl list = friendLibraryFileLists.remove(friend.getId());
        if(list != null) {
            friendLibraryEventListener.handleEvent(new FriendRemoteLibraryEvent(FriendRemoteLibraryEvent.Type.FRIEND_LIBRARY_REMOVED, list, friend));
            list.dispose();
        }
    }
    
    private class GnutellaFileList extends LocalFileListImpl implements FileListListener {

        private final FileManager fileManager;
        
        private final Map<File, FileItem> lookup;
        
        public GnutellaFileList(FileManager fileManager) {
            super(new BasicEventList<LocalFileItem>());
            
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
            super(combinedFriendShareLists.createAndAddMemberList());
            
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
            super(new BasicEventList<LocalFileItem>());
            
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
        protected volatile TransformedList<LocalFileItem, LocalFileItem> swingEventList;
        
        LocalFileListImpl(EventList<LocalFileItem> eventList) {
            this.eventList = GlazedLists.threadSafeList(eventList);
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
        
        void dispose() {
            if(swingEventList != null) {
                swingEventList.dispose();
            }
            
            combinedFriendShareLists.removeMemberList(eventList);
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
    
    private class CombinedFriendShareList implements FileList<LocalFileItem> {
        private final CompositeList<LocalFileItem> compositeList;
        private final EventList<LocalFileItem> threadSafeUniqueList;
        private volatile TransformedList<LocalFileItem, LocalFileItem> swingList;
        
        public CombinedFriendShareList() {
            compositeList = new CompositeList<LocalFileItem>();
            threadSafeUniqueList = new UniqueList<LocalFileItem>(GlazedLists.threadSafeList(compositeList),
                    new Comparator<LocalFileItem>() {
                @Override
                public int compare(LocalFileItem o1, LocalFileItem o2) {
                    return o1.getFile().getPath().compareTo(o2.getFile().getPath());
                }
            });
        }
        
        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }
        
        public void removeMemberList(EventList<LocalFileItem> eventList) {
            compositeList.getReadWriteLock().writeLock().lock();
            try {
                compositeList.removeMemberList(eventList);
            } finally {
                compositeList.getReadWriteLock().writeLock().unlock();    
            }
        }

        public EventList<LocalFileItem> createAndAddMemberList() {
            EventList<LocalFileItem> list = compositeList.createMemberList();
            compositeList.getReadWriteLock().writeLock().lock();
            try {
                compositeList.addMemberList(list);
            } finally {
                compositeList.getReadWriteLock().writeLock().unlock();    
            }
            return list;
        }

        @Override
        public EventList<LocalFileItem> getModel() {
            return threadSafeUniqueList;
        }
        
        @Override
        public EventList<LocalFileItem> getSwingModel() {
            assert EventQueue.isDispatchThread();
            if(swingList == null) {
                swingList = GlazedListsSwing.swingThreadProxyList(threadSafeUniqueList);
            }
            return swingList;
        }
        
        @Override
        public int size() {
            return threadSafeUniqueList.size();
        }        
    }
}
