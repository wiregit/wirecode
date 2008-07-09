package com.limegroup.gnutella;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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
     * Returns the FileList containing Shared files
     */
    public FileList getSharedFileList();
    
    /**
     * Returns the FileList containing Store files
     */
    public FileList getStoreFileList();

    /**
     * Returns the number of pending files.
     */
    public abstract int getNumPendingFiles();

    /**
     * Returns the <tt>FileDesc</tt> that is wrapping this <tt>File</tt>
     * or null if the file is not shared or not a store file.
     */
    public abstract FileDesc getFileDescForFile(File f);

    /**
     * Returns the <tt>FileDesc</tt> for the specified URN.  This only returns 
     * one <tt>FileDesc</tt>, even though multiple indices are possible with 
     * HUGE v. 0.93.
     *
     * @param urn the urn for the file
     * @return the <tt>FileDesc</tt> corresponding to the requested urn, or
     *  <tt>null</tt> if no matching <tt>FileDesc</tt> could be found
     */
    public abstract FileDesc getFileDescForUrn(final URN urn);

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
     * Removes a given directory from being completely shared.
     */
    public abstract void removeFolderIfShared(File folder);

    /**
     * Adds a set of folders to be shared and a black list of subfolders that should
     * not be shared.
     * 
     * @param folders set of folders to  be shared
     * @param blackListedSet the subfolders or subsubfolders that are not to be
     * shared
     */
    public abstract void addSharedFolders(Set<File> folders, Set<File> blackListedSet);

    public abstract Set<File> getFolderNotToShare();

    /**
     * Adds a given folder to be shared.
     */
    public abstract boolean addSharedFolder(File folder);

    /**
     * Always shares the given file.
     */
    public abstract void addFileAlways(File file);

    /**
     * Always shares the given file, using the given list of metadata.
     */
    public abstract void addFileAlways(File file, List<? extends LimeXMLDocument> list);

    /**
     * adds a file that will be shared during this session of limewire
     * only.
     */
    public abstract void addFileForSession(File file);

    /**
     * Adds the given file if it's shared.
     */
    public abstract void addFileIfShared(File file);

    /**
     * Adds the file if it's shared, using the given list of metadata.
     */
    public abstract void addFileIfShared(File file, List<? extends LimeXMLDocument> list);

    /**
     * Removes the file if it is being shared, and then removes the file from
     * the special lists as necessary.
     * @return The FileDesc associated with this file, or null if the file was
     * not shared. 
     */
    public abstract FileDesc stopSharingFile(File file);

    /**
     * @modifies this
     * @effects ensures the first instance of the given file is not
     *  shared.  Returns FileDesc iff the file was removed.  
     *  In this case, the file's index will not be assigned to any 
     *  other files.  Note that the file is not actually removed from
     *  disk.
     */
    public abstract FileDesc removeFileIfSharedOrStore(File f);

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
    public abstract void fileChanged(File f);

    /** Attempts to validate the given FileDesc. */
    public abstract void validate(final FileDesc fd);

    /**
     * If oldName isn't shared, returns false.  Otherwise removes "oldName",
     * adds "newName", and returns true iff newName is actually shared.  The new
     * file may or may not have the same index as the original.
     */
    public abstract void renameFileIfSharedOrStore(File oldName, File newName);

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
     * Determines if there are any files shared that are not in completely shared directories.
     */
    public abstract boolean hasIndividualFiles();

    /**
     * Determines if there are any LWS files that are located in a shared
     * directory
     */
    public abstract boolean hasIndividualStoreFiles();

    /**
     * @return true if currently we have any files that are 
     * shared by the application.
     */
    public abstract boolean hasApplicationSharedFiles();

    /**
     * Returns all files that are shared while not in shared directories.
     */
    public abstract File[] getIndividualFiles();

    /**
     * Returns all files that are LWS files that are located in a shared
     *	directory
     */
    public abstract File[] getIndividualStoreFiles();

    /**
     * Returns true if the file is a store file and is located in a shared directory,
     * false otherwise
     */
    public abstract boolean isIndividualStore(File f);

    /**
     * Returns true if the file is not a store file and is shared but the entire folder 
     * is not shared, false otherwise
     */
    public abstract boolean isIndividualShare(File f);

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
     * registers a listener for FileManagerEvents
     */
    public abstract void addFileEventListener(FileEventListener listener);

    /**
     * unregisters a listener for FileManagerEvents
     */
    public abstract void removeFileEventListener(FileEventListener listener);

    /** 
     * Returns an iterator for all shared files. 
     */
    public abstract Iterator<FileDesc> getIndexingIterator();
    
    /**
     * Notification that an IncompleteFileDesc has been updated.
     */
    public void fileURNSUpdated(FileDesc ifd);

}