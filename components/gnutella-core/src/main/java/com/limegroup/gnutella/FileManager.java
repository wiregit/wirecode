package com.limegroup.gnutella;

import java.io.File;
import java.util.List;
import java.util.Set;

import org.limewire.collection.IntSet;

import com.limegroup.gnutella.downloader.VerifyingFile;
import com.limegroup.gnutella.xml.LimeXMLDocument;


/**
 * Provides operations to add and remove individual files, directory, or sets
 * of directories.
 */
public interface FileManager {

    /**   
     * Asynchronously loads all files by calling loadSettings.
     */
    public abstract void start();

    public abstract void stop();

    /**
     * Returns the FileList containing Shared files.
     */
    public FileList getSharedFileList();
    
    /**
     * Returns the FileList containing Store files.
     */
    public FileList getStoreFileList();
    
    /**
     * Returns the FileList containing files shared with
     * all buddys.
     */
    public FileList getBuddyFileList();
    
    /**
     * Returns the FileList containing Shared Buddy files of this name. If
     * this buddy list does not exist, a new Buddylist with this name will 
     * be created.
     */
    public FileList getBuddyFileList(String name);
    
    /**
     * Creates a new FileList with the given name.
     */
    public void addBuddyFileList(String name);
    
    /**
     * Removes the shared Buddy list containing this name.
     * @param name
     */
    public void removeBuddyFileList(String name);
    
    /**
     * Returns true if a shared buddy list of this name exists, false
     * otherwise.
     */
    public boolean containsBuddyFileList(String name);
    
    /**
     * Returns the FileList containing Incomplete files.
     */
    public FileList getIncompleteFileList();

    /**
     * Returns the number of pending files.
     */
    public abstract int getNumPendingFiles();

    /**
     * Returns the <tt>FileDesc</tt> that is wrapping this <tt>File</tt>
     * or null if the file is not shared or not a store file.
     */
    public FileDesc getFileDesc(File f);

    /**
     * Returns the <tt>FileDesc</tt> for the specified URN.  This only returns 
     * one <tt>FileDesc</tt>, even though multiple indices are possible with 
     * HUGE v. 0.93.
     *
     * @param urn the urn for the file
     * @return the <tt>FileDesc</tt> corresponding to the requested urn, or
     *  <tt>null</tt> if no matching <tt>FileDesc</tt> could be found
     */
    public FileDesc getFileDesc(final URN urn);

    /**
     * Starts a new revision of the library, ensuring that only items present
     * in the appropriate sharing settings are shared.
     *
     * This method is non-blocking and thread-safe.
     *
     * @modifies this
     */
    public abstract void loadSettings();

    /**
     * Loads the FileManager with a new list of directories.
     */
    public abstract void loadWithNewDirectories(Set<? extends File> shared, Set<File> blackListSet);

    /**
     * Returns whether or not the loading is finished.
     */
    public abstract boolean isLoadFinished();

    /**
     * Returns whether or not the updating is finished.
     */
    public abstract boolean isUpdating();
 
    /**
     * Adds a given folder to be shared.
     * NOTE: this remains for backwards compatibility. This should not be
     * used as of 5.0. Instead a generic folder should be added using addFolder().
     */
    public abstract boolean addSharedFolder(File folder);
    
    /**
     * Removes a given directory from being completely shared.
     * NOTE: this remains for backwards compatibility. This should not be
     * used as of 5.0. Instead a generic folder should be added using removeFolder().
     */
    public abstract void removeSharedFolder(File folder);

    /**
     * Adds a set of folders to be shared and a black list of subfolders that should
     * not be shared.
     * NOTE: this remains for backwards compatibility. This should not be
     * used as of 5.0. Instead a generic folder should be added using addFolders
     * 
     * @param folders set of folders to  be shared
     * @param blackListedSet the subfolders or subsubfolders that are not to be
     * shared
     */
    public abstract void addSharedFolders(Set<File> folders, Set<File> blackListedSet);

    public abstract Set<File> getFolderNotToShare();
    
    /**
     * Adds a folder to be managed. This folder will be checked everytime a refresh occurs
     */
    public void addFolder(File folder);
    
    /**
     * Adds a set of folders to be managed.
     */
    public void addFolders(Set<File> folders);
    
    /**
     * Removes a managed folder.
     * @param folder - folder to remove
     */
    public void removeFolder(File folder);

    /**
     * Creates a FileDesc for the file if one doesn't yet exist, then
     * adds the FileDesc to the sharedFileList
     */
    public void addSharedFile(File file);
    
    /**
     * Creates a FileDesc for the file if one doesn't yet exist using the supplied
     * xml document, then adds the FileDesc to the sharedFileList
     */
    public void addSharedFile(File file, List<? extends LimeXMLDocument> list);
    
    /**
     * Creates a FileDesc for the file if one doesn't yet exist, then 
     * adds the FileDesc to the sharedFileList even if the file is not
     * shareable by default.
     */
    public void addSharedFileAlways(File file);
    
