package com.limegroup.gnutella.library;

import org.limewire.friend.api.Friend;
import org.limewire.inspection.DataCategory;
import org.limewire.inspection.InspectionPoint;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
class FileCollectionManagerImpl implements FileCollectionManager {
    
    @InspectionPoint(value = "gnutella shared file list", category = DataCategory.USAGE)
    private final SharedFileCollectionImpl defaultSharedCollection;
    
    @InspectionPoint(value = "incomplete file list", category = DataCategory.USAGE)
    private final IncompleteFileCollectionImpl incompleteCollection;
    
    @Inject public FileCollectionManagerImpl(
            IncompleteFileCollectionImpl incompleteFileCollectionImpl,
            SharedFileCollectionImplFactory sharedFileCollectionImplFactory) {
        this.incompleteCollection = incompleteFileCollectionImpl;
        this.incompleteCollection.initialize();
        this.defaultSharedCollection = sharedFileCollectionImplFactory.createSharedFileCollectionImpl(LibraryFileData.DEFAULT_SHARED_COLLECTION_ID, true, Friend.P2P_FRIEND_ID);
        this.defaultSharedCollection.initialize();
    }
    
    @Override
    public SharedFileCollection getSharedFileCollection() {
        return defaultSharedCollection;
    }    
}
