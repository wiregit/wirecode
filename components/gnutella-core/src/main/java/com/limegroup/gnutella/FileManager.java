package com.limegroup.gnutella;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.downloader.VerifyingFile;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.routing.QueryRouteTable;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.FileUtils;
import com.limegroup.gnutella.util.Function;
import com.limegroup.gnutella.util.I18NConvert;
import com.limegroup.gnutella.util.IntSet;
import com.limegroup.gnutella.util.ManagedThread;
import com.limegroup.gnutella.util.StringUtils;
import com.limegroup.gnutella.util.Trie;
import com.limegroup.gnutella.xml.LimeXMLDocument;

/**
 * The list of all shared files.  Provides operations to add and remove
 * individual files, directory, or sets of directories.  Provides a method to
 * efficiently query for files whose names contain certain keywords.<p>
 *
 * This class is thread-safe.
 */
public abstract class FileManager {
	
    private static final Log LOG = LogFactory.getLog(FileManager.class);

    /** The string used by Clip2 reflectors to index hosts. */
    public static final String INDEXING_QUERY = "    ";
    
    /** The string used by LimeWire to browse hosts. */
    public static final String BROWSE_QUERY = "*.*";
    
    /** Subdirectory that is always shared */
    public static final File FORCED_SHARE;
    
    static {
        File forceShare = new File(".", ".NetworkShare").getAbsoluteFile();
        try {
            forceShare = FileUtils.getCanonicalFile(forceShare);
        } catch(IOException ignored) {}
        FORCED_SHARE = forceShare;
    }

    /**********************************************************************
     * LOCKING: obtain this's monitor before modifying this. The exception
     * is _loadThread, which is controlled by _loadThreadLock.
     **********************************************************************/

    /** 
     * The list of complete and incomplete files.  An entry is null if it
     *  is no longer shared.
     * INVARIANT: for all i, _files[i]==null, or _files[i].index==i and either
     *  _files[i]._path is in a shared directory with a shareable extension or
     *  _files[i]._path is the incomplete directory if _files[i] is an IncompleteFileDesc.
     */
    private List /* of FileDesc */ _files;
    
    /**
     * The total size of all complete files, in bytes.
     * INVARIANT: _filesSize=sum of all size of the elements of _files,
     *   except IncompleteFileDescs, whose size may change at any time.
     */
    private long _filesSize;
    
    /**
     * The number of complete files.
     * INVARIANT: _numFiles==number of elements of _files that are not null
     *  and not IncompleteFileDescs.
     */
    private int _numFiles;
    
    /** 
     * The total number of files that are pending sharing.
     *  (ie: awaiting hashing or being added)
     */
    private int _numPendingFiles;
    
    /**
     * The total number of incomplete files.
     * INVARIANT: _numFiles + _numIncompleteFiles == the number of
     *  elements of _files that are not null.
     */
    private int _numIncompleteFiles;
    
    /**
     * The number of files that are forcibly shared over the network.
     * INVARIANT: _numFiles >= _numForcedFiles.
     */
    private int _numForcedFiles;
    
    /**
     * An index that maps a <tt>File</tt> on disk to the 
     *  <tt>FileDesc</tt> holding it.
     *
     * INVARIANT: For all keys k in _fileToFileDescMap, 
     *  _files[_fileToFileDescMap.get(k).getIndex()].getFile().equals(k)
     *
     * Keys must be canonical <tt>File</tt> instances.
     */
    private Map /* of File -> FileDesc */ _fileToFileDescMap;

    /**
     * A trie mapping keywords in complete filenames to the indices in _files.
     * Keywords are the tokens when the filename is tokenized with the
     * characters from DELIMITERS as delimiters.
     * 
     * IncompleteFile keywords are NOT stored.
     * 
     * INVARIANT: For all keys k in _keywordTrie, for all i in the IntSet
     * _keywordTrie.get(k), _files[i]._path.substring(k)!=-1. Likewise for all
     * i, for all k in _files[i]._path where _files[i] is not an
     * IncompleteFileDesc, _keywordTrie.get(k) contains i.
     */
    private Trie /* String -> IntSet  */ _keywordTrie;
    
    /**
     * A map of appropriately case-normalized URN strings to the
     * indices in _files.  Used to make query-by-hash faster.
     * 
     * INVARIANT: for all keys k in _urnMap, for all i in _urnMap.get(k),
     * _files[i].containsUrn(k).  Likewise for all i, for all k in
     * _files[i].getUrns(), _urnMap.get(k) contains i.
     */
    private Map /* URN -> IntSet  */ _urnMap;
    
    /**
     * The set of file extensions to share, sorted by StringComparator. 
     * INVARIANT: all extensions are lower case.
     */
    private static Set /* of String */ _extensions;
    
    /**
     * A mapping whose keys are shared directories and any subdirectories
     * reachable through those directories. The value for any key is the set of
     * indices of all shared files in that directory.
     * 
     * INVARIANT: for any key k with value v in _sharedDirectories, for all i in
     * v, _files[i]._path == k + _files[i]._name.
     * 
     * Likewise, for all j s.t. _files[j] != null and !(_files[j] instanceof
     * IncompleteFileDesc), _sharedDirectories.get( _files[j]._path -
     * _files[j]._name).contains(j).  Here "==" is shorthand for file path
     * comparison and "a-b" is short for string 'a' with suffix 'b' removed.
     * 
     * INVARIANT: all keys in this are canonicalized files, sorted by a
     * FileComparator.
     * 
     * Incomplete shared files are NOT stored in this data structure, but are
     * instead in the _incompletesShared IntSet.
     */
    private Map /* of File -> IntSet */ _sharedDirectories;
    
	/**
	 * A Set of shared directories that are completely shared.  Files in these
	 * directories are shared by default and will be shared unless the File is
	 * listed in SharingSettings.FILES_NOT_TO_SHARE.
	 */
	private Set /* of File */ _completelySharedDirectories;
	
    /**
     * The IntSet for incomplete shared files.
     * 
     * INVARIANT: for all i in _incompletesShared,
     *       _files[i]._path == the incomplete directory.
     *       _files[i] instanceof IncompleteFileDesc
     *  Likewise, for all i s.t.
     *    _files[i] != null and _files[i] instanceof IncompleteFileDesc,
     *       _incompletesShared.contains(i)
     * 
     * This structure is not strictly needed for correctness, but it allows
     * others to retrieve all the incomplete shared files, which is
     * relatively useful.                                                                                                       
     */
    private IntSet _incompletesShared;

    /**
     *  The thread responsible for adding contents of _sharedDirectories to
     *  this, or null if no load has yet been triggered.  This is necessary
     *  because file indexing can be slow.  Interrupt this thread to stop the
     *  loading; it will periodically check its interrupted status. 
     *  LOCKING: obtain _loadThreadLock before modifying and before obtaining
     *  this (to prevent deadlock).
     */
    private Thread _loadThread;
    
    /**
     * The lock for _loadThread.  Necessary to prevent deadlocks in
     * loadSettings.
     */
    private Object _loadThreadLock = new Object();
    
    /**
     *  True if _loadThread.interrupt() was called.  This is needed because
     *  _loadThread.isInterrupted() does not behave as expected.  See
     *  http://developer.java.sun.com/developer/bugParade/bugs/4092438.html
     */
    private boolean _loadThreadInterrupted = false;
    
    /**
     * Whether or not we are finished loading the files.
     */
    private boolean _loadFinished = false;
    
    /**
     * The filter object to use to discern shareable files.
     */
    private final FileFilter SHAREABLE_FILE_FILTER = new FileFilter() {
        public boolean accept(File f) {
            return isFileShareable(f);
        }
    };    
        
    /**
     * The filter object to use to determine directories.
     */
    private static final FileFilter DIRECTORY_FILTER = new FileFilter() {
        public boolean accept(File f) {
            return f.isDirectory();
        }        
    };
         
