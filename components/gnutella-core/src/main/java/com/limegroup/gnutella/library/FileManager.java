package com.limegroup.gnutella.library;

/**
 * Provides operations to add and remove individual files, directory, or sets of
 * directories.
 * 
 * This returns both {@link SharedFileCollection SharedFileCollection} and {@link FileView FileViews}.
 * The difference is that a {@link SharedFileCollection} can contain a list of files and be shared
 * with any number of people (or the p2p network).  A {@link FileView} represents all files that
 * a particular person (or the p2p network) has access to. 
 */
public interface FileManager {

    /**
     * Asynchronously loads all files by calling loadSettings.
     */
    void start();

    void stop();

    /**
     * Unload files in friend's file list. This has the effect of making the file sharing
     * characteristics invisible externally.
     *
     * @param id friend id
     */
    public void unloadFilesForFriend(String friendId);
    
    /**
     * Returns the {@link Library}.
     */
    Library getLibrary();

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
    SharedFileCollection getOrCreateSharedCollection(int collectionId);

    /** Removes the shared collection. */
    void removeSharedCollection(int collectionId);

    /** Returns the {@link FileCollection} containing Incomplete files. */
    IncompleteFileCollection getIncompleteFileCollection();
    
    /** Returns the GnutellaFileView from which all files shared with Gnutella can be viewed. */
    GnutellaFileView getGnutellaFileView();
    
    /** Returns a {@link FileView} that represents all files that the given id has access to. */
    FileView getFileViewForId(String friendId);

    // TODO: REMOVE THIS!
    SharedFileCollection getOrCreateSharedCollectionByName(String name);
    void removeSharedCollectionByName(String name);
}