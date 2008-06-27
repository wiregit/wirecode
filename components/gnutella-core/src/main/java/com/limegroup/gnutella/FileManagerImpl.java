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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.Comparators;
import org.limewire.collection.IntSet;
import org.limewire.collection.MultiCollection;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.inspection.Inspectable;
import org.limewire.inspection.InspectableContainer;
import org.limewire.inspection.InspectableForSize;
import org.limewire.inspection.InspectablePrimitive;
import org.limewire.inspection.InspectionPoint;
import org.limewire.lifecycle.Service;
import org.limewire.setting.StringArraySetting;
import org.limewire.statistic.StatsUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.NameValue;
import org.limewire.util.RPNParser;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.FileManagerEvent.Type;
import com.limegroup.gnutella.altlocs.AltLocManager;
import com.limegroup.gnutella.auth.ContentManager;
import com.limegroup.gnutella.auth.ContentResponseData;
import com.limegroup.gnutella.auth.ContentResponseObserver;
import com.limegroup.gnutella.downloader.VerifyingFile;
import com.limegroup.gnutella.library.LibraryData;
import com.limegroup.gnutella.library.SharingUtils;
import com.limegroup.gnutella.licenses.LicenseType;
import com.limegroup.gnutella.metadata.MetaDataReader;
import com.limegroup.gnutella.metadata.audio.AudioMetaData;
import com.limegroup.gnutella.routing.HashFunction;
import com.limegroup.gnutella.routing.QueryRouteTable;
import com.limegroup.gnutella.settings.DHTSettings;
import com.limegroup.gnutella.settings.MessageSettings;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.simpp.SimppListener;
import com.limegroup.gnutella.simpp.SimppManager;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLDocumentFactory;
import com.limegroup.gnutella.xml.LimeXMLNames;
import com.limegroup.gnutella.xml.LimeXMLSchema;
import com.limegroup.gnutella.xml.LimeXMLSchemaRepository;
import com.limegroup.gnutella.xml.LimeXMLUtils;

/**
 * The list of all shared files.  Provides operations to add and remove
 * individual files, directory, or sets of directories.  Provides a method to
 * efficiently query for files whose names contain certain keywords.<p>
 *
 * This class is thread-safe.
 */
@Singleton 
public class FileManagerImpl implements FileManager, Service {
	
    private static final Log LOG = LogFactory.getLog(FileManagerImpl.class);

    private static final ExecutorService LOADER = ExecutorsHelper.newProcessingQueue("FileManagerLoader");
     
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
    
    protected FileList sharedFileList = new SynchronizedFileList(new SharedFileListImpl());
    private FileList storeFileList = new SynchronizedFileList(new FileListImpl());
    
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
     * Files that are shared only for this LW session.
     * INVARIANT: no file can be in this and _data.SPECIAL_FILES_TO_SHARE
     * at the same time
     */
    @InspectableForSize("number of transiently shared files")
    private Set<File> _transientSharedFiles = new HashSet<File>();

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
    
    private Saver saver;
    
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
         
    /** Contains the definition of a rare file */
    private final RareFileDefinition rareDefinition;
    
