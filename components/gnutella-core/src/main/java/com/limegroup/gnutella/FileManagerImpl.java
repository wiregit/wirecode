package com.limegroup.gnutella;

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
import java.util.Locale;
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
import org.limewire.collection.MultiIterator;
import org.limewire.collection.StringTrie;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.inspection.Inspectable;
import org.limewire.inspection.InspectableContainer;
import org.limewire.inspection.InspectableForSize;
import org.limewire.inspection.InspectablePrimitive;
import org.limewire.inspection.InspectionPoint;
import org.limewire.setting.StringArraySetting;
import org.limewire.statistic.StatsUtils;
import org.limewire.util.ByteUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.I18NConvert;
import org.limewire.util.RPNParser;
import org.limewire.util.StringUtils;

import com.google.inject.Inject;
import com.limegroup.gnutella.FileManagerEvent.Type;
import com.limegroup.gnutella.auth.ContentResponseData;
import com.limegroup.gnutella.auth.ContentResponseObserver;
import com.limegroup.gnutella.downloader.VerifyingFile;
import com.limegroup.gnutella.library.LibraryData;
import com.limegroup.gnutella.library.SharingUtils;
import com.limegroup.gnutella.licenses.LicenseType;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.routing.HashFunction;
import com.limegroup.gnutella.routing.QueryRouteTable;
import com.limegroup.gnutella.settings.DHTSettings;
import com.limegroup.gnutella.settings.MessageSettings;
import com.limegroup.gnutella.settings.SearchSettings;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.simpp.SimppListener;
import com.limegroup.gnutella.util.LimeWireUtils;
import com.limegroup.gnutella.xml.LimeXMLDocument;

/**
 * The list of all shared files.  Provides operations to add and remove
 * individual files, directory, or sets of directories.  Provides a method to
 * efficiently query for files whose names contain certain keywords.<p>
 *
 * This class is thread-safe.
 */
//@Singleton // abstract, so not a singleton -- subclasses are.
public abstract class FileManagerImpl implements FileManager {
	
    private static final Log LOG = LogFactory.getLog(FileManagerImpl.class);

    private static final ExecutorService LOADER = ExecutorsHelper.newProcessingQueue("FileManagerLoader");
    
    /**
     * delay between qrp updates should the simpp words change.
     * Not final for testing.  Betas update faster for experiments.
     */
    private static long QRP_DELAY = (LimeWireUtils.isBetaRelease() ? 1 : 60) * 60 * 1000;
     
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
     * The list of store files. 
     */
    private List<FileDesc> _storeFiles;
    
    /**
     * The total size of all complete files, in bytes.
     * INVARIANT: _filesSize=sum of all size of the elements of _files,
     *   except IncompleteFileDescs, whose size may change at any time.
     */
    @InspectablePrimitive("total size of shared files")
    private long _filesSize;
    
    /**
     * The number of complete files.
     * INVARIANT: _numFiles==number of elements of _files that are not null
     *  and not IncompleteFileDescs.
     */
    @InspectablePrimitive("number of shared files")
    private int _numFiles;
    
    /** 
     * The total number of files that are pending sharing.
     *  (ie: awaiting hashing or being added)
     */
    @InspectablePrimitive("number of pending files")
    private int _numPendingFiles;
    
    /**
     * The total number of incomplete files.
     * INVARIANT: _numFiles + _numIncompleteFiles == the number of
     *  elements of _files that are not null.
     */
    @InspectablePrimitive("number of incomplete files")
    private int _numIncompleteFiles;
    
    /**
     * The number of files that are forcibly shared over the network.
     * INVARIANT: _numFiles >= _numForcedFiles.
     */
    @InspectablePrimitive("number force-shared files")
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
    @InspectableForSize("size of keyword trie")
    private StringTrie<IntSet> _keywordTrie;
    
    /**
     * A trie mapping keywords in complete filenames to the indices in _files.
     * Contains ONLY incomplete keywords.
     */
    @InspectableForSize("size of incomplete keyword trie")
    private StringTrie<IntSet> _incompleteKeywordTrie;
    
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
     * A map of appropriately case-normalized URN strings to the
     * indices of _store files. Needed to allow store file modification
     * to happen.
     */
    private Map<URN, IntSet> _urnStoreMap;
    
    /**
     * The set of file extensions to share, sorted by StringComparator. 
     * INVARIANT: all extensions are lower case.
     */
    private static Set<String> _extensions;
    
	/**
	 * A Set of shared directories that are completely shared.  Files in these
	 * directories are shared by default and will be shared unless the File is
	 * listed in SharingSettings.FILES_NOT_TO_SHARE.
	 */
    @InspectableForSize("number completely shared directories")
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
    @InspectableForSize("number incompletely shared files")
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
    @InspectableForSize("number of transiently shared files")
    private Set<File> _transientSharedFiles = new HashSet<File>();
    
    /**
     * An index that maps a LWS <tt>File</tt> on disk to the 
     *  <tt>FileDesc</tt> holding it.
     *
     * INVARIANT: For all keys k in _fileToFileDescMap, 
     *  _files[_fileToFileDescMap.get(k).getIndex()].getFile().equals(k)
     *
     * Keys must be canonical <tt>File</tt> instances.
     */
    private Map<File, FileDesc> _storeToFileDescMap;

    /**
     *  The directory for downloading LWS songs to and any subdirectories
     *  that may recursively exist
     */    
    @InspectableForSize("number of directories for the store")
    private Set<File> _storeDirectories;
    
    /**
     * Individual files that are not in a shared folder.
     */
    @InspectableForSize("number of individually shared files")
    private Collection<File> _individualSharedFiles; 
    
    /**
     * The revision of the library.  Every time 'loadSettings' is called, the revision
     * is incremented.
     */
    @InspectablePrimitive("filemanager revision")
    protected volatile int _revision = 0;
    
    /**
     * The revision that finished loading all pending files.
     */
    @InspectablePrimitive("revision that finished loading")
    private volatile int _pendingFinished = -1;
    
    /**
     * The revision that finished updating shared directories.
     */
    private volatile int _updatingFinished = -1;
    
    /**
     * If true, indicates that the FileManager is currently updating.
     */
    @InspectablePrimitive("filemanager currently updating")
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
     *  Different types of files to be added to the filemanager
     */
    public enum AddType{

        ADD_SHARE(Type.ADD_FILE, Type.ADD_FAILED_FILE),
        ADD_STORE(Type.ADD_STORE_FILE, Type.ADD_STORE_FAILED_FILE);
        
        private final Type success;
        private final Type failure;
        
        AddType(Type success, Type failure) {
            this.success = success;
            this.failure = failure;
        }
        
        public Type getSuccessType(){
            return success;
        }
        
        public Type getFailureType(){
            return failure;
        }
    };
    
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

