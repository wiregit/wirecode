package org.limewire.core.impl.library;

import java.awt.EventQueue;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.Category;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.FileList;
import org.limewire.core.api.library.FriendFileList;
import org.limewire.core.api.library.FriendShareListEvent;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.listener.EventListener;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import ca.odell.glazedlists.CompositeList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.TransformedList;
import ca.odell.glazedlists.matchers.Matcher;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.FileListChangedEvent;
import com.limegroup.gnutella.library.FileManager;

@Singleton
class ShareListManagerImpl implements ShareListManager {
    
    private static final Log LOG = LogFactory.getLog(ShareListManagerImpl.class);
    
    private final FileManager fileManager;
    private final LibraryManager libraryManager;
    
    private final CombinedShareList combinedShareList;
    
    private final GnutellaFileList gnutellaFileList;    
    
    private final ConcurrentHashMap<String, FriendFileListImpl> friendLocalFileLists;

    private final EventListener<FriendShareListEvent> friendShareListEventListener;
    
    private final CoreLocalFileItemFactory coreLocalFileItemFactory;

    @Inject
    ShareListManagerImpl(FileManager fileManager, CoreLocalFileItemFactory coreLocalFileItemFactory,
            EventListener<FriendShareListEvent> friendShareListEventListener, LibraryManager libraryManager) {
        this.fileManager = fileManager;
        this.libraryManager = libraryManager;
        this.coreLocalFileItemFactory = coreLocalFileItemFactory;
        this.friendShareListEventListener = friendShareListEventListener;
        this.combinedShareList = new CombinedShareList();
        this.gnutellaFileList = new GnutellaFileList(fileManager.getGnutellaFileList());
        this.friendLocalFileLists = new ConcurrentHashMap<String, FriendFileListImpl>();
    }

    @Override
    public FriendFileList getGnutellaShareList() {
        return gnutellaFileList;
    }
    
    @Override
    public FileList<LocalFileItem> getCombinedShareList() {
        return combinedShareList;
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
        FriendFileListImpl list = friendLocalFileLists.remove(friend.getId());
        if(list != null) {
            friendShareListEventListener.handleEvent(new FriendShareListEvent(FriendShareListEvent.Type.FRIEND_SHARE_LIST_REMOVED, list, friend));
            list.dispose();
        }
    }
    
    private class GnutellaFileList extends LocalFileListImpl implements FriendFileList {
        private final com.limegroup.gnutella.library.GnutellaFileList shareList;

        private final PropertyChangeSupport propertyChange = new PropertyChangeSupport(this);
        
        public GnutellaFileList(com.limegroup.gnutella.library.GnutellaFileList shareList) {
            super(combinedShareList.createMemberList(), coreLocalFileItemFactory);            
            this.shareList = shareList;
            this.shareList.addFileListListener(newEventListener());
            combinedShareList.addMemberList(baseList);
        }
        
        @Override
        protected com.limegroup.gnutella.library.GnutellaFileList getCoreFileList() {
            return shareList;
        }

        public boolean isAddNewAudioAlways() {
            return getCoreFileList().isAddNewAudioAlways();
        }
        
        public void setAddNewAudioAlways(boolean value) {
            getCoreFileList().setAddNewAudioAlways(value);
            fireAudioCollectionChange(propertyChange, value);
            setSharing(value, Category.AUDIO);
        }
        
        public boolean isAddNewVideoAlways() {
            return getCoreFileList().isAddNewVideoAlways();
        }
        
        public void setAddNewVideoAlways(boolean value) {
            getCoreFileList().setAddNewVideoAlways(value);
            fireVideoCollectionChange(propertyChange, value);
            setSharing(value, Category.VIDEO);
        }
        
        public boolean isAddNewImageAlways() {
            return getCoreFileList().isAddNewImageAlways();
        }
        
        public void setAddNewImageAlways(boolean value) {
            getCoreFileList().setAddNewImageAlways(value);
            fireImageCollectionChange(propertyChange, value);
            setSharing(value, Category.IMAGE);
        }
        
        private void setSharing(boolean value, Category category) {
            if(value)
                addAll(com.limegroup.gnutella.library.GnutellaFileList.ID, category);
            else
                removeAll(com.limegroup.gnutella.library.GnutellaFileList.ID, category, readOnlyList);
        }

        public void addPropertyChangeListener(PropertyChangeListener listener) {
            propertyChange.addPropertyChangeListener(listener);
        }
        
        public void removePropertyChangeListener(PropertyChangeListener listener) {
            propertyChange.removePropertyChangeListener(listener);
        }
    }
    
    private class FriendFileListImpl extends LocalFileListImpl implements FriendFileList {
        private final FileManager fileManager;
        private final String name;
        private volatile boolean committed = false;
        private volatile EventListener<FileListChangedEvent> eventListener;
        
        private final PropertyChangeSupport propertyChange = new PropertyChangeSupport(this);
        
        FriendFileListImpl(FileManager fileManager, String name) {
            super(combinedShareList.createMemberList(), coreLocalFileItemFactory);            
            this.fileManager = fileManager;
            this.name = name;
        }
        
        @Override
        protected com.limegroup.gnutella.library.FriendFileList getCoreFileList() {
            return fileManager.getFriendFileList(name);
        }
        
