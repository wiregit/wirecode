package com.limegroup.gnutella.library;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.limewire.friend.api.Friend;
import org.limewire.inspection.InspectionPoint;
import org.limewire.inspection.Inspectable;
import org.limewire.inspection.InspectableContainer;
import org.limewire.listener.EventBroadcaster;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
class FileCollectionManagerImpl implements FileCollectionManager {
    
    @InspectionPoint("gnutella shared file list")
    private final SharedFileCollectionImpl defaultSharedCollection;
    
    @InspectionPoint("incomplete file list")
    private final IncompleteFileCollectionImpl incompleteCollection;
    
    @SuppressWarnings("unused")
    @InspectableContainer
    private class LazyInspectableContainer {
        
        @InspectionPoint("friend file lists")
        private final Inspectable FRIEND_FILE_LIST = new Inspectable() {
            @Override
            public Object inspect() {
                // cycle thru all sharedFileCollection objects, and track file list size by friend
                // list of friend share sizes is what is returned as inspection.
                Map<String, Object> data = new HashMap<String, Object>();
                Map<String, Integer> friendToShareSize = new HashMap<String, Integer>();
                
                synchronized (this) {
                    for (SharedFileCollectionImpl shareList : sharedCollections.values()) {
                        List<String> friendList = shareList.getFriendList();
                        int shareSize = shareList.size();
                        for (String friendName : friendList) {
                            int totalShareSizeForFriend = shareSize;
                            if (friendToShareSize.get(friendName) != null) {
                                totalShareSizeForFriend += friendToShareSize.get(friendName);
                            }
                            friendToShareSize.put(friendName, totalShareSizeForFriend);    
                        }
                    }
                }
                List<Integer> sizes = new ArrayList<Integer>(friendToShareSize.keySet().size());
                for (String friend: friendToShareSize.keySet()) {
                    sizes.add(friendToShareSize.get(friend));    
                }
                data.put("sizes", sizes);
                return data;
            }
        };
    }
    
    private final SharedFileCollectionImplFactory sharedFileCollectionImplFactory;
    
    private final Map<Integer, SharedFileCollectionImpl> sharedCollections =
        new HashMap<Integer,SharedFileCollectionImpl>();

    private final EventBroadcaster<SharedFileCollectionChangeEvent> sharedBroadcaster;
    
    private final Provider<LibraryFileData> libraryFileData;
    
    @Inject public FileCollectionManagerImpl(
            IncompleteFileCollectionImpl incompleteFileCollectionImpl,
            SharedFileCollectionImplFactory sharedFileCollectionImplFactory,
            EventBroadcaster<SharedFileCollectionChangeEvent> sharedBroadcaster,
            Provider<LibraryFileData> libraryFileData) {
        this.libraryFileData = libraryFileData;
        this.incompleteCollection = incompleteFileCollectionImpl;
        this.incompleteCollection.initialize();
        this.sharedFileCollectionImplFactory = sharedFileCollectionImplFactory;
        this.sharedBroadcaster = sharedBroadcaster;
        this.defaultSharedCollection = sharedFileCollectionImplFactory.createSharedFileCollectionImpl(LibraryFileData.DEFAULT_SHARED_COLLECTION_ID, true, Friend.P2P_FRIEND_ID);
        this.defaultSharedCollection.initialize();
    }
    
    void loadStoredCollections() {
        for(Integer id : libraryFileData.get().getStoredCollectionIds()) {
            if(!sharedCollections.containsKey(id) && id != LibraryFileData.DEFAULT_SHARED_COLLECTION_ID) {
                SharedFileCollectionImpl collection =  sharedFileCollectionImplFactory.createSharedFileCollectionImpl(id, false);
                collection.initialize();
                synchronized(this) {
                    sharedCollections.put(id, collection);
                }
                sharedBroadcaster.broadcast(new SharedFileCollectionChangeEvent(SharedFileCollectionChangeEvent.Type.COLLECTION_ADDED, collection));
            }
        }
    }    

    FileCollection getGnutellaCollection() {
        return defaultSharedCollection;
    }

    @Override
    public synchronized SharedFileCollection getCollectionById(int collectionId) {
        if(collectionId == LibraryFileData.DEFAULT_SHARED_COLLECTION_ID) {
            return defaultSharedCollection;
        } else {
            return sharedCollections.get(collectionId);
        }
    }

    @Override
    public void removeCollectionById(int collectionId) {
        // Cannot remove the default collection.
        if(collectionId != LibraryFileData.DEFAULT_SHARED_COLLECTION_ID) {        
            // if it was a valid key, remove saved references to it
            SharedFileCollectionImpl removeFileList;
            synchronized(this) {
                removeFileList = sharedCollections.remove(collectionId);
            }
            
            if(removeFileList != null) {
                removeFileList.dispose();
                sharedBroadcaster.broadcast(new SharedFileCollectionChangeEvent(SharedFileCollectionChangeEvent.Type.COLLECTION_REMOVED, removeFileList));
            }
        }
    }
    
    private synchronized SharedFileCollectionImpl createNewCollectionImpl(String name) {
        int newId = libraryFileData.get().createNewCollection(name);
        SharedFileCollectionImpl collection =  sharedFileCollectionImplFactory.createSharedFileCollectionImpl(newId, false);
        collection.initialize();
        sharedCollections.put(newId, collection);
        return collection;
    }
    
    @Override
    public SharedFileCollection createNewCollection(String name) {
        SharedFileCollectionImpl collection = createNewCollectionImpl(name);
        sharedBroadcaster.broadcast(new SharedFileCollectionChangeEvent(SharedFileCollectionChangeEvent.Type.COLLECTION_ADDED, collection));
        return collection;
    }
    
    @Override
    public synchronized List<SharedFileCollection> getSharedFileCollections() {
        List<SharedFileCollection> collections = new ArrayList<SharedFileCollection>(sharedCollections.size() + 1);
        collections.add(defaultSharedCollection);
        collections.addAll(sharedCollections.values());
        return collections;
    }
    
}