    /**
     * The QueryRouteTable kept by this.  The QueryRouteTable will be 
     * lazily rebuilt when necessary.
     */
    protected static QueryRouteTable _queryRouteTable;
    
    /**
     * Boolean for checking if the QRT needs to be rebuilt.
     */
    protected static volatile boolean _needRebuild = true;

    /**
     * Characters used to tokenize queries and file names.
     */
    public static final String DELIMITERS = " -._+/*()\\,";
    private static final boolean isDelimiter(char c) {
        switch (c) {
        case ' ':
        case '-':
        case '.':
        case '_':
        case '+':
        case '/':
        case '*':
        case '(':
        case ')':
        case '\\':
        case ',':
            return true;
        default:
            return false;
        }
    }

	/**
	 * Creates a new <tt>FileManager</tt> instance.
	 */
    public FileManager() {
        // We'll initialize all the instance variables so that the FileManager
        // is ready once the constructor completes, even though the
        // thread launched at the end of the constructor will immediately
        // overwrite all these variables
        resetVariables();
    }
    
    /**
     * Method that resets all of the variables for this class, maintaining
     * all invariants.  This is necessary, for example, when the shared
     * files are reloaded.
     */
    private void resetVariables()  {
        _filesSize = 0;
        _numFiles = 0;
        _numIncompleteFiles = 0;
        _numPendingFiles = 0;
        _numForcedFiles = 0;
        _files = new ArrayList();
        _keywordTrie = new Trie(true);  //ignore case
        _urnMap = new HashMap();
        _extensions = new HashSet();
        _sharedDirectories = new HashMap();
		_completelySharedDirectories = new HashSet();
        _incompletesShared = new IntSet();
        _fileToFileDescMap = new HashMap();
    }

    /** Asynchronously loads all files by calling loadSettings.  Sets this's
     *  callback to be "callback", and notifies "callback" of all file loads.
     *      @modifies this
     *      @see loadSettings */
    public void start() {
		loadSettings(false);
    }

	
    ///////////////////////////////////////////////////////////////////////////
    //  Accessors
    ///////////////////////////////////////////////////////////////////////////
		
    /**
     * Returns the size of all files, in <b>bytes</b>.  Note that the largest
     *  value that can be returned is Integer.MAX_VALUE, i.e., ~2GB.  If more
     *  bytes are being shared, returns this value.
     */
    public int getSize() {
		return ByteOrder.long2int(_filesSize); 
	}

    /**
     * Returns the number of files.
     * This number does NOT include incomplete files or forcibly shared network files.
     */
    public int getNumFiles() {
		return _numFiles - _numForcedFiles;
	}
    
    /**
     * Returns the number of shared incomplete files.
     */
    public int getNumIncompleteFiles() {
        return _numIncompleteFiles;
    }
    
    /**
     * Returns the number of pending files.
     */
    public int getNumPendingFiles() {
        return _numPendingFiles;
    }
    
    /**
     * Returns the number of forcibly shared files.
     */
    public int getNumForcedFiles() {
        return _numForcedFiles;
    }

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
    public synchronized FileDesc get(int i) {
        return (FileDesc)_files.get(i);
    }

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
    public synchronized boolean isValidIndex(int i) {
        return (i >= 0 && i < _files.size());
    }


    /**
     * Returns the <tt>URN<tt> for the File.  May return null;
     */    
    public synchronized URN getURNForFile(File f) {
        FileDesc fd = getFileDescForFile(f);
        if (fd != null) return fd.getSHA1Urn();
        return null;
    }


    /**
     * Returns the <tt>FileDesc</tt> that is wrapping this <tt>File</tt>
     * or null if the file is not shared.
     */
    public synchronized FileDesc getFileDescForFile(File f) {
        try {
            f = FileUtils.getCanonicalFile(f);
        } catch(IOException ioe) {
            return null;
        }

        return (FileDesc)_fileToFileDescMap.get(f);
    }
    
    /**
     * Determines whether or not the specified URN is shared in the library
     * as a complete file.
     */
    public synchronized boolean isUrnShared(final URN urn) {
        FileDesc fd = getFileDescForUrn(urn);
        return fd != null && !(fd instanceof IncompleteFileDesc);
    }

	/**
	 * Returns the <tt>FileDesc</tt> for the specified URN.  This only returns 
	 * one <tt>FileDesc</tt>, even though multiple indices are possible with 
	 * HUGE v. 0.93.
	 *
	 * @param urn the urn for the file
	 * @return the <tt>FileDesc</tt> corresponding to the requested urn, or
	 *  <tt>null</tt> if no matching <tt>FileDesc</tt> could be found
	 */
	public synchronized FileDesc getFileDescForUrn(final URN urn) {
		IntSet indices = (IntSet)_urnMap.get(urn);
		if(indices == null) return null;

		IntSet.IntSetIterator iter = indices.iterator();
		
        //Pick the first non-null non-Incomplete FileDesc.
        FileDesc ret = null;
		while ( iter.hasNext() 
               && ( ret == null || ret instanceof IncompleteFileDesc) ) {
			int index = iter.next();
            ret = (FileDesc)_files.get(index);
		}
        return ret;
	}
	
	/**
	 * Returns a list of all shared incomplete file descriptors.
	 */
	public synchronized FileDesc[] getIncompleteFileDescriptors() {
        if (_incompletesShared == null)
            return null;
        
        FileDesc[] ret = new FileDesc[_incompletesShared.size()];
        IntSet.IntSetIterator iter = _incompletesShared.iterator();
        for (int i = 0; iter.hasNext(); i++) {
            FileDesc fd = (FileDesc)_files.get(iter.next());
            Assert.that(fd != null, "Directory has null entry");
            ret[i]=fd;
        }
        
        return ret;
    }
    
    /**
     * Returns an array of all shared file descriptors.
     */
    public synchronized FileDesc[] getAllSharedFileDescriptors() {
        // Instead of using _files.toArray, use
        // _fileToFileDescMap.values().toArray.  This is because
        // _files will still contain null values for removed
        // shared files, but _fileToFileDescMap will not.
        FileDesc[] fds = new FileDesc[_fileToFileDescMap.size()];        
        fds = (FileDesc[])_fileToFileDescMap.values().toArray(fds);
        return fds;
    }

    /**
     * Returns a list of all shared file descriptors in the given directory,
     * in any order.
     * Returns null if directory is not shared, or a zero-length array if it is
     * shared but contains no files.  This method is not recursive; files in 
     * any of the directory's children are not returned.
     */    
    public synchronized FileDesc[] getSharedFileDescriptors(File directory) {
        if (directory == null)
            throw new NullPointerException("null directory");
        
        // a. Remove case, trailing separators, etc.
        try {
            directory = FileUtils.getCanonicalFile(directory);
        } catch (IOException e) { // invalid directory ?
            return new FileDesc[0];
        }
        
        //Lookup indices of files in the given directory...
        IntSet indices = (IntSet)_sharedDirectories.get(directory);
        if (indices == null)  // directory not shared.
			return new FileDesc[0];
		
        FileDesc[] fds = new FileDesc[indices.size()];
        IntSet.IntSetIterator iter = indices.iterator();
        for (int i = 0; iter.hasNext(); i++) {
            FileDesc fd = (FileDesc)_files.get(iter.next());
            Assert.that(fd != null, "Directory has null entry");
            fds[i] = fd;
        }
        
        return fds;
    }


