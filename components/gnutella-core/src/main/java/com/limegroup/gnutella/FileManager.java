package com.limegroup.gnutella;

import java.io.File;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import com.limegroup.gnutella.downloader.VerifyingFile;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.routing.QueryRouteTable;
import com.limegroup.gnutella.xml.LimeXMLDocument;


/**
 * Provides operations to add and remove
 * individual files, directory, or sets of directories.  Provides a method to
 * efficiently query for files whose names contain certain keywords.<p>
 */
public interface FileManager {

    /** The string used by Clip2 reflectors to index hosts. */
    public static final String INDEXING_QUERY = "    ";

    /** The string used by LimeWire to browse hosts. */
    public static final String BROWSE_QUERY = "*.*";

    /** A type-safe empty LimeXMLDocument list. */
    public static final List<LimeXMLDocument> EMPTY_DOCUMENTS = Collections.emptyList();

    /**
     * Characters used to tokenize queries and file names.
     */
    public static final String DELIMITERS = " -._+/*()\\,";
   
    /** Asynchronously loads all files by calling loadSettings.  Sets this's
     *  callback to be "callback", and notifies "callback" of all file loads.
     *      @modifies this
     *      @see loadSettings */
    public abstract void start();

    /**
     * Invokes {@link #start()} and waits for <code>timeout</code>
     * milliseconds for the initialization to finish.
     * 
     * @param timeout timeout in milliseconds
     * @throws InterruptedException if interrupted while waiting
     * @throws TimeoutException if timeout elapsed before initialization completed 
     */
    public abstract void startAndWait(long timeout) throws InterruptedException, TimeoutException;

    public abstract void stop();

    /**
     * Returns the size of all files, in <b>bytes</b>.  Note that the largest
     *  value that can be returned is Integer.MAX_VALUE, i.e., ~2GB.  If more
     *  bytes are being shared, returns this value.
     */
    public abstract int getSize();

    /**
     * Returns the number of files.
     * This number does NOT include incomplete files or forcibly shared network files.
     */
    public abstract int getNumFiles();

    /**
     * returns the number of store files
     * This number does NOT include incomplete files
     */
    public abstract int getNumStoreFiles();

    /**
     * Returns the number of shared incomplete files.
     */
    public abstract int getNumIncompleteFiles();

    /**
     * Returns the number of pending files.
     */
    public abstract int getNumPendingFiles();

    /**
     * Returns the number of forcibly shared files.
     */
    public abstract int getNumForcedFiles();

    /**
     * Returns the file descriptor with the given index.  Throws
     * IndexOutOfBoundsException if the index is out of range.  It is also
     * possible for the index to be within range, but for this method to
     * return <tt>null</tt>, such as the case where the file has been
     * unshared.
     *
     * @param i the index of the <tt>FileDesc</tt> to access
     * @throws <tt>IndexOutOfBoundsException</tt> if the index is out of 
     *  range
     * @return the <tt>FileDesc</tt> at the specified index, which may
     *  be <tt>null</tt>
     */
    public abstract FileDesc get(int i);

    /**
     * Determines whether or not the specified index is valid.  The index
     * is valid if it is within range of the number of files shared, i.e.,
     * if:<p>
     *
     * i >= 0 && i < _files.size() <p>
     *
     * @param i the index to check
     * @return <tt>true</tt> if the index is within range of our shared
     *  file data structure, otherwise <tt>false</tt>
     */
    public abstract boolean isValidSharedIndex(int i);

    /**
     * Returns the <tt>URN<tt> for the File.  May return null;
     */
    public abstract URN getURNForFile(File f);

    /**
     * Returns the <tt>FileDesc</tt> that is wrapping this <tt>File</tt>
     * or null if the file is not shared.
     */
    public abstract FileDesc getFileDescForFile(File f);

    /**
     * Determines whether or not the specified URN is shared in the library
     * as a complete file.
     */
    public abstract boolean isUrnShared(final URN urn);

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
     * Returns the shared <tt>FileDesc</tt> for the specified URN. This only returns 
     * one <tt>FileDesc</tt>, even though multiple indices are possible with 
     * HUGE v. 0.93.
     *
     * @param urn the urn for the file
     * @return the <tt>FileDesc</tt> corresponding to the requested urn, or
     *  <tt>null</tt> if no matching shared <tt>FileDesc</tt> could be found
     */
    public FileDesc getSharedFileDescForUrn(final URN urn);