        @Override
        void dispose() {
            super.dispose();
            if(committed) {
                combinedShareList.removeMemberList(baseList);
                com.limegroup.gnutella.library.FriendFileList fileList =
                    getCoreFileList();
                if(fileList != null)
                    fileList.removeFileListListener(eventListener);
            }
        }
        
        /* Commits to using this list. */
        void commit() {
            committed = true;
            eventListener = newEventListener();
            fileManager.getOrCreateFriendFileList(name).addFileListListener(eventListener);
            combinedShareList.addMemberList(baseList);

            com.limegroup.gnutella.library.FileList fileList = fileManager.getFriendFileList(name);

            fileList.getReadLock().lock();
            try {
                for (FileDesc fileDesc : fileList) {
                    addFileDesc(fileDesc);
                }
            } finally {
                fileList.getReadLock().unlock();
            }
        }
        
        public boolean isAddNewAudioAlways() {
            return getCoreFileList().isAddNewAudioAlways();
        }
        
        public void setAddNewAudioAlways(boolean value) {
            getCoreFileList().setAddNewAudioAlways(value);
            fireAudioCollectionChange(propertyChange, value);
            setSharing(value, Category.AUDIO);
        }
        
        public boolean isAddNewVideoAlways() {
            return getCoreFileList().isAddNewVideoAlways();
        }
        
        public void setAddNewVideoAlways(boolean value) {
            getCoreFileList().setAddNewVideoAlways(value);
            fireVideoCollectionChange(propertyChange, value);
            setSharing(value, Category.VIDEO);
        }
        
        public boolean isAddNewImageAlways() {
            return getCoreFileList().isAddNewImageAlways();
        }
        
        public void setAddNewImageAlways(boolean value) {
            getCoreFileList().setAddNewImageAlways(value);
            fireImageCollectionChange(propertyChange, value);
            setSharing(value, Category.IMAGE);
        }
        
        private void setSharing(boolean value, Category category) {
            if(value)
                addAll(name, category);
            else
                removeAll(name, category, readOnlyList);
        }
        
        public void addPropertyChangeListener(PropertyChangeListener listener) {
            propertyChange.addPropertyChangeListener(listener);
        }
        
        public void removePropertyChangeListener(PropertyChangeListener listener) {
            propertyChange.removePropertyChangeListener(listener);
        }
    } 
    
    private class CombinedShareList implements FileList<LocalFileItem> {
        private final CompositeList<LocalFileItem> compositeList;
        private final EventList<LocalFileItem> threadSafeUniqueList;
        private volatile TransformedList<LocalFileItem, LocalFileItem> swingList;
        
        public CombinedShareList() {
            compositeList = new CompositeList<LocalFileItem>();
            threadSafeUniqueList = GlazedListsFactory.uniqueList(GlazedListsFactory.threadSafeList(
                    GlazedListsFactory.readOnlyList(compositeList)),
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
    
    private void addAll(String friend, Category category) {
        final com.limegroup.gnutella.library.FileList fileList = fileManager.getFriendFileList(friend);
        EventList<LocalFileItem> filtered = GlazedListsFactory.filterList(libraryManager.getLibraryManagedList().getModel(), new CategoryFilter(category));
        final LocalFileItem[] items = filtered.toArray(new LocalFileItem[filtered.size()]);
        //TODO: change this to an executor, quick change to fix build
        Thread t = new Thread(new Runnable(){
            @Override
            public void run() {
                for(LocalFileItem item : items) {
                    fileList.add(item.getFile());
                }
            }
        });
        t.start();
    }
    
    private void removeAll(String friend, Category category, EventList<LocalFileItem> list) {
        final com.limegroup.gnutella.library.FileList fileList = fileManager.getFriendFileList(friend);
        EventList<LocalFileItem> filtered = GlazedListsFactory.filterList(list, new CategoryFilter(category));
        final LocalFileItem[] items = filtered.toArray(new LocalFileItem[filtered.size()]);
        //TODO: change this to an executor, quick change to fix build
        Thread t = new Thread(new Runnable(){
            @Override
            public void run() {
                for(LocalFileItem item : items) {
                    if(item != null)
                        fileList.remove(item.getFile());
                }
            }
        });
        t.start();
    }
    
    private void fireAudioCollectionChange(PropertyChangeSupport propertyChange, boolean newValue) {
        propertyChange.firePropertyChange("audioCollection", !newValue, newValue);
    }
    
    private void fireVideoCollectionChange(PropertyChangeSupport propertyChange, boolean newValue) {
        propertyChange.firePropertyChange("videoCollection", !newValue, newValue);
    }
    
    private void fireImageCollectionChange(PropertyChangeSupport propertyChange, boolean newValue) {
        propertyChange.firePropertyChange("imageCollection", !newValue, newValue);
    }
    
    public class CategoryFilter implements Matcher<FileItem>{
        private final Category category;
        
        public CategoryFilter(Category category) {
            this.category = category;
        }

        @Override
        public boolean matches(FileItem item) {
            if (item == null) {
                return false;
            }
            
            if (category == null) {
                return true;
            }

            return item.getCategory().equals(category);
        }
    }
}
