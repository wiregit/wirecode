package com.limegroup.gnutella;

import static com.limegroup.gnutella.Constants.MAX_FILE_SIZE;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.Comparators;
import org.limewire.collection.Function;
import org.limewire.collection.IntSet;
import org.limewire.collection.MultiCollection;
import org.limewire.collection.StringTrie;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.inspection.Inspectable;
import org.limewire.inspection.InspectableForSize;
import org.limewire.inspection.InspectablePrimitive;
import org.limewire.statistic.StatsUtils;
import org.limewire.util.ByteOrder;
import org.limewire.util.CommonUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.I18NConvert;
import org.limewire.util.OSUtils;
import org.limewire.util.RPNParser;
import org.limewire.util.StringUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.FileManagerEvent.Type;
import com.limegroup.gnutella.auth.ContentResponseData;
import com.limegroup.gnutella.auth.ContentResponseObserver;
import com.limegroup.gnutella.downloader.VerifyingFile;
import com.limegroup.gnutella.library.LibraryData;
import com.limegroup.gnutella.licenses.LicenseType;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.routing.HashFunction;
import com.limegroup.gnutella.routing.QueryRouteTable;
import com.limegroup.gnutella.settings.DHTSettings;
import com.limegroup.gnutella.settings.MessageSettings;
import com.limegroup.gnutella.settings.SearchSettings;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.simpp.SimppListener;
import com.limegroup.gnutella.xml.LimeXMLDocument;

/**
 * The list of all shared files.  Provides operations to add and remove
 * individual files, directory, or sets of directories.  Provides a method to
 * efficiently query for files whose names contain certain keywords.<p>
 *
 * This class is thread-safe.
 */
