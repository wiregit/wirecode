package com.limegroup.gnutella.library;

import java.util.List;

public interface FileCollectionManager {

    /**
     * Returns the {@link FileCollection} that always shares things with the p2p network.
     * Things in this collection may also be shared with other friends.
     * 
     * This is <b>NOT</b> the collection or view that represents everything shared with
     * the p2p network.  For that, use {@link FileViewManager#getGnutellaFileView()}. 
     */
    FileCollection getGnutellaCollection();

    /** Returns a {@link SharedFileCollection} with the given id. */
    SharedFileCollection getCollectionById(int collectionId);

    /** Removes the shared collection. */
    void removeCollectionById(int collectionId);

    /** Returns the {@link FileCollection} containing Incomplete files. */
    IncompleteFileCollection getIncompleteFileCollection();
    
    /** Returns a new collection named the given name. */
    SharedFileCollection createNewCollection(String name);
    
    /** Returns all current shared collections. */
    List<SharedFileCollection> getSharedFileCollections();

}