    /**
     * Returns a list of all shared incomplete file descriptors.
     */
    public abstract FileDesc[] getIncompleteFileDescriptors();

    /**
     * Returns an array of all shared file descriptors.
     */
    public abstract FileDesc[] getAllSharedFileDescriptors();

    /**
     * Returns a list of all shared file descriptors in the given directory,
     * in any order.
     * Returns null if directory is not shared, or a zero-length array if it is
     * shared but contains no files.  This method is not recursive; files in 
     * any of the directory's children are not returned.
     */
    public abstract List<FileDesc> getSharedFilesInDirectory(File directory);

    /**
     * Starts a new revision of the library, ensuring that only items present
     * in the appropriate sharing settings are shared.
     *
     * This method is non-blocking and thread-safe.
     *
     * @modifies this
     */
    public abstract void loadSettings();

    public abstract void loadSettingsAndWait(long timeout) throws InterruptedException,
            TimeoutException;

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
     * Always shares a file, notifying the given callback when shared.
     */
    public abstract void addFileAlways(File file, FileEventListener callback);

    /**
     * Always shares the given file, using the given list of metadata.
     */
    public abstract void addFileAlways(File file, List<? extends LimeXMLDocument> list);

    /**
     * Adds the given file to share, with the given list of metadata,
     * even if it exists outside of what is currently accepted to be shared.
     * <p>
     * Too large files are still not shareable this way.
     *
     * The listener is notified if this file could or couldn't be shared.
     */
    public abstract void addFileAlways(File file, List<? extends LimeXMLDocument> list,
            FileEventListener callback);

    /**
     * adds a file that will be shared during this session of limewire
     * only.
     */
    public abstract void addFileForSession(File file);

    /**
     * adds a file that will be shared during this session of limewire
     * only.
     * 
     * The listener is notified if this file could or couldn't be shared.
     */
    public abstract void addFileForSession(File file, FileEventListener callback);

    /**
     * Adds the given file if it's shared.
     */
    public abstract void addFileIfShared(File file);

    /**
     * Adds the given file if it's shared, notifying the given callback.
     */
    public abstract void addFileIfShared(File file, FileEventListener callback);

    /**
     * Adds the file if it's shared, using the given list of metadata.
     */
    public abstract void addFileIfShared(File file, List<? extends LimeXMLDocument> list);

    /**
     * Adds the file if it's shared, using the given list of metadata,
     * informing the specified listener about the status of the sharing.
     */
    public abstract void addFileIfShared(File file, List<? extends LimeXMLDocument> list,
            FileEventListener callback);

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
     * Renames a from from 'oldName' to 'newName'.
     */
    public abstract void renameFileIfSharedOrStore(File oldName, File newName);

    /** 
     * If oldName isn't shared, returns false.  Otherwise removes "oldName",
     * adds "newName", and returns true iff newName is actually shared.  The new
     * file may or may not have the same index as the original.
     *
     * This assumes that oldName has been deleted & newName exists now.
     * @modifies this 
     */
    public abstract void renameFileIfSharedOrStore(final File oldName, final File newName,
            final FileEventListener callback);

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

    /**
     * Returns true if the given file is shared by the FileManager.
     * The provided file should be in canonical form. 
     */
    public abstract boolean isFileShared(File file);

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
     * @return true if this file is a song purchased from the LWS, false otherwise
     * 
     * This does a simple lookup in the lists of store
     * files and returns true if that file is found in a store list and false otherwise. This
     * method assumes all files have already had there ID3 information examined and placed in
     * one of these lists when appropriate
     */
    public abstract boolean isStoreFileLoaded(File file);

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
     * Returns the QRTable. If the shared files have changed, then it will
     * rebuild the QRT. A copy is returned so that FileManager does not expose
     * its internal data structure.
     */
    public abstract QueryRouteTable getQRT();

    /**
     * Returns an array of all responses matching the given request.  If there
     * are no matches, the array will be empty (zero size).
     *
     * Incomplete Files are NOT returned in responses to queries.
     *
     * Design note: returning an empty array requires no extra allocations,
     * as empty arrays are immutable.
     */
    public abstract Response[] query(QueryRequest request);

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
    public abstract Iterator<Response> getIndexingIterator(final boolean includeXML);
    
    /**
     * Notification that an IncompleteFileDesc has been updated.
     */
    public void fileURNSUpdated(FileDesc ifd);

}