@Singleton
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
    
    /** Subdirectory used to share special application files */
    public static final File APPLICATION_SPECIAL_SHARE;
    
    static {
        File forceShare = new File(".", ".NetworkShare").getAbsoluteFile();
        try {
            forceShare = FileUtils.getCanonicalFile(forceShare);
        } catch(IOException ignored) {}
        PROGRAM_SHARE = forceShare;
        
        forceShare = 
            new File(CommonUtils.getUserSettingsDir(), ".NetworkShare").getAbsoluteFile();
        try {
            forceShare = FileUtils.getCanonicalFile(forceShare);
        } catch(IOException ignored) {}
        PREFERENCE_SHARE = forceShare;
        
        forceShare = 
            new File(CommonUtils.getUserSettingsDir(), ".AppSpecialShare").getAbsoluteFile();
        forceShare.mkdir();
        try {
            forceShare = FileUtils.getCanonicalFile(forceShare);
        } catch(IOException ignored) {}
        APPLICATION_SPECIAL_SHARE = forceShare;
    }

    /** A type-safe empty LimeXMLDocument list. */
    public static final List<LimeXMLDocument> EMPTY_DOCUMENTS = Collections.emptyList();
    
    private static final ExecutorService LOADER = ExecutorsHelper.newProcessingQueue("FileManagerLoader");
    
    /**
     * delay between qrp updates should the simpp words change.
     * Not final for testing.
     */
    private static long QRP_DELAY = 60 * 60 * 1000;
     
    /** List of event listeners for FileManagerEvents. */
    private volatile CopyOnWriteArrayList<FileEventListener> eventListeners 
        = new CopyOnWriteArrayList<FileEventListener>();
    
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
    @InspectablePrimitive
    private long _filesSize;
    
    /**
     * The number of complete files.
     * INVARIANT: _numFiles==number of elements of _files that are not null
     *  and not IncompleteFileDescs.
     */
    @InspectablePrimitive
    private int _numFiles;
    
    /** 
     * The total number of files that are pending sharing.
     *  (ie: awaiting hashing or being added)
     */
    @InspectablePrimitive
    private int _numPendingFiles;
    
    /**
     * The total number of incomplete files.
     * INVARIANT: _numFiles + _numIncompleteFiles == the number of
     *  elements of _files that are not null.
     */
    @InspectablePrimitive
    private int _numIncompleteFiles;
    
    /**
     * The number of files that are forcibly shared over the network.
     * INVARIANT: _numFiles >= _numForcedFiles.
     */
    @InspectablePrimitive
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
    @InspectableForSize
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
    @InspectableForSize
    private Map<File, IntSet> _sharedDirectories;
    
	/**
	 * A Set of shared directories that are completely shared.  Files in these
	 * directories are shared by default and will be shared unless the File is
	 * listed in SharingSettings.FILES_NOT_TO_SHARE.
	 */
    @InspectableForSize
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
    @InspectableForSize
    private IntSet _incompletesShared;
    
    /**
     * A Set of URNs that we're currently requesting validation for.
     * This is NOT cleared on new revisions, because it'll always be
     * valid.
     */
    private Set<URN> _requestingValidation = Collections.synchronizedSet(new HashSet<URN>());
    
    /**
     * Files that are shared only for this LW session.
     * INVARIANT: no file can be in this and _data.SPECIAL_FILES_TO_SHARE
     * at the same time
     */
    @InspectableForSize
    private Set<File> _transientSharedFiles = new HashSet<File>();
    
    /**
     * Individual files that are not in a shared folder.
     */
    @InspectableForSize
    private Collection<File> _individualSharedFiles; 
    
    /**
     * Individual files removed from shared directories that were purchased from the LWS
     */
    @InspectableForSize
    private Collection<File> _individualStoreFiles;
    
    /**
     * The revision of the library.  Every time 'loadSettings' is called, the revision
     * is incremented.
     */
    @InspectablePrimitive
    protected volatile int _revision = 0;
    
    /**
     * The revision that finished loading all pending files.
     */
    @InspectablePrimitive
    private volatile int _pendingFinished = -1;
    
    /**
     * The revision that finished updating shared directories.
     */
    private volatile int _updatingFinished = -1;
    
    /**
     * If true, indicates that the FileManager is currently updating.
     */
    @InspectablePrimitive
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
    
    private final QRPUpdater qrpUpdater = new QRPUpdater();

    public final FMInspectables inspectables = new FMInspectables();
    
    /** Contains the definition of a rare file */
    private final RareFileDefinition rareDefinition;
    
    protected final FileManagerController fileManagerController;
    
	/**
	 * Creates a new <tt>FileManager</tt> instance.
	 */
    @Inject
    public FileManager(FileManagerController fileManagerController) {
        this.fileManagerController = fileManagerController;
        // We'll initialize all the instance variables so that the FileManager
        // is ready once the constructor completes, even though the
        // thread launched at the end of the constructor will immediately
        // overwrite all these variables
        rareDefinition = new RareFileDefinition();
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
        // the transient files and the special files.
        _individualSharedFiles = Collections.synchronizedCollection(
        		new MultiCollection<File>(_transientSharedFiles, _data.SPECIAL_FILES_TO_SHARE));
        _individualStoreFiles = Collections.synchronizedCollection(
                _data.SPECIAL_STORE_FILES);
    }

    /** Asynchronously loads all files by calling loadSettings.  Sets this's
     *  callback to be "callback", and notifies "callback" of all file loads.
     *      @modifies this
     *      @see loadSettings */
    public void start() {
        _data.clean();
        cleanIndividualFiles();
		loadSettings();
		fileManagerController.addSimppListener(qrpUpdater);
    }
    
    /**
     * Invokes {@link #start()} and waits for <code>timeout</code>
     * milliseconds for the initialization to finish.
     * 
     * @param timeout timeout in milliseconds
     * @throws InterruptedException if interrupted while waiting
     * @throws TimeoutException if timeout elapsed before initialization completed 
     */
    public void startAndWait(long timeout) throws InterruptedException, TimeoutException {
        final CountDownLatch startedLatch = new CountDownLatch(1);
        FileEventListener listener = new FileEventListener() {
            public void handleFileEvent(FileManagerEvent evt) {
                if (evt.getType() == Type.FILEMANAGER_LOADED) {
                    startedLatch.countDown();
                }
            }            
        };
        try {
            addFileEventListener(listener);
            start();
            if (!startedLatch.await(timeout, TimeUnit.MILLISECONDS)) {
                throw new TimeoutException("Initialization of FileManager did not complete within " + timeout + " ms");
            }
        } finally {
            removeFileEventListener(listener);
        }
    }
    
    public void stop() {
        save();
        fileManagerController.removeSimppListener(qrpUpdater);
        shutdown = true;
    }

    protected void save(){
        _data.save();
        fileManagerController.save();
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
            assert fd != null : "Directory has null entry";
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
            assert fd != null : "Directory has null entry";
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
        
        LOADER.execute(new Runnable() {
            public void run() {
                loadStarted(currentRevision);
                loadSettingsInternal(currentRevision);
            }
        });
    }
    
    public void loadSettingsAndWait(long timeout) throws InterruptedException, TimeoutException {
        final CountDownLatch loadedLatch = new CountDownLatch(1);
        FileEventListener listener = new FileEventListener() {
            public void handleFileEvent(FileManagerEvent evt) {
                if (evt.getType() == Type.FILEMANAGER_LOADED) {
                    loadedLatch.countDown();
                }
            }            
        };
        try {
            addFileEventListener(listener);
            loadSettings();
            if (!loadedLatch.await(timeout, TimeUnit.MILLISECONDS)) {
                throw new TimeoutException("Loading of FileManager settings did not complete within " + timeout + " ms");
            }
        } finally {
            removeFileEventListener(listener);
        }
    }

    /**
     * Loads the FileManager with a new list of directories.
     */
    public void loadWithNewDirectories(Set<? extends File> shared, Set<File> blackListSet) {
        SharingSettings.DIRECTORIES_TO_SHARE.setValue(shared);
        synchronized(_data.DIRECTORIES_NOT_TO_SHARE) {
            _data.DIRECTORIES_NOT_TO_SHARE.clear();
            _data.DIRECTORIES_NOT_TO_SHARE.addAll(canonicalize(blackListSet));
        }
	    loadSettings();
    }
    
    /**
     * Kicks off necessary stuff for a load being started.
     */
    protected void loadStarted(int revision) {
        fileManagerController.loadStarted();
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
        fileManagerController.loadFinished();
        save();
        fileManagerController.loadFinishedPostSave();
        dispatchFileEvent(new FileManagerEvent(this, Type.FILEMANAGER_LOADED));
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
        fileManagerController.fileManagerLoading();
        dispatchFileEvent(new FileManagerEvent(this, Type.FILEMANAGER_LOADING));
        
        // Update the FORCED_SHARE directory.
        updateSharedDirectories(PROGRAM_SHARE, null, revision);
        updateSharedDirectories(PREFERENCE_SHARE, null, revision);
        
        //Load the shared directories and add their files.
        _isUpdating = true;
        for(int i = 0; i < directories.length && _revision == revision; i++)
            updateSharedDirectories(directories[i], null, revision);          

        // Add specially shared files
        Collection<File> specialFiles = _individualSharedFiles;
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
        
        // Update the store directory and add only files from the LWS
        File storeDir = SharingSettings.getSaveLWSDirectory();
        updateStoreDirectories(storeDir.getAbsoluteFile(), null, revision);
        
        Collection<File> specialStoreFiles = _individualStoreFiles;
        ArrayList<File> storeList;
        synchronized (specialStoreFiles) {
            storeList = new ArrayList<File>(specialStoreFiles);
        }
        
        for(File file: storeList) {
            if(_revision != revision)
                break;
            addStoreFile(file, EMPTY_DOCUMENTS, true, revision, null);
        }
        
        _isUpdating = false;

        trim();
        
        if(LOG.isDebugEnabled())
            LOG.debug("Finished queueing shared files for revision: " + revision);
            
        synchronized (this) {
            _updatingFinished = revision;
            if(_numPendingFiles == 0) // if we didn't even try adding any files, pending is finished also.
                _pendingFinished = revision;
        }
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
		// Do not share unsharable directories.
        if(!isFolderShareable(directory, true))
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
                if (!fileManagerController.warnAboutSharingSensitiveDirectory(directory))
                    return;
            }
        }
		
        // Exit quickly (without doing the dir lookup) if revisions changed.
        if(_revision != revision)
            return;

        
