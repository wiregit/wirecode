package com.limegroup.gnutella.library;

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
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.Comparators;
import org.limewire.collection.IntSet;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.core.settings.DHTSettings;
import org.limewire.core.settings.MessageSettings;
import org.limewire.core.settings.SharingSettings;
import org.limewire.inspection.Inspectable;
import org.limewire.inspection.InspectableContainer;
import org.limewire.inspection.InspectableForSize;
import org.limewire.inspection.InspectablePrimitive;
import org.limewire.inspection.InspectionPoint;
import org.limewire.lifecycle.Service;
import org.limewire.listener.EventBroadcaster;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SourcedEventMulticaster;
import org.limewire.setting.StringArraySetting;
import org.limewire.statistic.StatsUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.RPNParser;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.altlocs.AltLocManager;
import com.limegroup.gnutella.auth.ContentManager;
import com.limegroup.gnutella.auth.ContentResponseData;
import com.limegroup.gnutella.auth.ContentResponseObserver;
import com.limegroup.gnutella.downloader.VerifyingFile;
import com.limegroup.gnutella.library.FileManagerEvent.Type;
import com.limegroup.gnutella.routing.HashFunction;
import com.limegroup.gnutella.routing.QueryRouteTable;
import com.limegroup.gnutella.simpp.SimppListener;
import com.limegroup.gnutella.simpp.SimppManager;
import com.limegroup.gnutella.xml.LimeXMLDocument;

/**
 * The list of all known files. This creates and maintains a list of 
 * directories and FileDescs. It also creates a set of FileLists which 
 * may contain subsets of all FileDescs. Files can be added to just the 
 * FileManager or loaded into both the FileManager and a specified FileList
 * once the FileDesc has been created. <p>
 *
 * This class is thread-safe.
 */
@Singleton 
class FileManagerImpl implements FileManager, Service {
	
    private static final Log LOG = LogFactory.getLog(FileManagerImpl.class);

    private static final ExecutorService LOADER = ExecutorsHelper.newProcessingQueue("FileManagerLoader");
     
    /** List of event listeners for FileManagerEvents. */
    private final ListenerSupport<FileManagerEvent> listenerSupport;
    
    /** The firer of events. */
    private final EventBroadcaster<FileManagerEvent> eventBroadcaster;
    
    /**********************************************************************
     * LOCKING: obtain this's monitor before modifying this.
     **********************************************************************/

    /**
     * All of the data for FileManager.
     */
    protected final LibraryData _data = new LibraryData();
    
    private final FileListPackage sharedFileList;
    private final FileListPackage storeFileList; 
    private final FileListPackage allFriendsFileList;
    private final FileListPackage incompleteFileList;
    private final Map<String, FileListPackage> friendFileLists = new HashMap<String,FileListPackage>();
    
    /** 
     * The list of complete and incomplete files.  An entry is null if it
     *  is no longer shared.
     * INVARIANT: for all i, files[i]==null, or files[i].index==i and either
     *  files[i]._path is in a shared directory with a shareable extension or
     *  files[i]._path is the incomplete directory if files[i] is an IncompleteFileDesc.
     */
    protected List<FileDesc> files;
    
    protected int numFiles;
    
    /**
     * An index that maps a <tt>File</tt> on disk to the 
     *  <tt>FileDesc</tt> holding it.
     *
     * INVARIANT: For all keys k in _fileToFileDescMap, 
     *  files[_fileToFileDescMap.get(k).getIndex()].getFile().equals(k)
     *
     * Keys must be canonical <tt>File</tt> instances.
     */
    protected Map<File, FileDesc> fileToFileDescMap;
 
    /**
     * A map of appropriately case-normalized URN strings to the
     * indices in files.  Used to make query-by-hash faster.
     * 
     * INVARIANT: for all keys k in urnMap, for all i in urnMap.get(k),
     * files[i].containsUrn(k).  Likewise for all i, for all k in
     *files[i].getUrns(), rnMap.get(k) contains i.
     */
    private Map<URN, IntSet> urnMap;
    
    /** 
     * The total number of files that are pending sharing.
     *  (ie: awaiting hashing or being added)
     */
    @InspectablePrimitive("number of pending files")
    private int _numPendingFiles;
    
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
     * A Set of URNs that we're currently requesting validation for.
     * This is NOT cleared on new revisions, because it'll always be
     * valid.
     */
    private Set<URN> _requestingValidation = Collections.synchronizedSet(new HashSet<URN>());
    
    /**
     *  The directory for downloading LWS songs to and any subdirectories
     *  that may recursively exist
     */    
    @InspectableForSize("number of directories for the store")
    private Set<File> storeDirectories;
    
    /**
     * The revision of the library.  Every time 'loadSettings' is called, the revision
     * is incremented.
     */
    @InspectablePrimitive("filemanager revision")
    private volatile int _revision = 0;
    
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
    private volatile boolean shutdown;
    
    private Saver saver;
    