    private final Provider<SimppManager> simppManager;
    private final Provider<UrnCache> urnCache;
    private final Provider<ContentManager> contentManager;
    private final Provider<AltLocManager> altLocManager;
    protected final Provider<ActivityCallback> activityCallback;
    protected final ScheduledExecutorService backgroundExecutor;
    protected final LimeXMLDocumentFactory limeXMLDocumentFactory;
    protected final MetaDataReader metaDataReader;
    protected final Provider<LimeXMLSchemaRepository> limeXMLSchemaRepository;
    
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
            LimeXMLDocumentFactory limeXMLDocumentFactory,
            MetaDataReader metaDataReader,
            Provider<LimeXMLSchemaRepository> limeXMLSchemaRepository) {
        this.simppManager = simppManager;
        this.urnCache = urnCache;
        this.contentManager = contentManager;
        this.altLocManager = altLocManager;
        this.activityCallback = activityCallback;
        this.backgroundExecutor = backgroundExecutor;
        this.limeXMLDocumentFactory = limeXMLDocumentFactory;
        this.metaDataReader = metaDataReader;
        this.limeXMLSchemaRepository = limeXMLSchemaRepository;
        
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
        sharedFileList.resetVariables();
        storeFileList.resetVariables();
        _numPendingFiles = 0;
        _extensions = new HashSet<String>();
		_completelySharedDirectories = new HashSet<File>();
        // the transient files and the special files.
        _individualSharedFiles = Collections.synchronizedCollection(
        		new MultiCollection<File>(_transientSharedFiles, _data.SPECIAL_FILES_TO_SHARE));
        _storeDirectories = new HashSet<File>();
    }

    public String getServiceName() {
        return org.limewire.i18n.I18nMarker.marktr("Shared Files");
    }
    
    public void initialize() {
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

    protected void save(){
        dispatchFileEvent(new FileManagerEvent(this, Type.FILEMANAGER_SAVE));
        _data.save();
    }
    
    public FileList getSharedFileList() {
        return sharedFileList;
    }
    
    public FileList getStoreFileList() {
        return storeFileList;
    }
	
    ///////////////////////////////////////////////////////////////////////////
    //  Accessors
    ///////////////////////////////////////////////////////////////////////////
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#getNumPendingFiles()
     */
    public int getNumPendingFiles() {
        return _numPendingFiles;
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
        if(sharedFileList.contains(f))
            return sharedFileList.getFileDesc(f);
        else
            return storeFileList.getFileDesc(f);
    }

	/* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#getFileDescForUrn(com.limegroup.gnutella.URN)
     */
	public synchronized FileDesc getFileDescForUrn(final URN urn) {
        if (!urn.isSHA1())
            throw new IllegalArgumentException();
        
        if( sharedFileList.contains(urn)){
            return sharedFileList.getFileDesc(urn);
        } else if ( storeFileList.contains(urn)) { 
            return storeFileList.getFileDesc(urn);
        } else {
            return null;
        }
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
            _storeDirectories.clear();
            _storeDirectories.add(SharingSettings.getSaveLWSDirectory());
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
    protected void loadFinished(int revision) {
        // save ourselves to disk every minute
        if (saver == null) {
            saver = new Saver();
            backgroundExecutor.scheduleWithFixedDelay(saver, 60 * 1000, 60 * 1000, 
                    TimeUnit.MILLISECONDS);
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
            addFileIfSharedOrStore(file, LimeXMLDocument.EMPTY_LIST, _revision, Type.ADD_FILE, Type.ADD_FAILED_FILE);
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
            addFileIfSharedOrStore(file, LimeXMLDocument.EMPTY_LIST, _revision, Type.ADD_STORE_FILE, Type.ADD_STORE_FAILED_FILE);
        }
        
        _isUpdating = false;
        
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
        
        synchronized (this) {
            // if it was already added, ignore.
            if (_completelySharedDirectories.contains(directory))
                return;

            if (!_storeDirectories.contains(directory))
				_completelySharedDirectories.add(directory);
            if (!isForcedShare) {
                dispatchFileEvent(new FileManagerEvent(this, Type.ADD_FOLDER, rootShare, depth, directory, parent));
            }
        }
        
        // STEP 2:
        // Scan subdirectory for the amount of shared files.
        File[] file_list = directory.listFiles(SHAREABLE_FILE_FILTER);
        if (file_list == null)
            return;
        for(int i = 0; i < file_list.length && _revision == revision; i++)
            addFileIfSharedOrStore(file_list[i], LimeXMLDocument.EMPTY_LIST, _revision, Type.ADD_FILE, Type.ADD_FAILED_FILE);
            
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
            dispatchFileEvent(new FileManagerEvent(this, Type.ADD_STORE_FOLDER, directory, parent));
        }
        
        // STEP 2:
        // Scan subdirectory for the amount of shared files.
        File[] file_list = directory.listFiles();
        if (file_list == null)
            return;
        for(int i = 0; i < file_list.length && _revision == revision; i++)
            addFileIfSharedOrStore(file_list[i], LimeXMLDocument.EMPTY_LIST, _revision, Type.ADD_STORE_FILE, Type.ADD_STORE_FAILED_FILE);
            
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
                            urnCache.get().clearPendingHashesFor(f, this);
                        if(storeFileList.contains(f)) 
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
		addFileAlways(file, LimeXMLDocument.EMPTY_LIST);
	}
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#addFileAlways(java.io.File, java.util.List, com.limegroup.gnutella.FileEventListener)
     */
	 public void addFileAlways(File file, List<? extends LimeXMLDocument> list) {
		_data.FILES_NOT_TO_SHARE.remove(file);
		if (!isFileShareable(file))
			_data.SPECIAL_FILES_TO_SHARE.add(file);
			
		addFileIfSharedOrStore(file,list,_revision, Type.ADD_FILE, Type.ADD_FAILED_FILE);
	}

     /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#addFileForSession(java.io.File, com.limegroup.gnutella.FileEventListener)
     */
     public void addFileForSession(File file) {
         _data.FILES_NOT_TO_SHARE.remove(file);
         if (!isFileShareable(file))
             _transientSharedFiles.add(file);
         addFileIfSharedOrStore(file, LimeXMLDocument.EMPTY_LIST, _revision, Type.ADD_FILE, Type.ADD_FAILED_FILE);
     }
	
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#addFileIfShared(java.io.File)
     */
   public void addFileIfShared(File file) {
       addFileIfSharedOrStore(file, LimeXMLDocument.EMPTY_LIST, _revision, Type.ADD_FILE, Type.ADD_FAILED_FILE);
   }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#addFileIfShared(java.io.File, java.util.List)
     */
    public void addFileIfShared(File file, List<? extends LimeXMLDocument> list) {
        addFileIfSharedOrStore(file, list, _revision, Type.ADD_FILE, Type.ADD_FAILED_FILE);
    }
    
    protected void addFileIfSharedOrStore(File file, List<? extends LimeXMLDocument> metadata, int revision,
            Type successType, Type failureType) {
        addFileIfSharedOrStore(file, metadata, revision, successType, failureType, null);
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
    protected void addFileIfSharedOrStore(File file, List<? extends LimeXMLDocument> metadata, int revision, 
            Type successType, Type failureType, FileDesc oldFileDesc) {
        if(LOG.isDebugEnabled())
            LOG.debug("Attempting to load store or shared file: " + file);

        // Make sure capitals are resolved properly, etc.
        try {
            file = FileUtils.getCanonicalFile(file);
        } catch (IOException e) {
            resolveAndDispatchFileEvent(failureType, oldFileDesc, file);
            return;
	    }
	    
			// if file is not shareable, also remove it from special files
			// to share since in that case it's not physically shareable then
        if (!isFileShareable(file) && !isFileLocatedStoreDirectory(file)) { 
		    	_individualSharedFiles.remove(file);
		    resolveAndDispatchFileEvent(failureType, oldFileDesc, file);
            return;
        }
        
        if(storeFileList.contains(file)) {
            return;
		}
        
        if(sharedFileList.contains(file)) {
            dispatchFileEvent( new FileManagerEvent(this, Type.ALREADY_SHARED_FILE, file));
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
    protected UrnCallback getNewUrnCallback(final File file, final List<? extends LimeXMLDocument> metadata, final int revision, 
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
                        int fileIndex = sharedFileList.getListLength();
                        fd = createFileDesc(file, urns, fileIndex);
                    }
                }
		        
		        if(fd == null) {
    		        // If URNs was empty, or loading failed, notify...
		            resolveAndDispatchFileEvent(failureType, oldFileDesc, file);
                    return;
		        }
                    
                // try loading the fd so we can check the LimeXML info
		        dispatchFileEvent(new FileManagerEvent(FileManagerImpl.this, Type.LOAD_FILE, metadata, fd));

                // check LimeXML to determine if is a store file or the sha1 is mapped to a store file 
                //  (the sha1 check is needed if duplicate store files are loaded since the second file 
                //  will not have a unique LimeXMLDoc associated with it)
                if (isStoreXML(fd.getXMLDocument()) || storeFileList.contains(fd.getSHA1Urn()) ) { 
                    addStoreFile(fd, file, urns, successType, oldFileDesc);
                } else {
                    addSharedFile(file, fd, urns, successType, oldFileDesc);
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
                return o == this;
            }
        };
    }
  
    /**
     * Creates a file descriptor for the given file and places the fd into the set
     * of LWS file descriptors
     */
    private synchronized void addStoreFile(FileDesc fd, File file, Set<? extends URN> urns, Type successType, FileDesc oldFileDesc) {
        if(LOG.isDebugEnabled())
            LOG.debug("Store file: " + file);

        // if this file is in a shared folder, add to individual store files 
        if( successType == Type.ADD_FILE) {
            _data.SPECIAL_STORE_FILES.add(file);
            // recast event to addStoreFile type
            successType = Type.ADD_STORE_FILE;
        }

        //store files are not part of the _files list so recreate fd with an index into store file list
        FileDesc fileDesc = createFileDesc(file, urns, storeFileList.getListLength());
        //add the xml doc to the new FileDesc
        if( fd.getXMLDocument() != null )
            fileDesc.addLimeXMLDocument(fd.getXMLDocument());

        storeFileList.addFile(file, fileDesc);

        //If the event is a addStoreFile event, just pass along the newly added FileDesc
        //else it was an UpdateEvent so pass along the oldFileDesc and newly added one
        if(successType == Type.ADD_STORE_FILE)
            dispatchFileEvent(new FileManagerEvent(this, successType, fileDesc));
        else
            dispatchFileEvent(new FileManagerEvent(this,successType, oldFileDesc, fileDesc));

    }
    
    /**
     * Handles the actual sharing of a file by placing the file descriptor into the set of shared files
     */
    private synchronized void addSharedFile(File file, FileDesc fileDesc, Set<? extends URN> urns, Type successType, FileDesc oldFileDesc) {
        if(LOG.isDebugEnabled())
            LOG.debug("Sharing file: " + file);
               
        // since we created the FD to test the XML for being an instance of LWS, check to make sure
        //  the FD is still valid before continuing
        if( fileDesc.getIndex() != sharedFileList.getListLength()){
            LimeXMLDocument doc = fileDesc.getXMLDocument();
            fileDesc = createFileDesc(file, urns, sharedFileList.getListLength());
            if( doc != null )
                fileDesc.addLimeXMLDocument(doc);
        }

        sharedFileList.addFile(file, fileDesc);
        
        //If the event is a addFile event, just pass along the newly added FileDesc
        //else it was an UpdateEvent so pass along the oldFileDesc and newly added one
        if(successType == Type.ADD_FILE)
            dispatchFileEvent(new FileManagerEvent(this, successType, fileDesc));
        else
            dispatchFileEvent(new FileManagerEvent(this,successType, oldFileDesc, fileDesc));
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
        ContentResponseData r = contentManager.get().getResponse(fileDesc.getSHA1Urn());
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
		if( storeFileList.contains(file))
		    return null;
		
		// remove file already here to heed against race conditions
		// wrt to filemanager events being handled on other threads
		boolean removed = _individualSharedFiles.remove(file); 
		FileDesc fd = removeFileIfSharedOrStore(file);
		if (fd == null) {
		    urnCache.get().clearPendingHashesFor(file, this);
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
        
        if( storeFileList.contains(toRemove) )
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
        if (!sharedFileList.contains(f))
            return null;
            
        FileDesc fd = sharedFileList.getFileDesc(f);

        // If it's an incomplete file, the only reference we 
        // have is the URN, so remove that and be done.
        // We also return false, because the file was never really
        // "shared" to begin with.
        if (fd instanceof IncompleteFileDesc) {
            sharedFileList.removeIncomplete((IncompleteFileDesc)fd);
            removeSharedUrnIndex(fd, false);

            // Notify the GUI...                                              
            if(notify)
                dispatchFileEvent(new FileManagerEvent(this, Type.REMOVE_FILE, fd));

            return fd;
        }

        sharedFileList.remove(fd);
        File parent = f.getParentFile();
        // files that are forcibly shared over the network aren't counted
        if(SharingUtils.isForcedShareDirectory(parent)) {
            notify = false;
        }


        //Remove hash information.
        removeSharedUrnIndex(fd, true);
  
        // Notify the GUI...        
        if(notify)
            dispatchFileEvent(new FileManagerEvent(this, Type.REMOVE_FILE, fd));
        dispatchFileEvent(new FileManagerEvent(this, Type.REMOVE_FD, fd));
      
        return fd;
    }
    
    protected synchronized FileDesc removeStoreFile(File f, boolean notify){
        //Take care of case, etc.
        try {
            f = FileUtils.getCanonicalFile(f);
        } catch (IOException e) {
            return null;
        }        

        // Look for matching file ...         
        if (!storeFileList.contains(f))
            return null;
        
        FileDesc fd = storeFileList.getFileDesc(f);
        storeFileList.remove(fd);

        _data.SPECIAL_STORE_FILES.remove(f);

        //Remove hash information.
        removeStoreUrnIndex(fd, true);
 
        // Notify the GUI...              
        if(notify)
            dispatchFileEvent(new FileManagerEvent(this, Type.REMOVE_STORE_FILE, fd));
        dispatchFileEvent(new FileManagerEvent(this, Type.REMOVE_FD, fd));

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
            // nothing was shared for this URN, look at another
            if (!sharedFileList.contains(urn))
                continue;
            
            IntSet shared = sharedFileList.getIndicesForUrn(urn);
            for (IntSet.IntSetIterator isIter = shared.iterator(); isIter.hasNext(); ) {
                int i = isIter.next();
                FileDesc desc = sharedFileList.get(i);
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
        int fileIndex = sharedFileList.getListLength();
        
        IncompleteFileDesc ifd = new IncompleteFileDesc(
        incompleteFile, urns, fileIndex, name, size, vf);
        sharedFileList.addIncompleteFile(incompleteFile, ifd);
        fileURNSUpdated(ifd);
        
        dispatchFileEvent(new FileManagerEvent(this, Type.ADD_FILE, ifd));
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
                       removeFileIfSharedOrStore(fd.getFile());
               }
            }, 5000);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    //  Search, utility, etc...
    ///////////////////////////////////////////////////////////////////////////
	
    public synchronized void fileURNSUpdated(FileDesc fd) {
        FileManagerEvent event = null; 
        synchronized (this) {
            sharedFileList.updateUrnIndex(fd);
            if (fd instanceof IncompleteFileDesc) {
                IncompleteFileDesc ifd = (IncompleteFileDesc) fd;
                if (SharingSettings.ALLOW_PARTIAL_SHARING.getValue() &&
                        SharingSettings.LOAD_PARTIAL_KEYWORDS.getValue() &&
                        ifd.hasUrnsAndPartialData()) {
                    event = new FileManagerEvent(this, Type.CHANGE_FILE, fd);
                }
            }
        }
        if (event != null) {
            dispatchFileEvent(event);
        }

    }

    /** 
     * Removes any URN index information for desc from shared files
     * @param purgeState true if any state should also be removed (creation time, altlocs) 
     */
    private synchronized void removeSharedUrnIndex(FileDesc fileDesc, boolean purgeState) {
        removeUrnIndex(fileDesc, sharedFileList, purgeState);
    }
    
    /** 
     * Removes any URN index information for desc from store files
     * @param purgeState true if any state should also be removed (creation time, altlocs) 
     */
    private synchronized void removeStoreUrnIndex(FileDesc fileDesc, boolean purgeState) {
        removeUrnIndex(fileDesc, storeFileList, purgeState);
    }
    
    private synchronized void removeUrnIndex(FileDesc fileDesc, FileList fileList, boolean purgeState) {
        for(URN urn : fileDesc.getUrns()) {
            if (!urn.isSHA1())
                continue;
            //Lookup each of desc's URN's ind _urnMap.  
            //(It better be there!)
            IntSet indices= fileList.getIndicesForUrn(urn);
            if (indices == null) {
                assert fileDesc instanceof IncompleteFileDesc;
                return;
            }

            //Delete index from set.  Remove set if empty.
            indices.remove(fileDesc.getIndex());
            if (indices.size()==0 && purgeState) {
                fileList.remove(urn);
                dispatchFileEvent(new FileManagerEvent(this,Type.REMOVE_URN, urn));
            }
        }
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#renameFileIfSharedOrStore(java.io.File, java.io.File, com.limegroup.gnutella.FileEventListener)
     */
    public synchronized void renameFileIfSharedOrStore(final File oldName, final File newName) {
        FileDesc toRemove = getFileDescForFile(oldName);
        if (toRemove == null ) {
            dispatchFileEvent(new FileManagerEvent(this, Type.ADD_FAILED_FILE, oldName));
            return;
        }
        
        if(LOG.isDebugEnabled())
            LOG.debug("Attempting to rename: " + oldName + " to: "  + newName);
            
        List<LimeXMLDocument> xmlDocs = new LinkedList<LimeXMLDocument>(toRemove.getLimeXMLDocuments());
        
        // if its a shared file, store files all have the same index in their filedescriptor
        if( !storeFileList.contains(toRemove)) {
		    final FileDesc removed = removeFileIfShared(oldName, false);
            assert removed == toRemove : "invariant broken.";
		    if (_data.SPECIAL_FILES_TO_SHARE.remove(oldName) && !isFileInCompletelySharedDirectory(newName))
			    _data.SPECIAL_FILES_TO_SHARE.add(newName);
			
            // Prepopulate the cache with new URNs.
            urnCache.get().addUrns(newName, removed.getUrns());

            addFileIfSharedOrStore(newName, xmlDocs, _revision, Type.RENAME_FILE, Type.REMOVE_FILE, removed);
        }   
        // its a store files
        else  {
            final FileDesc removed = removeStoreFile(oldName, false);
            assert removed == toRemove : "invariant broken.";
            if (_data.SPECIAL_STORE_FILES.remove(oldName)) 
                _data.SPECIAL_STORE_FILES.add(newName);
            // Prepopulate the cache with new URNs.
            urnCache.get().addUrns(newName, removed.getUrns());
    
            addFileIfSharedOrStore(newName, xmlDocs, _revision, Type.RENAME_FILE, Type.REMOVE_STORE_FILE, removed);
        }
    }
    

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManager#fileChanged(java.io.File)
     */
    public void fileChanged(File f) {
        if (LOG.isTraceEnabled())
            LOG.debug("File Changed: " + f);

        FileDesc fd = getFileDescForFile(f);
        if (fd == null)
            return;

        List<LimeXMLDocument> xmlDocs = fd.getLimeXMLDocuments();
        if (LimeXMLUtils.isEditableFormat(f)) {
            try {
                LimeXMLDocument diskDoc = metaDataReader.readDocument(f);
                xmlDocs = resolveWriteableDocs(xmlDocs, diskDoc);
            } catch (IOException e) {
                // if we were unable to read this document,
                // then simply add the file without metadata.
                xmlDocs = Collections.emptyList();
            }
        }

        final FileDesc removed = removeFileIfSharedOrStore(f, false);
        assert fd == removed : "wanted to remove: " + fd + "\ndid remove: " + removed;

        addFileIfSharedOrStore(f, xmlDocs, _revision, Type.CHANGE_FILE, Type.REMOVE_FILE, removed);
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
    		if (sharedFileList.contains(f))
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
     * @see com.limegroup.gnutella.FileManager#isFileApplicationShared(java.lang.String)
     */
    public boolean isFileApplicationShared(String name) {
    	File file = new File(SharingUtils.APPLICATION_SPECIAL_SHARE, name);
    	try {
    		file = FileUtils.getCanonicalFile(file);
    	} catch (IOException bad) {
    		return false;
    	}
    	return sharedFileList.contains(file);
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
     * Chooses how to dispatch the event.
     * 
     * AddFileIfSharedOrStore can be called for adding a new file, or when updating
     * a file such as a rename action. Additions actions don't have FDs at this point but
     * Update actions do. If a FD exists, will dispatch an event with that, if not
     * the file is used during dispatching
     */
    protected void resolveAndDispatchFileEvent(Type eventType, FileDesc fd, File file) {
        if(fd != null) 
            dispatchFileEvent(new FileManagerEvent(this,eventType,fd));
        else
            dispatchFileEvent(new FileManagerEvent(this,eventType,file));
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
    public Iterator<FileDesc> getIndexingIterator() {
        return new Iterator<FileDesc>() {
            int startRevision = _revision;
            /** Points to the index that is to be examined next. */
            int index = 0;
            FileDesc preview;

            private boolean preview() {
                assert preview == null;

                if (_revision != startRevision) {
                    return false;
                }

                synchronized (sharedFileList) {
                    while (index < sharedFileList.getListLength()) {
                        FileDesc desc = sharedFileList.get(index);
                        index++;

                        // skip, if the file was unshared or is an incomplete file,
                        if (desc == null || desc instanceof IncompleteFileDesc || SharingUtils.isForcedShare(desc)) 
                            continue;

                        preview = desc;
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
                    if (sharedFileList.get(index-1) == null) {
                        // file was removed in the meantime
                        preview = null;
                    }
                }
                return preview != null || preview();
            }

            public FileDesc next() {
                if (hasNext()) {
                    FileDesc item = preview;
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
    
    /**
     * Finds the audio metadata document in allDocs, and makes it's id3 fields
     * identical with the fields of id3doc (which are only id3).
     */
    private List<LimeXMLDocument> resolveWriteableDocs(List<LimeXMLDocument> allDocs,
            LimeXMLDocument id3Doc) {
        LimeXMLDocument audioDoc = null;
        LimeXMLSchema audioSchema = limeXMLSchemaRepository.get().getSchema(LimeXMLNames.AUDIO_SCHEMA);
        
        for (LimeXMLDocument doc : allDocs) {
            if (doc.getSchema() == audioSchema) {
                audioDoc = doc;
                break;
            }
        }

        if (id3Doc.equals(audioDoc)) // No issue -- both documents are the
                                        // same
            return allDocs; // did not modify list, keep using it

        List<LimeXMLDocument> retList = new ArrayList<LimeXMLDocument>();
        retList.addAll(allDocs);

        if (audioDoc == null) {// nothing to resolve
            retList.add(id3Doc);
            return retList;
        }

        // OK. audioDoc exists, remove it
        retList.remove(audioDoc);

        // now add the non-id3 tags from audioDoc to id3doc
        List<NameValue<String>> audioList = audioDoc.getOrderedNameValueList();
        List<NameValue<String>> id3List = id3Doc.getOrderedNameValueList();
        for (int i = 0; i < audioList.size(); i++) {
            NameValue<String> nameVal = audioList.get(i);
            if (AudioMetaData.isNonLimeAudioField(nameVal.getName()))
                id3List.add(nameVal);
        }
        audioDoc = limeXMLDocumentFactory.createLimeXMLDocument(id3List, LimeXMLNames.AUDIO_SCHEMA);
        retList.add(audioDoc);
        return retList;
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
                    for (FileDesc fd : sharedFileList.getAllFileDescs()){
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

            List<FileDesc> fds = sharedFileList.getAllFileDescs();
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
    
    private class Saver implements Runnable {
        public void run() {
            if (!shutdown && isLoadFinished())
                save();
        }
    }
}