//        for( File file: directory.listFiles())
//            if( _individualStoreFiles.contains( file ) )
//                directory.
        // STEP 1:
        // Add directory
        boolean isForcedShare = isForcedShareDirectory(directory);
        
        synchronized (this) {
            // if it was already added, ignore.
            if (_completelySharedDirectories.contains(directory))
                return;

//            if(LOG.isDebugEnabled())
//                LOG.debug("Adding completely shared directory: " + directory);

			_completelySharedDirectories.add(directory);
            if (!isForcedShare) {
                dispatchFileEvent(
                    new FileManagerEvent(this, Type.ADD_FOLDER, directory, parent));
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

    //TODO: finish this
    private void updateStoreDirectories(File directory, File parent, int revision) {
        //We have to get the canonical path to make sure "D:\dir" and "d:\DIR"
        //are the same on Windows but different on Unix.
        try {
            directory = FileUtils.getCanonicalFile(directory);
        } catch (IOException e) {
            return;
        }
        
        if(!directory.exists())
            return;
        
        
        // Exit quickly (without doing the dir lookup) if revisions changed.
        if(_revision != revision)
            return;

        
        synchronized (this) {
            // if it was already added, ignore.
            dispatchFileEvent(
                    new FileManagerEvent(this, Type.ADD_STORE_FOLDER, directory, parent));
        }
        
        // STEP 2:
        // Scan subdirectory for the amount of shared files.
        File[] file_list = directory.listFiles();
        if (file_list == null)
            return;
        for(int i = 0; i < file_list.length && _revision == revision; i++)
            addStoreFile(file_list[i], EMPTY_DOCUMENTS, true, revision, null);
            
        // Exit quickly (without doing the dir lookup) if revisions changed.
        if(_revision != revision)
            return;

        // STEP 3:
        // Recursively add subdirectories.
        // This has the effect of ensuring that the number of pending files
        // is closer to correct number.
        
        // Do not share subdirectories of the forcibly shared dir.
        File[] dir_list = directory.listFiles(DIRECTORY_FILTER);
        if(dir_list != null) {
            for(int i = 0; i < dir_list.length && _revision == revision; i++)
                updateStoreDirectories(dir_list[i], directory, revision);
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
                // Add the directory in the exclude list if it wasn't in the DIRECTORIES_NOT_TO_SHARE,
                // or if it was *and* a parent folder of it is fully shared.
                boolean explicitlyShared = SharingSettings.DIRECTORIES_TO_SHARE.remove(folder);
                if(!explicitlyShared || isCompletelySharedDirectory(folder.getParentFile()))
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
                    else if(f.isFile() && !_individualSharedFiles.contains(f)) {
                        if(removeFileIfShared(f) == null)
                            fileManagerController.clearPendingShare(f);
                    }
                }
            }
            
            // send the event last.  this is a hack so that the GUI can properly
            // receive events with the children first, moving any leftover children up to
            // potential parent directories.
            dispatchFileEvent(
                new FileManagerEvent(this, Type.REMOVE_FOLDER, folder));
        }
    }
    
	/**
	 * Adds a set of folders to be shared and a black list of subfolders that should
	 * not be shared.
	 * 
	 * @param folders set of folders to  be shared
	 * @param blackListedSet the subfolders or subsubfolders that are not to be
	 * shared
	 */
	public void addSharedFolders(Set<File> folders, Set<File> blackListedSet) {
		if (folders.isEmpty()) {
			throw new IllegalArgumentException("Only blacklisting without sharing, not allowed");
		}
	    _data.DIRECTORIES_NOT_TO_SHARE.addAll(canonicalize(blackListedSet));
	    for (File folder : folders) {
	    	addSharedFolder(folder);
	    }
	}
	
	/**
	 * Returns set of canonicalized files or the same set if there
	 * was an IOException for one of the files while canconicalizing. 
	 */
	private static Set<File> canonicalize(Set<File> files) {
	    // canonicalize blacklist
        Set<File> canonical = new HashSet<File>(files.size());
        try {
            for (File excluded : files) {
                canonical.add(FileUtils.getCanonicalFile(excluded));
            }
        } catch (IOException ie) {
            // use original black list if we run into problems
            canonical = files;
        }
        return canonical;
	}
	
	public Set<File> getFolderNotToShare() {
	    synchronized (_data.DIRECTORIES_NOT_TO_SHARE) {
	        return new HashSet<File>(_data.DIRECTORIES_NOT_TO_SHARE);
	    }
	}
	
    /**
     * Adds a given folder to be shared.
     */
    public boolean addSharedFolder(File folder) {
		if (!folder.isDirectory())
			throw new IllegalArgumentException("Expected a directory, but given: "+folder);
	
        try {
            folder = FileUtils.getCanonicalFile(folder);
        } catch(IOException ignored) {}
        
        if(!isFolderShareable(folder, false))
            return false;
        
        _data.DIRECTORIES_NOT_TO_SHARE.remove(folder);
		if (!isCompletelySharedDirectory(folder.getParentFile()))
			SharingSettings.DIRECTORIES_TO_SHARE.add(folder);
        _isUpdating = true;
        updateSharedDirectories(folder, null, _revision);
        _isUpdating = false;
        
        return true;
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
	  * adds a file that will be shared during this session of limewire
	  * only.
	  */
	 public void addFileForSession(File file) {
		 addFileForSession(file, null);
	 }
     
     /**
      * adds a file that will be shared during this session of limewire
      * only.
      * 
      * The listener is notified if this file could or couldn't be shared.
      */
     public void addFileForSession(File file, FileEventListener callback) {
         _data.FILES_NOT_TO_SHARE.remove(file);
         if (!isFileShareable(file))
             _transientSharedFiles.add(file);
         addFileIfShared(file, EMPTY_DOCUMENTS, true, _revision, callback);
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
        // test the license type to see if it can be shared
        boolean shareable = isShareable(metadata, file);
        if( !shareable ) {
            stopSharingFile(file);
            // add it to the list of files store files to display
            _data.SPECIAL_STORE_FILES.add(file);
            return;
        }
        
        if(LOG.isDebugEnabled())
            LOG.debug("Attempting to share file: " + file);
        if(callback == null)
            callback = EMPTY_CALLBACK;

        if(revision != _revision) {
            callback.handleFileEvent(new FileManagerEvent(this, Type.ADD_FAILED_FILE, file));
            return;
        }
        
        // Make sure capitals are resolved properly, etc.
        try {
            file = FileUtils.getCanonicalFile(file);
        } catch (IOException e) {
            callback.handleFileEvent(new FileManagerEvent(this, Type.ADD_FAILED_FILE, file));
            return;
	    }
	    
        synchronized(this) {
		    if (revision != _revision) {
		    	callback.handleFileEvent(new FileManagerEvent(this, Type.ADD_FAILED_FILE, file));
                return;
            }
			// if file is not shareable, also remove it from special files
			// to share since in that case it's not physically shareable then
		    if (!isFileShareable(file)) {
		    	_individualSharedFiles.remove(file);
		    	callback.handleFileEvent(new FileManagerEvent(this, Type.ADD_FAILED_FILE, file));
                return;
		    }
        
            if(isFileShared(file)) {
                callback.handleFileEvent(new FileManagerEvent(this, Type.ALREADY_SHARED_FILE, file));
                return;
            }
            
            _numPendingFiles++;
            // make sure _pendingFinished does not hold _revision
            // while we're still adding files
            _pendingFinished = -1;
        }

		fileManagerController.calculateAndCacheUrns(file, getNewUrnCallback(file, metadata, notify, revision, callback));
    }
    
    protected void addStoreFile(File file, List<? extends LimeXMLDocument> metadata, boolean notify,
            int revision, FileEventListener callback){
        
        if(LOG.isDebugEnabled())
            LOG.debug("Attempting to share file: " + file);
        if(callback == null)
            callback = EMPTY_CALLBACK;

        if(revision != _revision) {
            callback.handleFileEvent(new FileManagerEvent(this, Type.ADD_STORE_FAILED_FILE, file));
            return;
        }
        
        // Make sure capitals are resolved properly, etc.
        try {
            file = FileUtils.getCanonicalFile(file);
        } catch (IOException e) {
            callback.handleFileEvent(new FileManagerEvent(this, Type.ADD_STORE_FAILED_FILE, file));
            return;
        }
        
        synchronized(this) {
            if (revision != _revision) {
                callback.handleFileEvent(new FileManagerEvent(this, Type.ADD_STORE_FAILED_FILE, file));
                return;
            }
//            // if file is not shareable, also remove it from special files
//            // to share since in that case it's not physically shareable then
//            if (!isFileShareable(file)) {
//                _individualSharedFiles.remove(file);
//                callback.handleFileEvent(new FileManagerEvent(this, Type.ADD_STORE_FAILED_FILE, file));
//                return;
//            }
        
//            if(isFileShared(file)) {
//                callback.handleFileEvent(new FileManagerEvent(this, Type.ALREADY_SHARED_FILE, file));
//                return;
//            }
            
            _numPendingFiles++;
            // make sure _pendingFinished does not hold _revision
            // while we're still adding files
            _pendingFinished = -1;
        }
        synchronized (this) {
            // if it was already added, ignore.
            dispatchFileEvent(
                    new FileManagerEvent(this, Type.ADD_STORE_FILE, file, new File(file.getParent())));
        }
//        System.out.println("here " + file.getName());
//        getNewStoreCallback(file, metadata, notify, revision, callback);
//        fileManagerController.calculateAndCacheUrns(file, getNewStoreCallback(file, metadata, notify, revision, callback));
    }
    
    public boolean isShareable(final List<? extends LimeXMLDocument> metadata, File file ) { 
        if( file == null )
            throw new IllegalArgumentException("File can't be null");
        if( metadata == null )
            return true;
        if( _data.SPECIAL_STORE_FILES.contains(file) )
                return false;
        for( LimeXMLDocument doc : metadata ) {
            if( doc != null && doc.getLicenseString() != null && 
                    doc.getLicenseString().equals(LicenseType.LIMEWIRE_STORE_PURCHASE.toString()) )
            {
//                removeFileIfShared(file);
                // remove it from being shared
//                stopSharingFile(file);
//                // add it to the list of files store files to display
//                _data.SPECIAL_STORE_FILES.add(file);
                return false;
            }
        }
        return true;
    }
    
    /**
     * Constructs a new UrnCallback that will possibly load the file with the given URNs.
     */
    protected UrnCallback getNewStoreCallback(final File file, final List<? extends LimeXMLDocument> metadata, final boolean notify,
                                            final int revision, final FileEventListener callback) {
        return new UrnCallback() {
            public void urnsCalculated(File f, Set<? extends URN> urns) {
//              if(LOG.isDebugEnabled())
//                  LOG.debug("URNs calculated for file: " + f);
                
                FileDesc fd = null;
                synchronized(FileManager.this) {
                    if(revision != _revision) {
                        LOG.warn("Revisions changed, dropping share.");
                        callback.handleFileEvent(new FileManagerEvent(FileManager.this, Type.ADD_STORE_FAILED_FILE, file));
                        return;
                    }
                
                    _numPendingFiles--;
                    
                    // Only load the file if we were able to calculate URNs and
                    // the file is still shareable.
                    if(!urns.isEmpty()) {//)&& isFileShareable(file)) {
                        fd = addFile(file, urns);
//                        fd = new FileDesc(file, urns, _files.size());
//                        
//                        long fileLength = file.length();
//                        _filesSize += fileLength;        
//                        _files.add(fd);
//                        _fileToFileDescMap.put(file, fd);
//                        _numFiles++;
                    }
                }
                    
                if(fd != null) {
                    loadFile(fd, file, metadata, urns);

//                    // test the license type to see if it can be shared
//                    boolean shareable = isShareable(fd.getLimeXMLDocuments(), file);
//                    if( shareable ) {
//////                        stopSharingFile(file);
//////                        // add it to the list of store files to display
////                        _data.SPECIAL_STORE_FILES.add(file);
//                        return;
//                    }
                    
                    FileManagerEvent evt = new FileManagerEvent(FileManager.this, Type.ADD_STORE_FILE, file);
                    if(notify) // sometimes notify the GUI
                        dispatchFileEvent(evt);
                    callback.handleFileEvent(evt); // always notify the individual callback.
                } else {
                    System.out.println("failing");
                    return;
                    // If URNs was empty, or loading failed, notify...
//                    callback.handleFileEvent(new FileManagerEvent(FileManager.this, Type.ADD_STORE_FAILED_FILE, file));
                }
                
                boolean finished = false;
                synchronized (this) {
                    if(_numPendingFiles == 0) {
                        _pendingFinished = revision;
                        finished = true;
                    }
                }
                
                if (finished) {
                    tryToFinish();
                }
            }
            
            public boolean isOwner(Object o) {
                return o == fileManagerController;
            }
        };
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
                        callback.handleFileEvent(new FileManagerEvent(FileManager.this, Type.ADD_FAILED_FILE, file));
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

                    // test the license type to see if it can be shared
                    boolean shareable = isShareable(fd.getLimeXMLDocuments(), file);
                    System.out.println("shareable " + shareable);
                    if( !shareable ) {
                        stopSharingFile(file);
                        // add it to the list of files store files to display
                        _data.SPECIAL_STORE_FILES.add(file);
                        return;
                    }
                    
                    FileManagerEvent evt = new FileManagerEvent(FileManager.this, Type.ADD_FILE, fd);
                    if(notify) // sometimes notify the GUI
                        dispatchFileEvent(evt);
                    callback.handleFileEvent(evt); // always notify the individual callback.
                } else {
                    // If URNs was empty, or loading failed, notify...
                    callback.handleFileEvent(new FileManagerEvent(FileManager.this, Type.ADD_FAILED_FILE, file));
                }
                
                boolean finished = false;
                synchronized (this) {
                    if(_numPendingFiles == 0) {
                        _pendingFinished = revision;
                        finished = true;
                    }
                }
                
                if (finished) {
                    tryToFinish();
                }
            }
            
            public boolean isOwner(Object o) {
                return o == fileManagerController;
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
        if(LOG.isDebugEnabled())
            LOG.debug("Sharing file: " + file);
        

        int fileIndex = _files.size();
        FileDesc fileDesc = new FileDesc(file, urns, fileIndex);
        ContentResponseData r = fileManagerController.getResponseDataFor(fileDesc.getSHA1Urn());
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
        assert parent != null : "Null parent to \""+file+"\"";
        
        // Check if file is a specially shared file.  If not, ensure that
        // it is located in a shared directory.
		IntSet siblings = _sharedDirectories.get(parent);
		if (siblings == null) {
			siblings = new IntSet();
			_sharedDirectories.put(parent, siblings);
		}
		
		boolean added = siblings.add(fileIndex);
		assert added : "File "+fileIndex+" already found in "+siblings;
        
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
            fileManagerController.fileAdded(file, fileDesc.getSHA1Urn());
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
		boolean removed = _individualSharedFiles.remove(file); 
		FileDesc fd = removeFileIfShared(file);
		if (fd == null) {
            fileManagerController.clearPendingShare(file);
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
        assert _files.get(i).getFile().equals(f) : "invariant broken!";
        
        _files.set(i, null);
        _fileToFileDescMap.remove(f);
        _needRebuild = true;

        // If it's an incomplete file, the only reference we 
        // have is the URN, so remove that and be done.
        // We also return false, because the file was never really
        // "shared" to begin with.
        if (fd instanceof IncompleteFileDesc) {
            removeUrnIndex(fd);
            _numIncompleteFiles--;
            boolean removed = _incompletesShared.remove(i);
            assert removed : "File "+i+" not found in " + _incompletesShared;

			// Notify the GUI...
	        if (notify) {
	            FileManagerEvent evt = new FileManagerEvent(this, Type.REMOVE_FILE, fd);
	                                            
	            dispatchFileEvent(evt);
	        }
            return fd;
        }

        _numFiles--;
        _filesSize -= fd.getFileSize();

        //Remove references to this from directory listing
        File parent = f.getParentFile();
        IntSet siblings = _sharedDirectories.get(parent);
        assert siblings != null : "Removed file's directory \""+parent+"\" not in "+_sharedDirectories;
        boolean removed = siblings.remove(i);
        assert removed : "File "+i+" not found in "+siblings;

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
        removeUrnIndex(fd);
  
        // Notify the GUI...
        if (notify) {
            FileManagerEvent evt = new FileManagerEvent(this, Type.REMOVE_FILE, fd);
                                            
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
                                               long size,
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
        //TODO: check here for lws files
        
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
        dispatchFileEvent(new FileManagerEvent(this, Type.ADD_FILE, ifd));
    }

    /**
     * Notification that a file has changed and new hashes should be
     * calculated.
     */
    public abstract void fileChanged(File f);
    
    /** Attempts to validate the given FileDesc. */
    public void validate(final FileDesc fd) {
        if(_requestingValidation.add(fd.getSHA1Urn())) {
            fileManagerController.requestValidation(fd.getSHA1Urn(), new ContentResponseObserver() {
               public void handleResponse(URN urn, ContentResponseData r) {
                   _requestingValidation.remove(fd.getSHA1Urn());
                   if(r != null && !r.isOK())
                       removeFileIfShared(fd.getFile());
               }
            });
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
            assert indices!=null : "Invariant broken";

            //Delete index from set.  Remove set if empty.
            indices.remove(fileDesc.getIndex());
            if (indices.size()==0) {
                fileManagerController.lastUrnRemoved(urn);
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
            FileManagerEvent evt = new FileManagerEvent(this, Type.ADD_FAILED_FILE, oldName);
            dispatchFileEvent(evt);
            if(callback != null)
                callback.handleFileEvent(evt);
            return;
        }
        
        if(LOG.isDebugEnabled())
            LOG.debug("Attempting to rename: " + oldName + " to: "  + newName);
            
        List<LimeXMLDocument> xmlDocs = new LinkedList<LimeXMLDocument>(toRemove.getLimeXMLDocuments());
		final FileDesc removed = removeFileIfShared(oldName, false);
        assert removed == toRemove : "invariant broken.";
		if (_data.SPECIAL_FILES_TO_SHARE.remove(oldName) && !isFileInCompletelySharedDirectory(newName))
			_data.SPECIAL_FILES_TO_SHARE.add(newName);
			
        // Prepopulate the cache with new URNs.
        fileManagerController.addUrns(newName, removed.getUrns());

        addFileIfShared(newName, xmlDocs, false, _revision, new FileEventListener() {
            public void handleFileEvent(FileManagerEvent evt) {
                if(LOG.isDebugEnabled())
                    LOG.debug("Add of newFile returned callback: " + evt);

                // Retarget the event for the GUI.
                FileManagerEvent newEvt = null;
                if(evt.isAddEvent()) {
                    FileDesc fd = evt.getFileDescs()[0];
                    newEvt = new FileManagerEvent(FileManager.this, Type.RENAME_FILE, removed, fd);
                } else {
                    newEvt = new FileManagerEvent(FileManager.this, Type.REMOVE_FILE, removed);
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
    
    public boolean hasIndividualStoreFiles() {
        return !_individualStoreFiles.isEmpty();
    }
    
    /**
     * @return true if currently we have any files that are 
     * shared by the application.
     */
    public boolean hasApplicationSharedFiles() {
    	File [] files = APPLICATION_SPECIAL_SHARE.listFiles();
    	if (files == null)
    		return false;
    	
    	// if at least one of the files in the application special
    	// share are currently shared, return true.
    	for (File f: files) {
    		if (isFileShared(f))
    			return true;
    	}
    	
    	return false;
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
    
    public File[] getIndividualStoreFiles() {
        Set<File> candidates = _data.SPECIAL_STORE_FILES;
        synchronized (candidates) {
            ArrayList<File> files = new ArrayList<File>(candidates.size());
            for(File f : candidates) {
            //    System.out.println(f.getName() + " " + f.isFile());
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
            && isFilePhysicallyShareable(f)
            && !isApplicationSpecialShare(f);
    }
    
    public boolean isFolderShared(File f) {
        return _sharedDirectories.containsKey(f);
    }
    
    /**
     * Cleans all stale entries from the Set of individual files.
     */
    private void cleanIndividualFiles() {
        Set<File> files = _data.SPECIAL_FILES_TO_SHARE;
        synchronized(files) {
            for(Iterator<File> i = files.iterator(); i.hasNext(); ) {
                File f = i.next();
                if(!(isFilePhysicallyShareable(f)))
                    i.remove();
            }
        }
    }

	/**
	 * Returns true if the given file is shared by the FileManager.
	 * The provided file should be in canonical form. 
	 */
	public synchronized boolean isFileShared(File file) {
		if (file == null)
			return false;
		if (_fileToFileDescMap.get(file) == null)
			return false;
		return true;
	}
    
    public boolean isRareFile(FileDesc fd) {
        return rareDefinition.evaluate(fd);
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
		if (!isFilePhysicallyShareable(file))
			return false;
		if (_individualSharedFiles.contains(file))
			return true;
		if (_data.FILES_NOT_TO_SHARE.contains(file))
			return false;
        if (_individualStoreFiles.contains(file))
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
    
    public boolean isFileStore(File file) {
        if(_individualStoreFiles.contains(file))
            return true;
        return false;
    }
    
    /**
     * Returns true if this file is not too large, not too small,
     * not null, is a directory, can be read, is not hidden.  
     * Returns false otherwise.
     */
    public static boolean isFilePhysicallyShareable(File file) {
        if (file == null || !file.exists() || file.isDirectory() || !file.canRead() || file.isHidden() ) 
            return false;
                
        long fileLength = file.length();
        if (fileLength <= 0 || fileLength > MAX_FILE_SIZE) 
            return false;
        
        return true;
    }
    
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
    public boolean isFolderShareable(File folder, boolean includeExcludedDirectories) {
        if(!folder.isDirectory() || !folder.canRead())
            return false;
        
        if (folder.equals(SharingSettings.INCOMPLETE_DIRECTORY.getValue()))
            return false;
        
        if (isApplicationSpecialShareDirectory(folder)) {
            return false;
        }
        
        // Do not share directories on the do not share list
        if (includeExcludedDirectories && _data.DIRECTORIES_NOT_TO_SHARE.contains(folder))
            return false;
        
        //  check for system roots
        File[] faRoots = File.listRoots();
        if (faRoots != null && faRoots.length > 0) {
            for (int i = 0; i < faRoots.length; i++) {
                if (folder.equals(faRoots[i]))
                    return false;
            }
        }
        
        return true;
    }
	
    /**
     * Returns true iff <tt>file</tt> is a sensitive directory.
     */
    public static boolean isSensitiveDirectory(File folder) {
        if (folder == null)
            return false;

        String userHome = System.getProperty("user.home");
        if (folder.equals(new File(userHome)))
            return true;

        String[] sensitive;
        if (OSUtils.isWindows()) {
            sensitive = new String[] { "Documents and Settings",
                    "My Documents", "Desktop", "Program Files", "Windows",
                    "WINNT", "Users" };
        } else if (OSUtils.isMacOSX()) {
            sensitive = new String[] { "Users", "System", "System Folder",
                    "Previous Systems", "private", "Volumes", "Desktop",
                    "Applications", "Applications (Mac OS 9)", "Network" };
        } else if (OSUtils.isPOSIX()) {
            sensitive = new String[] { "bin", "boot", "dev", "etc", "home",
                    "mnt", "opt", "proc", "root", "sbin", "usr", "var" };
        } else {
            sensitive = new String[0];
        }

        String folderName = folder.getName();
        for (String name : sensitive) {
            if (folderName.equals(name))
                return true;
        }

        return false;
    }
    
    /**
     * Returns the QRTable. If the shared files have changed, then it will
     * rebuild the QRT. A copy is returned so that FileManager does not expose
     * its internal data structure.
     */
    public synchronized QueryRouteTable getQRT() {
        if(_needRebuild) {
            qrpUpdater.cancelRebuild();
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
        if (SearchSettings.SEND_LIME_RESPONSES.getBoolean()) {
            for (String entry : SearchSettings.LIME_QRP_ENTRIES.getValue())
                _queryRouteTable.addIndivisible(entry);
        }
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
            assert desc != null : "unexpected null in FileManager for query:\n"+ request;

            if ((filter != null) && !filter.allow(desc.getFileName()))
                continue;

            desc.incrementHitCount();
            fileManagerController.handleSharedFileUpdate(desc.getFile());

            Response resp = fileManagerController.createResponse(desc);
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
        List<URN> urnList = fileManagerController.getNewestUrns(request, 3);
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
            Response r = fileManagerController.createResponse(desc);
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
        
            assert j<ret.length : "_numFiles is too small";
            ret[j] = fileManagerController.createResponse(desc);
            if(includeXML)
                addXMLToResponse(ret[j], desc);
            j++;
        }
        assert j==ret.length : "_numFiles is too large";
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
     * Determines if this File is an application special share.
     */
    public static boolean isApplicationSpecialShare(File file) {
        File parent = file.getParentFile();
        return parent != null && isApplicationSpecialShareDirectory(parent);
    }
    
    /**
     * @return true if there exists an application-shared file with the
     * provided name.
     */
    public boolean isFileApplicationShared(String name) {
    	File file = new File(APPLICATION_SPECIAL_SHARE, name);
    	try {
    		file = FileUtils.getCanonicalFile(file);
    	} catch (IOException bad) {
    		return false;
    	}
    	return isFileShared(file);
    }
    
    /**
     * Determines if this File is a network shared directory.
     */
    public static boolean isForcedShareDirectory(File f) {
        return f.equals(PROGRAM_SHARE) || f.equals(PREFERENCE_SHARE);
    }
    
    public static boolean isApplicationSpecialShareDirectory(File directory) {
        return directory.equals(APPLICATION_SPECIAL_SHARE);
    }
    
    /**
     * registers a listener for FileManagerEvents
     */
    public void addFileEventListener(FileEventListener listener) {
        if (listener == null) {
            throw new NullPointerException("FileEventListener is null");
        }
        eventListeners.addIfAbsent(listener);
    }

    /**
     * unregisters a listener for FileManagerEvents
     */
    public void removeFileEventListener(FileEventListener listener) {
        if (listener == null) {
            throw new NullPointerException("FileEventListener is null");
        }
        eventListeners.remove(listener);
    }

    /**
     * dispatches a FileManagerEvent to any registered listeners 
     */
    protected void dispatchFileEvent(FileManagerEvent evt) {
        for(FileEventListener listener : eventListeners) {
            listener.handleFileEvent(evt);
        }
    }
    
    /** 
     * Returns an iterator for all shared files. 
     */
    public Iterator<Response> getIndexingIterator(final boolean includeXML) {
        return new Iterator<Response>() {
            int startRevision = _revision;
            /** Points to the index that is to be examined next. */
            int index = 0;
            Response preview;

            private boolean preview() {
                assert preview == null;

                if (_revision != startRevision) {
                    return false;
                }

                synchronized (FileManager.this) {
                    while (index < _files.size()) {
                        FileDesc desc = _files.get(index);
                        index++;

                        // skip, if the file was unshared or is an incomplete file,
                        if (desc == null || desc instanceof IncompleteFileDesc || isForcedShare(desc)) 
                            continue;

                        preview = fileManagerController.createResponse(desc);
                        if(includeXML)
                            addXMLToResponse(preview, desc);
                        return true;
                    }
                    return false;
                }
            }

            public boolean hasNext() {
                if (_revision != startRevision) {
                    return false;
                }

                if (preview != null) {
                    synchronized (FileManager.this) {
                        if (_files.get(index - 1) == null) {
                            // file was removed in the meantime
                            preview = null;
                        }
                    }
                }
                return preview != null || preview();
            }

            public Response next() {
                if (hasNext()) {
                    Response item = preview;
                    preview = null;
                    return item;
                }
                throw new NoSuchElementException();               
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }

        };
    }
        
    private class QRPUpdater implements SimppListener {
        private boolean buildInProgress;
        private final Set<String> qrpWords = new HashSet<String>();
        public QRPUpdater() {
            synchronized(this) {
                for (String entry : SearchSettings.LIME_QRP_ENTRIES.getValue())
                    qrpWords.add(entry);
            }
        }
        
        public synchronized void simppUpdated(int newVersion) {
            if (buildInProgress)
                return;
            
            Set<String> newWords = new HashSet<String>();
            for (String entry : SearchSettings.LIME_QRP_ENTRIES.getValue())
                newWords.add(entry);
            
            // any change in words?
            if (newWords.containsAll(qrpWords) && qrpWords.containsAll(newWords)) 
                return;
            
            qrpWords.clear();
            qrpWords.addAll(newWords);

            buildInProgress = true;
            
            // schedule a rebuild sometime in the next hour
            fileManagerController.scheduleWithFixedDelay(new Runnable() {
                public void run() {
                    synchronized(QRPUpdater.this) {
                        if (!buildInProgress)
                            return;
                        buildInProgress = false;
                        _needRebuild = true;
                    }
                }
            }, (int)(Math.random() * QRP_DELAY), 0, TimeUnit.MILLISECONDS);
        }
        
        public synchronized void cancelRebuild() {
            buildInProgress = false;
        }
    }

    /** A bunch of inspectables for FileManager */
    private class FMInspectables {
        /*
         * 1 - used to create smaller qrp table
         * 2 - just sends the current table
         */
        private static final int VERSION = 2;
        private void addVersion(Map<String, Object> m) {
            m.put("ver", VERSION);
        }

        /** An inspectable that returns some info about the QRP */
        public final Inspectable QRP = new Inspectable() {
            public Object inspect() {
                Map<String, Object> ret = new HashMap<String, Object>();
                addVersion(ret);

                synchronized(FileManager.this) {
                    ret.put("qrt",getQRT().getRawDump());
                }

                return ret;
            }
        };

        /** An inspectable that returns stats about hits, uploads & alts */
        public final Inspectable FDS = new FDInspectable(false);
        /** An inspectable that returns stats about hits, uploads & alts > 0 */
        public final Inspectable FDSNZ = new FDInspectable(true);
        
        /** An inspectable that counts how many shared fds match a custom criteria */
        public final Inspectable CUSTOM = new Inspectable() {
            public Object inspect() {
                Map<String, Object> ret = new HashMap<String,Object>();
                ret.put("ver",1);
                ret.put("crit", MessageSettings.CUSTOM_FD_CRITERIA.getValueAsString());
                int total = 0;
                int matched = 0;
                try {
                    RPNParser parser = new RPNParser(MessageSettings.CUSTOM_FD_CRITERIA.getValue());
                    synchronized(FileManager.this) {
                        for (FileDesc fd : getAllSharedFileDescriptors()) {
                            total++;
                            if (parser.evaluate(fd))
                                matched++;
                        }
                    }
                } catch (IllegalArgumentException badSimpp) {
                    ret.put("error",badSimpp.toString());
                    return ret;
                }
                ret.put("match",matched);
                ret.put("total",total);
                return ret;
            }
        };
    }
    
    /** Inspectable with information about File Descriptors */
    private class FDInspectable implements Inspectable {
        private final boolean nonZero;
        /**
         * @param nonZero whether to return only results greater than 0
         */
        FDInspectable(boolean nonZero) {
            this.nonZero = nonZero;
        }
        
        public Object inspect() {
            Map<String, Object> ret = new HashMap<String, Object>();
            ret.put("ver", FMInspectables.VERSION);
            // the actual values
            ArrayList<Double> hits = new ArrayList<Double>();
            ArrayList<Double> uploads = new ArrayList<Double>();
            ArrayList<Double> completeUploads = new ArrayList<Double>();
            ArrayList<Double> alts = new ArrayList<Double>();
            ArrayList<Double> keywords = new ArrayList<Double>();
            
            // differences for t-test 
            ArrayList<Double> altsHits = new ArrayList<Double>();
            ArrayList<Double> altsUploads = new ArrayList<Double>();
            ArrayList<Double> hitsUpload = new ArrayList<Double>();
            ArrayList<Double> hitsKeywords = new ArrayList<Double>();
            ArrayList<Double> uploadsToComplete = new ArrayList<Double>();
            
            Map<Integer, FileDesc> topHitsFDs = new TreeMap<Integer, FileDesc>(Comparators.inverseIntegerComparator());
            Map<Integer, FileDesc> topUpsFDs = new TreeMap<Integer, FileDesc>(Comparators.inverseIntegerComparator());
            Map<Integer, FileDesc> topAltsFDs = new TreeMap<Integer, FileDesc>(Comparators.inverseIntegerComparator());
            Map<Integer, FileDesc> topCupsFDs = new TreeMap<Integer, FileDesc>(Comparators.inverseIntegerComparator());
            synchronized(FileManager.this) {
                FileDesc[] fds = getAllSharedFileDescriptors();
                hits.ensureCapacity(fds.length);
                uploads.ensureCapacity(fds.length);
                int rare = 0;
                int total = 0;
                for(int i = 0; i < fds.length; i++) {
                    if (fds[i] instanceof IncompleteFileDesc)
                        continue;
                    total++;
                    if (isRareFile(fds[i]))
                        rare++;
                    // locking FM->ALM ok.
                    int numAlts = fileManagerController.getAlternateLocationCount(fds[i].getSHA1Urn());
                    if (!nonZero || numAlts > 0) {
                        alts.add((double)numAlts);
                        topAltsFDs.put(numAlts,fds[i]);
                    }
                    int hitCount = fds[i].getHitCount();
                    if (!nonZero || hitCount > 0) {
                        hits.add((double)hitCount);
                        topHitsFDs.put(hitCount, fds[i]);
                    }
                    int upCount = fds[i].getAttemptedUploads();
                    if (!nonZero || upCount > 0) {
                        uploads.add((double)upCount);
                        topUpsFDs.put(upCount, fds[i]);
                    }
                    int cupCount = fds[i].getCompletedUploads();
                    if (!nonZero || cupCount > 0) {
                        completeUploads.add((double)upCount);
                        topCupsFDs.put(cupCount, fds[i]);
                    }
                    
                    // keywords per fd
                    double keywordsCount = 
                        HashFunction.getPrefixes(HashFunction.keywords(fds[i].getPath())).length;
                    keywords.add(keywordsCount);
                    
                    // populate differences
                    if (!nonZero) {
                        int index = hits.size() - 1;
                        hitsUpload.add(hits.get(index) - uploads.get(index));
                        altsHits.add(alts.get(index) - hits.get(index));
                        altsUploads.add(alts.get(index)  - uploads.get(index));
                        hitsKeywords.add(hits.get(index) - keywordsCount);
                        uploadsToComplete.add(uploads.get(index) - completeUploads.get(index));
                    }
                }
                ret.put("rare",Double.doubleToLongBits((double)rare / total));
            }
            ret.put("hits",StatsUtils.quickStatsDouble(hits).getMap());
            ret.put("hitsh", StatsUtils.getHistogram(hits, 10)); // small, will compress
            ret.put("ups",StatsUtils.quickStatsDouble(uploads).getMap());
            ret.put("upsh", StatsUtils.getHistogram(uploads, 10));
            ret.put("cups",StatsUtils.quickStatsDouble(completeUploads).getMap());
            ret.put("cupsh", StatsUtils.getHistogram(completeUploads, 10));
            ret.put("alts", StatsUtils.quickStatsDouble(alts).getMap());
            ret.put("altsh", StatsUtils.getHistogram(alts, 10));
            ret.put("kw", StatsUtils.quickStatsDouble(keywords).getMap());
            ret.put("kwh", StatsUtils.getHistogram(keywords, 10));
            
            // t-test values
            ret.put("hut",StatsUtils.quickStatsDouble(hitsUpload).getTTestMap());
            ret.put("aht",StatsUtils.quickStatsDouble(altsHits).getTTestMap());
            ret.put("aut",StatsUtils.quickStatsDouble(altsUploads).getTTestMap());
            ret.put("hkt",StatsUtils.quickStatsDouble(hitsKeywords).getTTestMap());
            ret.put("ucut",StatsUtils.quickStatsDouble(uploadsToComplete).getTTestMap());
            
            QueryRouteTable topHits = new QueryRouteTable();
            QueryRouteTable topUps = new QueryRouteTable();
            QueryRouteTable topCups = new QueryRouteTable();
            QueryRouteTable topAlts = new QueryRouteTable();
            Iterator<FileDesc> hitIter = topHitsFDs.values().iterator();
            Iterator<FileDesc> upIter = topUpsFDs.values().iterator();
            Iterator<FileDesc> cupIter = topCupsFDs.values().iterator();
            Iterator<FileDesc> altIter = topAltsFDs.values().iterator();
            for (int i = 0; i < 10; i++) {
                if (hitIter.hasNext())
                    topHits.add(hitIter.next().getPath());
                if (upIter.hasNext())
                    topUps.add(upIter.next().getPath());
                if (altIter.hasNext())
                    topAlts.add(altIter.next().getPath());
                if (cupIter.hasNext())
                    topCups.add(cupIter.next().getPath());
            }
            // we return all qrps, but since they will have very few entries
            // they will compress very well
            ret.put("hitsq",topHits.getRawDump());
            ret.put("upsq",topUps.getRawDump());
            ret.put("cupsq",topCups.getRawDump());
            ret.put("altsq",topAlts.getRawDump());
            
            return ret;
        }
        
    }
    
    private class RareFileDefinition implements SimppListener {
        
        private RPNParser parser;
        RareFileDefinition() {
            simppUpdated(0);
            // TODO cleanup listener leaking
            fileManagerController.addSimppListener(this);
        }
        
        public synchronized void simppUpdated(int ignored) {
            parser = new RPNParser(DHTSettings.RARE_FILE_DEFINITION.getValue());
        }
        
        private synchronized boolean evaluate(FileDesc fd) {
            try {
                return parser.evaluate(fd);
            } catch (IllegalArgumentException badSimpp) {
                return false;
            }
        }
    }
}