    /** Contains the definition of a rare file */
    private final RareFileDefinition rareDefinition;
    
    protected final FileManagerController fileManagerController;
    
	/**
	 * Creates a new <tt>FileManager</tt> instance.
	 */
    @Inject
    public FileManagerImpl(FileManagerController fileManagerController) {
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
        _storeFiles = new ArrayList<FileDesc>();
        _keywordTrie = new StringTrie<IntSet>(true);  //ignore case
        _incompleteKeywordTrie = new StringTrie<IntSet>(true);
        _urnMap = new HashMap<URN, IntSet>();
        _urnStoreMap = new HashMap<URN, IntSet>();
        _extensions = new HashSet<String>();
		_completelySharedDirectories = new HashSet<File>();
        _incompletesShared = new IntSet();
        _fileToFileDescMap = new HashMap<File, FileDesc>();
        // the transient files and the special files.
        _individualSharedFiles = Collections.synchronizedCollection(
        		new MultiCollection<File>(_transientSharedFiles, _data.SPECIAL_FILES_TO_SHARE));
        _storeToFileDescMap = new HashMap<File, FileDesc>();
        _storeDirectories = new HashSet<File>();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#start()
     */
    public void start() {
        _data.clean();
        cleanIndividualFiles();
		loadSettings();
		fileManagerController.addSimppListener(qrpUpdater);
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#startAndWait(long)
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
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#stop()
     */
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
		
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#getSize()
     */
    public int getSize() {
		return ByteUtils.long2int(_filesSize); 
	}

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#getNumFiles()
     */
    public int getNumFiles() {
		return _numFiles - _numForcedFiles;
	}
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#getNumStoreFiles()
     */
    public int getNumStoreFiles() {
        return _storeToFileDescMap.size();
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#getNumIncompleteFiles()
     */
    public int getNumIncompleteFiles() {
        return _numIncompleteFiles;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#getNumPendingFiles()
     */
    public int getNumPendingFiles() {
        return _numPendingFiles;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#getNumForcedFiles()
     */
    public int getNumForcedFiles() {
        return _numForcedFiles;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#get(int)
     */
    public synchronized FileDesc get(int i) {
        return _files.get(i);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#isValidIndex(int)
     */
    public synchronized boolean isValidSharedIndex(int i) {
        return (i >= 0 && i < _files.size());
    }


    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#getURNForFile(java.io.File)
     */    
    public synchronized URN getURNForFile(File f) {
        FileDesc fd = getFileDescForFile(f);
        if (fd != null) return fd.getSHA1Urn();
        return null;
    }


    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#getFileDescForFile(java.io.File)
     */
    public synchronized FileDesc getFileDescForFile(File f) {
        try {
            f = FileUtils.getCanonicalFile(f);
        } catch(IOException ioe) {
            return null;
        }
        if(_fileToFileDescMap.containsKey(f))
        	return _fileToFileDescMap.get(f);
        else
            return _storeToFileDescMap.get(f);
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#isUrnShared(com.limegroup.gnutella.URN)
     */
    public synchronized boolean isUrnShared(final URN urn) {
        FileDesc fd = getSharedFileDescForUrn(urn);
        return fd != null && !(fd instanceof IncompleteFileDesc);
    }

	/* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#getFileDescForUrn(com.limegroup.gnutella.URN)
     */
	public synchronized FileDesc getFileDescForUrn(final URN urn) {
        if (!urn.isSHA1())
            throw new IllegalArgumentException();
        
        if( _urnMap.get(urn) != null ) {
            return getSharedFileDescForUrn(urn);
        } else if ( _urnStoreMap.get(urn) != null ) {
            return getStoreFileDescForUrn(urn);
        } else {
            return null;
        }
	}
	
	/**
	 * Given a urn, attempts to locate a Shared FileDesc for that urn
	 */
	public synchronized FileDesc getSharedFileDescForUrn(final URN urn) {
        if (!urn.isSHA1())
            throw new IllegalArgumentException();
        
        if( _urnMap.get(urn) != null ) 
        	return getFileDescForUrn(urn, _urnMap, _files);
        else
        	return null;
	}
	
	/**
	 * Given a urn, attempts to locate a Store FileDesc for that urn
	 */
	private synchronized FileDesc getStoreFileDescForUrn(final URN urn) {
		return getFileDescForUrn(urn, _urnStoreMap, _storeFiles);
	}
	
	/**
	 * Given a URN returns the FileDesc associated with that URN.
	 */
	private synchronized FileDesc getFileDescForUrn(final URN urn, Map<URN, IntSet> map, List<FileDesc> files) {
	    IntSet indices = map.get(urn);
        if(indices == null) return null;
	    
        IntSet.IntSetIterator iter = indices.iterator();
	        
	    //Pick the first non-null non-Incomplete FileDesc.
	    FileDesc ret = null;
	    while ( iter.hasNext() 
	               && ( ret == null || ret instanceof IncompleteFileDesc) ) {
	        int index = iter.next();
	        ret = files.get(index);
	    }
	    return ret;
	}
	
	/* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#getIncompleteFileDescriptors()
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
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#getAllSharedFileDescriptors()
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
     * 
     * Returns null if directory is not shared, or a zero-length array if it is
     * shared but contains no files.  This method is not recursive; files in 
     * any of the directory's children are not returned.
     * 
     * This operation is <b>not</b> efficient, and should not be done often.
     */  
    public synchronized List<FileDesc> getSharedFilesInDirectory(File directory) {
        if (directory == null)
            throw new NullPointerException("null directory");
        
        // a. Remove case, trailing separators, etc.
        try {
            directory = FileUtils.getCanonicalFile(directory);
        } catch (IOException e) { // invalid directory ?
            return Collections.emptyList();
        }
        
        List<FileDesc> shared = new ArrayList<FileDesc>();
        for(FileDesc fd : _fileToFileDescMap.values()) {
            if(directory.equals(fd.getFile().getParentFile()))
                shared.add(fd);
        }
        
        return shared;
    }


    ///////////////////////////////////////////////////////////////////////////
    //  Loading 
    ///////////////////////////////////////////////////////////////////////////

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#loadSettings()
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
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#loadSettingsAndWait(long)
     */
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

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#loadWithNewDirectories(java.util.Set, java.util.Set)
     */
    public void loadWithNewDirectories(Set<? extends File> shared, Set<File> blackListSet) {
        SharingSettings.DIRECTORIES_TO_SHARE.setValue(shared);
        synchronized(_data.DIRECTORIES_NOT_TO_SHARE) {
            _data.DIRECTORIES_NOT_TO_SHARE.clear();
            _data.DIRECTORIES_NOT_TO_SHARE.addAll(canonicalize(blackListSet));
            _storeDirectories.clear();
            _storeDirectories.add(SharingSettings.getSaveLWSDirectory());
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
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#isLoadFinished()
     */
    public boolean isLoadFinished() {
        return _loadingFinished == _revision;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#isUpdating()
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
            String[] extensions = StringArraySetting.decode(SharingSettings.EXTENSIONS_TO_SHARE.getValue().toLowerCase(Locale.US));
                        
            for( String ext : extensions ) {
                _extensions.add(ext);
            }
            
            // Add any extra extensions per chance           
            if (SharingSettings.EXTENSIONS_LIST_CUSTOM.getValue().length() > 0) {
                String[] array = StringArraySetting.decode(SharingSettings.EXTENSIONS_LIST_CUSTOM.getValue());                
                for( String ext : array ) {
                    _extensions.add(ext);
                }
            }
            
            // Assert no sensitive extensions are shared            
            if (SharingSettings.DISABLE_SENSITIVE.getValue()) {
                for( String ext : SharingSettings.getDefaultDisabledExtensions() ) {
                    _extensions.remove(ext);
                }
            }
            
            // Assert no disabled extensions are shared            
            if (SharingSettings.EXTENSIONS_LIST_UNSHARED.getValue().length() > 0) {
                String[] array = StringArraySetting.decode(SharingSettings.EXTENSIONS_LIST_UNSHARED.getValue());                
                for( String ext : array ) {
                    _extensions.remove(ext);
                }
            }


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
        updateSharedDirectories(SharingUtils.PROGRAM_SHARE, null, revision);
        updateSharedDirectories(SharingUtils.PREFERENCE_SHARE, null, revision);
        
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
            addFileIfSharedOrStore(file, EMPTY_DOCUMENTS, true, _revision, null, AddType.ADD_SHARE);
        }
        
        // Update the store directory and add only files from the LWS
        File storeDir = SharingSettings.getSaveLWSDirectory();
        updateStoreDirectories(storeDir.getAbsoluteFile(), null, revision);
        
        // Optain the list of lws files found in shared directories
        Collection<File> specialStoreFiles = _data.SPECIAL_STORE_FILES;
        ArrayList<File> storeList;
        synchronized (specialStoreFiles) {
            storeList = new ArrayList<File>(specialStoreFiles);
        }
        
        // list lws files found in shared directories in the special
        //	store files node
        for(File file: storeList) {
            if(_revision != revision)
                break;
            addFileIfSharedOrStore(file, EMPTY_DOCUMENTS, true, _revision, null, AddType.ADD_STORE);
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
    
    
    private void updateSharedDirectories(File directory, File parent, int revision) {
        updateSharedDirectories(directory, directory, parent, revision, 1);
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
    private void updateSharedDirectories(File rootShare, File directory, File parent, int revision, int depth) {
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
        if (SharingUtils.isSensitiveDirectory(directory)) {
            //  go through directories that explicitly should not be shared
            if (_data.SENSITIVE_DIRECTORIES_NOT_TO_SHARE.contains(directory)) {
                return;
            }
            
            // if we haven't already validated the sensitive directory, ask about it.
            if (!_data.SENSITIVE_DIRECTORIES_VALIDATED.contains(directory)) {
                //  ask the user whether the sensitive directory should be shared
                // THIS CALL CAN BLOCK.
                if (!fileManagerController.warnAboutSharingSensitiveDirectory(directory))
                    return;
            }
        }
		
        // Exit quickly (without doing the dir lookup) if revisions changed.
        if(_revision != revision)
            return;

        // STEP 1:
        // Add directory
        boolean isForcedShare = SharingUtils.isForcedShareDirectory(directory);
        
        synchronized (this) {
            // if it was already added, ignore.
            if (_completelySharedDirectories.contains(directory))
                return;

            if (!_storeDirectories.contains(directory))
				_completelySharedDirectories.add(directory);
            if (!isForcedShare) {
                dispatchFileEvent(
                        new FileManagerEvent(this, Type.ADD_FOLDER, rootShare, depth, directory, parent));
            }
        }
        // STEP 2:
        // Scan subdirectory for the amount of shared files.
        File[] file_list = directory.listFiles(SHAREABLE_FILE_FILTER);
        if (file_list == null)
            return;
        for(int i = 0; i < file_list.length && _revision == revision; i++)
            addFileIfSharedOrStore(file_list[i], EMPTY_DOCUMENTS, true, _revision, null, AddType.ADD_SHARE);
            
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
                updateSharedDirectories(rootShare, dir_list[i], directory, revision, depth+1);
        }
    }

    /**
     * Recursively add files from the LWS download directory.  Does nothing
     * if <tt>directory</tt> doesn't exist, isn't a directory, or has already
     * been added.  This method is thread-safe.  It acquires locks on a
     * per-directory basis.  If the current revision ever changes from the
     * expected revision, this returns immediately.
     * 
     * @requires directory is part of _storeDirectories or one of its
     *           children, and parent is directory's store directory parent 
     * @modifies this
     */
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
            if (_storeDirectories.contains(directory))
                return;
            
            //otherwise add this directory to list to avoid rescanning it
            _storeDirectories.add(directory);
            dispatchFileEvent(
                    new FileManagerEvent(this, Type.ADD_STORE_FOLDER, directory, parent));
        }
        
        // STEP 2:
        // Scan subdirectory for the amount of shared files.
        File[] file_list = directory.listFiles();
        if (file_list == null)
            return;
        for(int i = 0; i < file_list.length && _revision == revision; i++)
            addFileIfSharedOrStore(file_list[i], EMPTY_DOCUMENTS, true, _revision, null, AddType.ADD_STORE);
            
        // Exit quickly (without doing the dir lookup) if revisions changed.
        if(_revision != revision)
            return;

        // STEP 3:
        // Recursively add subdirectories.
        // This has the effect of ensuring that the number of pending files
        // is closer to correct number.     
        File[] dir_list = directory.listFiles(DIRECTORY_FILTER);
        if(dir_list != null) {
            for(int i = 0; i < dir_list.length && _revision == revision; i++)
                updateStoreDirectories(dir_list[i], directory, revision);
        }
            
    }

	///////////////////////////////////////////////////////////////////////////
	//  Adding and removing shared files and directories
	///////////////////////////////////////////////////////////////////////////

	/* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#removeFolderIfShared(java.io.File)
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
                if(!explicitlyShared || isFolderShared(folder.getParentFile()))
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
                        if(removeFileIfSharedOrStore(f) == null)
                            fileManagerController.clearPendingShare(f);
                        if(isStoreFileLoaded(f)) 
                            _data.SPECIAL_STORE_FILES.remove(f);	
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
    
	/* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#addSharedFolders(java.util.Set, java.util.Set)
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
	
	/* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#getFolderNotToShare()
     */
	public Set<File> getFolderNotToShare() {
	    synchronized (_data.DIRECTORIES_NOT_TO_SHARE) {
	        return new HashSet<File>(_data.DIRECTORIES_NOT_TO_SHARE);
	    }
	}
	
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#addSharedFolder(java.io.File)
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
		if (!isFolderShared(folder.getParentFile()))
			SharingSettings.DIRECTORIES_TO_SHARE.add(folder);
        _isUpdating = true;
        updateSharedDirectories(folder, null, _revision);
        _isUpdating = false;
        
        return true;
    }
    
	/* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#addFileAlways(java.io.File)
     */
	public void addFileAlways(File file) {
		addFileAlways(file, EMPTY_DOCUMENTS, null);
	}
	
	/* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#addFileAlways(java.io.File, com.limegroup.gnutella.FileEventListener)
     */
	public void addFileAlways(File file, FileEventListener callback) {
	    addFileAlways(file, EMPTY_DOCUMENTS, callback);
	}
	
	/* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#addFileAlways(java.io.File, java.util.List)
     */
	public void addFileAlways(File file, List<? extends LimeXMLDocument> list) {
	    addFileAlways(file, list, null);
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#addFileAlways(java.io.File, java.util.List, com.limegroup.gnutella.FileEventListener)
     */
	 public void addFileAlways(File file, List<? extends LimeXMLDocument> list, FileEventListener callback) {
		_data.FILES_NOT_TO_SHARE.remove(file);
		if (!isFileShareable(file))
			_data.SPECIAL_FILES_TO_SHARE.add(file);
			
        addFileIfSharedOrStore(file, list, true, _revision, callback, AddType.ADD_SHARE);
	}
	 
	 /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#addFileForSession(java.io.File)
     */
	 public void addFileForSession(File file) {
		 addFileForSession(file, null);
	 }
     
     /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#addFileForSession(java.io.File, com.limegroup.gnutella.FileEventListener)
     */
     public void addFileForSession(File file, FileEventListener callback) {
         _data.FILES_NOT_TO_SHARE.remove(file);
         if (!isFileShareable(file))
             _transientSharedFiles.add(file);
         addFileIfSharedOrStore(file, EMPTY_DOCUMENTS, true, _revision, callback, AddType.ADD_SHARE);
     }
	
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#addFileIfShared(java.io.File)
     */
   public void addFileIfShared(File file) {
       addFileIfSharedOrStore(file, EMPTY_DOCUMENTS, true, _revision, null, AddType.ADD_SHARE);
   }
   
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#addFileIfShared(java.io.File, com.limegroup.gnutella.FileEventListener)
     */
    public void addFileIfShared(File file, FileEventListener callback) {
        addFileIfSharedOrStore(file, EMPTY_DOCUMENTS, true, _revision, callback, AddType.ADD_SHARE);
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#addFileIfShared(java.io.File, java.util.List)
     */
    public void addFileIfShared(File file, List<? extends LimeXMLDocument> list) {
        addFileIfSharedOrStore(file, list, true, _revision, null, AddType.ADD_SHARE);
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#addFileIfShared(java.io.File, java.util.List, com.limegroup.gnutella.FileEventListener)
     */
    public void addFileIfShared(File file, List<? extends LimeXMLDocument> list, FileEventListener callback) {
        addFileIfSharedOrStore(file, list, true, _revision, callback, AddType.ADD_SHARE);
    }
    
    
    /**
     * Adds a file that is either associated with the store or is shared. Tries to create 
     * a FileDescriptor for the file and ew. Files are handled differently depending on their AddType. 
     * 
     * 
     * @param file - the file to be added
     * @param metadata - any LimeXMLDocs associated with this file
     * @param notify - if true signals the front-end via 
     *        ActivityCallback.handleFileManagerEvent() about the Event
     * @param revision - current  version of LimeXMLDocs being used
     * @param callback - the listener to notify about the event
     * @param addType - type of add that for this file
     */
    protected void addFileIfSharedOrStore(File file, List<? extends LimeXMLDocument> metadata, boolean notify,
            int revision, FileEventListener callback, AddType addFileType) {
        if(LOG.isDebugEnabled())
            LOG.debug("Attempting to load store or shared file: " + file);
        if(callback == null)
            callback = EMPTY_CALLBACK;

        // Make sure capitals are resolved properly, etc.
        try {
            file = FileUtils.getCanonicalFile(file);
        } catch (IOException e) {
            callback.handleFileEvent(new FileManagerEvent(this, addFileType.getFailureType(), file));
            return;
	    }
	    
			// if file is not shareable, also remove it from special files
			// to share since in that case it's not physically shareable then
        if (!isFileShareable(file) && !isFileLocatedStoreDirectory(file)) { 
		    	_individualSharedFiles.remove(file);
            callback.handleFileEvent(new FileManagerEvent(FileManagerImpl.this, addFileType.getFailureType(), file));
            return;
        }
        
        if(isStoreFileLoaded(file)) {
            return;
		}
        
        if(isFileShared(file)) {
            callback.handleFileEvent(new FileManagerEvent(FileManagerImpl.this, Type.ALREADY_SHARED_FILE, file));
            return;
        }

        synchronized(this) {
            if (revision != _revision) {
                callback.handleFileEvent(new FileManagerEvent(this, addFileType.getFailureType(), file));
                return;
            }
            
            _numPendingFiles++;
            // make sure _pendingFinished does not hold _revision
            // while we're still adding files
            _pendingFinished = -1;
        }
        fileManagerController.calculateAndCacheUrns(file, getNewUrnCallback(file, metadata, notify, revision, callback, 
                addFileType));
    }
    
    /**
     * Constructs a new UrnCallback that will possibly load the file with the given URNs.
     */
    protected UrnCallback getNewUrnCallback(final File file, final List<? extends LimeXMLDocument> metadata, final boolean notify,
                                            final int revision, final FileEventListener callback, final AddType addFileType) {
        return new UrnCallback() {
		    public void urnsCalculated(File f, Set<? extends URN> urns) {
		        FileDesc fd = null;
		        synchronized(FileManagerImpl.this) {
    		        if(revision != _revision) {
    		            LOG.warn("Revisions changed, dropping share.");
                        callback.handleFileEvent(new FileManagerEvent(FileManagerImpl.this, addFileType.getFailureType(), file));
                        return;
                    }
                
                    _numPendingFiles--;
                    
                    // Only load the file if we were able to calculate URNs 
                    // assume the fd is being shared
                    if(!urns.isEmpty()) {
                        int fileIndex = _files.size();
                        fd = createFileDesc(file, urns, fileIndex);
                    }
                }
		        
		        if(fd == null) {
    		        // If URNs was empty, or loading failed, notify...
                    callback.handleFileEvent(new FileManagerEvent(FileManagerImpl.this, addFileType.getFailureType(), file));
                    return;
		        }
                    
                // try loading the fd so we can check the LimeXML info
                loadFile(fd, file, metadata, urns);

                // check LimeXML to determine if is a store file or the sha1 is mapped to a store file 
                //  (the sha1 check is needed if duplicate store files are loaded since the second file 
                //  will not have a unique LimeXMLDoc associated with it)
                if (isStoreXML(fd.getXMLDocument()) || _urnStoreMap.containsKey(fd.getSHA1Urn()) ) { 
                    addStoreFile(fd, file, urns, addFileType, notify, callback);
                } else if (addFileType == AddType.ADD_SHARE) {
                    addSharedFile(file, fd, urns, addFileType, notify, callback);
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
     * Creates a file descriptor for the given file and places the fd into the set
     * of LWS file descriptors
     */
    private synchronized void addStoreFile(FileDesc fd, File file, Set<? extends URN> urns, AddType addFileType,
            final boolean notify, final FileEventListener callback) {
        if(LOG.isDebugEnabled())
            LOG.debug("Store file: " + file);

        // if this file is in a shared folder, add to individual store files
        if( addFileType == AddType.ADD_SHARE)
            _data.SPECIAL_STORE_FILES.add(file);

        //store files are not part of the _files list so recreate fd with invalid index in _files
        FileDesc fileDesc = createFileDesc(file, urns, _storeFiles.size());
        //add the xml doc to the new FileDesc
        if( fd.getXMLDocument() != null )
            fileDesc.addLimeXMLDocument(fd.getXMLDocument());
    
        _storeFiles.add(fileDesc); 
        _storeToFileDescMap.put(file, fileDesc);
        
        fileManagerController.fileAdded(file, fileDesc.getSHA1Urn());
        
        // Ensure file can be found by URN lookups
        this.updateStoreUrnIndex(fileDesc);
        _needRebuild = true;   
        
        FileManagerEvent evt = new FileManagerEvent(FileManagerImpl.this, Type.ADD_STORE_FILE, fileDesc);
        if(notify) // sometimes notify the GUI
            dispatchFileEvent(evt);
        callback.handleFileEvent(evt); // always notify the individual callback.
    }
    
    /**
     * Handles the actual sharing of a file by placing the file descriptor into the set of shared files
     */
    private synchronized void addSharedFile(File file, FileDesc fileDesc, Set<? extends URN> urns, AddType addFileType,
            final boolean notify, final FileEventListener callback) {
        if(LOG.isDebugEnabled())
            LOG.debug("Sharing file: " + file);
               
        // since we created the FD to test the XML for being an instance of LWS, check to make sure
        //  the FD is still valid before continuing
        if( fileDesc.getIndex() != _files.size()) {
            LimeXMLDocument doc = fileDesc.getXMLDocument();
            fileDesc = createFileDesc(file, urns, _files.size());
            if( doc != null )
                fileDesc.addLimeXMLDocument(doc);
        }
        

        long fileLength = file.length();
        _filesSize += fileLength;        
        _files.add(fileDesc);
        _fileToFileDescMap.put(file, fileDesc);
        _numFiles++;
	
        //Register this file with its parent directory.
        File parent = file.getParentFile();
        assert parent != null : "Null parent to \""+file+"\"";        
        // files that are forcibly shared over the network
        // aren't counted or shown.
        if(SharingUtils.isForcedShareDirectory(parent))
            _numForcedFiles++;
	
       loadKeywords(_keywordTrie, fileDesc);
	
        // Commit the time in the CreactionTimeCache, but don't share
        // the installer.  We populate free LimeWire's with free installers
        // so we have to make sure we don't influence the what is new
        // result set.
        if (!SharingUtils.isForcedShare(file)) {
            fileManagerController.fileAdded(file, fileDesc.getSHA1Urn());
        }

        // Ensure file can be found by URN lookups
        this.updateSharedUrnIndex(fileDesc);
        _needRebuild = true;            
        
        FileManagerEvent evt = new FileManagerEvent(FileManagerImpl.this, addFileType.getSuccessType(), fileDesc);
        if(notify) // sometimes notify the GUI
            dispatchFileEvent(evt);
        callback.handleFileEvent(evt); // always notify the individual callback.
    }
    
    /**
     * @param trie to update
     * @param fd to load keywords from
     */
    private void loadKeywords(StringTrie<IntSet> trie, FileDesc fd) {
        // Index the filename.  For each keyword...
        String[] keywords = extractKeywords(fd);
        
        for (int i = 0; i < keywords.length; i++) {
            String keyword = keywords[i];
            //Ensure the _keywordTrie has a set of indices associated with keyword.
            IntSet indices = trie.get(keyword);
            if (indices == null) {
                indices = new IntSet();
                trie.add(keyword, indices);
            }
            //Add fileIndex to the set.
            indices.add(fd.getIndex());
        }
    }
    
    /**
     * Creates a file descriptor for a given file and a set of urns
     * @param file - file to create descriptor for
     * @param urns - urns to use
     * @param index - index to use
     * @return
     */
    private FileDesc createFileDesc(File file, Set<? extends URN> urns, int index){
        FileDesc fileDesc = new FileDesc(file, urns, index);
        ContentResponseData r = fileManagerController.getResponseDataFor(fileDesc.getSHA1Urn());
        // if we had a response & it wasn't good, don't add this FD.
        if(r != null && !r.isOK())
            return null;
        else
        	return fileDesc;
    }

	/* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#stopSharingFile(java.io.File)
     */
	public synchronized FileDesc stopSharingFile(File file) {
		try {
			file = FileUtils.getCanonicalFile(file);
		} catch (IOException e) {
			return null;
		}
		
		// if its a store file it can't be shared thus it can't be unshared,
		//    just return null
		if( isStoreFileLoaded(file))
		    return null;
		
		// remove file already here to heed against race conditions
		// wrt to filemanager events being handled on other threads
		boolean removed = _individualSharedFiles.remove(file); 
		FileDesc fd = removeFileIfSharedOrStore(file);
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
	
	/* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#removeFileIfShared(java.io.File)
     */
    public synchronized FileDesc removeFileIfSharedOrStore(File f) {
        return removeFileIfSharedOrStore(f, true);
    }
    
    /**
     * removes a file desc from the map if it exists and is shared or is a store
     * file
     */
    protected synchronized FileDesc removeFileIfSharedOrStore(File f, boolean notify) {
        FileDesc toRemove = getFileDescForFile(f);
        if( toRemove == null )
            return null;
        
        if( _storeFiles.contains(toRemove) )
            return removeStoreFile(f, notify);
        else
            return removeFileIfShared(f, notify);
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
            removeSharedUrnIndex(fd, false);
            removeKeywords(_incompleteKeywordTrie, fd);
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

        File parent = f.getParentFile();
        // files that are forcibly shared over the network aren't counted
        if(SharingUtils.isForcedShareDirectory(parent)) {
            notify = false;
            _numForcedFiles--;
        }


        removeKeywords(_keywordTrie, fd);

        //Remove hash information.
        removeSharedUrnIndex(fd, true);
  
        // Notify the GUI...
        if (notify) {
            FileManagerEvent evt = new FileManagerEvent(this, Type.REMOVE_FILE, fd);
                                            
            dispatchFileEvent(evt);
        }
        
        return fd;
    }
    
    private void removeKeywords(StringTrie<IntSet> trie, FileDesc fd) {
        //Remove references to this from index.
        String[] keywords = extractKeywords(fd);
        for (int j = 0; j < keywords.length; j++) {
            String keyword = keywords[j];
            IntSet indices = trie.get(keyword);
            if (indices != null) {
                indices.remove(fd.getIndex());
                if (indices.size() == 0)
                    trie.remove(keyword);
            }
        }        
    }
    
    protected synchronized FileDesc removeStoreFile(File f, boolean notify){
        //Take care of case, etc.
        try {
            f = FileUtils.getCanonicalFile(f);
        } catch (IOException e) {
            return null;
        }        

        // Look for matching file ...         
        FileDesc fd = _storeToFileDescMap.get(f);
        if (fd == null)
            return null;
        
        int i = fd.getIndex();
        assert _storeFiles.get(i).getFile().equals(f) : "invariant broken!";
        
        _storeFiles.set(i, null);

        _data.SPECIAL_STORE_FILES.remove(f);
        _storeToFileDescMap.remove(f);

        //Remove hash information.
        removeStoreUrnIndex(fd, true);
 
        // Notify the GUI...
        if (notify) {
            FileManagerEvent evt = new FileManagerEvent(this, Type.REMOVE_STORE_FILE, fd);                                           
            dispatchFileEvent(evt);
        } 
        return fd;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#addIncompleteFile(java.io.File, java.util.Set, java.lang.String, long, com.limegroup.gnutella.downloader.VerifyingFile)
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
            if (!urn.isSHA1())
                continue;
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
        fileURNSUpdated(ifd);
        _numIncompleteFiles++;
        _needRebuild = true;
        dispatchFileEvent(new FileManagerEvent(this, Type.ADD_FILE, ifd));
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#fileChanged(java.io.File)
     */
    public abstract void fileChanged(File f);
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#validate(com.limegroup.gnutella.FileDesc)
     */
    public void validate(final FileDesc fd) {
        if(_requestingValidation.add(fd.getSHA1Urn())) {
            fileManagerController.requestValidation(fd.getSHA1Urn(), new ContentResponseObserver() {
               public void handleResponse(URN urn, ContentResponseData r) {
                   _requestingValidation.remove(fd.getSHA1Urn());
                   if(r != null && !r.isOK())
                       removeFileIfSharedOrStore(fd.getFile());
               }
            });
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    //  Search, utility, etc...
    ///////////////////////////////////////////////////////////////////////////
	
    public synchronized void fileURNSUpdated(FileDesc fd) {
        updateSharedUrnIndex(fd);
        if (fd instanceof IncompleteFileDesc) {
            IncompleteFileDesc ifd = (IncompleteFileDesc) fd;
            if (SharingSettings.ALLOW_PARTIAL_SHARING.getValue() &&
                    SharingSettings.LOAD_PARTIAL_KEYWORDS.getValue() &&
                    ifd.hasUrnsAndPartialData()) {
                loadKeywords(_incompleteKeywordTrie, fd);
                _needRebuild = true;
            }
        }
    }
    
    /**
     * @modifies this
     * @effects enters the given FileDesc into the _urnMap under all its 
     * reported URNs
     */
    private synchronized void updateSharedUrnIndex(FileDesc fileDesc) {
        updateUrnIndex(fileDesc, _urnMap);
    }
    
    /**
     * @effects enters the given FileDesc into the _urnStoreMap under all its 
     * reported URNs, store urns are not returned in queries
     */
    private synchronized void updateStoreUrnIndex(FileDesc fileDesc) {
        updateUrnIndex(fileDesc, _urnStoreMap);
    }
    
    /**
	 * Generic method for adding a fileDesc's URNS to a map
	 */
    private synchronized void updateUrnIndex(FileDesc fileDesc, Map<URN, IntSet> map) {
        for(URN urn : fileDesc.getUrns()) {
            if (!urn.isSHA1())
                continue;
            IntSet indices=map.get(urn);
            if (indices==null) {
                indices=new IntSet();
                map.put(urn, indices);
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

    /** 
     * Removes any URN index information for desc from shared files
     * @param purgeState true if any state should also be removed (creation time, altlocs) 
     */
    private synchronized void removeSharedUrnIndex(FileDesc fileDesc, boolean purgeState) {
        removeUrnIndex(fileDesc, _urnMap, purgeState);
    }
    
    /** 
     * Removes any URN index information for desc from store files
     * @param purgeState true if any state should also be removed (creation time, altlocs) 
     */
    private synchronized void removeStoreUrnIndex(FileDesc fileDesc, boolean purgeState) {
        removeUrnIndex(fileDesc, _urnStoreMap, purgeState);
    }
    
    private synchronized void removeUrnIndex(FileDesc fileDesc, Map<URN, IntSet> map, boolean purgeState) {
        for(URN urn : fileDesc.getUrns()) {
            if (!urn.isSHA1())
                continue;
            //Lookup each of desc's URN's ind _urnMap.  
            //(It better be there!)
            IntSet indices=map.get(urn);
            if (indices == null) {
                assert fileDesc instanceof IncompleteFileDesc;
                return;
            }

            //Delete index from set.  Remove set if empty.
            indices.remove(fileDesc.getIndex());
            if (indices.size()==0 && purgeState) {
                fileManagerController.lastUrnRemoved(urn);
                map.remove(urn);
            }
		}
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#renameFileIfSharedOrStore(java.io.File, java.io.File)
     */
    public void renameFileIfSharedOrStore(File oldName, File newName) {
        renameFileIfSharedOrStore(oldName, newName, null);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#renameFileIfSharedOrStore(java.io.File, java.io.File, com.limegroup.gnutella.FileEventListener)
     */
    public synchronized void renameFileIfSharedOrStore(final File oldName, final File newName, final FileEventListener callback) {
        FileDesc toRemove = getFileDescForFile(oldName);
        if (toRemove == null ) {
            FileManagerEvent evt = new FileManagerEvent(this, Type.ADD_FAILED_FILE, oldName);
            dispatchFileEvent(evt);
            if(callback != null)
                callback.handleFileEvent(evt);
            return;
        }
        
        if(LOG.isDebugEnabled())
            LOG.debug("Attempting to rename: " + oldName + " to: "  + newName);
            
        List<LimeXMLDocument> xmlDocs = new LinkedList<LimeXMLDocument>(toRemove.getLimeXMLDocuments());
        
        // if its a shared file, store files all have the same index in their filedescriptor
        if( !_storeFiles.contains(toRemove)) {
		    final FileDesc removed = removeFileIfShared(oldName, false);
            assert removed == toRemove : "invariant broken.";
		    if (_data.SPECIAL_FILES_TO_SHARE.remove(oldName) && !isFileInCompletelySharedDirectory(newName))
			    _data.SPECIAL_FILES_TO_SHARE.add(newName);
			
            // Prepopulate the cache with new URNs.
            fileManagerController.addUrns(newName, removed.getUrns());

            addFileIfSharedOrStore( newName, xmlDocs, false, _revision, new FileEventListener(){
            public void handleFileEvent(FileManagerEvent evt) {
                if(LOG.isDebugEnabled())
                    LOG.debug("Add of newFile returned callback: " + evt);

                // Retarget the event for the GUI.
                FileManagerEvent newEvt = null;
                if(evt.isAddEvent()) {
                    FileDesc fd = evt.getFileDescs()[0];
                    newEvt = new FileManagerEvent(FileManagerImpl.this, Type.RENAME_FILE, removed, fd);
                } else {
                    newEvt = new FileManagerEvent(FileManagerImpl.this, Type.REMOVE_FILE, removed);
                }
                dispatchFileEvent(newEvt);
                if(callback != null)
                    callback.handleFileEvent(newEvt);
            }
            }, AddType.ADD_SHARE);
        }   
        // its a store files
        else  {
            final FileDesc removed = removeStoreFile(oldName, false);
            assert removed == toRemove : "invariant broken.";
            if (_data.SPECIAL_STORE_FILES.remove(oldName)) 
                _data.SPECIAL_STORE_FILES.add(newName);
            // Prepopulate the cache with new URNs.
            fileManagerController.addUrns(newName, removed.getUrns());
    
            addFileIfSharedOrStore(newName, xmlDocs, false, _revision, new FileEventListener() {
                public void handleFileEvent(FileManagerEvent evt) {
                  FileManagerEvent newEvt = null; 
                  if(evt.isAddStoreEvent()) {
                      FileDesc fd = evt.getFileDescs()[0];
                      newEvt = new FileManagerEvent(FileManagerImpl.this, Type.RENAME_FILE, removed, fd);
                  } else {
                      newEvt = new FileManagerEvent(FileManagerImpl.this, Type.REMOVE_STORE_FILE, removed);
                  }
                  dispatchFileEvent(newEvt);
                  if(callback != null)
                      callback.handleFileEvent(newEvt);
                }
            }, AddType.ADD_STORE
            );
        }
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

    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#validateSensitiveFile(java.io.File)
     */
	public void validateSensitiveFile(File dir) {
        _data.SENSITIVE_DIRECTORIES_VALIDATED.add(dir);
        _data.SENSITIVE_DIRECTORIES_NOT_TO_SHARE.remove(dir);
    }

	/* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#invalidateSensitiveFile(java.io.File)
     */
	public void invalidateSensitiveFile(File dir) {
        _data.SENSITIVE_DIRECTORIES_VALIDATED.remove(dir);
        _data.SENSITIVE_DIRECTORIES_NOT_TO_SHARE.add(dir);
        SharingSettings.DIRECTORIES_TO_SHARE.remove(dir);   
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#hasIndividualFiles()
     */
    public boolean hasIndividualFiles() {
        return !_data.SPECIAL_FILES_TO_SHARE.isEmpty();
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#hasIndividualStoreFiles()
     */
    public boolean hasIndividualStoreFiles() {
        return !_data.SPECIAL_STORE_FILES.isEmpty();
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#hasApplicationSharedFiles()
     */
    public boolean hasApplicationSharedFiles() {
    	File [] files = SharingUtils.APPLICATION_SPECIAL_SHARE.listFiles();
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
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#getIndividualFiles()
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
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#getIndividualStoreFiles()
     */
    public File[] getIndividualStoreFiles() {
        Set<File> candidates = _data.SPECIAL_STORE_FILES;
        synchronized (candidates) {
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
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#isIndividualStore(java.io.File)
     */
    public boolean isIndividualStore(File f) {
        return _data.SPECIAL_STORE_FILES.contains(f);
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#isIndividualShare(java.io.File)
     */
    public boolean isIndividualShare(File f) {
    	return _data.SPECIAL_FILES_TO_SHARE.contains(f) 
            && SharingUtils.isFilePhysicallyShareable(f)
            && !SharingUtils.isApplicationSpecialShare(f);
    }
       
    /**
     * Cleans all stale entries from the Set of individual files.
     */
    private void cleanIndividualFiles() {
        Set<File> files = _data.SPECIAL_FILES_TO_SHARE;
        synchronized(files) {
            for(Iterator<File> i = files.iterator(); i.hasNext(); ) {
                File f = i.next();
                if(!(SharingUtils.isFilePhysicallyShareable(f)))
                    i.remove();
            }
        }
    }

	/* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#isFileShared(java.io.File)
     */
	public synchronized boolean isFileShared(File file) {
		if (file == null)
			return false;
		if (_fileToFileDescMap.get(file) == null)
			return false;
		return true;
	}
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#isRareFile(com.limegroup.gnutella.FileDesc)
     */
    public boolean isRareFile(FileDesc fd) {
        return rareDefinition.evaluate(fd);
    }
	
    /** Returns true if file has a shareable extension.  Case is ignored. */
    private static boolean hasShareableExtension(File file) {
        if(file == null)
            return false;
        
        String extension = FileUtils.getFileExtension(file);
        if (extension != null) {
            return _extensions.contains(extension.toLowerCase(Locale.US));
        } else {
            return false;
        }
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#isFileInCompletelySharedDirectory(java.io.File)
     */
    public boolean isFileInCompletelySharedDirectory(File f) {
        File dir = f.getParentFile();
        if (dir == null) 
            return false;

		synchronized (this) {
			return _completelySharedDirectories.contains(dir);
		}
	}
	
	/* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#isSharedDirectory(java.io.File)
     */
	public boolean isFolderShared(File dir) {
		if (dir == null)
			return false;
		
		synchronized (this) {
			return _completelySharedDirectories.contains(dir);
		}
	}

	/* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#isStoreFile(java.io.File)
     */
    public boolean isStoreFileLoaded(File file) {
        if( _storeToFileDescMap.containsKey(file) || 
                _storeDirectories.contains(file)) {
                return true;
        }
        return false;
    }
    
	/**
	 * Returns true if the given file is in a completely shared directory
	 * or if it is specially shared.
     * NOTE: this does not determine if a file is unshareable as a result of
     * being a LWS file
	 */
	private boolean isFileShareable(File file) {
		if (!SharingUtils.isFilePhysicallyShareable(file))
			return false;
		if (_individualSharedFiles.contains(file))
			return true;
		if (_data.FILES_NOT_TO_SHARE.contains(file))
			return false;
		if (isFileInCompletelySharedDirectory(file)) {
	        if (file.getName().toUpperCase(Locale.US).startsWith("LIMEWIRE"))
				return true;
			if (!hasShareableExtension(file))
	        	return false;
			return true;
		}
			
		return false;
	}
    
    private boolean isFileLocatedStoreDirectory(File file) {
        return ( _storeDirectories.contains(file.getParentFile()));
    }
    
    /**
     * Returns true if the XML doc contains information regarding the LWS
     */
    private boolean isStoreXML(LimeXMLDocument doc) {
       return doc != null && doc.getLicenseString() != null &&
               doc.getLicenseString().equals(LicenseType.LIMEWIRE_STORE_PURCHASE.name());
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#isStoreDirectory(java.io.File)
     */
    public boolean isStoreDirectory(File file) {
        return _storeDirectories.contains(file);
    }
       
 
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#isFolderShareable(java.io.File, boolean)
     */
    public boolean isFolderShareable(File folder, boolean includeExcludedDirectories) {
        if(!folder.isDirectory() || !folder.canRead())
            return false;
        
        if (folder.equals(SharingSettings.INCOMPLETE_DIRECTORY.getValue()))
            return false;
        
        if (SharingUtils.isApplicationSpecialShareDirectory(folder)) {
            return false;
        }
        
        // Do not share directories on the do not share list
        if (includeExcludedDirectories && _data.DIRECTORIES_NOT_TO_SHARE.contains(folder))
            return false;
        
        if(SharingUtils.isFolderBanned(folder))
            return false;
        
        return true;
    }
	
 
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#getQRT()
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
        if (SearchSettings.PUBLISH_LIME_KEYWORDS.getBoolean()) {
            for (String entry : SearchSettings.LIME_QRP_ENTRIES.getValue())
                _queryRouteTable.addIndivisible(entry);
        }
        FileDesc[] fds = getAllSharedFileDescriptors();
        for(int i = 0; i < fds.length; i++) {
            if (fds[i] instanceof IncompleteFileDesc) {
                if (!SharingSettings.ALLOW_PARTIAL_SHARING.getValue())
                    continue;
                if (!SharingSettings.PUBLISH_PARTIAL_QRP.getValue())
                    continue;
                IncompleteFileDesc ifd = (IncompleteFileDesc)fds[i];
                if (!ifd.hasUrnsAndPartialData())
                    continue;
                
                _queryRouteTable.add(ifd.getFileName());
            } else
                _queryRouteTable.add(fds[i].getPath());
        }
    }

    ////////////////////////////////// Queries ///////////////////////////////

    /**
     * Constant for an empty <tt>Response</tt> array to return when there are
     * no matches.
     */
    private static final Response[] EMPTY_RESPONSES = new Response[0];

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#query(com.limegroup.gnutella.messages.QueryRequest)
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
        IntSet matches = search(str, null, request.desiresPartialResults());
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
    	// NOTE: this doesn't return Store files. getNewestUrns only 
    	//		 returns the top 3 shared files
        List<URN> urnList = fileManagerController.getNewestSharedUrns(request, 3);
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
            if (desc==null || desc instanceof IncompleteFileDesc || SharingUtils.isForcedShare(desc)) 
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
    protected IntSet search(String query, IntSet priors, boolean partial) {
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
            if (SharingSettings.ALLOW_PARTIAL_SHARING.getValue() &&
                    SharingSettings.ALLOW_PARTIAL_RESPONSES.getValue() &&
                    partial)
                iter = new MultiIterator<IntSet>(iter,_incompleteKeywordTrie.getPrefixedBy(query, i, j));
            
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
    
 
    

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#isFileApplicationShared(java.lang.String)
     */
    public boolean isFileApplicationShared(String name) {
    	File file = new File(SharingUtils.APPLICATION_SPECIAL_SHARE, name);
    	try {
    		file = FileUtils.getCanonicalFile(file);
    	} catch (IOException bad) {
    		return false;
    	}
    	return isFileShared(file);
    }
    
  
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#addFileEventListener(com.limegroup.gnutella.FileEventListener)
     */
    public void addFileEventListener(FileEventListener listener) {
        if (listener == null) {
            throw new NullPointerException("FileEventListener is null");
        }
        eventListeners.addIfAbsent(listener);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#removeFileEventListener(com.limegroup.gnutella.FileEventListener)
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
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#getIndexingIterator(boolean)
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

                synchronized (FileManagerImpl.this) {
                    while (index < _files.size()) {
                        FileDesc desc = _files.get(index);
                        index++;

                        // skip, if the file was unshared or is an incomplete file,
                        if (desc == null || desc instanceof IncompleteFileDesc || SharingUtils.isForcedShare(desc)) 
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
                    synchronized (FileManagerImpl.this) {
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
    @InspectableContainer
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
        @InspectionPoint("FileManager QRP info")
        public final Inspectable QRP = new Inspectable() {
            public Object inspect() {
                Map<String, Object> ret = new HashMap<String, Object>();
                addVersion(ret);

                synchronized(FileManagerImpl.this) {
                    ret.put("qrt",getQRT().getRawDump());
                }

                return ret;
            }
        };

        /** An inspectable that returns stats about hits, uploads & alts */
        @InspectionPoint("FileManager h/u/a stats")
        public final Inspectable FDS = new FDInspectable(false);
        /** An inspectable that returns stats about hits, uploads & alts > 0 */
        @InspectionPoint("FileManager h/u/a stats > 0")
        public final Inspectable FDSNZ = new FDInspectable(true);
        
        /** An inspectable that counts how many shared fds match a custom criteria */
        @InspectionPoint("FileManager custom criteria")
        public final Inspectable CUSTOM = new Inspectable() {
            public Object inspect() {
                Map<String, Object> ret = new HashMap<String,Object>();
                ret.put("ver",1);
                ret.put("crit", MessageSettings.CUSTOM_FD_CRITERIA.getValueAsString());
                int total = 0;
                int matched = 0;
                try {
                    RPNParser parser = new RPNParser(MessageSettings.CUSTOM_FD_CRITERIA.getValue());
                    synchronized(FileManagerImpl.this) {
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
            synchronized(FileManagerImpl.this) {
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