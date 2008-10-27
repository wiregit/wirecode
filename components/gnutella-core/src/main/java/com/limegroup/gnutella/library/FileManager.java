package com.limegroup.gnutella.library;


/**
 * Provides operations to add and remove individual files, directory, or sets of
 * directories.
 */
public interface FileManager {

    /**
     * Asynchronously loads all files by calling loadSettings.
     */
    void start();

    void stop();
    
    /**
     * Returns the Managed file list.
     */
    ManagedFileList getManagedFileList();

    /**
     * Returns the FileList containing Shared files.
     */
    GnutellaFileList getGnutellaSharedFileList();
    
    /**
     * Returns the FileList containing Shared Friend files of this name.
     * If no list exists, returns null.
     */
    FriendFileList getFriendFileList(String name);

    /**
     * Returns a FileList for the given friend, or creates
     * one if it doesn't exist already.
     */
    FriendFileList getOrCreateFriendFileList(String name);

    /**
     * Removes the shared Friend list containing this name.
     */
    void removeFriendFileList(String name);

    /**
     * Returns the FileList containing Incomplete files.
     */
    IncompleteFileList getIncompleteFileList();
}