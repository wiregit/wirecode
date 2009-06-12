package org.limewire.core.impl.library;

import java.util.Collection;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.SharedFileList;
import org.limewire.core.api.library.SharedFileListManager;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.util.NotImplementedException;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.library.FileCollectionManager;
import com.limegroup.gnutella.library.SharedFileCollection;
import com.limegroup.gnutella.library.SharedFileCollectionChangeEvent;

@Singleton
class SharedFileListManagerImpl implements SharedFileListManager {
    
//    private static final Log LOG = LogFactory.getLog(ShareListManagerImpl.class);
    
    private final FileCollectionManager collectionManager;    
    private final CoreLocalFileItemFactory coreLocalFileItemFactory;
    
    private final EventList<SharedFileList> sharedLists = GlazedListsFactory.threadSafeList(new BasicEventList<SharedFileList>());
    private final EventList<SharedFileList> readOnlySharedLists = GlazedListsFactory.readOnlyList(sharedLists);
    
    @Inject
    SharedFileListManagerImpl(FileCollectionManager collectionManager,
            CoreLocalFileItemFactory coreLocalFileItemFactory,
            LibraryManager libraryManager) {
        this.collectionManager = collectionManager;
        this.coreLocalFileItemFactory = coreLocalFileItemFactory;
    }
    
    @Inject void register(ListenerSupport<SharedFileCollectionChangeEvent> support) {
        for(SharedFileCollection collection : collectionManager.getSharedFileCollections()) {
            collectionAdded(collection);
        }
        
        support.addListener(new EventListener<SharedFileCollectionChangeEvent>() {
            @Override
            public void handleEvent(SharedFileCollectionChangeEvent event) {
                switch(event.getType()) { 
                case COLLECTION_ADDED:
                    collectionAdded(event.getSource());
                    break;
                case COLLECTION_REMOVED:
                    collectionRemoved(event.getSource());
                    break;
                case FRIEND_ADDED:
                    friendAddedToCollection(event.getSource(), event.getFriendId());
                    break;
                case FRIEND_IDS_CHANGED:
                    friendsSetInCollection(event.getSource(), event.getNewFriendIds());
                    break;
                case FRIEND_REMOVED:
                    friendRemoved(event.getSource(), event.getFriendId());
                    break;
                case NAME_CHANGED:
                    nameChanged(event.getSource());
                    break;
                }
            }
        });
    }

    // we technically don't have to change anything here, but we want to
    // make the list trigger an event to signify that something changed,
    // so we get the index of where it used to be & reset it.
    private void nameChanged(SharedFileCollection collection) {
        sharedLists.getReadWriteLock().writeLock().lock();
        try {
            for (int i = 0; i < sharedLists.size(); i++) {
                SharedFileListImpl impl = (SharedFileListImpl)sharedLists.get(i);
                if (impl.getCoreCollection() == collection) {
                    sharedLists.set(i, impl); // reset it to trigger event.
                    break;
                }
            }
        } finally {
            sharedLists.getReadWriteLock().writeLock().unlock();
        }
    }

    private void friendRemoved(SharedFileCollection collection, String friendId) {
        getListForCollection(collection).friendRemoved(friendId);
    }

    private void friendsSetInCollection(SharedFileCollection collection, Collection<String> newFriendIds) {
        getListForCollection(collection).friendsSet(newFriendIds);
    }

    private void friendAddedToCollection(SharedFileCollection collection, String friendId) {
        getListForCollection(collection).friendAdded(friendId);
    }    
    
    private void collectionAdded(SharedFileCollection collection) {
        SharedFileListImpl listImpl = new SharedFileListImpl(coreLocalFileItemFactory, collection);
        listImpl.friendsSet(collection.getFriendList());
        sharedLists.add(listImpl);
    }
    
    private void collectionRemoved(SharedFileCollection collection) {
        sharedLists.remove(getListForCollection(collection));
    }
    
    private SharedFileListImpl getListForCollection(SharedFileCollection collection) {
       sharedLists.getReadWriteLock().readLock().lock();
        try {
            for (SharedFileList list : sharedLists) {
                SharedFileListImpl impl = (SharedFileListImpl) list;
                if (impl.getCoreCollection() == collection) {
                    return impl;
                }
            }
            return null;
        } finally {
            sharedLists.getReadWriteLock().readLock().unlock();
        }
    }

    @Override
    public void createNewSharedFileList(String name) {
        collectionManager.createNewCollection(name);        
    }

    @Override
    public EventList<SharedFileList> getModel() {
        return readOnlySharedLists;
    }

    @Override
    public SharedFileList getSharedFileList(String name) {
        // TODO: this needs to be in a different thread.
        sharedLists.getReadWriteLock().readLock().lock();
        try {
            for(SharedFileList fileList : sharedLists) {
                if(fileList.getCollectionName().equals(name))
                    return fileList;
            }
        } finally {
            sharedLists.getReadWriteLock().readLock().unlock();
        }
        return null;
    }


    @Override
    public void deleteSharedFileList(String name) {
        throw new NotImplementedException("Deletion of filelists not implemented");
    }

    @Override
    public void renameSharedFileList(String currentName, String newName) {
        throw new NotImplementedException("Rename of filelists not implemented");
    }
}