    ///////////////////////////////////////////////////////////////////////////
    //  Loading 
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Ensures this contains exactly the files specified by the
     * EXTENSIONS_TO_SHARE and DIRECTORIES_TO_SHARE properties.
     * That is, clears this and loads all files with the given extensions in the
     * given directories <i>and their children</i>.  Note that some files in
     * this before the call will not be in this after the call, or they may have
     * a different index.  If DIRECTORIES_TO_SHARE contains duplicate
     * directories, the duplicates will be ignored.  If it contains files, they
     * will be ignored.<p>
	 *
     * This method is thread-safe but non-blocking.  When the method returns,
     * the directory and extension settings used by addFile() are
     * initialized.  However, files will actually be indexed asynchronously in
     * another thread.  This is useful because indexing may take up to 30
     * seconds or so if sharing many files.  If loadSettings is subsequently
     * called before the indexing is complete, the original settings are
     * discarded, and loading starts over immediately.
     *
     * Modification 8/01 - This method is still non-blocking and thread safe,
     * but it was refactored to make for easier subclassing of FileManager.
     * Now, a protected method called loadSettingsBlocking() is used to index
     * the files asynchronously.  Subclasses can override or extend this method
     * to impose their own functionality.  For example, see MetaFileManager.
     *
     * @modifies this 
	 * @param notifyOnClear if true, callback is notified via clearSharedFiles
     *  when the previous load settings thread has been killed.
     */
    public void loadSettings(boolean notifyOnClear) {		
        synchronized (_loadThreadLock) {
            //If settings are already being loaded, interrupt and wait for them
            //to complete.  Note that we cannot hold this' monitor when calling
            //join(); doing so may result in deadlock!  On the other hand, we
            //have to hold _loadThreadLock's monitor to prevent a load thread
            //from starting up immediately after checking for null.
            //
            //TODO: the call to join would block if the call to File.list called
            //by listFiles called by updateSharedDirectories blocks.  If this is the 
            //case, we need to spawn a thread before join'ing.
            if (_loadThread!=null) {
                _loadThreadInterrupted = true;
                _loadThread.interrupt();
                try {
                    _loadThread.join();
                } catch (InterruptedException e) {
                    return;
                }
            }

            final boolean notifyOnClearFinal = notifyOnClear;
            _loadThreadInterrupted = false;
            _loadFinished = false;            
            _loadThread = new ManagedThread("FileManager.loadSettingsBlocking") {
                public void managedRun() {
					try {
						loadSettingsBlocking(notifyOnClearFinal);
						RouterService.getCallback().fileManagerLoaded();
					} catch(Throwable t) {
						ErrorService.error(t);
					}
					_loadFinished = true;
					// make sure SavedFileManager starts its processing ASAP
					SavedFileManager.instance().run();
                }
            };
            _loadThread.start();
        } 
    }

    /** Returns true if the load thread has been interrupted and this should stop
     *  loading files. */
    protected boolean loadThreadInterrupted() {
        return _loadThreadInterrupted;
    }
    
    /**
     * Returns whether or not the loading is finished.
     */
    public boolean isLoadFinished() {
        return _loadFinished;
    }

    /** Clears and reloads this's extensions, generates an array of
     *  directories, and then indexes the generated directories files.
     *  NOTE TO SUBCLASSES: extend this method as needed, it shall be
     *  threaded and run asynchronously as to not slow down the main
     *  thread (the GUI).
     *  @modifies this */
    protected void loadSettingsBlocking(boolean notifyOnClear) {
        final File[] directories = new File[SharingSettings.getSharedDirectoriesCount() + 1];
        synchronized (this) {
            // Reset the file list info
            resetVariables();

            // Load the extensions.
            String[] extensions = 
                StringUtils.split(
                    SharingSettings.EXTENSIONS_TO_SHARE.getValue(), ";");
            
            for(int i = 0; i < extensions.length && !loadThreadInterrupted(); i++)
                _extensions.add(extensions[i].toLowerCase());

            //Ideally we'd like to ensure that "C:\dir\" is loaded BEFORE
            //C:\dir\subdir.  Although this isn't needed for correctness, it may
            //help the GUI show "subdir" as a subdirectory of "dir".  One way of
            //doing this is to do a full topological sort, but that's a lot of 
            //work. So we just approximate this by sorting by filename length, 
            //from smallest to largest.  Unless directories are specified as
            //"C:\dir\..\dir\..\dir", this will do the right thing.
            
            // add 'NetworkShare' as an always shared subdirectory.
            File[] tmpDirs = SharingSettings.getSharedDirectories();
            System.arraycopy(tmpDirs, 0, directories, 0, tmpDirs.length);
            directories[tmpDirs.length] = FORCED_SHARE;

            Arrays.sort(directories, new Comparator() {
                public int compare(Object a, Object b) {
                    return (a.toString()).length()-(b.toString()).length();
                }
            });
        }

        //clear this, list of directories retrieved
        if (notifyOnClear) 
            RouterService.getCallback().clearSharedFiles();
        
        //Load the shared directories and their files.
        //Duplicates in the directories list will be ignored.  Note that the
        //runner thread only obtain this's monitor when adding individual
        //files.
        {
            Set added = new LinkedHashSet();

            // Add contents of each directory as long as we're not interrupted.
            for(int i = 0; i < directories.length && !loadThreadInterrupted(); i++) {
                added.addAll(updateSharedDirectories(directories[i], null));
            }

            // Add specially shared files as long as we're not interrupted, 
            // and update the number of pending files.
            File[] specialFiles = SharingSettings.SPECIAL_FILES_TO_SHARE.getValue();
            int preSpecialSize = added.size();
            added.addAll(Arrays.asList(specialFiles));
            _numPendingFiles += (added.size() - preSpecialSize);
            
            // Add the files that were just marked as being shareable.
            if (!loadThreadInterrupted()) {
				Iterator it = added.iterator();
				while (it.hasNext() && !loadThreadInterrupted()) {
					synchronized (this) { _numPendingFiles--; }
					addFileIfShared((File)it.next(), true);
				}
			}
            
            // Compact the index once.  As an optimization, we skip this
            // if loadSettings has subsequently been called.
            if (!loadThreadInterrupted())
                trim();                    
        }
        
        // Tell the download manager to notify us of incomplete files.
		if (!loadThreadInterrupted())
		    RouterService.getDownloadManager().getIncompleteFileManager().
		        registerAllIncompleteFiles();

        // prune away old creation times that may still exist
        CreationTimeCache.instance().pruneTimes();
		    
		// write out the cache of URNs and creation times
		UrnCache.instance().persistCache();
        CreationTimeCache.instance().persistCache();
    }
    
    /**
     * Recursively adds this directory and all subdirectories to the shared
     * directories and updates the number of pending shared files.  Does nothing
     * if <tt>directory</tt> doesn't exist, isn't a directory, or has already
     * been added.  This method is thread-safe.  It acquires locks on a
     * per-directory basis.  If the _loadThread is interrupted while scanning
     * the contents of <tt>directory</tt>, it returns immediately.
     * 
     * @requires directory is part of DIRECTORIES_TO_SHARE or one of its
     *           children, and parent is directory's shared parent or null if
     *           directory's parent is not shared.
     * @modifies this
     * @return A Set of Files to add.
     */
    private Set updateSharedDirectories(File directory, File parent) {
        //We have to get the canonical path to make sure "D:\dir" and "d:\DIR"
        //are the same on Windows but different on Unix.
        try {
            directory = FileUtils.getCanonicalFile(directory);
        } catch (IOException e) {
            return Collections.EMPTY_SET;  //doesn't exist?
        }
        
        // STEP 0:
		// Do not share certain the incomplete directory, directories on the
		// do not share list, or sensitive directories.
        if (directory.equals(SharingSettings.INCOMPLETE_DIRECTORY.getValue()))
            return Collections.EMPTY_SET;

		// Do not share directories on the do not share list
		if (SharingSettings.DIRECTORIES_NOT_TO_SHARE.contains(directory))
			return Collections.EMPTY_SET;
        
        // Do not share sensitive directories
        if (isSensitiveDirectory(directory)) {
            //  go through directories that explicitly should not be shared
            if (SharingSettings.SENSITIVE_DIRECTORIES_NOT_TO_SHARE.contains(directory))
                return Collections.EMPTY_SET;
            
            //  go through directories that explicitly should be shared
            if (!SharingSettings.SENSITIVE_DIRECTORIES_TO_SHARE.contains(directory)) {
                //  ask the user whether the sensitive directory should be shared
                if (!RouterService.getCallback().warnAboutSharingSensitiveDirectory(directory))
                    return Collections.EMPTY_SET;
            }
        }
		
        // STEP 1:
        // Add directory
        boolean isForcedShare = directory.equals(FORCED_SHARE);
        synchronized (this) {
            // if it was already added, ignore.
            if (_completelySharedDirectories.contains(directory))
                return Collections.EMPTY_SET;
                
			_completelySharedDirectories.add(directory);
			_sharedDirectories.put(directory, new IntSet());
            if (!isForcedShare)
                RouterService.getCallback().addSharedDirectory(directory, parent);
        }
		
        // STEP 2:
        // Scan subdirectory for the amount of shared files.
        File[] dir_list = directory.listFiles(DIRECTORY_FILTER);
        File[] file_list = directory.listFiles(SHAREABLE_FILE_FILTER);
        
        // no shared files or subdirs
        if (dir_list == null && file_list == null)
            return Collections.EMPTY_SET;

		synchronized (this) {
            _numPendingFiles += file_list.length;
        }
        Set added = new LinkedHashSet();
        added.addAll(Arrays.asList(file_list));
        
        // STEP 3:
        // Recursively add subdirectories.
        // This has the effect of ensuring that the number of pending files
        // is closer to correct number.
        // Do not share subdirectories if this directory is on the
        // directories-to-share-non-recursively list.
        if (SharingSettings.DIRECTORIES_TO_SHARE_NON_RECURSIVELY.contains(directory)) 
            return added;
        
        // Do not share subdirectories of the forcibly shared dir.
        if (!isForcedShare) { 
            for (int i = 0; i < dir_list.length && !loadThreadInterrupted(); i++) {
                added.addAll(updateSharedDirectories(dir_list[i], directory));
            }
        }
        
        return added;
    }


