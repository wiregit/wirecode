package org.limewire.core.impl.library;

import java.util.concurrent.ConcurrentHashMap;

import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendEvent;
import org.limewire.core.api.library.FileList;
import org.limewire.core.api.library.FriendShareListEvent;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.listener.BlockingEvent;
import org.limewire.listener.EventBroadcaster;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.library.FileCollection;
import com.limegroup.gnutella.library.FileCollectionManager;
import com.limegroup.gnutella.library.FileViewManager;
import com.limegroup.gnutella.library.GnutellaFiles;

@Singleton
class ShareListManagerImpl implements ShareListManager {
    
    private static final Log LOG = LogFactory.getLog(ShareListManagerImpl.class);
    
    private final FileViewManager viewManager;
    private final FileCollectionManager collectionManager;
    
    private final CombinedShareList combinedShareList;
    
    private final LocalFileList gnutellaFileList;    
    
    private final ConcurrentHashMap<String, FriendFileListImpl> friendLocalFileLists;

    private final EventBroadcaster<FriendShareListEvent> friendShareListEventBroadcaster;
    
    private final CoreLocalFileItemFactory coreLocalFileItemFactory;

    @Inject
    ShareListManagerImpl(FileCollectionManager collectionManager, FileViewManager viewManager,
            CoreLocalFileItemFactory coreLocalFileItemFactory,
            EventBroadcaster<FriendShareListEvent> friendShareListEventListener,
            LibraryManager libraryManager,
            @GnutellaFiles FileCollection gnutellaFileCollection) {
        this.collectionManager = collectionManager;
        this.viewManager = viewManager;
        this.coreLocalFileItemFactory = coreLocalFileItemFactory;
        this.friendShareListEventBroadcaster = friendShareListEventListener;
        this.combinedShareList = new CombinedShareList(libraryManager.getLibraryListEventPublisher(), libraryManager.getReadWriteLock());
        this.gnutellaFileList = new SimpleFriendFileListImpl(coreLocalFileItemFactory, gnutellaFileCollection, viewManager.getGnutellaFileView(), combinedShareList);
        this.friendLocalFileLists = new ConcurrentHashMap<String, FriendFileListImpl>();
    }

    @Inject
    void register(@Named("known")ListenerSupport<FriendEvent> knownListeners) {

        knownListeners.addListener(new EventListener<FriendEvent>() {
            @BlockingEvent(queueName="share list friend event handler")
            @Override
            public void handleEvent(FriendEvent event) {
                Friend friend = event.getData();
                switch (event.getType()) {
                    case ADDED:
                        getOrCreateFriendShareList(friend);
                        break;
                    case REMOVED:
                        unloadFilesForFriend(friend);
                        break;
                    case DELETE:
                        deleteFriendShareList(friend);
                        break;
                }
            }
        });
    }

    /**
     * Similar to {@link #deleteFriendShareList}, but does not save.
     * 
     * 1. Removes managedFilelist listener
     * 2. Removes lists associated with this friend from maps
     * 3. Clears the files from the list, decrement share count for each file
     */
    private void unloadFilesForFriend(Friend friend) {  
        collectionManager.unloadCollectionByName(friend.getId());
        FriendFileListImpl list = friendLocalFileLists.remove(friend.getId());
        if(list != null) {
            friendShareListEventBroadcaster.broadcast(new FriendShareListEvent(FriendShareListEvent.Type.FRIEND_SHARE_LIST_REMOVED, list, friend));
            list.dispose();
        }        
    }

    @Override
    public LocalFileList getGnutellaShareList() {
        return gnutellaFileList;
    }
    
    @Override
    public FileList<LocalFileItem> getCombinedShareList() {
        return combinedShareList;
    }
    
    @Override
    public LocalFileList getOrCreateFriendShareList(Friend friend) {
        LOG.debugf("get|Create library for friend {0}", friend.getId());
        
        LocalFileList list = friendLocalFileLists.get(friend.getId());
        if(list != null){
            LOG.debugf("Returning existing library for friend {0}", friend.getId());
            return list;
        }
        
        FriendFileListImpl newList = new FriendFileListImpl(coreLocalFileItemFactory, collectionManager, viewManager, friend.getId(), combinedShareList);        
        LocalFileList existing = friendLocalFileLists.putIfAbsent(friend.getId(), newList);        
        
        if(existing == null) {
            LOG.debugf("No existing library for friend {0}", friend.getId());
            newList.commit();
            friendShareListEventBroadcaster.broadcast(new FriendShareListEvent(FriendShareListEvent.Type.FRIEND_SHARE_LIST_ADDED, newList, friend));
            return newList;
        } else {
            LOG.debugf("Already an existing lib for friend {0}", friend.getId());
            newList.dispose();
            return existing;
        }
    }

    @Override
    public LocalFileList getFriendShareList(Friend friend) {
        return friendLocalFileLists.get(friend.getId());
    }

    private void deleteFriendShareList(Friend friend) {  
        collectionManager.removeCollectionByName(friend.getId());
        FriendFileListImpl list = friendLocalFileLists.remove(friend.getId());
        if(list != null) {
            friendShareListEventBroadcaster.broadcast(new FriendShareListEvent(FriendShareListEvent.Type.FRIEND_SHARE_LIST_DELETED, list, friend));
            list.dispose();
        }        
    }
    
    
}
