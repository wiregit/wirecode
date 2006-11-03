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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.auth.ContentManager;
import com.limegroup.gnutella.auth.ContentResponseData;
import com.limegroup.gnutella.auth.ContentResponseObserver;
import com.limegroup.gnutella.downloader.VerifyingFile;
import com.limegroup.gnutella.library.LibraryData;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.routing.QueryRouteTable;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.FileUtils;
import com.limegroup.gnutella.util.Function;
import com.limegroup.gnutella.util.I18NConvert;
import com.limegroup.gnutella.util.IntSet;
import com.limegroup.gnutella.util.ProcessingQueue;
import com.limegroup.gnutella.util.StringUtils;
import com.limegroup.gnutella.util.StringTrie;
import com.limegroup.gnutella.version.UpdateHandler;
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
    public static final File PROGRAM_SHARE;
    
    /** Subdirectory that also is always shared. */
    public static final File PREFERENCE_SHARE;
    
    /** Always share torrent meta data directory. */
    public static final File TORRENT_META_DATA_SHARE;
    
    static {
        File forceShare = new File(".", ".NetworkShare").getAbsoluteFile();
        try {
            forceShare = FileUtils.getCanonicalFile(forceShare);
        } catch(IOException ignored) {}
        PROGRAM_SHARE = forceShare;
        
        forceShare = new File(CommonUtils.getUserSettingsDir(), ".NetworkShare").getAbsoluteFile();
        try {
            forceShare = FileUtils.getCanonicalFile(forceShare);
        } catch(IOException ignored) {}
        PREFERENCE_SHARE = forceShare;
        
        File torrentMetaShare = 
            new File(CommonUtils.getUserSettingsDir(), ".torrentMetaData").getAbsoluteFile();
        torrentMetaShare.mkdir();
        try {
            torrentMetaShare = FileUtils.getCanonicalFile(torrentMetaShare);
        } catch(IOException ignored) {}
        TORRENT_META_DATA_SHARE = torrentMetaShare;
    }

    /** A type-safe empty LimeXMLDocument list. */
    public static final List<LimeXMLDocument> EMPTY_DOCUMENTS = Collections.emptyList();
    
    private static final ProcessingQueue LOADER = new ProcessingQueue("FileManagerLoader");

     
    /** List of event listeners for FileManagerEvents. */
    private volatile CopyOnWriteArrayList<FileEventListener> eventListeners =
        new CopyOnWriteArrayList<FileEventListener>();
    
    /**********************************************************************
     * LOCKING: obtain this's monitor before modifying this.
     **********************************************************************/

    /**
     * All of the data for FileManager.
     */
    private final LibraryData _data = new LibraryData();

    /** 
     * The list of complete and incomplete files.  An entry is null if it
     *  is no longer shared.
     * INVARIANT: for all i, _files[i]==null, or _files[i].index==i and either
     *  _files[i]._path is in a shared directory with a shareable extension or
     *  _files[i]._path is the incomplete directory if _files[i] is an IncompleteFileDesc.
     */
    private List<FileDesc> _files;
    
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
    private Map<File, FileDesc> _fileToFileDescMap;

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
    private StringTrie<IntSet> _keywordTrie;
    
    /**
     * A map of appropriately case-normalized URN strings to the
     * indices in _files.  Used to make query-by-hash faster.
     * 
     * INVARIANT: for all keys k in _urnMap, for all i in _urnMap.get(k),
     * _files[i].containsUrn(k).  Likewise for all i, for all k in
     * _files[i].getUrns(), _urnMap.get(k) contains i.
     */
    private Map<URN, IntSet> _urnMap;
    
    /**
     * The set of file extensions to share, sorted by StringComparator. 
     * INVARIANT: all extensions are lower case.
     */
    private static Set<String> _extensions;
    
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
    private Map<File, IntSet> _sharedDirectories;
    
	/**
	 * A Set of shared directories that are completely shared.  Files in these
	 * directories are shared by default and will be shared unless the File is
	 * listed in SharingSettings.FILES_NOT_TO_SHARE.
	 */
	private Set<File> _completelySharedDirectories;
	
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
     * A Set of URNs that we're currently requesting validation for.
     * This is NOT cleared on new revisions, because it'll always be
     * valid.
     */
    private Set<URN> _requestingValidation = Collections.synchronizedSet(new HashSet<URN>());
    
    /**
     * The revision of the library.  Every time 'loadSettings' is called, the revision
     * is incremented.
     */
    protected volatile int _revision = 0;
    
    /**
     * The revision that finished loading all pending files.
     */
    private volatile int _pendingFinished = -1;
    
    /**
     * The revision that finished updating shared directories.
     */
    private volatile int _updatingFinished = -1;
    
    /**
     * If true, indicates that the FileManager is currently updating.
     */
    private volatile boolean _isUpdating = false;
    
    /**
     * The last revision that finished both pending & updating.
     */
    private volatile int _loadingFinished = -1;
    
    /**
     * Whether the FileManager has been shutdown.
     */
    protected volatile boolean shutdown;
    
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
     * An empty callback so we don't have to do != null checks everywhere.
     */
    private static final FileEventListener EMPTY_CALLBACK = new FileEventListener() {
        public void handleFileEvent(FileManagerEvent evt) {}
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
        _files = new ArrayList<FileDesc>();
        _keywordTrie = new StringTrie<IntSet>(true);  //ignore case
        _urnMap = new HashMap<URN, IntSet>();
        _extensions = new HashSet<String>();
        _sharedDirectories = new HashMap<File, IntSet>();
		_completelySharedDirectories = new HashSet<File>();
        _incompletesShared = new IntSet();
        _fileToFileDescMap = new HashMap<File, FileDesc>();
    }

    /** Asynchronously loads all files by calling loadSettings.  Sets this's
     *  callback to be "callback", and notifies "callback" of all file loads.
     *      @modifies this
     *      @see loadSettings */
    public void start() {
        _data.clean();
        cleanIndividualFiles();
		loadSettings();
    }
    
    public void stop() {
        save();
        shutdown = true;
    }

    protected void save(){
        _data.save();
            
        UrnCache.instance().persistCache();
        CreationTimeCache.instance().persistCache();
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
        return _files.get(i);
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

        return _fileToFileDescMap.get(f);
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
		IntSet indices = _urnMap.get(urn);
		if(indices == null) return null;

		IntSet.IntSetIterator iter = indices.iterator();
		
        //Pick the first non-null non-Incomplete FileDesc.
        FileDesc ret = null;
		while ( iter.hasNext() 
               && ( ret == null || ret instanceof IncompleteFileDesc) ) {
			int index = iter.next();
            ret = _files.get(index);
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
            FileDesc fd = _files.get(iter.next());
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
        fds = _fileToFileDescMap.values().toArray(fds);
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
        IntSet indices = _sharedDirectories.get(directory);
        if (indices == null)  // directory not shared.
			return new FileDesc[0];
		
        FileDesc[] fds = new FileDesc[indices.size()];
        IntSet.IntSetIterator iter = indices.iterator();
        for (int i = 0; iter.hasNext(); i++) {
            FileDesc fd = _files.get(iter.next());
            Assert.that(fd != null, "Directory has null entry");
            fds[i] = fd;
        }
        
        return fds;
    }


    ///////////////////////////////////////////////////////////////////////////
    //  Loading 
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Starts a new revision of the library, ensuring that only items present
     * in the appropriate sharing settings are shared.
     *
     * This method is non-blocking and thread-safe.
     *
     * @modifies this
     */
    public void loadSettings() {
        final int currentRevision = ++_revision;
        if(LOG.isDebugEnabled())
            LOG.debug("Starting new library revision: " + currentRevision);
        
        LOADER.add(new Runnable() {
            public void run() {
                loadStarted(currentRevision);
                loadSettingsInternal(currentRevision);
            }
        });
    }
    
    /**
     * Loads the FileManager with a new list of directories.
     */
    public void loadWithNewDirectories(Set<? extends File> shared) {
        SharingSettings.DIRECTORIES_TO_SHARE.setValue(shared);
        synchronized(_data.DIRECTORIES_NOT_TO_SHARE) {
            for(File file : shared)
                _data.DIRECTORIES_NOT_TO_SHARE.remove(file);
        }
	    RouterService.getFileManager().loadSettings();
    }
    
    /**
     * Kicks off necessary stuff for a load being started.
     */
    protected void loadStarted(int revision) {
        UrnCache.instance().clearPendingHashes(this);
    }
    
    /**
     * Notification that something finished loading.
     */
    private void tryToFinish() {
        int revision;
        synchronized(this) {
            if(_pendingFinished != _updatingFinished || // Pending's revision must == update
               _pendingFinished != _revision ||       // The revision must be the current library's
               _loadingFinished >= _revision)         // And we can't have already finished.
                return;
            _loadingFinished = _revision;
            revision = _loadingFinished;
        }
        
        loadFinished(revision);
    }
    
    /**
     * Kicks off necessary stuff for loading being done.
     */
    protected void loadFinished(int revision) {
        if(LOG.isDebugEnabled())
            LOG.debug("Finished loading revision: " + revision);
        
        // Various cleanup & persisting...
        trim();
        CreationTimeCache.instance().pruneTimes();
        RouterService.getDownloadManager().getIncompleteFileManager().registerAllIncompleteFiles();
        save();
        SavedFileManager.instance().run();
        UpdateHandler.instance().tryToDownloadUpdates();
        RouterService.getCallback().fileManagerLoaded();
    }
    
    /**
     * Returns whether or not the loading is finished.
     */
    public boolean isLoadFinished() {
        return _loadingFinished == _revision;
    }

    /**
     * Returns whether or not the updating is finished.
     */
    public boolean isUpdating() {
        return _isUpdating;
    }

    /** 
     * Loads all shared files, putting them in a queue for being added.
     *
     * If the current revision ever changed from the expected revision, this returns
     * immediately.
     */
    protected void loadSettingsInternal(int revision) {
        if(LOG.isDebugEnabled())
            LOG.debug("Loading Library Revision: " + revision);
        
        final File[] directories;
        synchronized (this) {
            // Reset the file list info
            resetVariables();

            // Load the extensions.
            String[] extensions = StringUtils.split(SharingSettings.EXTENSIONS_TO_SHARE.getValue(), ";");
            for(int i = 0; i < extensions.length; i++)
                _extensions.add(extensions[i].toLowerCase());

            //Ideally we'd like to ensure that "C:\dir\" is loaded BEFORE
            //C:\dir\subdir.  Although this isn't needed for correctness, it may
            //help the GUI show "subdir" as a subdirectory of "dir".  One way of
            //doing this is to do a full topological sort, but that's a lot of 
            //work. So we just approximate this by sorting by filename length, 
            //from smallest to largest.  Unless directories are specified as
            //"C:\dir\..\dir\..\dir", this will do the right thing.
            
            directories = SharingSettings.DIRECTORIES_TO_SHARE.getValueAsArray();
            Arrays.sort(directories, new Comparator<File>() {
                public int compare(File a, File b) {
                    return a.toString().length()-b.toString().length();
                }
            });
        }

        //clear this, list of directories retrieved
        RouterService.getCallback().fileManagerLoading();

        // Update the FORCED_SHARE directory.
        updateSharedDirectories(PROGRAM_SHARE, null, revision);
        updateSharedDirectories(PREFERENCE_SHARE, null, revision);
            
        //Load the shared directories and add their files.
        _isUpdating = true;
        for(int i = 0; i < directories.length && _revision == revision; i++)
            updateSharedDirectories(directories[i], null, revision);
            

        // Add specially shared files
        Set<File> specialFiles = _data.SPECIAL_FILES_TO_SHARE;
        ArrayList<File> list;
        synchronized(specialFiles) {
        	// iterate over a copied list, since addFileIfShared might call
        	// _data.SPECIAL_FILES_TO_SHARE.remove() which can cause a concurrent
        	// modification exception
        	list = new ArrayList<File>(specialFiles);
        }
        for(File file : list) {
            if(_revision != revision)
                break;
            addFileIfShared(file, EMPTY_DOCUMENTS, true, revision, null);
        }
        _isUpdating = false;

        trim();
        
        if(LOG.isDebugEnabled())
            LOG.debug("Finished queueing shared files for revision: " + revision);
            
        _updatingFinished = revision;
        if(_numPendingFiles == 0) // if we didn't even try adding any files, pending is finished also.
            _pendingFinished = revision;
        tryToFinish();
    }
    
    /**
     * Recursively adds this directory and all subdirectories to the shared
     * directories as well as queueing their files for sharing.  Does nothing
     * if <tt>directory</tt> doesn't exist, isn't a directory, or has already
     * been added.  This method is thread-safe.  It acquires locks on a
     * per-directory basis.  If the current revision ever changes from the
     * expected revision, this returns immediately.
     * 
     * @requires directory is part of DIRECTORIES_TO_SHARE or one of its
     *           children, and parent is directory's shared parent or null if
     *           directory's parent is not shared.
     * @modifies this
     */
    private void updateSharedDirectories(File directory, File parent, int revision) {
//        if(LOG.isDebugEnabled())
//            LOG.debug("Attempting to share directory: " + directory);
        
        //We have to get the canonical path to make sure "D:\dir" and "d:\DIR"
        //are the same on Windows but different on Unix.
        try {
            directory = FileUtils.getCanonicalFile(directory);
        } catch (IOException e) {
            return;
        }
        
        if(!directory.exists())
            return;
        
        // STEP 0:
		// Do not share certain the incomplete directory, directories on the
		// do not share list, or sensitive directories.
        if (directory.equals(SharingSettings.INCOMPLETE_DIRECTORY.getValue()))
            return;

		// Do not share directories on the do not share list
		if (_data.DIRECTORIES_NOT_TO_SHARE.contains(directory))
			return;
        
        // Do not share sensitive directories
        if (isSensitiveDirectory(directory)) {
            //  go through directories that explicitly should not be shared
            if (_data.SENSITIVE_DIRECTORIES_NOT_TO_SHARE.contains(directory))
                return;
            
            // if we haven't already validated the sensitive directory, ask about it.
            if (_data.SENSITIVE_DIRECTORIES_VALIDATED.contains(directory)) {
                //  ask the user whether the sensitive directory should be shared
                // THIS CALL CAN BLOCK.
                if (!RouterService.getCallback().warnAboutSharingSensitiveDirectory(directory))
                    return;
            }
        }
		
        // Exit quickly (without doing the dir lookup) if revisions changed.
        if(_revision != revision)
            return;

        // STEP 1:
        // Add directory
        boolean isForcedShare = isForcedShareDirectory(directory) 
            || isTorrentMetaDataShareDirectory(directory);
        synchronized (this) {
            // if it was already added, ignore.
            if (_completelySharedDirectories.contains(directory))
                return;

//            if(LOG.isDebugEnabled())
//                LOG.debug("Adding completely shared directory: " + directory);

			_completelySharedDirectories.add(directory);
            if (!isForcedShare) {
                dispatchFileEvent(
                    new FileManagerEvent(this, FileManagerEvent.ADD_FOLDER, directory, parent));
            }
        }
		
        // STEP 2:
        // Scan subdirectory for the amount of shared files.
        File[] file_list = directory.listFiles(SHAREABLE_FILE_FILTER);
        if (file_list == null)
            return;
        for(int i = 0; i < file_list.length && _revision == revision; i++)
            addFileIfShared(file_list[i], EMPTY_DOCUMENTS, true, revision, null);
            
        // Exit quickly (without doing the dir lookup) if revisions changed.
        if(_revision != revision)
            return;

        // STEP 3:
        // Recursively add subdirectories.
        // This has the effect of ensuring that the number of pending files
        // is closer to correct number.
        // TODO: when we add non-recursive support, add it here.
        if (isForcedShare) 
            return;
        
        // Do not share subdirectories of the forcibly shared dir.
        File[] dir_list = directory.listFiles(DIRECTORY_FILTER);
        if(dir_list != null) {
            for(int i = 0; i < dir_list.length && _revision == revision; i++)
                updateSharedDirectories(dir_list[i], directory, revision);
        }
            
    }


	///////////////////////////////////////////////////////////////////////////
	//  Adding and removing shared files and directories
	///////////////////////////////////////////////////////////////////////////

	/**
	 * Removes a given directory from being completely shared.
	 */
	public void removeFolderIfShared(File folder) {
        _isUpdating = true;
        removeFolderIfShared(folder, null);
        _isUpdating = false;

	}
	
	/**
	 * Removes a given directory from being completed shared.
	 * If 'parent' is null, this will remove it from the root-level of
	 * shared folders if it existed there.  (If it is non-null & it was
	 * a root-level shared folder, the folder remains shared.)
	 *
	 * The first time this is called, parent must be non-null in order to ensure
	 * it works correctly.  Otherwise, we'll end up adding tons of stuff
	 * to the DIRECTORIES_NOT_TO_SHARE.
	 */
	protected void removeFolderIfShared(File folder, File parent) {
		if (!folder.isDirectory() && folder.exists())
			throw new IllegalArgumentException("Expected a directory, but given: "+folder);
		
	    try {
	        folder = FileUtils.getCanonicalFile(folder);
	    } catch(IOException ignored) {}

        // grab the value quickly.  release the lock
        // so that we don't hold it during a long recursive function.
        // it's no big deal if it changes, we'll just do some extra work for a short
        // bit of time.
        boolean contained;
        synchronized(this) {
            contained = _completelySharedDirectories.contains(folder);
        }
        
        if(contained) {
            if(parent != null && SharingSettings.DIRECTORIES_TO_SHARE.contains(folder)) {
                // we don't wanna remove it, since it's a root-share, nor do we want
                // to remove any of its children, so we return immediately.
                return;
            } else if(parent == null) {
                if(!SharingSettings.DIRECTORIES_TO_SHARE.remove(folder))
                    _data.DIRECTORIES_NOT_TO_SHARE.add(folder);
            }
            
            // note that if(parent != null && not a root share)
            // we DO NOT ADD to DIRECTORIES_NOT_TO_SHARE.
            // this is by design, because the parent has already been removed
            // from sharing, which inherently will remove the child directories.
            // there's no need to clutter up DIRECTORIES_NOT_TO_SHARE with useless
            // entries.
           
            synchronized(this) {
                _completelySharedDirectories.remove(folder);
            }
            
            File[] subs = folder.listFiles();
            if(subs != null) {
                for(int i = 0; i < subs.length; i++) {
                    File f = subs[i];
                    if(f.isDirectory())
                        removeFolderIfShared(f, folder);
                    else if(f.isFile() && !_data.SPECIAL_FILES_TO_SHARE.contains(f)) {
                        if(removeFileIfShared(f) == null)
                            UrnCache.instance().clearPendingHashesFor(f, this);
                    }
                }
            }
            
            // send the event last.  this is a hack so that the GUI can properly
            // receive events with the children first, moving any leftover children up to
            // potential parent directories.
            dispatchFileEvent(
                new FileManagerEvent(this, FileManagerEvent.REMOVE_FOLDER, folder));
        }
    }
    
    /**
     * Adds a given folder to be shared.
     */
    public void addSharedFolder(File folder) {
		if (!folder.isDirectory())
			throw new IllegalArgumentException("Expected a directory, but given: "+folder);
		
        try {
            folder = FileUtils.getCanonicalFile(folder);
        } catch(IOException ignored) {}
        
        _data.DIRECTORIES_NOT_TO_SHARE.remove(folder);
		if (!isCompletelySharedDirectory(folder.getParentFile()))
			SharingSettings.DIRECTORIES_TO_SHARE.add(folder);
        _isUpdating = true;
        updateSharedDirectories(folder, null, _revision);
        _isUpdating = false;
    }
    
    public void addTorrentMetaDataFile(File file) {
        File shareMetaFile = file;
        if(!isTorrentMetaDataShare(file)) {
            shareMetaFile = new File(TORRENT_META_DATA_SHARE, file.getName());
            try {
                FileUtils.copyFile(file, shareMetaFile);
            } catch (IOException ioex) {
                ErrorService.error(ioex);
            }
        }
        addFileAlways(shareMetaFile);
    }
	
	/**
	 * Always shares the given file.
	 */
	public void addFileAlways(File file) {
		addFileAlways(file, EMPTY_DOCUMENTS, null);
	}
	
	/**
	 * Always shares a file, notifying the given callback when shared.
	 */
	public void addFileAlways(File file, FileEventListener callback) {
	    addFileAlways(file, EMPTY_DOCUMENTS, callback);
	}
	
	/**
	 * Always shares the given file, using the given list of metadata.
	 */
	public void addFileAlways(File file, List<? extends LimeXMLDocument> list) {
	    addFileAlways(file, list, null);
    }
    
    /**
	 * Adds the given file to share, with the given list of metadata,
	 * even if it exists outside of what is currently accepted to be shared.
	 * <p>
	 * Too large files are still not shareable this way.
	 *
	 * The listener is notified if this file could or couldn't be shared.
	 */
	 public void addFileAlways(File file, List<? extends LimeXMLDocument> list, FileEventListener callback) {
		_data.FILES_NOT_TO_SHARE.remove(file);
		if (!isFileShareable(file))
			_data.SPECIAL_FILES_TO_SHARE.add(file);
			
		addFileIfShared(file, list, true, _revision, callback);
	}
	
    /**
     * Adds the given file if it's shared.
     */
   public void addFileIfShared(File file) {
       addFileIfShared(file, EMPTY_DOCUMENTS, true, _revision, null);
   }
   
    /**
     * Adds the given file if it's shared, notifying the given callback.
     */
    public void addFileIfShared(File file, FileEventListener callback) {
        addFileIfShared(file, EMPTY_DOCUMENTS, true, _revision, callback);
    }
    
    /**
     * Adds the file if it's shared, using the given list of metadata.
     */
    public void addFileIfShared(File file, List<? extends LimeXMLDocument> list) {
        addFileIfShared(file, list, true, _revision, null);
    }
    
    /**
     * Adds the file if it's shared, using the given list of metadata,
     * informing the specified listener about the status of the sharing.
     */
    public void addFileIfShared(File file, List<? extends LimeXMLDocument> list, FileEventListener callback) {
        addFileIfShared(file, list, true, _revision, callback);
    }
    
    /**
     * The actual implementation of addFileIfShared(File)
     * @param file the file to add
     * @param notify if true signals the front-end via 
     *        ActivityCallback.handleFileManagerEvent() about the Event
     */
    protected void addFileIfShared(File file, List<? extends LimeXMLDocument> metadata, boolean notify,
                                   int revision, FileEventListener callback) {
//        if(LOG.isDebugEnabled())
//            LOG.debug("Attempting to share file: " + file);
        if(callback == null)
            callback = EMPTY_CALLBACK;

        if(revision != _revision) {
            callback.handleFileEvent(new FileManagerEvent(this, FileManagerEvent.FAILED, file));
            return;
        }
        
        // Make sure capitals are resolved properly, etc.
        try {
            file = FileUtils.getCanonicalFile(file);
        } catch (IOException e) {
            callback.handleFileEvent(new FileManagerEvent(this, FileManagerEvent.FAILED, file));
            return;
	    }
	    
        synchronized(this) {
		    if (revision != _revision) { 
		    	callback.handleFileEvent(new FileManagerEvent(this, FileManagerEvent.FAILED, file));
                return;
            }
			// if file is not shareable, also remove it from special files
			// to share since in that case it's not physically shareable then
		    if (!isFileShareable(file)) {
		    	_data.SPECIAL_FILES_TO_SHARE.remove(file);
		    	callback.handleFileEvent(new FileManagerEvent(this, FileManagerEvent.FAILED, file));
                return;
		    }
        
            if(isFileShared(file)) {
                callback.handleFileEvent(new FileManagerEvent(this, FileManagerEvent.ALREADY_SHARED, file));
                return;
            }
            
            _numPendingFiles++;
            // make sure _pendingFinished does not hold _revision
            // while we're still adding files
            _pendingFinished = -1;
        }

		UrnCache.instance().calculateAndCacheUrns(file, getNewUrnCallback(file, metadata, notify, revision, callback));
    }
    
    /**
     * Constructs a new UrnCallback that will possibly load the file with the given URNs.
     */
    protected UrnCallback getNewUrnCallback(final File file, final List<? extends LimeXMLDocument> metadata, final boolean notify,
                                            final int revision, final FileEventListener callback) {
        return new UrnCallback() {
		    public void urnsCalculated(File f, Set<? extends URN> urns) {
//		        if(LOG.isDebugEnabled())
//		            LOG.debug("URNs calculated for file: " + f);
		        
		        FileDesc fd = null;
		        synchronized(FileManager.this) {
    		        if(revision != _revision) {
    		            LOG.warn("Revisions changed, dropping share.");
                        callback.handleFileEvent(new FileManagerEvent(FileManager.this, FileManagerEvent.FAILED, file));
                        return;
                    }
                
                    _numPendingFiles--;
                    
                    // Only load the file if we were able to calculate URNs and
                    // the file is still shareable.
                    if(!urns.isEmpty() && isFileShareable(file)) {
                        fd = addFile(file, urns);
                    }
                }
                    
                if(fd != null) {
                    loadFile(fd, file, metadata, urns);
                    
                    FileManagerEvent evt = new FileManagerEvent(FileManager.this, FileManagerEvent.ADD, fd);
                    if(notify) // sometimes notify the GUI
                        dispatchFileEvent(evt);
                    callback.handleFileEvent(evt); // always notify the individual callback.
                } else {
                    // If URNs was empty, or loading failed, notify...
                    callback.handleFileEvent(new FileManagerEvent(FileManager.this, FileManagerEvent.FAILED, file));
                }
                
                if(_numPendingFiles == 0) {
                    _pendingFinished = revision;
                    tryToFinish();
                }
            }
            
            public boolean isOwner(Object o) {
                return o == FileManager.this;
            }
        };
    }
    
    /**
     * Loads a single shared file.
     */
    protected void loadFile(FileDesc fd, File file, List<? extends LimeXMLDocument> metadata, Set<? extends URN> urns) {
    }   
  
    /**
     * @requires the given file exists and is in a shared directory
     * @modifies this
     * @effects adds the given file to this if it is of the proper extension and
     *  not too big (>~2GB).  Returns true iff the file was actually added.
     *
     * @return the <tt>FileDesc</tt> for the new file if it was successfully 
     *  added, otherwise <tt>null</tt>
     */
    private synchronized FileDesc addFile(File file, Set<? extends URN> urns) {
   //     if(LOG.isDebugEnabled())
   //         LOG.debug("Sharing file: " + file);
        

        int fileIndex = _files.size();
        FileDesc fileDesc = new FileDesc(file, urns, fileIndex);
        ContentResponseData r = RouterService.getContentManager().getResponse(fileDesc.getSHA1Urn());
        // if we had a response & it wasn't good, don't add this FD.
        if(r != null && !r.isOK())
            return null;
        

        long fileLength = file.length();
        _filesSize += fileLength;        
        _files.add(fileDesc);
        _fileToFileDescMap.put(file, fileDesc);
        _numFiles++;
	
        //Register this file with its parent directory.
        File parent = file.getParentFile();
        Assert.that(parent != null, "Null parent to \""+file+"\"");
        
        // Check if file is a specially shared file.  If not, ensure that
        // it is located in a shared directory.
		IntSet siblings = _sharedDirectories.get(parent);
		if (siblings == null) {
			siblings = new IntSet();
			_sharedDirectories.put(parent, siblings);
		}
		
		boolean added = siblings.add(fileIndex);
        Assert.that(added, "File "+fileIndex+" already found in "+siblings);
        
        // files that are forcibly shared over the network
        // aren't counted or shown.
        if(isForcedShareDirectory(parent))
            _numForcedFiles++;
	
        //Index the filename.  For each keyword...
        String[] keywords = extractKeywords(fileDesc);
        
        for (int i = 0; i < keywords.length; i++) {
            String keyword = keywords[i];
            //Ensure the _keywordTrie has a set of indices associated with keyword.
            IntSet indices = _keywordTrie.get(keyword);
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
        return fileDesc;
    }

	/**
	 * Removes the file if it is being shared, and then removes the file from
	 * the special lists as necessary.
     * @return The FileDesc associated with this file, or null if the file was
     * not shared. 
	 */
	public synchronized FileDesc stopSharingFile(File file) {
		try {
			file = FileUtils.getCanonicalFile(file);
		} catch (IOException e) {
			return null;
		}
		
		// remove file already here to heed against race conditions
		// wrt to filemanager events being handled on other threads
		boolean removed = _data.SPECIAL_FILES_TO_SHARE.remove(file); 
		FileDesc fd = removeFileIfShared(file);
		if (fd == null) {
		    UrnCache.instance().clearPendingHashesFor(file, this);
        } else {
			file = fd.getFile();
			// if file was not specially shared, add it to files_not_to_share
			if (!removed)
				_data.FILES_NOT_TO_SHARE.add(file);
		}
        return fd;
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
        //Take care of case, etc.
        try {
            f = FileUtils.getCanonicalFile(f);
        } catch (IOException e) {
            return null;
        }        

		// Look for matching file ...         
        FileDesc fd = _fileToFileDescMap.get(f);
        if (fd == null)
            return null;

        int i = fd.getIndex();
        Assert.that(_files.get(i).getFile().equals(f),
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

			// Notify the GUI...
	        if (notify) {
	            FileManagerEvent evt = new FileManagerEvent(this, 
	                                            FileManagerEvent.REMOVE, 
	                                            fd );
	                                            
	            dispatchFileEvent(evt);
	        }
            return fd;
        }

        _numFiles--;
        _filesSize -= fd.getFileSize();

        //Remove references to this from directory listing
        File parent = f.getParentFile();
        IntSet siblings = _sharedDirectories.get(parent);
        Assert.that(siblings != null,
            "Removed file's directory \""+parent+"\" not in "+_sharedDirectories);
        boolean removed = siblings.remove(i);
        Assert.that(removed, "File "+i+" not found in "+siblings);

        // files that are forcibly shared over the network aren't counted
        if(isForcedShareDirectory(parent)) {
            notify = false;
            _numForcedFiles--;
        }

        //Remove references to this from index.
        String[] keywords = extractKeywords(fd);
        for (int j = 0; j < keywords.length; j++) {
            String keyword = keywords[j];
            IntSet indices = _keywordTrie.get(keyword);
            if (indices != null) {
                indices.remove(i);
                if (indices.size() == 0)
                	_keywordTrie.remove(keyword);
            }
        }

        //Remove hash information.
        this.removeUrnIndex(fd);
        //Remove creation time information
        if (_urnMap.get(fd.getSHA1Urn()) == null)
            CreationTimeCache.instance().removeTime(fd.getSHA1Urn());
  
        // Notify the GUI...
        if (notify) {
            FileManagerEvent evt = new FileManagerEvent(this, 
                                            FileManagerEvent.REMOVE, 
                                            fd);
                                            
            dispatchFileEvent(evt);
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
                                               Set<? extends URN> urns,
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
        for(URN urn : urns) {
            // if there were indices for this URN, exit.
            IntSet shared = _urnMap.get(urn);
            // nothing was shared for this URN, look at another
            if (shared == null)
                continue;
                
            for (IntSet.IntSetIterator isIter = shared.iterator(); isIter.hasNext(); ) {
                int i = isIter.next();
                FileDesc desc = _files.get(i);
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
        dispatchFileEvent(new FileManagerEvent(this, FileManagerEvent.ADD, ifd));
    }

    /**
     * Notification that a file has changed and new hashes should be
     * calculated.
     */
    public abstract void fileChanged(File f);
    
    /** Attempts to validate the given FileDesc. */
    public void validate(final FileDesc fd) {
        ContentManager cm = RouterService.getContentManager();
        if(_requestingValidation.add(fd.getSHA1Urn())) {
            cm.request(fd.getSHA1Urn(), new ContentResponseObserver() {
               public void handleResponse(URN urn, ContentResponseData r) {
                   _requestingValidation.remove(fd.getSHA1Urn());
                   if(r != null && !r.isOK())
                       removeFileIfShared(fd.getFile());
               }
            }, 5000);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    //  Search, utility, etc...
    ///////////////////////////////////////////////////////////////////////////
		
    /**
     * @modifies this
     * @effects enters the given FileDesc into the _urnMap under all its 
     * reported URNs
     */
    private synchronized void updateUrnIndex(FileDesc fileDesc) {
        for(URN urn : fileDesc.getUrns()) {
			IntSet indices=_urnMap.get(urn);
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
        for(URN urn : fileDesc.getUrns()) {
            //Lookup each of desc's URN's ind _urnMap.  
            //(It better be there!)
            IntSet indices=_urnMap.get(urn);
            Assert.that(indices!=null, "Invariant broken");

            //Delete index from set.  Remove set if empty.
            indices.remove(fileDesc.getIndex());
            if (indices.size()==0) {
                RouterService.getAltlocManager().purge(urn);
                _urnMap.remove(urn);
            }
		}
    }
    
    /**
     * Renames a from from 'oldName' to 'newName'.
     */
    public void renameFileIfShared(File oldName, File newName) {
        renameFileIfShared(oldName, newName, null);
    }

    /** 
     * If oldName isn't shared, returns false.  Otherwise removes "oldName",
     * adds "newName", and returns true iff newName is actually shared.  The new
     * file may or may not have the same index as the original.
     *
     * This assumes that oldName has been deleted & newName exists now.
     * @modifies this 
     */
    public synchronized void renameFileIfShared(File oldName, final File newName, final FileEventListener callback) {
        FileDesc toRemove = getFileDescForFile(oldName);
        if (toRemove == null) {
            FileManagerEvent evt = new FileManagerEvent(this, FileManagerEvent.FAILED, oldName);
            dispatchFileEvent(evt);
            if(callback != null)
                callback.handleFileEvent(evt);
            return;
        }
        
        if(LOG.isDebugEnabled())
            LOG.debug("Attempting to rename: " + oldName + " to: "  + newName);
            
        List<LimeXMLDocument> xmlDocs = new LinkedList<LimeXMLDocument>(toRemove.getLimeXMLDocuments());
		final FileDesc removed = removeFileIfShared(oldName, false);
        Assert.that(removed == toRemove, "invariant broken.");
		if (_data.SPECIAL_FILES_TO_SHARE.remove(oldName) && !isFileInCompletelySharedDirectory(newName))
			_data.SPECIAL_FILES_TO_SHARE.add(newName);
			
        // Prepopulate the cache with new URNs.
        UrnCache.instance().addUrns(newName, removed.getUrns());

        addFileIfShared(newName, xmlDocs, false, _revision, new FileEventListener() {
            public void handleFileEvent(FileManagerEvent evt) {
                if(LOG.isDebugEnabled())
                    LOG.debug("Add of newFile returned callback: " + evt);

                // Retarget the event for the GUI.
                FileManagerEvent newEvt = null;
                if(evt.isAddEvent()) {
                    FileDesc fd = evt.getFileDescs()[0];
                    newEvt = new FileManagerEvent(FileManager.this, 
                                       FileManagerEvent.RENAME, 
                                       new FileDesc[]{removed,fd});
                } else {
                    newEvt = new FileManagerEvent(FileManager.this, 
                                       FileManagerEvent.REMOVE,
                                       removed);
                }
                dispatchFileEvent(newEvt);
                if(callback != null)
                    callback.handleFileEvent(newEvt);
            }
        });
    }


    /** Ensures that this's index takes the minimum amount of space.  Only
     *  affects performance, not correctness; hence no modifies clause. */
    private synchronized void trim() {
        _keywordTrie.trim(new Function<IntSet, IntSet>() {
            public IntSet apply(IntSet intSet) {
                intSet.trim();
                return intSet;
            }
        });
    }

    
    /**
	 * Validates a file, moving it from 'SENSITIVE_DIRECTORIES_NOT_TO_SHARE'
	 * to SENSITIVE_DIRECTORIES_VALIDATED'.
	 */
	public void validateSensitiveFile(File dir) {
        _data.SENSITIVE_DIRECTORIES_VALIDATED.add(dir);
        _data.SENSITIVE_DIRECTORIES_NOT_TO_SHARE.remove(dir);
    }

	/**
	 * Invalidates a file, removing it from the shared directories, validated
	 * sensitive directories, and adding it to the sensitive directories
	 * not to share (so we don't ask again in the future).
	 */
	public void invalidateSensitiveFile(File dir) {
        _data.SENSITIVE_DIRECTORIES_VALIDATED.remove(dir);
        _data.SENSITIVE_DIRECTORIES_NOT_TO_SHARE.add(dir);
        SharingSettings.DIRECTORIES_TO_SHARE.remove(dir);   
    }
    
    /**
     * Determines if there are any files shared that are not in completely shared directories.
     */
    public boolean hasIndividualFiles() {
        return !_data.SPECIAL_FILES_TO_SHARE.isEmpty();
    }
    
    /**
     * Returns all files that are shared while not in shared directories.
     */
    public File[] getIndividualFiles() {
        Set<File> candidates = _data.SPECIAL_FILES_TO_SHARE;
        synchronized(candidates) {
    		ArrayList<File> files = new ArrayList<File>(candidates.size());
            for(File f : candidates) {
    			if (f.exists())
    				files.add(f);
    		}
    		
    		if (files.isEmpty())
    			return new File[0];
            else
    		    return files.toArray(new File[files.size()]);
        }
    }
    
    /**
     * Determines if a given file is shared while not in a completely shared directory.
     */
    public boolean isIndividualShare(File f) {
    	return _data.SPECIAL_FILES_TO_SHARE.contains(f) 
            && FileUtils.isFilePhysicallyShareable(f)
            && !isTorrentMetaDataShare(f);
    }
    
    /**
     * Cleans all stale entries from the Set of individual files.
     */
    private void cleanIndividualFiles() {
        Set<File> files = _data.SPECIAL_FILES_TO_SHARE;
        synchronized(files) {
            for(Iterator<File> i = files.iterator(); i.hasNext(); ) {
                File f = i.next();
                if(!(FileUtils.isFilePhysicallyShareable(f)))
                    i.remove();
            }
        }
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
        File dir = f.getParentFile();
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
		if (!FileUtils.isFilePhysicallyShareable(file))
			return false;
		if (_data.SPECIAL_FILES_TO_SHARE.contains(file))
			return true;
		if (_data.FILES_NOT_TO_SHARE.contains(file))
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
        String userHome = System.getProperty("user.home");
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
        if(request.getQueryUrns().size() > 0)
            matches = urnSearch(request.getQueryUrns(),matches);
        
        if (matches==null)
            return EMPTY_RESPONSES;

        List<Response> responses = new LinkedList<Response>();
        final MediaType.Aggregator filter = MediaType.getAggregator(request);
        LimeXMLDocument doc = request.getRichQuery();

        // Iterate through our hit indices to create a list of results.
        for (IntSet.IntSetIterator iter=matches.iterator(); iter.hasNext();) { 
            int i = iter.next();
            FileDesc desc = _files.get(i);
            if(desc == null)
                Assert.that(false, 
                            "unexpected null in FileManager for query:\n"+
                            request);

            if ((filter != null) && !filter.allow(desc.getFileName()))
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
        return responses.toArray(new Response[responses.size()]);
    }

    /**
     * Responds to a what is new request.
     */
    private Response[] respondToWhatIsNewRequest(QueryRequest request, 
                                                 boolean includeXML) {
        // see if there are any files to send....
        // NOTE: we only request up to 3 urns.  we don't need to worry
        // about partial files because we don't add them to the cache.
        List<URN> urnList = CreationTimeCache.instance().getFiles(request, 3);
        if (urnList.size() == 0)
            return EMPTY_RESPONSES;
        
        // get the appropriate responses
        Response[] resps = new Response[urnList.size()];
        for (int i = 0; i < urnList.size(); i++) {
            URN currURN = urnList.get(i);
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
            FileDesc desc = _files.get(i);
            // If the file was unshared or is an incomplete file,
            // DO NOT SEND IT.
            if (desc==null || desc instanceof IncompleteFileDesc || isForcedShare(desc)) 
                continue;
        
            Assert.that(j<ret.length, "_numFiles is too small");
            ret[j] = new Response(desc);
            if(includeXML)
                addXMLToResponse(ret[j], desc);
            j++;
        }
        Assert.that(j==ret.length, "_numFiles is too large");
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
            Iterator<IntSet> iter= _keywordTrie.getPrefixedBy(query, i, j);
            if (iter.hasNext()) {
                //Got match.  Union contents of the iterator and store in
                //matches.  As an optimization, if this is the only keyword and
                //there is only one set returned, return that set without 
                //copying.
                IntSet matches=null;
                while (iter.hasNext()) {                
                    IntSet s= iter.next();
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
    private synchronized IntSet urnSearch(Iterable<URN> urnsIter, IntSet priors) {
        IntSet ret = priors;
        for(URN urn : urnsIter) {
            IntSet hits = _urnMap.get(urn);
            if(hits!=null) {
                // double-check hits to be defensive (not strictly needed)
                IntSet.IntSetIterator iter = hits.iterator();
                while(iter.hasNext()) {
                    FileDesc fd = _files.get(iter.next());
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
        return parent != null && isForcedShareDirectory(parent);
    }
    
    /**
     * Determines if this File is a torrent meta data share.
     */
    public static boolean isTorrentMetaDataShare(File file) {
        File parent = file.getParentFile();
        return parent != null && isTorrentMetaDataShareDirectory(parent);
    }
    
    /**
     * Determines if this File is a network shared directory.
     */
    public static boolean isForcedShareDirectory(File f) {
        return f.equals(PROGRAM_SHARE) || f.equals(PREFERENCE_SHARE);
    }
    
    public static boolean isTorrentMetaDataShareDirectory(File f) {
        return f.equals(TORRENT_META_DATA_SHARE);
    }
    
    /**
     * registers a listener for FileManagerEvents
     */
    public void registerFileManagerEventListener(FileEventListener listener) {
        eventListeners.addIfAbsent(listener);
    }

    /**
     * unregisters a listener for FileManagerEvents
     */
    public void unregisterFileManagerEventListener(FileEventListener listener) {
        eventListeners.remove(listener);
    }

    /**
     * dispatches a FileManagerEvent to any registered listeners 
     */
    public void dispatchFileEvent(FileManagerEvent evt) {
        for(FileEventListener listener : eventListeners) {
            listener.handleFileEvent(evt);
        }
    }
}