	///////////////////////////////////////////////////////////////////////////
	//  Adding and removing shared files and directories
	///////////////////////////////////////////////////////////////////////////
	
    /**
	 * Adds the given file to this, even if it exists outside of what is
	 * currently accepted to be shared.
	 * <p>
	 * Too large files are still not shareable this way.
	 */
	public FileDesc addFileAlways(File file, List list) {
		SharingSettings.SPECIAL_FILES_NOT_TO_SHARE.remove(file);
		if (!isFileShareable(file))
			SharingSettings.SPECIAL_FILES_TO_SHARE.add(file);
		return addFileIfShared(file, list);
	}
    
	/**
	 * Convenience wrapper for {@link #addFileAlways(File, List) addFileAlways
	 * @param file
	 * @return
	 */
	public FileDesc addFileAlways(File file) {
		return addFileAlways(file, Collections.EMPTY_LIST);
	}
	
    /**
     * @modifies this
     * @effects adds the given file to list of shared files,
     * if it is shareable, see {@link #isFileShareable(File)}.
     * 
     * @return the <tt>FileDesc</tt> for the new file if it was successfully
     *         added, otherwise <tt>null</tt>
     */
   public FileDesc addFileIfShared(File file) {
       return addFileIfShared(file, true);
   }
	
    /**
     * The actual implementation of addFileIfShared(File)
     * @param file the file to add
     * @param notify if true signals the front-end via 
     * ActivityCallback.handleFileManagerEvent() about
     * the Event
     * @return Returns FileDesc or null
     */
    protected FileDesc addFileIfShared(File file, boolean notify) {
        
        // Make sure capitals are resolved properly, etc.
        File f = null;
        try {
            f = FileUtils.getCanonicalFile(file);
            if (!f.exists())
				return null;
        } catch (IOException e) {
            return null;
	    }

        //TODO: if overwriting an existing, take special care.
		if (!isFileShareable(f))
			return null;
		
		synchronized (this) { _numPendingFiles++; }
        FileDesc fd = addFile(f);
        synchronized(this) { _numPendingFiles--; }
        _needRebuild = true;
        
        // Notify the GUI...
        if (notify && fd != null) 
            RouterService.getCallback().handleFileManagerEvent(
					new FileManagerEvent(this, FileManagerEvent.ADD, new FileDesc[] { fd }));
        
        return fd;
    }

    /**
     * @modifies this
     * @effects calls addFileIfShared(file), then optionally stores any metadata
     *  in the given XML documents.  metadata may be null if there is no data.
     *  Returns the value from addFileIfShared. <b>WARNING: this is a potential
     *  security hazard.</b> 
     *
     * @return the <tt>FileDesc</tt> for the new file if it was successfully 
     *  added, otherwise <tt>null</tt>
     */
    public FileDesc addFileIfShared(File file, List metadata) {
        return addFileIfShared(file, metadata, true);
    }
    
    /**
     * The actual implemenation of addFileIfShared(File file, List metadata)
     */
    protected abstract FileDesc addFileIfShared(File file, List metadata, boolean notify);
  
    /**
     * @requires the given file exists and is in a shared directory
     * @modifies this
     * @effects adds the given file to this if it is of the proper extension and
     *  not too big (>~2GB).  Returns true iff the file was actually added.
     *
     * @return the <tt>FileDesc</tt> for the new file if it was successfully 
     *  added, otherwise <tt>null</tt>
     */
    private FileDesc addFile(File file) {
        repOk();

        if (!isFileShareable(file))
			return null;
            
		//Calculate hash OUTSIDE of lock.
        Set urns = null;
        try {
            urns = FileDesc.calculateAndCacheURN(file);  
        } catch(IOException e) {
            // there was an IO error calculating the hash, so we can't
            // add the file
            return null;
        } catch(InterruptedException e) {
            // the hash calculation was interrupted, so we can't add
            // the file -- should get reloaded
            return null;
        }
        if (loadThreadInterrupted() || urns.size() == 0) {
            // the URN was not calculated correctly for some reason
            return null;
        }

		long fileLength = file.length();
        synchronized (this) {
            _filesSize += fileLength;
            int fileIndex = _files.size();
            FileDesc fileDesc = new FileDesc(file, urns, fileIndex);
            _files.add(fileDesc);
            _fileToFileDescMap.put(file, fileDesc);
            _numFiles++;
		
            //Register this file with its parent directory.
            File parent = FileUtils.getParentFile(file);
            Assert.that(parent != null, "Null parent to \""+file+"\"");
	        
            // Check if file is a specially shared file.  If not, ensure that
            // it is located in a shared directory.
			IntSet siblings = (IntSet)_sharedDirectories.get(parent);
			if (siblings == null) {
				siblings = new IntSet();
				_sharedDirectories.put(parent, siblings);
			}
			boolean added = siblings.add(fileIndex);
            Assert.that(added, "File "+fileIndex+" already found in "+siblings);
            
            // files that are forcibly shared over the network
            // aren't counted or shown.
            if (!parent.equals(FORCED_SHARE))
                RouterService.getCallback().addSharedFile(fileDesc, parent);
            else
                _numForcedFiles++;
		
            //Index the filename.  For each keyword...
            String[] keywords = extractKeywords(fileDesc);
            
            for (int i = 0; i < keywords.length; i++) {
                String keyword = keywords[i];
                //Ensure the _keywordTrie has a set of indices associated with keyword.
                IntSet indices = (IntSet)_keywordTrie.get(keyword);
                if (indices == null) {
                    indices = new IntSet();
                    _keywordTrie.add(keyword, indices);
                }
                //Add fileIndex to the set.
                indices.add(fileIndex);
            }
		
            // Commit the time in the CreactionTimeCache, but don't share
            // the installer.  We populate free LimeWire's with free installers
            // so we have to make sure we don't influence the what is new
            // result set.
            if (!isForcedShare(file)) {
                URN mainURN = fileDesc.getSHA1Urn();
                CreationTimeCache ctCache = CreationTimeCache.instance();
                synchronized (ctCache) {
                    Long cTime = ctCache.getCreationTime(mainURN);
                    if (cTime == null)
                        cTime = new Long(file.lastModified());
                    // if cTime is non-null but 0, then the IO subsystem is
                    // letting us know that the file was FNF or an IOException
                    // occurred - the best course of action is to
                    // ignore the issue and not add it to the CTC, hopefully
                    // we'll get a correct reading the next time around...
                    if (cTime.longValue() > 0) {
                        // these calls may be superfluous but are quite fast....
                        ctCache.addTime(mainURN, cTime.longValue());
                        ctCache.commitTime(mainURN);
                    }
                }
            }

            // Ensure file can be found by URN lookups
            this.updateUrnIndex(fileDesc);
            _needRebuild = true;            
            repOk();
            return fileDesc;
        }
    }