    /**
     * Creates a FileDesc for the file if one doesn't yet exist, then 
     * adds the FileDesc to the sharedFileList even if the file is not
     * shareable by default.
     */
    public void addSharedFileAlways(File file, List<? extends LimeXMLDocument> list);
    
    /**
     * Creates a FileDesc for the file if one doesn't yet exist, then 
     * adds the FileDesc to the sharedFileList only for the session.
     */
    public void addSharedFileForFession(File file);
    
    /**
     * Creates a FileDesc for the file if one doesn't yet exist, then adds the
     * FileDesc to the buddy list with the given name. If no buddy list by that
     * name exists no action is performed.
     */
    public void addBuddyFile(String name, File file);
    
    /**
     * Adds the file to the master file list. 
     */
    public void addFile(File file);

    /**
     * Adds the file to the master file list, using the given list of metadata.
     */
    public void addFile(File file, List<? extends LimeXMLDocument> list);

    /**
     * Returns the FileDesc located at the index in the list
     * @return the FileDesc if it exists, or null if the file no longer
     *          exists in this list.
     */
    public FileDesc get(int index);
    
    /**
     * Returns true if 
     */
    public boolean isValidIndex(int index);

    /**
     * @modifies this
     * @effects ensures the first instance of the given file is not
     *  shared.  Returns FileDesc iff the file was removed.  
     *  In this case, the file's index will not be assigned to any 
     *  other files.  Note that the file is not actually removed from
     *  disk.
     */
    public FileDesc removeFile(File f);

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
    public abstract void addIncompleteFile(File incompleteFile, Set<? extends URN> urns,
            String name, long size, VerifyingFile vf);

    /**
     * Notification that a file has changed and new hashes should be
     * calculated.
     */
    public void fileChanged(File f, List<LimeXMLDocument> xmlDocs);

    /** Attempts to validate the given FileDesc. */
    public abstract void validate(final FileDesc fd);

    /**
     * If oldName isn't shared, returns false.  Otherwise removes "oldName",
     * adds "newName", and returns true iff newName is actually shared.  The new
     * file may or may not have the same index as the original.
     */
    public void renameFile(File oldName, File newName);

    /**
     * Validates a file, moving it from 'SENSITIVE_DIRECTORIES_NOT_TO_SHARE'
     * to SENSITIVE_DIRECTORIES_VALIDATED'.
     */
    public abstract void validateSensitiveFile(File dir);

    /**
     * Invalidates a file, removing it from the shared directories, validated
     * sensitive directories, and adding it to the sensitive directories
     * not to share (so we don't ask again in the future).
     */
    public abstract void invalidateSensitiveFile(File dir);

    /**
     * @return true if currently we have any files that are 
     * shared by the application.
     */
    public abstract boolean hasApplicationSharedFiles();


    public abstract boolean isRareFile(FileDesc fd);

    /**
     * Returns true if this file is in a directory that is completely shared.
     */
    public abstract boolean isFileInCompletelySharedDirectory(File f);

    /**
     * Returns true if this dir is completely shared. 
     */
    public abstract boolean isFolderShared(File dir);

    /**
     * Returns true if this directory is the folder that is selected to download LWS songs into
     * or is a subfolder of the LWS folder and false otherwise. NOTE: this folder can be
     * shared and can be the same as the currently selected root shared folder.
     * 
     * @param file
     * @return
     */
    public abstract boolean isStoreDirectory(File file);

    /**
     * Returns true if this folder is sharable.
     * <p>
     * Unsharable folders include:
     * <ul>
     * <li>A non-directory or unreadable folder</li>
     * <li>The incomplete directory</li>
     * <li>The 'application special share directory'</li>
     * <li>Any root directory</li>
     * <li>Any directory listed in 'directories not to share' (<i>Only if
     * includeExcludedDirectories is true</i>)</li>
     * </ul>
     * 
     * @param folder The folder to check for sharability
     * @param includeExcludedDirectories True if this should exclude the folder
     *        from sharability if it is listed in DIRECTORIES_NOT_TO_SHARE
     * @return true if the folder can be shared
     */
    public abstract boolean isFolderShareable(File folder, boolean includeExcludedDirectories);

    /**
     * @return true if there exists an application-shared file with the
     * provided name.
     */
    public abstract boolean isFileApplicationShared(String name);
    
    /**
     * Notification that an IncompleteFileDesc has been updated.
     */
    public void fileURNSUpdated(FileDesc ifd);

    public IntSet getIndices(URN urn);
    
    /**
     * Returns the number of files
     */
    public int size();

    /**
     * registers a listener for FileManagerEvents
     */
    public abstract void addFileEventListener(FileEventListener listener);

    /**
     * unregisters a listener for FileManagerEvents
     */
    public abstract void removeFileEventListener(FileEventListener listener);
}