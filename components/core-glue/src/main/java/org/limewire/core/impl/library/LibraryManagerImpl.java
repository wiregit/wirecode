package org.limewire.core.impl.library;

import java.awt.EventQueue;
import java.io.File;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.FileList;
import org.limewire.core.api.library.FriendFileList;
import org.limewire.core.api.library.FriendShareListEvent;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.listener.EventListener;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.CompositeList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.TransformedList;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.FileListChangedEvent;
import com.limegroup.gnutella.library.FileManager;

@Singleton
class LibraryManagerImpl implements ShareListManager, LibraryManager {
    
    private static final Log LOG = LogFactory.getLog(LibraryManagerImpl.class);
    
    private final FileManager fileManager;
    
    private final LibraryFileListImpl libraryFileList;
    private final GnutellaFileList gnutellaFileList;
    
    private final CombinedFriendShareList combinedFriendShareLists;
    
    private final ConcurrentHashMap<String, FriendFileListImpl> friendLocalFileLists;

    private final EventListener<FriendShareListEvent> friendShareListEventListener;
    
    private final CoreLocalFileItemFactory coreLocalFileItemFactory;

    @Inject
    LibraryManagerImpl(FileManager fileManager, CoreLocalFileItemFactory coreLocalFileItemFactory,
            EventListener<FriendShareListEvent> friendShareListEventListener) {
        this.fileManager = fileManager;
        this.coreLocalFileItemFactory = coreLocalFileItemFactory;
        this.friendShareListEventListener = friendShareListEventListener;
        this.combinedFriendShareLists = new CombinedFriendShareList();
        this.libraryFileList = new LibraryFileListImpl(fileManager, coreLocalFileItemFactory);
        this.gnutellaFileList = new GnutellaFileList(fileManager);
        this.friendLocalFileLists = new ConcurrentHashMap<String, FriendFileListImpl>();
    }