	/**
	 * Removes the file if it is being shared, and then removes the file from
	 * the special lists as necessary.
	 */
	public synchronized void stopSharingFile(File file) {
		if (file == null)
			return;
		FileDesc fd = removeFileIfShared(file);
		if (fd == null)
			return;

		//  Remove from setting if individually shared, otherwise
		//  add to files not to share
		file = fd.getFile();
		if (file == null)
			return;
		if (SharingSettings.SPECIAL_FILES_TO_SHARE.contains(file))
			SharingSettings.SPECIAL_FILES_TO_SHARE.remove(file);
		else
			if (!SharingSettings.SPECIAL_FILES_NOT_TO_SHARE.contains(file))
				SharingSettings.SPECIAL_FILES_NOT_TO_SHARE.add(file);
	}
	
	/**
     * @modifies this
     * @effects ensures the first instance of the given file is not
     *  shared.  Returns FileDesc iff the file was removed.  
     *  In this case, the file's index will not be assigned to any 
     *  other files.  Note that the file is not actually removed from
     *  disk.
     */
    public synchronized FileDesc removeFileIfShared(File f) {
        return removeFileIfShared(f, true);
    }
    
    /**
     * The actual implementation of removeFileIfShared(File)
     */
    protected synchronized FileDesc removeFileIfShared(File f, boolean notify) {
        repOk();
        
        //Take care of case, etc.
        try {
            f = FileUtils.getCanonicalFile(f);
        } catch (IOException e) {
            repOk();
            return null;
        }        

		// Look for matching file ...         
        FileDesc fd = (FileDesc)_fileToFileDescMap.get(f);
        if (fd == null)
            return null;

        int i = fd.getIndex();
        Assert.that(((FileDesc)_files.get(i)).getFile().equals(f),
                    "invariant broken!");
        
        _files.set(i, null);
        _fileToFileDescMap.remove(f);
        _needRebuild = true;

        // If it's an incomplete file, the only reference we 
        // have is the URN, so remove that and be done.
        // We also return false, because the file was never really
        // "shared" to begin with.
        if (fd instanceof IncompleteFileDesc) {
            this.removeUrnIndex(fd);
            _numIncompleteFiles--;
            boolean removed = _incompletesShared.remove(i);
            Assert.that(removed,
                "File "+i+" not found in " + _incompletesShared);
            repOk();

			// Notify the GUI...
	        if (notify) {
	            FileManagerEvent evt = new FileManagerEvent(this, 
	                                            FileManagerEvent.REMOVE, 
	                                            new FileDesc[] { fd });
	                                            
	            RouterService.getCallback().handleFileManagerEvent(evt);
	        }
            return fd;
        }

        _numFiles--;
        _filesSize -= fd.getSize();

        //Remove references to this from directory listing
        File parent = FileUtils.getParentFile(f);
        IntSet siblings = (IntSet)_sharedDirectories.get(parent);
        Assert.that(siblings != null,
            "Removed file's directory \""+parent+"\" not in "+_sharedDirectories);
        boolean removed = siblings.remove(i);
        Assert.that(removed, "File "+i+" not found in "+siblings);

        // files that are forcibly shared over the network aren't counted        
        if (parent.equals(FORCED_SHARE)) {
            notify = false;
            _numForcedFiles--;
        }

        //Remove references to this from index.
        String[] keywords = extractKeywords(fd);
        for (int j = 0; j < keywords.length; j++) {
            String keyword = keywords[j];
            IntSet indices = (IntSet)_keywordTrie.get(keyword);
            if (indices != null) {
                indices.remove(i);
                //TODO2: prune tree if possible.  call
                //_keywordTrie.remove(keyword) if indices.size()==0.
            }
        }

        //Remove hash information.
        this.removeUrnIndex(fd);
        //Remove creation time information
        if (_urnMap.get(fd.getSHA1Urn()) == null)
            CreationTimeCache.instance().removeTime(fd.getSHA1Urn());
  
        repOk();
        
        // Notify the GUI...
        if (notify) {
            FileManagerEvent evt = new FileManagerEvent(this, 
                                            FileManagerEvent.REMOVE, 
                                            new FileDesc[] { fd });
                                            
            RouterService.getCallback().handleFileManagerEvent(evt);
        }
        
        return fd;
    }
    
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
    public synchronized void addIncompleteFile(File incompleteFile,
                                               Set urns,
                                               String name,
                                               int size,
                                               VerifyingFile vf) {
        try {
            incompleteFile = FileUtils.getCanonicalFile(incompleteFile);
        } catch(IOException ioe) {
            //invalid file?... don't add incomplete file.
            return;
        }

        // We want to ensure that incomplete files are never added twice.
        // This may happen if IncompleteFileManager is deserialized before
        // FileManager finishes loading ...
        // So, every time an incomplete file is added, we check to see if
        // it already was... and if so, ignore it.
        // This is somewhat expensive, but it is called very rarely, so it's ok
		Iterator iter = urns.iterator();
		while (iter.hasNext()) {
            // if there were indices for this URN, exit.
            IntSet shared = (IntSet)_urnMap.get(iter.next());
            // nothing was shared for this URN, look at another
            if (shared == null)
                continue;
                
            for (IntSet.IntSetIterator isIter = shared.iterator(); isIter.hasNext(); ) {
                int i = isIter.next();
                FileDesc desc = (FileDesc)_files.get(i);
                // unshared, keep looking.
                if (desc == null)
                    continue;
                String incPath = incompleteFile.getAbsolutePath();
                String path  = desc.getFile().getAbsolutePath();
                // the files are the same, exit.
                if (incPath.equals(path))
                    return;
            }
        }
        
        // no indices were found for any URN associated with this
        // IncompleteFileDesc... add it.
        int fileIndex = _files.size();
        _incompletesShared.add(fileIndex);
        IncompleteFileDesc ifd = new IncompleteFileDesc(
            incompleteFile, urns, fileIndex, name, size, vf);            
        _files.add(ifd);
        _fileToFileDescMap.put(incompleteFile, ifd);
        this.updateUrnIndex(ifd);
        _numIncompleteFiles++;
        _needRebuild = true;
        File parent = FileUtils.getParentFile(incompleteFile);
        RouterService.getCallback().addSharedFile(ifd, parent);
    }

    /**
     * Notification that a file has changed and new hashes should be
     * calculated.
     * 
     * @return the new <tt>FileDesc</tt> for the file if it was successfully 
     *  changed, otherwise <tt>null</tt>
     */
    public FileDesc fileChanged(File f) {
        URN oldURN = getURNForFile(f);
        CreationTimeCache ctCache = CreationTimeCache.instance();
        Long cTime = ctCache.getCreationTime(oldURN);
        FileDesc removed = removeFileIfShared(f, false);
        if (removed == null) // nothing removed, exit.
            return null;
        FileDesc fd = addFileIfShared(f, false);
        //re-populate the ctCache
        if ((fd != null) && (cTime != null)) { 
            synchronized (ctCache) {
                ctCache.removeTime(fd.getSHA1Urn()); //createFileDesc() put lastModified
                ctCache.addTime(fd.getSHA1Urn(), cTime.longValue());
                ctCache.commitTime(fd.getSHA1Urn());
            }
        }
        
        // Notify the GUI about the changes...
        FileManagerEvent evt = null;
        
        if (fd != null) {
            evt = new FileManagerEvent(this, 
                                       FileManagerEvent.CHANGE, 
                                       new FileDesc[] { removed, fd }); 
        } else {
            evt = new FileManagerEvent(this, 
                                       FileManagerEvent.REMOVE, 
                                       new FileDesc[] { removed });
        }
        
        RouterService.getCallback().handleFileManagerEvent(evt);
        
        return fd;
    }
    

