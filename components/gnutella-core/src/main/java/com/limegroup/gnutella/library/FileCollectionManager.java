package com.limegroup.gnutella.library;

public interface FileCollectionManager {

    /**
     * Returns the {@link FileCollection} that always shares things with the p2p network.
     * 
     * This is <b>NOT</b> the collection or view that represents everything shared with
     * the p2p network.  For that, use {@link #getGnutellaFileView()}. 
     */
    GnutellaFileCollection getGnutellaCollection();
    
    /** Returns the {@link FileCollection} with the given id. */
    SharedFileCollection getSharedCollection(int collectionId);

    /** Returns a {@link FileCollection} with the given id, or creates one if it doesn't already exist. */
    SharedFileCollection getCollectionById(int collectionId);

    /** Removes the shared collection. */
    void removeCollectionById(int collectionId);

    /** Returns the {@link FileCollection} containing Incomplete files. */
    // TODO: Split read-only uses into FileView exposed from FileViewManager
    IncompleteFileCollection getIncompleteFileCollection();

    // TODO: REMOVE THIS!
    SharedFileCollection getOrCreateSharedCollectionByName(String name);
    void removeSharedCollectionByName(String name);
    


    /**
     * Unload files in friend's file list. This has the effect of making the file sharing
     * characteristics invisible externally.
     *
     * @param id friend id
     */
    public void unloadFilesForFriend(String friendId);

}
