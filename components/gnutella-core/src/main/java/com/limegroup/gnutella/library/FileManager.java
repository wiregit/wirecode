package com.limegroup.gnutella.library;

import java.io.File;
import java.util.List;
import java.util.Set;

import org.limewire.collection.IntSet;
import org.limewire.listener.EventListener;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.VerifyingFile;
import com.limegroup.gnutella.xml.LimeXMLDocument;

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
    FileList getManagedFileList();

    /**
     * Returns the FileList containing Shared files.
     */
    FileList getGnutellaSharedFileList();

    /**
     * Returns the FileList containing Store files.
     */
    FileList getStoreFileList();

    /**
     * Returns the FileList containing Shared Friend files of this name.
     * If no list exists, returns null.
     */
    FileList getFriendFileList(String name);

    /**
     * Returns a FileList for the given friend, or creates
     * one if it doesn't exist already.
     */
    FileList getOrCreateFriendFileList(String name);

    /**
     * Removes the shared Friend list containing this name.
     */
    void removeFriendFileList(String name);

    /**
     * Returns the FileList containing Incomplete files.
     */
    FileList getIncompleteFileList();

    /**
     * Starts a new revision of the library, ensuring that only items present in
     * the appropriate sharing settings are shared.
     * 
     * This method is non-blocking and thread-safe.
     * 
     * @modifies this
     */
    void loadSettings();

    /**
     * Loads the FileManager with a new list of directories.
     */
    void loadWithNewDirectories(Set<? extends File> shared, Set<File> blackListSet);

    /**
     * Returns whether or not the loading is finished.
     */
    boolean isLoadFinished();

    /**
     * Adds a given folder to be shared. NOTE: this remains for backwards
     * compatibility. This should not be used as of 5.0. Instead a generic
     * folder should be added using addFolder().
     */
    boolean addSharedFolder(File folder);

    /**
     * Removes a given directory from being completely shared. NOTE: this
     * remains for backwards compatibility. This should not be used as of 5.0.
     * Instead a generic folder should be added using removeFolder().
     */
    void removeSharedFolder(File folder);

    /**
     * Adds a set of folders to be shared and a black list of subfolders that
     * should not be shared. NOTE: this remains for backwards compatibility.
     * This should not be used as of 5.0. Instead a generic folder should be
     * added using addFolders
     * 
     * @param folders set of folders to be shared
     * @param blackListedSet the subfolders or subsubfolders that are not to be
     *        shared
     */
    void addSharedFolders(Set<File> folders, Set<File> blackListedSet);

    /**
     * Creates a FileDesc for the file if one doesn't yet exist, then adds the
     * FileDesc to the sharedFileList
     */
    void addSharedFile(File file);

    /**
     * Creates a FileDesc for the file if one doesn't yet exist using the
     * supplied xml document, then adds the FileDesc to the sharedFileList
     */
    void addSharedFile(File file, List<? extends LimeXMLDocument> list);

    /**
     * Creates a FileDesc for the file if one doesn't yet exist, then adds the
     * FileDesc to the sharedFileList even if the file is not shareable by
     * default.
     */
    void addSharedFileAlways(File file);

    /**
     * Creates a FileDesc for the file if one doesn't yet exist, then adds the
     * FileDesc to the sharedFileList even if the file is not shareable by
     * default.
     */
    void addSharedFileAlways(File file, List<? extends LimeXMLDocument> list);

    /**
     * Creates a FileDesc for the file if one doesn't yet exist, then adds the
     * FileDesc to the sharedFileList only for the session.
     */
    void addSharedFileForSession(File file);

    /**
     * Creates a FileDesc for the file if one doesn't yet exist, then adds the
     * FileDesc to the friend list with the given name. If no friend list by that
     * name exists no action is performed.
     */
    void addFriendFile(String id, File file);

    /**
     * Returns the FileDesc located at the index in the list
     * 
     * @return the FileDesc if it exists, or null if the file no longer exists
     *         in this list.
     */
    FileDesc get(int index);
    
    /** Returns all indexes this URN is at. */
    IntSet getIndices(URN urn);

    /**
     * Adds an incomplete file to be used for partial file sharing.
     * 
     * @modifies this
     * @param incompleteFile the incomplete file.
     * @param urns the set of all known URNs for this incomplete file
     * @param name the completed name of this incomplete file
     * @param size the completed size of this incomplete file
     * @param vf the VerifyingFile containing the ranges for this inc. file
     */
    void addIncompleteFile(File incompleteFile, Set<? extends URN> urns,
            String name, long size, VerifyingFile vf);

    /**
     * Notification that a file has changed and new hashes should be calculated.
     */
    void fileChanged(File f, List<LimeXMLDocument> xmlDocs);

    /** Attempts to validate the given FileDesc. */
    void validate(final FileDesc fd);

    /**
     * If oldName isn't shared, returns false. Otherwise removes "oldName", adds
     * "newName", and returns true iff newName is actually shared. The new file
     * may or may not have the same index as the original.
     */
    void renameFile(File oldName, File newName);

    /**
     * @return true if currently we have any files that are shared by the
     *         application.
     */
    boolean hasApplicationSharedFiles();

    /**
     * Returns true if this file is in a directory that is completely shared.
     */
    boolean isFileInCompletelySharedDirectory(File f);

    /**
     * @return true if there exists an application-shared file with the provided
     *         name.
     */
    boolean isFileApplicationShared(String name);

    /**
     * Allows a FileList to set a dirty flag so changes can be written to disk.
     */
    void setDirtySaveLater();

    /**
     * registers a listener for FileManagerEvents
     */
    void addFileEventListener(EventListener<FileManagerEvent> listener);

    /**
     * unregisters a listener for FileManagerEvents
     */
    void removeFileEventListener(EventListener<FileManagerEvent> listener);
}