    ///////////////////////////////////////////////////////////////////////////
    //  Search, etc.
    ///////////////////////////////////////////////////////////////////////////
		
    /**
     * @modifies this
     * @effects enters the given FileDesc into the _urnMap under all its 
     * reported URNs
     */
    private synchronized void updateUrnIndex(FileDesc fileDesc) {
		Iterator iter = fileDesc.getUrns().iterator();
		while (iter.hasNext()) {
			URN urn = (URN)iter.next();
			IntSet indices=(IntSet)_urnMap.get(urn);
			if (indices==null) {
				indices=new IntSet();
				_urnMap.put(urn, indices);
			}
			indices.add(fileDesc.getIndex());
		}
    }
    
    /**
     * Utility method to perform standardized keyword extraction for the given
     * <tt>FileDesc</tt>.  This handles extracting keywords according to 
     * locale-specific rules.
     * 
     * @param fd the <tt>FileDesc</tt> containing a file system path with 
     *  keywords to extact
     * @return an array of keyword strings for the given file
     */
    private static String[] extractKeywords(FileDesc fd) {
        return StringUtils.split(I18NConvert.instance().getNorm(fd.getPath()), 
            DELIMITERS);
    }

    /** Removes any URN index information for desc */
    private synchronized void removeUrnIndex(FileDesc fileDesc) {
		Iterator iter = fileDesc.getUrns().iterator();
		while (iter.hasNext()) {
            //Lookup each of desc's URN's ind _urnMap.  
            //(It better be there!)
			URN urn = (URN)iter.next();
            IntSet indices=(IntSet)_urnMap.get(urn);
            Assert.that(indices!=null, "Invariant broken");

            //Delete index from set.  Remove set if empty.
            indices.remove(fileDesc.getIndex());
            if (indices.size()==0)
                _urnMap.remove(urn);
		}
    }

    /** 
     * If oldName isn't shared, returns false.  Otherwise removes "oldName",
     * adds "newName", and returns true iff newName is actually shared.  The new
     * file may or may not have the same index as the original.
     * Note that this does not change the disk.
     * @modifies this 
     */
    public synchronized boolean renameFileIfShared(File oldName,
                                                   File newName) {
        FileDesc removed = getFileDescForFile(oldName);
        if (removed == null)
            return false;
        List xmlDocs = new LinkedList();
        xmlDocs.addAll(removed.getLimeXMLDocuments());
		if (SharingSettings.SPECIAL_FILES_TO_SHARE.contains(oldName) &&
			!isFileInCompletelySharedDirectory(newName))
			SharingSettings.SPECIAL_FILES_TO_SHARE.add(newName);
		removed = removeFileIfShared(oldName, false);
		SharingSettings.SPECIAL_FILES_NOT_TO_SHARE.remove(oldName);
			
        Assert.that(removed != null, "invariant broken.");
        // hash didn't change so no need to re-input creation time
        FileDesc fd = addFileIfShared(newName, xmlDocs, false);
        
        // Notify the GUI...
        if (fd != null) {
            FileManagerEvent evt = new FileManagerEvent(this, 
                                            FileManagerEvent.RENAME, 
                                            new FileDesc[]{removed,fd});
                                            
            RouterService.getCallback().handleFileManagerEvent(evt);
        }
        
        return (fd != null);
    }


    /** Ensures that this's index takes the minimum amount of space.  Only
     *  affects performance, not correctness; hence no modifies clause. */
    private synchronized void trim() {
        _keywordTrie.trim(new Function() {
            public Object apply(Object intSet) {
                ((IntSet)intSet).trim();
                return intSet;
            }
        });
    }

	/**
	 * Returns true if the given file is shared by the FileManager. 
	 */
	public boolean isFileShared(File file) {
		if (file == null)
			return false;
		if (_fileToFileDescMap.get(file) == null)
			return false;
		return true;
	}
	
    /** Returns true if file has a shareable extension.  Case is ignored. */
    private static boolean hasShareableExtension(File file) {
        if(file == null) return false;
        String filename = file.getName();
        int begin = filename.lastIndexOf(".");
        if (begin == -1)
            return false;

        String ext = filename.substring(begin + 1).toLowerCase();
        return _extensions.contains(ext);
    }
    
    /**
     * Returns true if this file is in a directory that is completely shared.
     */
    public boolean isFileInCompletelySharedDirectory(File f) {
        File dir = FileUtils.getParentFile(f);
        if (dir == null) 
            return false;

		synchronized (this) {
			return _completelySharedDirectories.contains(dir);
		}
	}
	
	/**
	 * Returns true if this dir is completely shared. 
	 */
	public boolean isCompletelySharedDirectory(File dir) {
		if (dir == null)
			return false;
		
		synchronized (this) {
			return _completelySharedDirectories.contains(dir);
		}
	}

	/**
	 * Returns true if the given file is in a completely shared directory
	 * or if it is specially shared.
	 */
	private boolean isFileShareable(File file) {
		if (!isFilePhysicallyShareable(file))
			return false;
		if (SharingSettings.SPECIAL_FILES_TO_SHARE.contains(file))
			return true;
		if (SharingSettings.SPECIAL_FILES_NOT_TO_SHARE.contains(file))
			return false;
		if (isFileInCompletelySharedDirectory(file)) {
	        if (file.getName().toUpperCase().startsWith("LIMEWIRE"))
				return true;
			if (!hasShareableExtension(file))
	        	return false;
			return true;
		}
			
		return false;
	}
	
    /**
     * Returns true if this file is not too large, not too small,
     * not null, is a directory, can be read, is not hidden.  Returns
     * true if file is a specially shared file or starts with "LimeWire".
     * Returns false otherwise.
     * @see isFileShareable(File) 
     */
    public static boolean isFilePhysicallyShareable(File file) {
		if (file == null || file.isDirectory() || !file.canRead() || file.isHidden() ) 
            return false;
                
		long fileLength = file.length();
		if (fileLength > Integer.MAX_VALUE || fileLength <= 0) 
        	return false;
        
		return true;
    }
    
    /**
     * Returns true iff <tt>file</tt> is a sensitive directory.
     */
    public static boolean isSensitiveDirectory(File file) {
        if (file == null)
            return false;
        
        //  check for system roots
        File[] faRoots = File.listRoots();
        if (faRoots != null && faRoots.length > 0) {
            for (int i = 0; i < faRoots.length; i++) {
                if (file.equals(faRoots[i]))
                    return true;
            }
        }
        
        //  check for user home directory
        String userHome = System.getProperty("user.dir");
        if (file.equals(new File(userHome)))
            return true;
        
        //  check for OS-specific directories:
        if (CommonUtils.isWindows()) {
            //  check for "Documents and Settings"
            if (file.getName().equals("Documents and Settings"))
                return true;
            
            //  check for "My Documents"
            if (file.getName().equals("My Documents"))
                return true;
            
            //  check for "Desktop"
            if (file.getName().equals("Desktop"))
                return true;
            
            //  check for "Program Files"
            if (file.getName().equals("Program Files"))
                return true;
            
            //  check for "Windows"
            if (file.getName().equals("Windows"))
                return true;
            
            //  check for "WINNT"
            if (file.getName().equals("WINNT"))
                return true;
        }
        
        if (CommonUtils.isMacOSX()) {
            //  check for /Users
            if (file.getName().equals("Users"))
                return true;
            
            //  check for /System
            if (file.getName().equals("System"))
                return true;
            
            //  check for /System Folder
            if (file.getName().equals("System Folder"))
                return true;
            
            //  check for /Previous Systems
            if (file.getName().equals("Previous Systems"))
                return true;
            
            //  check for /private
            if (file.getName().equals("private"))
                return true;
            
            //  check for /Volumes
            if (file.getName().equals("Volumes"))
                return true;
            
            //  check for /Desktop
            if (file.getName().equals("Desktop"))
                return true;
            
            //  check for /Applications
            if (file.getName().equals("Applications"))
                return true;
            
            //  check for /Applications (Mac OS 9)
            if (file.getName().equals("Applications (Mac OS 9)"))
                return true;
            
            //  check for /Network            
            if (file.getName().equals("Network"))
                return true;
        }
        
        if (CommonUtils.isPOSIX()) {
            //  check for /bin
            if (file.getName().equals("bin"))
                return true;
            
            //  check for /boot
            if (file.getName().equals("boot"))
                return true;
            
            //  check for /dev
            if (file.getName().equals("dev"))
                return true;
            
            //  check for /etc
            if (file.getName().equals("etc"))
                return true;
            
            //  check for /home
            if (file.getName().equals("home"))
                return true;
            
            //  check for /mnt
            if (file.getName().equals("mnt"))
                return true;
            
            //  check for /opt
            if (file.getName().equals("opt"))
                return true;
            
            //  check for /proc
            if (file.getName().equals("proc"))
                return true;
            
            //  check for /root
            if (file.getName().equals("root"))
                return true;
            
            //  check for /sbin
            if (file.getName().equals("sbin"))
                return true;
            
            //  check for /usr
            if (file.getName().equals("usr"))
                return true;
            
            //  check for /var
            if (file.getName().equals("var"))
                return true;
        }
        
        return false;
    }
    