    @Override
    public LibraryFileListImpl getLibraryManagedList() {
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
    public FriendFileList getOrCreateFriendShareList(Friend friend) {
        LOG.debugf("get|Create library for friend {0}", friend.getId());
        FriendFileListImpl newList = new FriendFileListImpl(fileManager, friend.getId());        
        FriendFileList existing = friendLocalFileLists.putIfAbsent(friend.getId(), newList);        
        
        if(existing == null) {
            LOG.debugf("No existing library for friend {0}", friend.getId());
            newList.commit();
            friendShareListEventListener.handleEvent(new FriendShareListEvent(FriendShareListEvent.Type.FRIEND_SHARE_LIST_ADDED, newList, friend));
            return newList;
        } else {
            LOG.debugf("Already an existing lib for friend {0}", friend.getId());
            newList.dispose();
            return existing;
        }
    }

    @Override
    public FriendFileList getFriendShareList(Friend friend) {
        return friendLocalFileLists.get(friend.getId());
    }

    @Override    
    public void removeFriendShareList(Friend friend) {
        fileManager.removeFriendFileList(friend.getId());
        
        // TODO: Should we clean up friendLocalFileLists?
        //       (is there a reason the old code didn't do this?)        
        FriendFileListImpl list = friendLocalFileLists.remove(friend.getId());
        if(list != null) {
            friendShareListEventListener.handleEvent(new FriendShareListEvent(FriendShareListEvent.Type.FRIEND_SHARE_LIST_REMOVED, list, friend));
            list.dispose();
        }
    }
    
    private class GnutellaFileList extends LocalFileListImpl implements EventListener<FileListChangedEvent> {

        private final FileManager fileManager;
        
        private final ConcurrentHashMap<File, FileItem> lookup;
        
        public GnutellaFileList(FileManager fileManager) {
            super(new BasicEventList<LocalFileItem>());
            
            this.fileManager = fileManager;
            this.fileManager.getGnutellaSharedFileList().addFileListListener(this);
            
            lookup = new ConcurrentHashMap<File, FileItem>();
        }
        
        @Override
        public void addFile(File file) {
            fileManager.getGnutellaSharedFileList().add(file);
        }

        @Override
        public void removeFile(File file) {
            fileManager.getGnutellaSharedFileList().remove(file);
        }
        
        @Override
        public void handleEvent(FileListChangedEvent event) {
            LocalFileItem newItem;
            switch(event.getType()) {
            case ADDED:
                newItem = coreLocalFileItemFactory.createCoreLocalFileItem(event.getFileDesc());  
                lookup.put(event.getFileDesc().getFile(), newItem);
                threadSafeList.add(newItem);
                break;
            case CHANGED:
                FileItem oldItem = lookup.remove(event.getOldValue().getFile());
                newItem = coreLocalFileItemFactory.createCoreLocalFileItem(event.getFileDesc());
                lookup.put(event.getFileDesc().getFile(), newItem);

                threadSafeList.remove(oldItem);
                threadSafeList.add(newItem);
                break;
            case REMOVED:
                FileItem old = lookup.remove(event.getFileDesc().getFile());
                threadSafeList.remove(old);
                break;
            }
        }
    }

    
    private class FriendFileListImpl extends LocalFileListImpl implements FriendFileList, EventListener<FileListChangedEvent> {

        private final FileManager fileManager;
        private final String name;        
        private final Map<File, FileItem> lookup;
        private volatile boolean committed = false;
        
        FriendFileListImpl(FileManager fileManager, String name) {
            super(combinedFriendShareLists.createMemberList());
            
            this.fileManager = fileManager;
            this.name = name;
            this.lookup = new ConcurrentHashMap<File, FileItem>();
        }
        
        @Override
        public void addFile(File file) {
            fileManager.getFriendFileList(name).add(file);
        }

        @Override
        public void removeFile(File file) {
            fileManager.getFriendFileList(name).remove(file);
        }
        
        @Override
        public void handleEvent(FileListChangedEvent event) {
            LocalFileItem newItem;
            switch(event.getType()) {
            case ADDED:
                newItem = coreLocalFileItemFactory.createCoreLocalFileItem(event.getFileDesc());
                lookup.put(event.getFileDesc().getFile(), newItem);
                threadSafeList.add(newItem);
                break;
            case CHANGED:
                FileItem oldItem = lookup.remove(event.getOldValue().getFile());
                newItem = coreLocalFileItemFactory.createCoreLocalFileItem(event.getFileDesc());
                lookup.put(event.getFileDesc().getFile(), newItem);

                threadSafeList.remove(oldItem);
                threadSafeList.add(newItem);
                break;
            case REMOVED:
                FileItem old = lookup.remove(event.getFileDesc().getFile());
                threadSafeList.remove(old);
                break;
            }
        }
        
        @Override
        void dispose() {
            super.dispose();
            if(committed) {
                combinedFriendShareLists.removeMemberList(baseList);
                fileManager.getOrCreateFriendFileList(name).removeFileListListener(this);
            }
        }
        
        /* Commits to using this list. */
        void commit() {
            committed = true;
            fileManager.getOrCreateFriendFileList(name).addFileListListener(this);
            combinedFriendShareLists.addMemberList(baseList);
            
            com.limegroup.gnutella.library.FileList fileList =
                fileManager.getFriendFileList(name);  

              fileList.getReadLock().lock();
              try {
                  for(FileDesc fileDesc : fileList.iterable()) {
                      LocalFileItem newItem = coreLocalFileItemFactory.createCoreLocalFileItem(fileDesc);
                      lookup.put(fileDesc.getFile(), newItem);
                      threadSafeList.add(newItem);
                  }
              } finally {
                  fileList.getReadLock().unlock();
              }
        }
        
        public boolean isAddNewAudioAlways() {
            return fileManager.getFriendFileList(name).isAddNewAudioAlways();
        }
        
        public void setAddNewAudioAlways(boolean value) {
            fileManager.getFriendFileList(name).setAddNewAudioAlways(value);
        }
        
        public boolean isAddNewVideoAlways() {
            return fileManager.getFriendFileList(name).isAddNewVideoAlways();
        }
        
        public void setAddNewVideoAlways(boolean value) {
            fileManager.getFriendFileList(name).setAddNewVideoAlways(value);
        }
        
        public boolean isAddNewImageAlways() {
            return fileManager.getFriendFileList(name).isAddNewImageAlways();
        }
        
        public void setAddNewImageAlways(boolean value) {
            fileManager.getFriendFileList(name).setAddNewImageAlways(value);
        }
    } 
    
    private class CombinedFriendShareList implements FileList<LocalFileItem> {
        private final CompositeList<LocalFileItem> compositeList;
        private final EventList<LocalFileItem> threadSafeUniqueList;
        private volatile TransformedList<LocalFileItem, LocalFileItem> swingList;
        
        public CombinedFriendShareList() {
            compositeList = new CompositeList<LocalFileItem>();
            threadSafeUniqueList = GlazedListsFactory.uniqueList(GlazedListsFactory.threadSafeList(compositeList),
                    new Comparator<LocalFileItem>() {
                @Override
                public int compare(LocalFileItem o1, LocalFileItem o2) {
                    return o1.getFile().getPath().compareTo(o2.getFile().getPath());
                }
            });
        }
        
        public void removeMemberList(EventList<LocalFileItem> eventList) {
            compositeList.getReadWriteLock().writeLock().lock();
            try {
                compositeList.removeMemberList(eventList);
            } finally {
                compositeList.getReadWriteLock().writeLock().unlock();    
            }
        }

        public EventList<LocalFileItem> createMemberList() {
            return compositeList.createMemberList();
        }
        
        public void addMemberList(EventList<LocalFileItem> eventList) {
            compositeList.getReadWriteLock().writeLock().lock();
            try {
                compositeList.addMemberList(eventList);
            } finally {
                compositeList.getReadWriteLock().writeLock().unlock();    
            }
        }

        @Override
        public EventList<LocalFileItem> getModel() {
            return threadSafeUniqueList;
        }
        
        @Override
        public EventList<LocalFileItem> getSwingModel() {
            assert EventQueue.isDispatchThread();
            if(swingList == null) {
                swingList =  GlazedListsFactory.swingThreadProxyEventList(threadSafeUniqueList);
            }
            return swingList;
        }
        
        @Override
        public int size() {
            return threadSafeUniqueList.size();
        }        
    }
}