    /**
     * The filter object to use to discern shareable files.
     */
    private final FileFilter SHAREABLE_FILE_FILTER = new FileFilter() {
        public boolean accept(File f) {
            return getGnutellaSharedFileList().isFileAddable(f);
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
         
    /** Contains the definition of a rare file */
    private final RareFileDefinition rareDefinition;
    
    private final Provider<SimppManager> simppManager;
    private final Provider<UrnCache> urnCache;
    private final Provider<ContentManager> contentManager;
    private final Provider<AltLocManager> altLocManager;
    private final Provider<ActivityCallback> activityCallback;
    private final ScheduledExecutorService backgroundExecutor;
    private final SourcedEventMulticaster<FileDescChangeEvent, FileDesc> fileDescMulticaster;
    
    private final Executor fileListExecutor;
    
	/**
	 * Creates a new <tt>FileManager</tt> instance.
	 */
    @Inject
    public FileManagerImpl(Provider<SimppManager> simppManager,
            Provider<UrnCache> urnCache,
            Provider<ContentManager> contentManager,
            Provider<AltLocManager> altLocManager,
            Provider<ActivityCallback> activityCallback,
            @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor,
            EventBroadcaster<FileManagerEvent> fileManagerEventBroadcaster,
            ListenerSupport<FileManagerEvent> listenerSupport,
            SourcedEventMulticaster<FileDescChangeEvent, FileDesc> fileDescMulticaster) {
        this.simppManager = simppManager;
        this.urnCache = urnCache;
        this.contentManager = contentManager;
        this.altLocManager = altLocManager;
        this.activityCallback = activityCallback;
        this.backgroundExecutor = backgroundExecutor;
        this.listenerSupport = listenerSupport;
        this.eventBroadcaster = fileManagerEventBroadcaster;
        this.fileDescMulticaster = fileDescMulticaster;
        
        fileListExecutor = ExecutorsHelper.newProcessingQueue("FileListDispatchThread");
        
        sharedFileList = new SynchronizedFileList(new GnutellaSharedFileListImpl(fileListExecutor, this, _data.SPECIAL_FILES_TO_SHARE, _data.FILES_NOT_TO_SHARE));
        storeFileList = new SynchronizedFileList(new StoreFileListImpl(fileListExecutor, this, _data.SPECIAL_STORE_FILES));
        allFriendsFileList = new SynchronizedFileList(new FriendFileListImpl(fileListExecutor, this, _data.getFriendList("All"), "All"));
        incompleteFileList = new SynchronizedFileList(new IncompleteFileListImpl(fileListExecutor, this, new HashSet<File>()));
        
        synchronized(this) {
            for(String name : SharingSettings.SHARED_FRIEND_LIST_NAMES.getValue()) {
                friendFileLists.put(name, new SynchronizedFileList(new FriendFileListImpl(fileListExecutor, this, _data.getFriendList(name), name)));
            }
        }
        
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
    protected synchronized void resetVariables()  {
        files = new ArrayList<FileDesc>();
        urnMap = new HashMap<URN, IntSet>();
        fileToFileDescMap = new HashMap<File, FileDesc>();
        
        getGnutellaSharedFileList().clear();
        getStoreFileList().clear();
        getIncompleteFileList().clear();
        for(FileList list : friendFileLists.values()) {
            list.clear();
        }

        numFiles = 0;
        _numPendingFiles = 0;
        _extensions = new HashSet<String>();
		_completelySharedDirectories = new HashSet<File>();
        storeDirectories = new HashSet<File>();
    }

    public String getServiceName() {
        return org.limewire.i18n.I18nMarker.marktr("Shared Files");
    }
    
    public void initialize() {
        fileDescMulticaster.addListener(new EventListener<FileDescChangeEvent>() {
            @Override
            public void handleEvent(FileDescChangeEvent event) {
                switch(event.getType()) {
                case URNS_CHANGED:
                    synchronized(FileManagerImpl.this) {
                        updateUrnIndex(event.getSource());
                    }
                    break;
                }
            }
            
        });
    }
    
    @Inject
    void register(org.limewire.lifecycle.ServiceRegistry registry) {
        registry.register(this);
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#start()
     */
    public void start() {
        _data.clean();
        cleanIndividualFiles();
		loadSettings();
    }
       
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#stop()
     */
    public void stop() {
        save();
        shutdown = true;
    }

    private void save(){
        dispatchFileEvent(new FileManagerEvent(this, Type.FILEMANAGER_SAVE));
        _data.save();
    }
    
    /////////////////////////////////////////////////////////////////////////
    //  FileList Accessors
    ////////////////////////////////////////////////////////////////////////
    
    public FileList getGnutellaSharedFileList() {
        return sharedFileList;
    }
    
    public FileList getStoreFileList() {
        return storeFileList;
    }
	
    public FileList getAllFriendsFileList() {
        return allFriendsFileList;
    }
    
    public synchronized FileList getFriendFileList(String name) {
        return friendFileLists.get(name);
    }
    
    public synchronized FileList getOrCreateFriendFileList(String name) {
        FileListPackage fileList = friendFileLists.get(name);
        if(fileList == null) {
            SharingSettings.addFriendListName(name);
            _data.addFriendList(name);
            fileList = new SynchronizedFileList(new FriendFileListImpl(fileListExecutor, this, _data.getFriendList(name), name));
            friendFileLists.put(name, fileList);
        }
        return fileList;
    }
    
    public synchronized void removeFriendFileList(String name) {
        // if it was a valid key, remove saved references to it
        FileList removeFileList = friendFileLists.get(name);
        if(removeFileList != null) {
            removeFileList.cleanupListeners();
            friendFileLists.remove(name);
            _data.removeFriendList(name);
            SharingSettings.removeFriendListName(name);
        }
    }

    public FileList getIncompleteFileList() {
        return incompleteFileList;
    }
    
    public synchronized Map<String, FileList> getFileListsForAllFriends(){
        Map<String, FileList> map = new HashMap<String, FileList>();
        map.putAll(friendFileLists);
        return map;
    }
    
    ///////////////////////////////////////////////////////////////////////////
    //  FileDesc Accessors
    ///////////////////////////////////////////////////////////////////////////
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#getNumPendingFiles()
     */
    public synchronized int getNumPendingFiles() {
        return _numPendingFiles;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#getFileDescForFile(java.io.File)
     */
    public synchronized FileDesc getFileDesc(File f) {
        try {
            f = FileUtils.getCanonicalFile(f);
        } catch(IOException ioe) {
            return null;
        }
        return fileToFileDescMap.get(f);
    }

	/* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#getFileDescForUrn(com.limegroup.gnutella.URN)
     */
    public synchronized FileDesc getFileDesc(final URN urn) {
        if (!urn.isSHA1())
            throw new IllegalArgumentException();
        
        IntSet indices = urnMap.get(urn);
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
    
    /**
     * Returns the indices into this list for a given URN
     */
    public synchronized IntSet getIndices(URN urn) {
        return urnMap.get(urn);
    }
    
    public synchronized FileDesc get(int index) {
        if(index < 0 || index >= files.size())
            return null;
        return files.get(index);
        }
    
    public synchronized boolean isValidIndex(int index) {
        if( index >= 0 && index < files.size() )
            return true;
        return false;
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
                dispatchFileEvent( new FileManagerEvent(FileManagerImpl.this, Type.FILEMANAGER_LOAD_STARTED));
                loadSettingsInternal(currentRevision);
            }
        });
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#loadWithNewDirectories(java.util.Set, java.util.Set)
     */
    public void loadWithNewDirectories(Set<? extends File> shared, Set<File> blackListSet) {
        SharingSettings.DIRECTORIES_TO_SHARE.setValue(shared);
        synchronized(_data.DIRECTORIES_NOT_TO_SHARE) {
            _data.DIRECTORIES_NOT_TO_SHARE.clear();
            _data.DIRECTORIES_NOT_TO_SHARE.addAll(canonicalize(blackListSet));
        }
        synchronized(storeDirectories) {
            storeDirectories.clear();
            storeDirectories.add(SharingSettings.getSaveLWSDirectory());
        }
	    loadSettings();
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
    private void loadFinished(int revision) {
        // save ourselves to disk every minute
        synchronized(this) {
            if (saver == null) {
                saver = new Saver();
                this.addFileEventListener(saver);
                backgroundExecutor.scheduleWithFixedDelay(saver, 60 * 1000, 60 * 1000, 
                        TimeUnit.MILLISECONDS);
            }
        }
        if(LOG.isDebugEnabled())
            LOG.debug("Finished loading revision: " + revision);
        dispatchFileEvent(new FileManagerEvent(this, Type.FILEMANAGER_LOAD_FINISHING));
        save();
        dispatchFileEvent(new FileManagerEvent(this, Type.FILEMANAGER_LOAD_COMPLETE));
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
    private void loadSettingsInternal(int revision) {
        if(LOG.isDebugEnabled())
            LOG.debug("Loading Library Revision: " + revision);
        
        synchronized (this) {
            resetVariables();

            loadExtensions();
        }
        loadDirectories(revision);

        if(LOG.isDebugEnabled())
            LOG.debug("Finished queueing files for revision: " + revision);
            
        synchronized (this) {
            _updatingFinished = revision;
            if(_numPendingFiles == 0) // if we didn't even try adding any files, pending is finished also.
                _pendingFinished = revision;
        }
        tryToFinish();
    }
    
    /**
     * Loads the extensions that can be shared and cannot be shared.
     * NOTE: this does not limit files that can be loaded into the library, just files
     * that can and cannot be added to the shared list
     */
    private void loadExtensions() {
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
    }

    private void loadDirectories(int revision) {
        _isUpdating = true;
        // Update the FORCED_SHARE directory.
        updateSharedDirectories(SharingUtils.PROGRAM_SHARE, null, revision);
        updateSharedDirectories(SharingUtils.PREFERENCE_SHARE, null, revision);
            
        // Shared folders are still treated as they were previously. In clean 5.0 installs
        // there should be no directories to add here. In upgrades from 4.x installs, 
        // there should be no noticable difference to files that are shared. 
        final File[] directories;
        synchronized (this) {
            directories = SharingSettings.DIRECTORIES_TO_SHARE.getValueAsArray();
            Arrays.sort(directories, new Comparator<File>() {
                public int compare(File a, File b) {
                    return a.toString().length()-b.toString().length();
                }
            });
        }
        for(int i = 0; i < directories.length && _revision == revision; i++)
            updateSharedDirectories(directories[i], null, revision);
            

        // Update the store directory and add only files from the LWS
        File storeDir = SharingSettings.getSaveLWSDirectory();
        updateDirectories(storeDir.getAbsoluteFile(), null, revision, storeDirectories);
        
        // Add specially shared files
        loadIndividualFiles(sharedFileList, revision);
            
        // Add individual store files
        loadIndividualFiles(storeFileList, revision);
    
        //Friend files
        for(String key : friendFileLists.keySet())
            loadIndividualFiles(friendFileLists.get(key), revision);
    
        _isUpdating = false;
    }
    
    /**
     * Takes a collection of files and adds them to the supplied FileList
     */
    private void loadIndividualFiles(FileListPackage fileList, int revision) {
        for (File file : fileList.getIndividualFiles()) {
            if (_revision != revision) {
                break;
            }
            fileList.addPendingFile(file);
            addFile(file);
        }
    }

    private void updateDirectories(File directory, File parent, int revision, Set<File> savedDirectories) {
        //We have to get the canonical path to make sure "D:\dir" and "d:\DIR"
        //are the same on Windows but different on Unix.
        try {
            directory = FileUtils.getCanonicalFile(directory);
        } catch (IOException e) {
            return;
        }
        if(!directory.exists())
            return;
        
        synchronized (savedDirectories) {
            // if it was already added, ignore.
            if (savedDirectories.contains(directory))
                return;
            
            //otherwise add this directory to list to avoid rescanning it
            savedDirectories.add(directory);
        }
        
        // STEP 2:
        // Scan subdirectory for the amount of shared files.
        File[] files = directory.listFiles();
        if (files == null)
            return;
        
        for(int i = 0; i < files.length && _revision == revision; i++) {
            addFile(files[i]);
        }
            
        // Exit quickly (without doing the dir lookup) if revisions changed.
        if(_revision != revision)
            return;

        // STEP 3:
        // Recursively add subdirectories.
        // This has the effect of ensuring that the number of pending files
        // is closer to correct number.     
        File[] directories = directory.listFiles(DIRECTORY_FILTER);
        if(directories != null) {
            for(int i = 0; i < directories.length && _revision == revision; i++)
                updateDirectories(directories[i], directory, revision, savedDirectories);
        }
    }

	///////////////////////////////////////////////////////////////////////////
    //  Adding and removing directories
	///////////////////////////////////////////////////////////////////////////

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
    
    //////////////////////////////////////////////////////////////////////////////
    //  File Accessors
    //////////////////////////////////////////////////////////////////////////////
	
    public void addFriendFile(String name, File file) {
        FileListPackage fileList;
        synchronized(this) {
            fileList = friendFileLists.get(name);
        }
        
        if(fileList != null) {
            fileList.addPendingFile(file);
            
            FileDesc fileDesc = getFileDesc(file);
            if(fileDesc != null) {
                fileList.add(fileDesc);
                dispatchFileEvent(new FileManagerEvent(this, Type.FILE_ALREADY_ADDED, fileDesc));
            } else {
                addFile(file);
            }
        }
	}
	
    public void addSharedFile(File file) {
        addSharedFile(file, LimeXMLDocument.EMPTY_LIST);
	}
	
    public void addSharedFile(File file, List<? extends LimeXMLDocument> list) {
        FileDesc fileDesc = getFileDesc(file);
        sharedFileList.addPendingFile(file);
	
        if(fileDesc != null) {
            dispatchFileEvent(new FileManagerEvent(this, Type.FILE_ALREADY_ADDED, fileDesc));
        } else {
            addFile(file, list);
        }
    }
        
    public void addSharedFileAlways(File file) {
        addSharedFileAlways(file, LimeXMLDocument.EMPTY_LIST);
    }
        
    public void addSharedFileAlways(File file, List<? extends LimeXMLDocument> list) {
        FileDesc fileDesc = getFileDesc(file);
        sharedFileList.addPendingFileAlways(file);
        if (fileDesc != null) {
            sharedFileList.add(fileDesc);
            dispatchFileEvent(new FileManagerEvent(this, Type.FILE_ALREADY_ADDED, fileDesc));
        } else {
            addFile(file, list);
        }
    }
    
    public void addSharedFileForSession(File file) {
        FileDesc fileDesc = getFileDesc(file);
        sharedFileList.addPendingFileForSession(file);

        if (fileDesc != null) {
            sharedFileList.add(fileDesc);
            dispatchFileEvent(new FileManagerEvent(this, Type.FILE_ALREADY_ADDED, fileDesc));
        } else {
            addFile(file);
        }
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#addFileIfShared(java.io.File)
     */
    public void addFile(File file) {
        addFile(file, LimeXMLDocument.EMPTY_LIST);
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#addFileIfShared(java.io.File, java.util.List)
     */
    public void addFile(File file, List<? extends LimeXMLDocument> list) {
        addFile(file, list, _revision, Type.ADD_FILE, Type.ADD_FAILED_FILE, null);
    }
    
    /**
     * Adds a file that is either associated with the store or is shared. Tries to create 
     * a FileDescriptor for the file and ew. Files are handled differently depending on their AddType. 
     * 
     * 
     * @param file - the file to be added
     * @param metadata - any LimeXMLDocs associated with this file
     * @param revision - current  version of LimeXMLDocs being used
     * @param successType - event type to return if add succeeds
     * @param failureType - event type to return if add fails
     */
    private void addFile(File file, List<? extends LimeXMLDocument> metadata, int revision, Type successType, 
            Type failureType, FileDesc oldFileDesc) {
        if(LOG.isDebugEnabled())
            LOG.debug("Attempting to load store or shared file: " + file);

        // Make sure capitals are resolved properly, etc.
        try {
            file = FileUtils.getCanonicalFile(file);
        } catch (IOException e) {
            resolveAndDispatchFileEvent(failureType, oldFileDesc, file);
            return;
        }
        
        // if the file is already added
        boolean contained = false;
        synchronized (this) {
            contained = fileToFileDescMap.containsKey(file);
        }
        
        if(contained) {
            dispatchFileEvent(new FileManagerEvent(this, Type.FILE_ALREADY_ADDED,
                    getFileDesc(file)));
            return;
        }
        
        //make sure a FileDesc can be created from this file
        if (!SharingUtils.isFilePhysicallyShareable(file)) {
            resolveAndDispatchFileEvent(failureType, oldFileDesc, file);
            return;
        }

        synchronized(this) {
            if (revision != _revision) {
                resolveAndDispatchFileEvent(failureType, oldFileDesc, file);
            } else {
                _numPendingFiles++;
                // make sure _pendingFinished does not hold _revision
                // while we're still adding files
                _pendingFinished = -1;
            }
        }
        urnCache.get().calculateAndCacheUrns(file, getNewUrnCallback(file, metadata, revision, successType, failureType, oldFileDesc));
    }
    
    /**
     * Constructs a new UrnCallback that will possibly load the file with the given URNs.
     */
    private UrnCallback getNewUrnCallback(final File file, final List<? extends LimeXMLDocument> metadata, final int revision, 
                                final Type successType, final Type failureType, final FileDesc oldFileDesc) {
        return new UrnCallback() {
            public void urnsCalculated(File f, Set<? extends URN> urns) {
                FileDesc fd = null;
                synchronized(FileManagerImpl.this) {
                    if(revision != _revision) {
                        LOG.warn("Revisions changed, dropping share.");
                        resolveAndDispatchFileEvent(failureType, oldFileDesc, file);
                        return;
                    }
                
                    _numPendingFiles--;
                    
                    // Only load the file if we were able to calculate URNs 
                    // assume the fd is being shared
                    if(!urns.isEmpty()) {
                        fd = createFileDesc(file, urns, files.size());
                    }
                }
		        
		        if(fd == null) {
    		        // If URNs was empty, or loading failed, notify...
		            resolveAndDispatchFileEvent(failureType, oldFileDesc, file);
                    return;
		        }
                    
                // try loading the XML for this fileDesc
                dispatchFileEvent(new FileManagerEvent(FileManagerImpl.this, Type.LOAD_FILE, metadata, fd));

                numFiles += 1;
                files.add(fd);
                fileToFileDescMap.put(file, fd);
                updateUrnIndex(fd);

                //If the event is a addStoreFile event, just pass along the newly added FileDesc
                //else it was an UpdateEvent so pass along the oldFileDesc and newly added one
                if(successType == Type.ADD_FILE)
                    dispatchFileEvent(new FileManagerEvent(FileManagerImpl.this, successType, fd));
                else if( successType == Type.CHANGE_FILE ||
                         successType == Type.RENAME_FILE)
                    dispatchFileEvent(new FileManagerEvent(FileManagerImpl.this,successType, oldFileDesc, fd));
                
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
                return o == this;
            }
        };
    }
  
    /**
     * Creates a file descriptor for a given file and a set of urns
     * @param file - file to create descriptor for
     * @param urns - urns to use
     * @param index - index to use
     * @return
     */
    private FileDesc createFileDesc(File file, Set<? extends URN> urns, int index){
        FileDesc fileDesc = new FileDescImpl(fileDescMulticaster, file, urns, index);
        ContentResponseData r = contentManager.get().getResponse(fileDesc.getSHA1Urn());
        // if we had a response & it wasn't good, don't add this FD.
        if(r != null && !r.isOK())
            return null;
        else
        	return fileDesc;
    }

	/* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#removeFileIfShared(java.io.File)
     */
    public synchronized FileDesc removeFile(File f) {
        if(LOG.isDebugEnabled())
            LOG.debug("Removing file " + f);                
                
        try {
            f = FileUtils.getCanonicalFile(f);
        } catch (IOException e) {
            return null;
        }   
        
        FileDesc fd = getFileDesc(f);
        if(fd == null || !fileToFileDescMap.containsKey(f))
            return fd;

        removeFileDesc(fd);
        
        dispatchFileEvent(new FileManagerEvent(this, Type.REMOVE_FILE, fd));

        return fd;   
    }
    
    /**
     * Actually removes references to this FileDesc
     * @param fileDesc
     */
    private void removeFileDesc(FileDesc fileDesc) {
        removeUrnIndex(fileDesc);
        numFiles -= 1;
        files.set(fileDesc.getIndex(), null);
        fileToFileDescMap.remove(fileDesc.getFile());
        fileDescMulticaster.removeListeners(fileDesc);
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

            // nothing was shared for this URN, look at another
            if(!getIncompleteFileList().contains(getFileDesc(urn)))
                continue;
            
            // if there were indices for this URN, exit.
            IntSet shared = getIndices(urn);
            for (IntSet.IntSetIterator isIter = shared.iterator(); isIter.hasNext(); ) {
                int i = isIter.next();
                FileDesc desc = get(i);
                // unshared, keep looking.
                if (desc == null || !getIncompleteFileList().contains(desc))
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
        IncompleteFileDesc incompleteFileDesc = new IncompleteFileDescImpl(fileDescMulticaster,
                incompleteFile, urns, files.size(), name, size, vf);
        
        numFiles += 1;
        files.add(incompleteFileDesc);
        fileToFileDescMap.put(incompleteFile, incompleteFileDesc);
        updateUrnIndex(incompleteFileDesc);
        dispatchFileEvent(new FileManagerEvent(this, Type.ADD_FILE, incompleteFileDesc));
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#validate(com.limegroup.gnutella.FileDesc)
     */
    public void validate(final FileDesc fd) {
        if(_requestingValidation.add(fd.getSHA1Urn())) {
            contentManager.get().request(fd.getSHA1Urn(), new ContentResponseObserver() {
               public void handleResponse(URN urn, ContentResponseData r) {
                   _requestingValidation.remove(fd.getSHA1Urn());
                   if(r != null && !r.isOK())
                       removeFile(fd.getFile());
               }
            }, 5000);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    //  Search, utility, etc...
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * Generic method for adding a fileDesc's URNS to a map
     */
    private void updateUrnIndex(FileDesc fileDesc) {
        for(URN urn : fileDesc.getUrns()) {
            if (!urn.isSHA1())
                continue;
            IntSet indices= urnMap.get(urn);
            if (indices==null) {
                indices=new IntSet();
                urnMap.put(urn, indices);
            }
            indices.add(fileDesc.getIndex());
        }
    }
    
    /** 
     * Removes stored indices for a URN associated with a given FileDesc
     */
    private void removeUrnIndex(FileDesc fileDesc) {
        for(URN urn : fileDesc.getUrns()) {
            if (!urn.isSHA1())
                continue;
            //Lookup each of desc's URN's ind _urnMap.  
            //(It better be there!)
            IntSet indices = getIndices(urn);
            if (indices == null) {
                assert fileDesc instanceof IncompleteFileDesc;
                return;
            }

            //Delete index from set.  Remove set if empty.
            indices.remove(fileDesc.getIndex());
        }
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#renameFileIfSharedOrStore(java.io.File, java.io.File, com.limegroup.gnutella.FileEventListener)
     */
    public synchronized void renameFile(File oldName, final File newName) {
        if(LOG.isDebugEnabled())
            LOG.debug("Attempting to rename: " + oldName + " to: "  + newName);      
        
        try {
            oldName = FileUtils.getCanonicalFile(oldName);
        } catch (IOException e) {
            dispatchFileEvent(new FileManagerEvent(this, Type.RENAME_FILE_FAILED, oldName, null));
            return;
        }      
        
        FileDesc fileDesc = getFileDesc(oldName);
        if (fileDesc == null) {
            // couldn't find a FileDesc for this file
            dispatchFileEvent(new FileManagerEvent(this, Type.RENAME_FILE_FAILED, oldName, null));
            return;
        }
    
        removeFileDesc(fileDesc);
        // Prepopulate the cache with new URNs.
        urnCache.get().addUrns(newName, fileDesc.getUrns());

        List<LimeXMLDocument> xmlDocs = new LinkedList<LimeXMLDocument>(fileDesc.getLimeXMLDocuments());
        
        addFile(newName, xmlDocs, _revision, Type.RENAME_FILE, Type.REMOVE_FILE, fileDesc);
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#fileChanged(java.io.File)
     */
    public synchronized void fileChanged(File f, List<LimeXMLDocument> xmlDocs) {
        if (LOG.isDebugEnabled())
            LOG.debug("File Changed: " + f);

        FileDesc fd = getFileDesc(f);
        if (fd == null) {
            dispatchFileEvent(new FileManagerEvent(this, Type.CHANGE_FILE_FAILED, f));
            return;            
        }

        removeFileDesc(fd);
        addFile(f, xmlDocs, _revision, Type.CHANGE_FILE, Type.REMOVE_FILE, fd);
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
     * @see com.limegroup.gnutella.FileManager#hasApplicationSharedFiles()
     */
    public boolean hasApplicationSharedFiles() {
    	File [] files = SharingUtils.APPLICATION_SPECIAL_SHARE.listFiles();
    	if (files == null)
    		return false;
    	
    	// if at least one of the files in the application special
    	// share are currently shared, return true.
    	for (File f: files) {
            if (getGnutellaSharedFileList().contains(getFileDesc(f)))
    			return true;
    	}
    	
    	return false;
    }
    
    /**
     * Cleans all stale entries from the Set of individual files.
     */
    //TODO: this needs to clean all files
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
     * @see com.limegroup.gnutella.FileManager#isRareFile(com.limegroup.gnutella.FileDesc)
     */
    public boolean isRareFile(FileDesc fd) {
        return rareDefinition.evaluate(fd);
    }
	
    /** Returns true if file has a shareable extension.  Case is ignored. */
    static boolean hasShareableExtension(File file) {
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

        synchronized (_completelySharedDirectories) {
			return _completelySharedDirectories.contains(dir);
		}
	}
	
	/* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#isSharedDirectory(java.io.File)
     */
	public boolean isFolderShared(File dir) {
		if (dir == null)
			return false;
		
        synchronized (_completelySharedDirectories) {
			return _completelySharedDirectories.contains(dir);
		}
	}
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#isStoreDirectory(java.io.File)
     */
    public boolean isStoreDirectory(File file) {
        if(file == null)
            return false;
        
        synchronized(storeDirectories) {
            return storeDirectories.contains(file);
        }
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
     * @see com.limegroup.gnutella.FileManager#isFileApplicationShared(java.lang.String)
     */
    public boolean isFileApplicationShared(String name) {
    	File file = new File(SharingUtils.APPLICATION_SPECIAL_SHARE, name);
    	try {
    		file = FileUtils.getCanonicalFile(file);
    	} catch (IOException bad) {
    		return false;
    	}
        return getGnutellaSharedFileList().contains(getFileDesc(file));
    }
    
    public int size() {
        return numFiles;
    }
  
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#addFileEventListener(com.limegroup.gnutella.FileEventListener)
     */
    public void addFileEventListener(EventListener<FileManagerEvent> listener) {
        listenerSupport.addListener(listener);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#removeFileEventListener(com.limegroup.gnutella.FileEventListener)
     */
    public void removeFileEventListener(EventListener<FileManagerEvent> listener) {
        listenerSupport.removeListener(listener);
    }

    /**
     * Chooses how to dispatch the event.
     * 
     * AddFileIfSharedOrStore can be called for adding a new file, or when updating
     * a file such as a rename action. Additions actions don't have FDs at this point but
     * Update actions do. If a FD exists, will dispatch an event with that, if not
     * the file is used during dispatching
     */
    private void resolveAndDispatchFileEvent(Type eventType, FileDesc fd, File file) {
        if(fd != null) 
            dispatchFileEvent(new FileManagerEvent(this,eventType,fd));
        else
            dispatchFileEvent(new FileManagerEvent(this,eventType,file));
    }
    
    /**
     * dispatches a FileManagerEvent to any registered listeners 
     */
    protected void dispatchFileEvent(FileManagerEvent evt) {
        eventBroadcaster.broadcast(evt);
    }
    
    /** A bunch of inspectables for FileManager */
    @InspectableContainer
    private class FMInspectables {
        /*
         * 1 - used to create smaller qrp table
         * 2 - just sends the current table
         */
        private static final int VERSION = 2;

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
                    for (FileDesc fd : getGnutellaSharedFileList().getAllFileDescs()){
                        total++;
                        if (parser.evaluate(fd))
                            matched++;
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

            List<FileDesc> fds = getGnutellaSharedFileList().getAllFileDescs();
            hits.ensureCapacity(fds.size());
            uploads.ensureCapacity(fds.size());
            int rare = 0;
            int total = 0;
            for(FileDesc fd : fds ) {
                if (fd instanceof IncompleteFileDesc)
                    continue;
                total++;
                if (isRareFile(fd))
                    rare++;
                // locking FM->ALM ok.
                int numAlts = altLocManager.get().getNumLocs(fd.getSHA1Urn());
                if (!nonZero || numAlts > 0) {
                    alts.add((double)numAlts);
                    topAltsFDs.put(numAlts,fd);
                }
                int hitCount = fd.getHitCount();
                if (!nonZero || hitCount > 0) {
                    hits.add((double)hitCount);
                    topHitsFDs.put(hitCount, fd);
                }
                int upCount = fd.getAttemptedUploads();
                if (!nonZero || upCount > 0) {
                    uploads.add((double)upCount);
                    topUpsFDs.put(upCount, fd);
                }
                int cupCount = fd.getCompletedUploads();
                if (!nonZero || cupCount > 0) {
                    completeUploads.add((double)upCount);
                    topCupsFDs.put(cupCount, fd);
                }
                
                // keywords per fd
                double keywordsCount = 
                    HashFunction.getPrefixes(HashFunction.keywords(fd.getPath())).length;
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
            simppManager.get().addListener(this);
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
    
    public void setDirtySaveLater() {
        if(saver != null)
            saver.setDirty();
    }
    
    private class Saver implements Runnable, EventListener<FileManagerEvent> {
        
        private final AtomicBoolean isDirty = new AtomicBoolean(false);
        
        public void run() {
            if (!shutdown && isDirty.get()) {
                isDirty.getAndSet(false);
                save();
            }
        }

        public void setDirty() {
            isDirty.getAndSet(true);
        }

        /**
         * If a change occurs, write changes to disk
         */
        public void handleEvent(FileManagerEvent evt) {
            switch(evt.getType()) {
                case ADD_FILE:
                case CHANGE_FILE:
                case REMOVE_FILE:
                case RENAME_FILE:
                    setDirty();
            }
        }
    }
    
    ////////////////////////////////////////////////////////////////////////////////////////////
    //  DO NOTE ADD ANYTHING BELOW THIS. The following are here for backwards compatibility 
    //  with pre 5.0 versions. 
    ////////////////////////////////////////////////////////////////////////////////////////////
    
    
  /* (non-Javadoc)
   * @see com.limegroup.gnutella.FileManager#removeFolderIfShared(java.io.File)
   */
    public void removeSharedFolder(File folder) {
        _isUpdating = true;
        removeSharedFolder(folder, null);
        _isUpdating = false;
    }
  
  /**
   * REMOVES A GIVEN DIRECTORY FROM BEING COMPLETED SHARED.
   * IF 'PARENT' IS NULL, THIS WILL REMOVE IT FROM THE ROOT-LEVEL OF
   * SHARED FOLDERS IF IT EXISTED THERE.  (IF IT IS NON-NULL & IT WAS
   * A ROOT-LEVEL SHARED FOLDER, THE FOLDER REMAINS SHARED.)
   *
   * THE FIRST TIME THIS IS CALLED, PARENT MUST BE NON-NULL IN ORDER TO ENSURE
   * IT WORKS CORRECTLY.  OTHERWISE, WE'LL END UP ADDING TONS OF STUFF
   * TO THE DIRECTORIES_NOT_TO_SHARE.
   */
    private void removeSharedFolder(File folder, File parent) {
        if(!folder.isDirectory() && folder.exists()) 
            throw new IllegalArgumentException("Expected a directory, but given: " + folder);
          
        try {
            folder = FileUtils.getCanonicalFile(folder);
        } catch (IOException ignored) {}


        // GRAB THE VALUE QUICKLY.  RELEASE THE LOCK
        // SO THAT WE DON'T HOLD IT DURING A LONG RECURSIVE FUNCTION.
        // IT'S NO BIG DEAL IF IT CHANGES, WE'LL JUST DO SOME EXTRA WORK FOR A SHORT
        // BIT OF TIME
        boolean contained;
        synchronized (_completelySharedDirectories) {
            contained = _completelySharedDirectories.contains(folder);
        }

        if(contained) {
            if(parent != null && SharingSettings.DIRECTORIES_TO_SHARE.contains(folder)) {
                // we don't want to remove it since its a root share and we do not want
                // to remove any of its children
                return;
            } else if(parent == null) {
                // add the directory in the excluded list if it wasn't in the directories_not_to_share
                // or if it was and parent folder of it is fully shared
                boolean explicityShared = SharingSettings.DIRECTORIES_TO_SHARE.remove(folder);
                if(!explicityShared || isFolderShared(folder.getParentFile()))
                    _data.DIRECTORIES_NOT_TO_SHARE.add(folder);
            }
          
            // NOTE: that if parent != null && not a root share)
            // we do not add to directories not to share.
            // This is by design, because the parent has already been removed
            // from sharing, which inherently will remove the child directories.
            // there's no need to clutter up directories no to share with useless
            // entries
            synchronized (_completelySharedDirectories) {
                _completelySharedDirectories.remove(folder);
            }
            
            File[] subs = folder.listFiles();
            if(subs != null) {
                for(File f : subs) {
                    if(f.isDirectory())
                        removeSharedFolder(f, folder);
                    else if(f.isFile() && !getGnutellaSharedFileList().isIndividualFile(f)){
                        if(removeFile(f) == null)
                            urnCache.get().clearPendingHashesFor(f, this);
                    }
                }
            }
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
        updateSharedDirectories(folder, folder, null, _revision, 1);
        _isUpdating = false;
       
        return true;
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
                 if (!activityCallback.get().warnAboutSharingSensitiveDirectory(directory))
                     return;
             }
         }
     
         // Exit quickly (without doing the dir lookup) if revisions changed.
         if(_revision != revision)
             return;
    
         // STEP 1:
         // Add directory
         boolean isForcedShare = SharingUtils.isForcedShareDirectory(directory);
         
         boolean isStoreDirectory;
         synchronized (storeDirectories) {
             isStoreDirectory = !storeDirectories.contains(directory);
         }
         
         synchronized (_completelySharedDirectories) {
             // if it was already added, ignore.
             if (_completelySharedDirectories.contains(directory))
                 return;
    
             if (isStoreDirectory)
                 _completelySharedDirectories.add(directory);
         }
     
         // STEP 2:
         // Scan subdirectory for the amount of shared files.
         File[] file_list = directory.listFiles(SHAREABLE_FILE_FILTER);
         if (file_list == null)
             return;
         for(int i = 0; i < file_list.length && _revision == revision; i++)
             addSharedFile(file_list[i]);
             
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
}