    /**
     * Returns the QRTable.
     * If the shared files have changed, then it will rebuild the QRT.
     * A copy is returned so that FileManager does not expose
     * its internal data structure.
     */
    public synchronized QueryRouteTable getQRT() {
        if(_needRebuild) {
            buildQRT();
            _needRebuild = false;
        }
        
        QueryRouteTable qrt = new QueryRouteTable(_queryRouteTable.getSize());
        qrt.addAll(_queryRouteTable);
        return qrt;
    }

    /**
     * build the qrt.  Subclasses can add other Strings to the
     * QRT by calling buildQRT and then adding directly to the 
     * _queryRouteTable variable. (see xml/MetaFileManager.java)
     */
    protected synchronized void buildQRT() {

        _queryRouteTable = new QueryRouteTable();
        FileDesc[] fds = getAllSharedFileDescriptors();
        for(int i = 0; i < fds.length; i++) {
            if (fds[i] instanceof IncompleteFileDesc)
                continue;
            
            _queryRouteTable.add(fds[i].getPath());
        }
    }

    ////////////////////////////////// Queries ///////////////////////////////

    /**
     * Constant for an empty <tt>Response</tt> array to return when there are
     * no matches.
     */
    private static final Response[] EMPTY_RESPONSES = new Response[0];

    /**
     * Returns an array of all responses matching the given request.  If there
     * are no matches, the array will be empty (zero size).
     *
     * Incomplete Files are NOT returned in responses to queries.
     *
     * Design note: returning an empty array requires no extra allocations,
     * as empty arrays are immutable.
     */
    public synchronized Response[] query(QueryRequest request) {
        String str = request.getQuery();
        boolean includeXML = shouldIncludeXMLInResponse(request);

        //Special case: return up to 3 of your 'youngest' files.
        if (request.isWhatIsNewRequest()) 
            return respondToWhatIsNewRequest(request, includeXML);

        //Special case: return everything for Clip2 indexing query ("    ") and
        //browse queries ("*.*").  If these messages had initial TTLs too high,
        //StandardMessageRouter will clip the number of results sent on the
        //network.  Note that some initial TTLs are filterd by GreedyQuery
        //before they ever reach this point.
        if (str.equals(INDEXING_QUERY) || str.equals(BROWSE_QUERY))
            return respondToIndexingQuery(includeXML);

        //Normal case: query the index to find all matches.  TODO: this
        //sometimes returns more results (>255) than we actually send out.
        //That's wasted work.
        //Trie requires that getPrefixedBy(String, int, int) passes
        //an already case-changed string.  Both search & urnSearch
        //do this kind of match, so we canonicalize the case for them.
        str = _keywordTrie.canonicalCase(str);        
        IntSet matches = search(str, null);
        if(request.getQueryUrns().size() > 0) {
            matches = urnSearch(request.getQueryUrns().iterator(),matches);
        }
        
        if (matches==null) {
            return EMPTY_RESPONSES;
		}

        List responses = new LinkedList();
        final MediaType.Aggregator filter = MediaType.getAggregator(request);
        LimeXMLDocument doc = request.getRichQuery();

        // Iterate through our hit indices to create a list of results.
        for (IntSet.IntSetIterator iter=matches.iterator(); iter.hasNext();) { 
            int i = iter.next();
            FileDesc desc = (FileDesc)_files.get(i);
            if(desc == null)
                Assert.that(false, 
                            "unexpected null in FileManager for query:\n"+
                            request);

            if ((filter != null) && !filter.allow(desc.getName()))
                continue;

            desc.incrementHitCount();
            RouterService.getCallback().handleSharedFileUpdate(desc.getFile());

            Response resp = new Response(desc);
            if(includeXML) {
                addXMLToResponse(resp, desc);
                if(doc != null && resp.getDocument() != null &&
                   !isValidXMLMatch(resp, doc))
                    continue;
            }
            responses.add(resp);
        }
        if (responses.size() == 0)
            return EMPTY_RESPONSES;
        return (Response[])responses.toArray(new Response[responses.size()]);
    }

    /**
     * Responds to a what is new request.
     */
    private Response[] respondToWhatIsNewRequest(QueryRequest request, 
                                                 boolean includeXML) {
        // see if there are any files to send....
        // NOTE: we only request up to 3 urns.  we don't need to worry
        // about partial files because we don't add them to the cache.
        List urnList = CreationTimeCache.instance().getFiles(request, 3);
        if (urnList.size() == 0)
            return EMPTY_RESPONSES;
        
        // get the appropriate responses
        Response[] resps = new Response[urnList.size()];
        for (int i = 0; i < urnList.size(); i++) {
            URN currURN = (URN) urnList.get(i);
            FileDesc desc = getFileDescForUrn(currURN);
            
            // should never happen since we don't add times for IFDs and
            // we clear removed files...
            if ((desc==null) || (desc instanceof IncompleteFileDesc))
                throw new RuntimeException("Bad Rep - No IFDs allowed!");
            
            // Formulate the response
            Response r = new Response(desc);
            if(includeXML)
                addXMLToResponse(r, desc);
            
            // Cache it
            resps[i] = r;
        }
        return resps;
    }

    /** Responds to a Indexing (mostly BrowseHost) query - gets all the shared
     *  files of this client.
     */
    private Response[] respondToIndexingQuery(boolean includeXML) {
        //Special case: if no shared files, return null
        // This works even if incomplete files are shared, because
        // they are added to _numIncompleteFiles and not _numFiles.
        if (_numFiles==0)
            return EMPTY_RESPONSES;
        //Extract responses for all non-null (i.e., not deleted) files.
        //Because we ignore all incomplete files, _numFiles continues
        //to work as the expected size of ret.
        Response[] ret=new Response[_numFiles-_numForcedFiles];
        int j=0;
        for (int i=0; i<_files.size(); i++) {
            FileDesc desc = (FileDesc)_files.get(i);
            // If the file was unshared or is an incomplete file,
            // DO NOT SEND IT.
            if (desc==null || desc instanceof IncompleteFileDesc || isForcedShare(desc)) 
                continue;
        
            Assert.that(j<ret.length,
                        "_numFiles is too small");
            ret[j] = new Response(desc);
            if(includeXML)
                addXMLToResponse(ret[j], desc);
            j++;
        }
        Assert.that(j==ret.length,
                    "_numFiles is too large");
        return ret;
    }

    
    /**
     * A normal FileManager will never include XML.
     * It is expected that MetaFileManager overrides this and returns
     * true in some instances.
     */
    protected abstract boolean shouldIncludeXMLInResponse(QueryRequest qr);
    
    /**
     * This implementation does nothing.
     */
    protected abstract void addXMLToResponse(Response res, FileDesc desc);
    
    /**
     * Determines whether we should include the response based on XML.
     */
    protected abstract boolean isValidXMLMatch(Response res, LimeXMLDocument doc);


    /**
     * Returns a set of indices of files matching q, or null if there are no
     * matches.  Subclasses may override to provide different notions of
     * matching.  The caller of this method must not mutate the returned
     * value.
     */
    protected IntSet search(String query, IntSet priors) {
        //As an optimization, we lazily allocate all sets in case there are no
        //matches.  TODO2: we can avoid allocating sets when getPrefixedBy
        //returns an iterator of one element and there is only one keyword.
        IntSet ret=priors;

        //For each keyword in the query....  (Note that we avoid calling
        //StringUtils.split and take advantage of Trie's offset/limit feature.)
        for (int i=0; i<query.length(); ) {
            if (isDelimiter(query.charAt(i))) {
                i++;
                continue;
            }
            int j;
            for (j=i+1; j<query.length(); j++) {
                if (isDelimiter(query.charAt(j)))
                    break;
            }

            //Search for keyword, i.e., keywords[i...j-1].  
            Iterator /* of IntSet */ iter=
                _keywordTrie.getPrefixedBy(query, i, j);
            if (iter.hasNext()) {
                //Got match.  Union contents of the iterator and store in
                //matches.  As an optimization, if this is the only keyword and
                //there is only one set returned, return that set without 
                //copying.
                IntSet matches=null;
                while (iter.hasNext()) {                
                    IntSet s=(IntSet)iter.next();
                    if (matches==null) {
                        if (i==0 && j==query.length() && !(iter.hasNext()))
                            return s;
                        matches=new IntSet();
                    }
                    matches.addAll(s);
                }

                //Intersect matches with ret.  If ret isn't allocated,
                //initialize to matches.
                if (ret==null)   
                    ret=matches;
                else
                    ret.retainAll(matches);
            } else {
                //No match.  Optimizaton: no matches for keyword => failure
                return null;
            }
            
            //Optimization: no matches after intersect => failure
            if (ret.size()==0)
                return null;        
            i=j;
        }
        if (ret==null || ret.size()==0)
            return null;
        return ret;
    }
    
    /**
     * Find all files with matching full URNs
     */
    private synchronized IntSet urnSearch(Iterator urnsIter,IntSet priors) {
        IntSet ret = priors;
        while(urnsIter.hasNext()) {
            URN urn = (URN)urnsIter.next();
            // TODO (eventually): case-normalize URNs as appropriate
            // for now, though, prevalent practice is same as local: 
            // lowercase "urn:<type>:", uppercase Base32 SHA1
            IntSet hits = (IntSet)_urnMap.get(urn);
            if(hits!=null) {
                // double-check hits to be defensive (not strictly needed)
                IntSet.IntSetIterator iter = hits.iterator();
                while(iter.hasNext()) {
                    FileDesc fd = (FileDesc)_files.get(iter.next());
        		    // If the file is unshared or an incomplete file
        		    // DO NOT SEND IT.
        		    if(fd == null || fd instanceof IncompleteFileDesc)
        			    continue;
                    if(fd.containsUrn(urn)) {
                        // still valid
                        if(ret==null) ret = new IntSet();
                        ret.add(fd.getIndex());
                    } 
                }
            }
        }
        return ret;
    }
    
    /**
     * Determines if this FileDesc is a network share.
     */
    public static boolean isForcedShare(FileDesc desc) {
        return isForcedShare(desc.getFile());
    }
    
    /**
     * Determines if this File is a network share.
     */
    public static boolean isForcedShare(File file) {
        File parent = file.getParentFile();
        return parent != null && parent.equals(FORCED_SHARE);
    }

	
    ///////////////////////////////////// Testing //////////////////////////////

    /** Checks this' rep. invariants.  VERY expensive. */
    private boolean DEBUG = false;
    protected synchronized void repOk() {
        if (!DEBUG)
            return;
        System.err.println("WARNING: running repOk()");

        //Verify index.  Get the set of indices in the _keywordTrie....
        IntSet indices = new IntSet();
        for (Iterator iter = _keywordTrie.getPrefixedBy(""); iter.hasNext(); ) {
            IntSet set = (IntSet)iter.next();
            indices.addAll(set);
        }
        //...and make sure all indices are in _files. 
        //(Note that we don't check filenames; I'm lazy)
        for (IntSet.IntSetIterator iter = indices.iterator(); iter.hasNext(); ) {
            int i = iter.next();
            FileDesc desc = (FileDesc)_files.get(i);
            Assert.that(desc != null,
                        "Null entry for index value "+i);
        }

        //Make sure all FileDesc named in _urnMap exist.
        for (Iterator iter = _urnMap.keySet().iterator(); iter.hasNext(); ) {
            URN urn = (URN)iter.next();
            IntSet indices2 = (IntSet)_urnMap.get(urn);
            for (IntSet.IntSetIterator iter2 = indices2.iterator(); iter2.hasNext(); ) {
                int i = iter2.next();
                FileDesc fd = (FileDesc)_files.get(i);
                Assert.that(fd != null, "Missing file for urn");
                Assert.that(fd.containsUrn(urn), "URN mismatch");
            }
        }

        //Verify directory listing.  Make sure directory only contains 
        //legal values.
        Iterator iter = _sharedDirectories.keySet().iterator();
        while (iter.hasNext()) {
            File directory = (File)iter.next();
            IntSet children = (IntSet)_sharedDirectories.get(directory);
            
            IntSet.IntSetIterator iter2 = children.iterator();
            while (iter2.hasNext()) {
                int i = iter2.next();
                Assert.that(i >= 0 && i < _files.size(),
                            "Bad index "+i+" in directory");
                FileDesc fd = (FileDesc)_files.get(i);
                Assert.that(fd != null, "Directory listing points to empty file");
            }
        }

        //For all files...
        int numFilesCount = 0;
        int sizeFilesCount = 0;
        for (int i = 0; i < _files.size(); i++) {
            if (_files.get(i) == null)
                continue;
            FileDesc desc = (FileDesc)_files.get(i);
            numFilesCount++;
            sizeFilesCount += desc.getSize();

            //a) Ensure is has the right index.
            Assert.that(desc.getIndex() == i,
                        "Bad index value.  Got "+desc.getIndex()+" not "+i);
            //b) Ensured name indexed indexed. 
            //   (Note we don't check filenames; I'm lazy.)
            Assert.that(indices.contains(i),
                        "Index does not contain entry for "+i);
            //c) Ensure properly listed in directory
            try {
                IntSet siblings = (IntSet)_sharedDirectories.get(
                    FileUtils.getCanonicalFile(
                        FileUtils.getParentFile(desc.getFile())));
                Assert.that(siblings != null, 
                    "Directory for "+desc.getPath()+" isn't shared");
                Assert.that(siblings.contains(i),
                    "Index "+i+" not in directory");
            } catch (IOException e) {
                Assert.that(false);
            }
            //d) Ensure URNs listed.
            for (iter = desc.getUrns().iterator(); iter.hasNext(); ) {
                URN urn = (URN)iter.next();
                IntSet indices2 = (IntSet)_urnMap.get(urn);
                Assert.that(indices2 != null, "Urn not found");
                Assert.that(indices2.contains(desc.getIndex()));
            }
        }   
        Assert.that(_numFiles == numFilesCount, _numFiles+" should be "+numFilesCount);
        Assert.that(_filesSize == sizeFilesCount, _filesSize+" should be "+sizeFilesCount);
    }

    //Unit tests: tests/com/limegroup/gnutella/FileManagerTest.java
    //            core/com/limegroup/gnutella/tests/UrnRequestTest.java
}






