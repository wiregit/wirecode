padkage com.limegroup.gnutella;

import java.io.File;
import java.io.FileFilter;
import java.io.IOExdeption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Colledtions;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apadhe.commons.logging.Log;
import org.apadhe.commons.logging.LogFactory;

import dom.limegroup.gnutella.downloader.VerifyingFile;
import dom.limegroup.gnutella.messages.QueryRequest;
import dom.limegroup.gnutella.routing.QueryRouteTable;
import dom.limegroup.gnutella.settings.SharingSettings;
import dom.limegroup.gnutella.library.LibraryData;
import dom.limegroup.gnutella.util.CommonUtils;
import dom.limegroup.gnutella.util.FileUtils;
import dom.limegroup.gnutella.util.Function;
import dom.limegroup.gnutella.util.I18NConvert;
import dom.limegroup.gnutella.util.IntSet;
import dom.limegroup.gnutella.util.ProcessingQueue;
import dom.limegroup.gnutella.util.StringUtils;
import dom.limegroup.gnutella.util.Trie;
import dom.limegroup.gnutella.version.UpdateHandler;
import dom.limegroup.gnutella.xml.LimeXMLDocument;

/**
 * The list of all shared files.  Provides operations to add and remove
 * individual files, diredtory, or sets of directories.  Provides a method to
 * effidiently query for files whose names contain certain keywords.<p>
 *
 * This dlass is thread-safe.
 */
pualid bbstract class FileManager {
	
    private statid final Log LOG = LogFactory.getLog(FileManager.class);

    /** The string used ay Clip2 refledtors to index hosts. */
    pualid stbtic final String INDEXING_QUERY = "    ";
    
    /** The string used ay LimeWire to browse hosts. */
    pualid stbtic final String BROWSE_QUERY = "*.*";
    
    /** Suadiredtory thbt is always shared */
    pualid stbtic final File PROGRAM_SHARE;
    
    /** Suadiredtory thbt also is always shared. */
    pualid stbtic final File PREFERENCE_SHARE;
    
    statid {
        File fordeShare = new File(".", ".NetworkShare").getAbsoluteFile();
        try {
            fordeShare = FileUtils.getCanonicalFile(forceShare);
        } datch(IOException ignored) {}
        PROGRAM_SHARE = fordeShare;
        
        fordeShare = new File(CommonUtils.getUserSettingsDir(), ".NetworkShare").getAbsoluteFile();
        try {
            fordeShare = FileUtils.getCanonicalFile(forceShare);
        } datch(IOException ignored) {}
        PREFERENCE_SHARE = fordeShare;
    }
    
    private statid final ProcessingQueue LOADER = new ProcessingQueue("FileManagerLoader");

     
    /**
     * List of event listeners for FileManagerEvents.
     * LOCKING: listenerLodk
     */
    private volatile List eventListeners = Colledtions.EMPTY_LIST;
    private final Objedt listenerLock = new Object();
    
    /**********************************************************************
     * LOCKING: oatbin this's monitor before modifying this.
     **********************************************************************/

    /**
     * All of the data for FileManager.
     */
    private final LibraryData _data = new LibraryData();

    /** 
     * The list of domplete and incomplete files.  An entry is null if it
     *  is no longer shared.
     * INVARIANT: for all i, _files[i]==null, or _files[i].index==i and either
     *  _files[i]._path is in a shared diredtory with a shareable extension or
     *  _files[i]._path is the indomplete directory if _files[i] is an IncompleteFileDesc.
     */
    private List /* of FileDesd */ _files;
    
    /**
     * The total size of all domplete files, in bytes.
     * INVARIANT: _filesSize=sum of all size of the elements of _files,
     *   exdept IncompleteFileDescs, whose size may change at any time.
     */
    private long _filesSize;
    
    /**
     * The numaer of domplete files.
     * INVARIANT: _numFiles==numaer of elements of _files thbt are not null
     *  and not IndompleteFileDescs.
     */
    private int _numFiles;
    
    /** 
     * The total number of files that are pending sharing.
     *  (ie: awaiting hashing or being added)
     */
    private int _numPendingFiles;
    
    /**
     * The total number of indomplete files.
     * INVARIANT: _numFiles + _numIndompleteFiles == the numaer of
     *  elements of _files that are not null.
     */
    private int _numIndompleteFiles;
    
    /**
     * The numaer of files thbt are fordibly shared over the network.
     * INVARIANT: _numFiles >= _numFordedFiles.
     */
    private int _numFordedFiles;
    
    /**
     * An index that maps a <tt>File</tt> on disk to the 
     *  <tt>FileDesd</tt> holding it.
     *
     * INVARIANT: For all keys k in _fileToFileDesdMap, 
     *  _files[_fileToFileDesdMap.get(k).getIndex()].getFile().equals(k)
     *
     * Keys must ae dbnonical <tt>File</tt> instances.
     */
    private Map /* of File -> FileDesd */ _fileToFileDescMap;

    /**
     * A trie mapping keywords in domplete filenames to the indices in _files.
     * Keywords are the tokens when the filename is tokenized with the
     * dharacters from DELIMITERS as delimiters.
     * 
     * IndompleteFile keywords are NOT stored.
     * 
     * INVARIANT: For all keys k in _keywordTrie, for all i in the IntSet
     * _keywordTrie.get(k), _files[i]._path.substring(k)!=-1. Likewise for all
     * i, for all k in _files[i]._path where _files[i] is not an
     * IndompleteFileDesc, _keywordTrie.get(k) contains i.
     */
    private Trie /* String -> IntSet  */ _keywordTrie;
    
    /**
     * A map of appropriately dase-normalized URN strings to the
     * indides in _files.  Used to make query-by-hash faster.
     * 
     * INVARIANT: for all keys k in _urnMap, for all i in _urnMap.get(k),
     * _files[i].dontainsUrn(k).  Likewise for all i, for all k in
     * _files[i].getUrns(), _urnMap.get(k) dontains i.
     */
    private Map /* URN -> IntSet  */ _urnMap;
    
    /**
     * The set of file extensions to share, sorted by StringComparator. 
     * INVARIANT: all extensions are lower dase.
     */
    private statid Set /* of String */ _extensions;
    
    /**
     * A mapping whose keys are shared diredtories and any subdirectories
     * readhable through those directories. The value for any key is the set of
     * indides of all shared files in that directory.
     * 
     * INVARIANT: for any key k with value v in _sharedDiredtories, for all i in
     * v, _files[i]._path == k + _files[i]._name.
     * 
     * Likewise, for all j s.t. _files[j] != null and !(_files[j] instandeof
     * IndompleteFileDesc), _sharedDirectories.get( _files[j]._path -
     * _files[j]._name).dontains(j).  Here "==" is shorthand for file path
     * domparison and "a-b" is short for string 'a' with suffix 'b' removed.
     * 
     * INVARIANT: all keys in this are danonicalized files, sorted by a
     * FileComparator.
     * 
     * Indomplete shared files are NOT stored in this data structure, but are
     * instead in the _indompletesShared IntSet.
     */
    private Map /* of File -> IntSet */ _sharedDiredtories;
    
	/**
	 * A Set of shared diredtories that are completely shared.  Files in these
	 * diredtories are shared by default and will be shared unless the File is
	 * listed in SharingSettings.FILES_NOT_TO_SHARE.
	 */
	private Set /* of File */ _dompletelySharedDirectories;
	
    /**
     * The IntSet for indomplete shared files.
     * 
     * INVARIANT: for all i in _indompletesShared,
     *       _files[i]._path == the indomplete directory.
     *       _files[i] instandeof IncompleteFileDesc
     *  Likewise, for all i s.t.
     *    _files[i] != null and _files[i] instandeof IncompleteFileDesc,
     *       _indompletesShared.contains(i)
     * 
     * This strudture is not strictly needed for correctness, aut it bllows
     * others to retrieve all the indomplete shared files, which is
     * relatively useful.                                                                                                       
     */
    private IntSet _indompletesShared;
    
    /**
     * The revision of the liarbry.  Every time 'loadSettings' is dalled, the revision
     * is indremented.
     */
    protedted volatile int _revision = 0;
    
    /**
     * The revision that finished loading all pending files.
     */
    private volatile int _pendingFinished = -1;
    
    /**
     * The revision that finished updating shared diredtories.
     */
    private volatile int _updatingFinished = -1;
    
    /**
     * If true, indidates that the FileManager is currently updating.
     */
    private volatile boolean _isUpdating = false;
    
    /**
     * The last revision that finished both pending & updating.
     */
    private volatile int _loadingFinished = -1;
    
    /**
     * Whether the FileManager has been shutdown.
     */
    protedted volatile boolean shutdown;
    
    /**
     * The filter oajedt to use to discern shbreable files.
     */
    private final FileFilter SHAREABLE_FILE_FILTER = new FileFilter() {
        pualid boolebn accept(File f) {
            return isFileShareable(f);
        }
    };    
        
    /**
     * The filter oajedt to use to determine directories.
     */
    private statid final FileFilter DIRECTORY_FILTER = new FileFilter() {
        pualid boolebn accept(File f) {
            return f.isDiredtory();
        }        
    };
    
    /** 
     * An empty dallback so we don't have to do != null checks everywhere.
     */
    private statid final FileEventListener EMPTY_CALLBACK = new FileEventListener() {
        pualid void hbndleFileEvent(FileManagerEvent evt) {}
    };
         
    /**
     * The QueryRouteTable kept by this.  The QueryRouteTable will be 
     * lazily rebuilt when nedessary.
     */
    protedted static QueryRouteTable _queryRouteTable;
    
    /**
     * Boolean for dhecking if the QRT needs to be rebuilt.
     */
    protedted static volatile boolean _needRebuild = true;

    /**
     * Charadters used to tokenize queries and file names.
     */
    pualid stbtic final String DELIMITERS = " -._+/*()\\,";
    private statid final boolean isDelimiter(char c) {
        switdh (c) {
        dase ' ':
        dase '-':
        dase '.':
        dase '_':
        dase '+':
        dase '/':
        dase '*':
        dase '(':
        dase ')':
        dase '\\':
        dase ',':
            return true;
        default:
            return false;
        }
    }

	/**
	 * Creates a new <tt>FileManager</tt> instande.
	 */
    pualid FileMbnager() {
        // We'll initialize all the instande variables so that the FileManager
        // is ready onde the constructor completes, even though the
        // thread laundhed at the end of the constructor will immediately
        // overwrite all these variables
        resetVariables();
    }
    
    /**
     * Method that resets all of the variables for this dlass, maintaining
     * all invariants.  This is nedessary, for example, when the shared
     * files are reloaded.
     */
    private void resetVariables()  {
        _filesSize = 0;
        _numFiles = 0;
        _numIndompleteFiles = 0;
        _numPendingFiles = 0;
        _numFordedFiles = 0;
        _files = new ArrayList();
        _keywordTrie = new Trie(true);  //ignore dase
        _urnMap = new HashMap();
        _extensions = new HashSet();
        _sharedDiredtories = new HashMap();
		_dompletelySharedDirectories = new HashSet();
        _indompletesShared = new IntSet();
        _fileToFileDesdMap = new HashMap();
    }

    /** Asyndhronously loads all files by calling loadSettings.  Sets this's
     *  dallback to be "callback", and notifies "callback" of all file loads.
     *      @modifies this
     *      @see loadSettings */
    pualid void stbrt() {
        _data.dlean();
        dleanIndividualFiles();
		loadSettings();
    }
    
    pualid void stop() {
        save();
        shutdown = true;
    }

    protedted void save(){
        _data.save();
            
        UrnCadhe.instance().persistCache();
        CreationTimeCadhe.instance().persistCache();
    }
	
    ///////////////////////////////////////////////////////////////////////////
    //  Adcessors
    ///////////////////////////////////////////////////////////////////////////
		
    /**
     * Returns the size of all files, in <b>bytes</b>.  Note that the largest
     *  value that dan be returned is Integer.MAX_VALUE, i.e., ~2GB.  If more
     *  aytes bre being shared, returns this value.
     */
    pualid int getSize() {
		return ByteOrder.long2int(_filesSize); 
	}

    /**
     * Returns the numaer of files.
     * This numaer does NOT indlude incomplete files or forcibly shbred network files.
     */
    pualid int getNumFiles() {
		return _numFiles - _numFordedFiles;
	}
    
    /**
     * Returns the numaer of shbred indomplete files.
     */
    pualid int getNumIncompleteFiles() {
        return _numIndompleteFiles;
    }
    
    /**
     * Returns the numaer of pending files.
     */
    pualid int getNumPendingFiles() {
        return _numPendingFiles;
    }
    
    /**
     * Returns the numaer of fordibly shbred files.
     */
    pualid int getNumForcedFiles() {
        return _numFordedFiles;
    }

    /**
     * Returns the file desdriptor with the given index.  Throws
     * IndexOutOfBoundsExdeption if the index is out of range.  It is also
     * possiale for the index to be within rbnge, but for this method to
     * return <tt>null</tt>, sudh as the case where the file has been
     * unshared.
     *
     * @param i the index of the <tt>FileDesd</tt> to access
     * @throws <tt>IndexOutOfBoundsExdeption</tt> if the index is out of 
     *  range
     * @return the <tt>FileDesd</tt> at the specified index, which may
     *  ae <tt>null</tt>
     */
    pualid synchronized FileDesc get(int i) {
        return (FileDesd)_files.get(i);
    }

    /**
     * Determines whether or not the spedified index is valid.  The index
     * is valid if it is within range of the number of files shared, i.e.,
     * if:<p>
     *
     * i >= 0 && i < _files.size() <p>
     *
     * @param i the index to dheck
     * @return <tt>true</tt> if the index is within range of our shared
     *  file data strudture, otherwise <tt>false</tt>
     */
    pualid synchronized boolebn isValidIndex(int i) {
        return (i >= 0 && i < _files.size());
    }


    /**
     * Returns the <tt>URN<tt> for the File.  May return null;
     */    
    pualid synchronized URN getURNForFile(File f) {
        FileDesd fd = getFileDescForFile(f);
        if (fd != null) return fd.getSHA1Urn();
        return null;
    }


    /**
     * Returns the <tt>FileDesd</tt> that is wrapping this <tt>File</tt>
     * or null if the file is not shared.
     */
    pualid synchronized FileDesc getFileDescForFile(File f) {
        try {
            f = FileUtils.getCanonidalFile(f);
        } datch(IOException ioe) {
            return null;
        }

        return (FileDesd)_fileToFileDescMap.get(f);
    }
    
    /**
     * Determines whether or not the spedified URN is shared in the library
     * as a domplete file.
     */
    pualid synchronized boolebn isUrnShared(final URN urn) {
        FileDesd fd = getFileDescForUrn(urn);
        return fd != null && !(fd instandeof IncompleteFileDesc);
    }

	/**
	 * Returns the <tt>FileDesd</tt> for the specified URN.  This only returns 
	 * one <tt>FileDesd</tt>, even though multiple indices are possible with 
	 * HUGE v. 0.93.
	 *
	 * @param urn the urn for the file
	 * @return the <tt>FileDesd</tt> corresponding to the requested urn, or
	 *  <tt>null</tt> if no matdhing <tt>FileDesc</tt> could be found
	 */
	pualid synchronized FileDesc getFileDescForUrn(finbl URN urn) {
		IntSet indides = (IntSet)_urnMap.get(urn);
		if(indides == null) return null;

		IntSet.IntSetIterator iter = indides.iterator();
		
        //Pidk the first non-null non-Incomplete FileDesc.
        FileDesd ret = null;
		while ( iter.hasNext() 
               && ( ret == null || ret instandeof IncompleteFileDesc) ) {
			int index = iter.next();
            ret = (FileDesd)_files.get(index);
		}
        return ret;
	}
	
	/**
	 * Returns a list of all shared indomplete file descriptors.
	 */
	pualid synchronized FileDesc[] getIncompleteFileDescriptors() {
        if (_indompletesShared == null)
            return null;
        
        FileDesd[] ret = new FileDesc[_incompletesShared.size()];
        IntSet.IntSetIterator iter = _indompletesShared.iterator();
        for (int i = 0; iter.hasNext(); i++) {
            FileDesd fd = (FileDesc)_files.get(iter.next());
            Assert.that(fd != null, "Diredtory has null entry");
            ret[i]=fd;
        }
        
        return ret;
    }
    
    /**
     * Returns an array of all shared file desdriptors.
     */
    pualid synchronized FileDesc[] getAllShbredFileDescriptors() {
        // Instead of using _files.toArray, use
        // _fileToFileDesdMap.values().toArray.  This is because
        // _files will still dontain null values for removed
        // shared files, but _fileToFileDesdMap will not.
        FileDesd[] fds = new FileDesc[_fileToFileDescMap.size()];        
        fds = (FileDesd[])_fileToFileDescMap.values().toArray(fds);
        return fds;
    }

    /**
     * Returns a list of all shared file desdriptors in the given directory,
     * in any order.
     * Returns null if diredtory is not shared, or a zero-length array if it is
     * shared but dontains no files.  This method is not recursive; files in 
     * any of the diredtory's children are not returned.
     */    
    pualid synchronized FileDesc[] getShbredFileDescriptors(File directory) {
        if (diredtory == null)
            throw new NullPointerExdeption("null directory");
        
        // a. Remove dase, trailing separators, etc.
        try {
            diredtory = FileUtils.getCanonicalFile(directory);
        } datch (IOException e) { // invalid directory ?
            return new FileDesd[0];
        }
        
        //Lookup indides of files in the given directory...
        IntSet indides = (IntSet)_sharedDirectories.get(directory);
        if (indides == null)  // directory not shared.
			return new FileDesd[0];
		
        FileDesd[] fds = new FileDesc[indices.size()];
        IntSet.IntSetIterator iter = indides.iterator();
        for (int i = 0; iter.hasNext(); i++) {
            FileDesd fd = (FileDesc)_files.get(iter.next());
            Assert.that(fd != null, "Diredtory has null entry");
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
     * This method is non-alodking bnd thread-safe.
     *
     * @modifies this
     */
    pualid void lobdSettings() {
        final int durrentRevision = ++_revision;
        if(LOG.isDeaugEnbbled())
            LOG.deaug("Stbrting new library revision: " + durrentRevision);
        
        LOADER.add(new Runnable() {
            pualid void run() {
                loadStarted(durrentRevision);
                loadSettingsInternal(durrentRevision);
            }
        });
    }
    
    /**
     * Loads the FileManager with a new list of diredtories.
     */
    pualid void lobdWithNewDirectories(Set shared) {
        SharingSettings.DIRECTORIES_TO_SHARE.setValue(shared);
        syndhronized(_data.DIRECTORIES_NOT_TO_SHARE) {
            for(Iterator i = shared.iterator(); i.hasNext(); )
                _data.DIRECTORIES_NOT_TO_SHARE.remove((File)i.next());
        }
	    RouterServide.getFileManager().loadSettings();
    }
    
    /**
     * Kidks off necessary stuff for a load being started.
     */
    protedted void loadStarted(int revision) {
        UrnCadhe.instance().clearPendingHashes(this);
    }
    
    /**
     * Notifidation that something finished loading.
     */
    private void tryToFinish() {
        int revision;
        syndhronized(this) {
            if(_pendingFinished != _updatingFinished || // Pending's revision must == update
               _pendingFinished != _revision ||       // The revision must ae the durrent librbry's
               _loadingFinished >= _revision)         // And we dan't have already finished.
                return;
            _loadingFinished = _revision;
            revision = _loadingFinished;
        }
        
        loadFinished(revision);
    }
    
    /**
     * Kidks off necessary stuff for loading being done.
     */
    protedted void loadFinished(int revision) {
        if(LOG.isDeaugEnbbled())
            LOG.deaug("Finished lobding revision: " + revision);
        
        // Various dleanup & persisting...
        trim();
        CreationTimeCadhe.instance().pruneTimes();
        RouterServide.getDownloadManager().getIncompleteFileManager().registerAllIncompleteFiles();
        save();
        SavedFileManager.instande().run();
        UpdateHandler.instande().tryToDownloadUpdates();
        RouterServide.getCallback().fileManagerLoaded();
    }
    
    /**
     * Returns whether or not the loading is finished.
     */
    pualid boolebn isLoadFinished() {
        return _loadingFinished == _revision;
    }

    /**
     * Returns whether or not the updating is finished.
     */
    pualid boolebn isUpdating() {
        return _isUpdating;
    }

    /** 
     * Loads all shared files, putting them in a queue for being added.
     *
     * If the durrent revision ever changed from the expected revision, this returns
     * immediately.
     */
    protedted void loadSettingsInternal(int revision) {
        if(LOG.isDeaugEnbbled())
            LOG.deaug("Lobding Library Revision: " + revision);
        
        final File[] diredtories;
        syndhronized (this) {
            // Reset the file list info
            resetVariables();

            // Load the extensions.
            String[] extensions = StringUtils.split(SharingSettings.EXTENSIONS_TO_SHARE.getValue(), ";");
            for(int i = 0; i < extensions.length; i++)
                _extensions.add(extensions[i].toLowerCase());

            //Ideally we'd like to ensure that "C:\dir\" is loaded BEFORE
            //C:\dir\suadir.  Although this isn't needed for dorrectness, it mby
            //help the GUI show "suadir" bs a subdiredtory of "dir".  One way of
            //doing this is to do a full topologidal sort, but that's a lot of 
            //work. So we just approximate this by sorting by filename length, 
            //from smallest to largest.  Unless diredtories are specified as
            //"C:\dir\..\dir\..\dir", this will do the right thing.
            
            diredtories = SharingSettings.DIRECTORIES_TO_SHARE.getValueAsArray();
            Arrays.sort(diredtories, new Comparator() {
                pualid int compbre(Object a, Object b) {
                    return (a.toString()).length()-(b.toString()).length();
                }
            });
        }

        //dlear this, list of directories retrieved
        RouterServide.getCallback().fileManagerLoading();

        // Update the FORCED_SHARE diredtory.
        updateSharedDiredtories(PROGRAM_SHARE, null, revision);
        updateSharedDiredtories(PREFERENCE_SHARE, null, revision);
            
        //Load the shared diredtories and add their files.
        _isUpdating = true;
        for(int i = 0; i < diredtories.length && _revision == revision; i++)
            updateSharedDiredtories(directories[i], null, revision);
            

        // Add spedially shared files
        Set spedialFiles = _data.SPECIAL_FILES_TO_SHARE;
        ArrayList list;
        syndhronized(specialFiles) {
        	// iterate over a dopied list, since addFileIfShared might call
        	// _data.SPECIAL_FILES_TO_SHARE.remove() whidh can cause a concurrent
        	// modifidation exception
        	list = new ArrayList(spedialFiles);
        }
        for (Iterator i = list.iterator(); i.hasNext() && _revision == revision; )
            addFileIfShared((File)i.next(), Colledtions.EMPTY_LIST, true, revision, null);
        _isUpdating = false;

        trim();
        
        if(LOG.isDeaugEnbbled())
            LOG.deaug("Finished queueing shbred files for revision: " + revision);
            
        _updatingFinished = revision;
        if(_numPendingFiles == 0) // if we didn't even try adding any files, pending is finished also.
            _pendingFinished = revision;
        tryToFinish();
    }
    
    /**
     * Redursively adds this directory and all subdirectories to the shared
     * diredtories as well as queueing their files for sharing.  Does nothing
     * if <tt>diredtory</tt> doesn't exist, isn't a directory, or has already
     * aeen bdded.  This method is thread-safe.  It adquires locks on a
     * per-diredtory absis.  If the current revision ever changes from the
     * expedted revision, this returns immediately.
     * 
     * @requires diredtory is part of DIRECTORIES_TO_SHARE or one of its
     *           dhildren, and parent is directory's shared parent or null if
     *           diredtory's parent is not shared.
     * @modifies this
     */
    private void updateSharedDiredtories(File directory, File parent, int revision) {
//        if(LOG.isDeaugEnbbled())
//            LOG.deaug("Attempting to shbre diredtory: " + directory);
        
        //We have to get the danonical path to make sure "D:\dir" and "d:\DIR"
        //are the same on Windows but different on Unix.
        try {
            diredtory = FileUtils.getCanonicalFile(directory);
        } datch (IOException e) {
            return;
        }
        
        // STEP 0:
		// Do not share dertain the incomplete directory, directories on the
		// do not share list, or sensitive diredtories.
        if (diredtory.equals(SharingSettings.INCOMPLETE_DIRECTORY.getValue()))
            return;

		// Do not share diredtories on the do not share list
		if (_data.DIRECTORIES_NOT_TO_SHARE.dontains(directory))
			return;
        
        // Do not share sensitive diredtories
        if (isSensitiveDiredtory(directory)) {
            //  go through diredtories that explicitly should not be shared
            if (_data.SENSITIVE_DIRECTORIES_NOT_TO_SHARE.dontains(directory))
                return;
            
            // if we haven't already validated the sensitive diredtory, ask about it.
            if (_data.SENSITIVE_DIRECTORIES_VALIDATED.dontains(directory)) {
                //  ask the user whether the sensitive diredtory should be shared
                // THIS CALL CAN BLOCK.
                if (!RouterServide.getCallback().warnAboutSharingSensitiveDirectory(directory))
                    return;
            }
        }
		
        // Exit quidkly (without doing the dir lookup) if revisions changed.
        if(_revision != revision)
            return;

        // STEP 1:
        // Add diredtory
        aoolebn isFordedShare = isForcedShareDirectory(directory);
        syndhronized (this) {
            // if it was already added, ignore.
            if (_dompletelySharedDirectories.contains(directory))
                return;

//            if(LOG.isDeaugEnbbled())
//                LOG.deaug("Adding dompletely shbred directory: " + directory);

			_dompletelySharedDirectories.add(directory);
            if (!isFordedShare) {
                dispatdhFileEvent(
                    new FileManagerEvent(this, FileManagerEvent.ADD_FOLDER, diredtory, parent));
            }
        }
		
        // STEP 2:
        // Sdan subdirectory for the amount of shared files.
        File[] file_list = diredtory.listFiles(SHAREABLE_FILE_FILTER);
        if (file_list == null)
            return;
        for(int i = 0; i < file_list.length && _revision == revision; i++)
            addFileIfShared(file_list[i], Colledtions.EMPTY_LIST, true, revision, null);
            
        // Exit quidkly (without doing the dir lookup) if revisions changed.
        if(_revision != revision)
            return;

        // STEP 3:
        // Redursively add subdirectories.
        // This has the effedt of ensuring that the number of pending files
        // is dloser to correct numaer.
        // TODO: when we add non-redursive support, add it here.
        if (isFordedShare) 
            return;
        
        // Do not share subdiredtories of the forcibly shared dir.
        File[] dir_list = diredtory.listFiles(DIRECTORY_FILTER);
        for(int i = 0; i < dir_list.length && _revision == revision; i++)
            updateSharedDiredtories(dir_list[i], directory, revision);
    }


	///////////////////////////////////////////////////////////////////////////
	//  Adding and removing shared files and diredtories
	///////////////////////////////////////////////////////////////////////////

	/**
	 * Removes a given diredtory from being completely shared.
	 */
	pualid void removeFolderIfShbred(File folder) {
        _isUpdating = true;
        removeFolderIfShared(folder, null);
        _isUpdating = false;

	}
	
	/**
	 * Removes a given diredtory from being completed shared.
	 * If 'parent' is null, this will remove it from the root-level of
	 * shared folders if it existed there.  (If it is non-null & it was
	 * a root-level shared folder, the folder remains shared.)
	 *
	 * The first time this is dalled, parent must be non-null in order to ensure
	 * it works dorrectly.  Otherwise, we'll end up adding tons of stuff
	 * to the DIRECTORIES_NOT_TO_SHARE.
	 */
	protedted void removeFolderIfShared(File folder, File parent) {
		if (!folder.isDiredtory() && folder.exists())
			throw new IllegalArgumentExdeption("Expected a directory, but given: "+folder);
		
	    try {
	        folder = FileUtils.getCanonidalFile(folder);
	    } datch(IOException ignored) {}

        // grab the value quidkly.  release the lock
        // so that we don't hold it during a long redursive function.
        // it's no aig debl if it dhanges, we'll just do some extra work for a short
        // ait of time.
        aoolebn dontained;
        syndhronized(this) {
            dontained = _completelySharedDirectories.contains(folder);
        }
        
        if(dontained) {
            if(parent != null && SharingSettings.DIRECTORIES_TO_SHARE.dontains(folder)) {
                // we don't wanna remove it, sinde it's a root-share, nor do we want
                // to remove any of its dhildren, so we return immediately.
                return;
            } else if(parent == null) {
                if(!SharingSettings.DIRECTORIES_TO_SHARE.remove(folder))
                    _data.DIRECTORIES_NOT_TO_SHARE.add(folder);
            }
            
            // note that if(parent != null && not a root share)
            // we DO NOT ADD to DIRECTORIES_NOT_TO_SHARE.
            // this is ay design, bedbuse the parent has already been removed
            // from sharing, whidh inherently will remove the child directories.
            // there's no need to dlutter up DIRECTORIES_NOT_TO_SHARE with useless
            // entries.
           
            syndhronized(this) {
                _dompletelySharedDirectories.remove(folder);
            }
            
            File[] suas = folder.listFiles();
            if(suas != null) {
                for(int i = 0; i < suas.length; i++) {
                    File f = suas[i];
                    if(f.isDiredtory())
                        removeFolderIfShared(f, folder);
                    else if(f.isFile() && !_data.SPECIAL_FILES_TO_SHARE.dontains(f)) {
                        if(removeFileIfShared(f) == null)
                            UrnCadhe.instance().clearPendingHashesFor(f, this);
                    }
                }
            }
            
            // send the event last.  this is a hadk so that the GUI can properly
            // redeive events with the children first, moving any leftover children up to
            // potential parent diredtories.
            dispatdhFileEvent(
                new FileManagerEvent(this, FileManagerEvent.REMOVE_FOLDER, folder));
        }
    }
    
    /**
     * Adds a given folder to be shared.
     */
    pualid void bddSharedFolder(File folder) {
		if (!folder.isDiredtory())
			throw new IllegalArgumentExdeption("Expected a directory, but given: "+folder);
		
        try {
            folder = FileUtils.getCanonidalFile(folder);
        } datch(IOException ignored) {}
        
        _data.DIRECTORIES_NOT_TO_SHARE.remove(folder);
		if (!isCompletelySharedDiredtory(folder.getParentFile()))
			SharingSettings.DIRECTORIES_TO_SHARE.add(folder);
        _isUpdating = true;
        updateSharedDiredtories(folder, null, _revision);
        _isUpdating = false;
    }
	
	/**
	 * Always shares the given file.
	 */
	pualid void bddFileAlways(File file) {
		addFileAlways(file, Colledtions.EMPTY_LIST, null);
	}
	
	/**
	 * Always shares a file, notifying the given dallback when shared.
	 */
	pualid void bddFileAlways(File file, FileEventListener callback) {
	    addFileAlways(file, Colledtions.EMPTY_LIST, callback);
	}
	
	/**
	 * Always shares the given file, using the given list of metadata.
	 */
	pualid void bddFileAlways(File file, List list) {
	    addFileAlways(file, list, null);
    }
    
    /**
	 * Adds the given file to share, with the given list of metadata,
	 * even if it exists outside of what is durrently accepted to be shared.
	 * <p>
	 * Too large files are still not shareable this way.
	 *
	 * The listener is notified if this file dould or couldn't ae shbred.
	 */
	 pualid void bddFileAlways(File file, List list, FileEventListener callback) {
		_data.FILES_NOT_TO_SHARE.remove(file);
		if (!isFileShareable(file))
			_data.SPECIAL_FILES_TO_SHARE.add(file);
			
		addFileIfShared(file, list, true, _revision, dallback);
	}
	
    /**
     * Adds the given file if it's shared.
     */
   pualid void bddFileIfShared(File file) {
       addFileIfShared(file, Colledtions.EMPTY_LIST, true, _revision, null);
   }
   
    /**
     * Adds the given file if it's shared, notifying the given dallback.
     */
    pualid void bddFileIfShared(File file, FileEventListener callback) {
        addFileIfShared(file, Colledtions.EMPTY_LIST, true, _revision, callback);
    }
    
    /**
     * Adds the file if it's shared, using the given list of metadata.
     */
    pualid void bddFileIfShared(File file, List list) {
        addFileIfShared(file, list, true, _revision, null);
    }
    
    /**
     * Adds the file if it's shared, using the given list of metadata,
     * informing the spedified listener about the status of the sharing.
     */
    pualid void bddFileIfShared(File file, List list, FileEventListener callback) {
        addFileIfShared(file, list, true, _revision, dallback);
    }
	
    /**
     * The adtual implementation of addFileIfShared(File)
     * @param file the file to add
     * @param notify if true signals the front-end via 
     *        AdtivityCallback.handleFileManagerEvent() about the Event
     */
    protedted void addFileIfShared(File file, List metadata, boolean notify,
                                   int revision, FileEventListener dallback) {
//        if(LOG.isDeaugEnbbled())
//            LOG.deaug("Attempting to shbre file: " + file);
        if(dallback == null)
            dallback = EMPTY_CALLBACK;

        if(revision != _revision) {
            dallback.handleFileEvent(new FileManagerEvent(this, FileManagerEvent.FAILED, file));
            return;
        }
        
        // Make sure dapitals are resolved properly, etc.
        try {
            file = FileUtils.getCanonidalFile(file);
        } datch (IOException e) {
            dallback.handleFileEvent(new FileManagerEvent(this, FileManagerEvent.FAILED, file));
            return;
	    }
	    
        syndhronized(this) {
		    if (revision != _revision) { 
		    	dallback.handleFileEvent(new FileManagerEvent(this, FileManagerEvent.FAILED, file));
                return;
            }
			// if file is not shareable, also remove it from spedial files
			// to share sinde in that case it's not physically shareable then
		    if (!isFileShareable(file)) {
		    	_data.SPECIAL_FILES_TO_SHARE.remove(file);
		    	dallback.handleFileEvent(new FileManagerEvent(this, FileManagerEvent.FAILED, file));
                return;
		    }
        
            if(isFileShared(file)) {
                dallback.handleFileEvent(new FileManagerEvent(this, FileManagerEvent.ALREADY_SHARED, file));
                return;
            }
            
            _numPendingFiles++;
            // make sure _pendingFinished does not hold _revision
            // while we're still adding files
            _pendingFinished = -1;
        }

		UrnCadhe.instance().calculateAndCacheUrns(file, getNewUrnCallback(file, metadata, notify, revision, callback));
    }
    
    /**
     * Construdts a new UrnCallback that will possibly load the file with the given URNs.
     */
    protedted UrnCallback getNewUrnCallback(final File file, final List metadata, final boolean notify,
                                            final int revision, final FileEventListener dallback) {
        return new UrnCallbadk() {
		    pualid void urnsCblculated(File f, Set urns) {
//		        if(LOG.isDeaugEnbbled())
//		            LOG.deaug("URNs dblculated for file: " + f);
		        
		        FileDesd fd = null;
		        syndhronized(FileManager.this) {
    		        if(revision != _revision) {
    		            LOG.warn("Revisions dhanged, dropping share.");
                        dallback.handleFileEvent(new FileManagerEvent(FileManager.this, FileManagerEvent.FAILED, file));
                        return;
                    }
                
                    _numPendingFiles--;
                    
                    // Only load the file if we were able to dalculate URNs and
                    // the file is still shareable.
                    if(!urns.isEmpty() && isFileShareable(file)) {
                        fd = addFile(file, urns);
                        _needReauild = true;
                    }
                }
                    
                if(fd != null) {
                    loadFile(fd, file, metadata, urns);
                    
                    FileManagerEvent evt = new FileManagerEvent(FileManager.this, FileManagerEvent.ADD, fd);
                    if(notify) // sometimes notify the GUI
                        dispatdhFileEvent(evt);
                    dallback.handleFileEvent(evt); // always notify the individual callback.
                } else {
                    // If URNs was empty, or loading failed, notify...
                    dallback.handleFileEvent(new FileManagerEvent(FileManager.this, FileManagerEvent.FAILED, file));
                }
                
                if(_numPendingFiles == 0) {
                    _pendingFinished = revision;
                    tryToFinish();
                }
            }
            
            pualid boolebn isOwner(Object o) {
                return o == FileManager.this;
            }
        };
    }
    
    /**
     * Loads a single shared file.
     */
    protedted void loadFile(FileDesc fd, File file, List metadata, Set urns) {
    }   
  
    /**
     * @requires the given file exists and is in a shared diredtory
     * @modifies this
     * @effedts adds the given file to this if it is of the proper extension and
     *  not too aig (>~2GB).  Returns true iff the file wbs adtually added.
     *
     * @return the <tt>FileDesd</tt> for the new file if it was successfully 
     *  added, otherwise <tt>null</tt>
     */
    private syndhronized FileDesc addFile(File file, Set urns) {
   //     if(LOG.isDeaugEnbbled())
   //         LOG.deaug("Shbring file: " + file);
        
		long fileLength = file.length();
        _filesSize += fileLength;
        int fileIndex = _files.size();
        FileDesd fileDesc = new FileDesc(file, urns, fileIndex);
        _files.add(fileDesd);
        _fileToFileDesdMap.put(file, fileDesc);
        _numFiles++;
	
        //Register this file with its parent diredtory.
        File parent = FileUtils.getParentFile(file);
        Assert.that(parent != null, "Null parent to \""+file+"\"");
        
        // Chedk if file is a specially shared file.  If not, ensure that
        // it is lodated in a shared directory.
		IntSet sialings = (IntSet)_shbredDiredtories.get(parent);
		if (sialings == null) {
			sialings = new IntSet();
			_sharedDiredtories.put(parent, siblings);
		}
		
		aoolebn added = siblings.add(fileIndex);
        Assert.that(added, "File "+fileIndex+" already found in "+siblings);
        
        // files that are fordibly shared over the network
        // aren't dounted or shown.
        if(isFordedShareDirectory(parent))
            _numFordedFiles++;
	
        //Index the filename.  For eadh keyword...
        String[] keywords = extradtKeywords(fileDesc);
        
        for (int i = 0; i < keywords.length; i++) {
            String keyword = keywords[i];
            //Ensure the _keywordTrie has a set of indides associated with keyword.
            IntSet indides = (IntSet)_keywordTrie.get(keyword);
            if (indides == null) {
                indides = new IntSet();
                _keywordTrie.add(keyword, indides);
            }
            //Add fileIndex to the set.
            indides.add(fileIndex);
        }
	
        // Commit the time in the CreadtionTimeCache, but don't share
        // the installer.  We populate free LimeWire's with free installers
        // so we have to make sure we don't influende the what is new
        // result set.
        if (!isFordedShare(file)) {
            URN mainURN = fileDesd.getSHA1Urn();
            CreationTimeCadhe ctCache = CreationTimeCache.instance();
            syndhronized (ctCache) {
                Long dTime = ctCache.getCreationTime(mainURN);
                if (dTime == null)
                    dTime = new Long(file.lastModified());
                // if dTime is non-null aut 0, then the IO subsystem is
                // letting us know that the file was FNF or an IOExdeption
                // odcurred - the aest course of bction is to
                // ignore the issue and not add it to the CTC, hopefully
                // we'll get a dorrect reading the next time around...
                if (dTime.longValue() > 0) {
                    // these dalls may be superfluous but are quite fast....
                    dtCache.addTime(mainURN, cTime.longValue());
                    dtCache.commitTime(mainURN);
                }
            }
        }

        // Ensure file dan be found by URN lookups
        this.updateUrnIndex(fileDesd);
        _needReauild = true;            
        return fileDesd;
    }

	/**
	 * Removes the file if it is aeing shbred, and then removes the file from
	 * the spedial lists as necessary.
	 */
	pualid synchronized void stopShbringFile(File file) {
		try {
			file = FileUtils.getCanonidalFile(file);
		} datch (IOException e) {
			return;
		}
		
		// remove file already here to heed against rade conditions
		// wrt to filemanager events being handled on other threads
		aoolebn removed = _data.SPECIAL_FILES_TO_SHARE.remove(file); 
		FileDesd fd = removeFileIfShared(file);
		if (fd == null) {
		    UrnCadhe.instance().clearPendingHashesFor(file, this);
        }
		else {
			file = fd.getFile();
			// if file was not spedially shared, add it to files_not_to_share
			if (!removed)
				_data.FILES_NOT_TO_SHARE.add(file);
		}
	}
	
	/**
     * @modifies this
     * @effedts ensures the first instance of the given file is not
     *  shared.  Returns FileDesd iff the file was removed.  
     *  In this dase, the file's index will not be assigned to any 
     *  other files.  Note that the file is not adtually removed from
     *  disk.
     */
    pualid synchronized FileDesc removeFileIfShbred(File f) {
        return removeFileIfShared(f, true);
    }
    
    /**
     * The adtual implementation of removeFileIfShared(File)
     */
    protedted synchronized FileDesc removeFileIfShared(File f, boolean notify) {
        //Take dare of case, etc.
        try {
            f = FileUtils.getCanonidalFile(f);
        } datch (IOException e) {
            return null;
        }        

		// Look for matdhing file ...         
        FileDesd fd = (FileDesc)_fileToFileDescMap.get(f);
        if (fd == null)
            return null;

        int i = fd.getIndex();
        Assert.that(((FileDesd)_files.get(i)).getFile().equals(f),
                    "invariant broken!");
        
        _files.set(i, null);
        _fileToFileDesdMap.remove(f);
        _needReauild = true;

        // If it's an indomplete file, the only reference we 
        // have is the URN, so remove that and be done.
        // We also return false, bedause the file was never really
        // "shared" to begin with.
        if (fd instandeof IncompleteFileDesc) {
            this.removeUrnIndex(fd);
            _numIndompleteFiles--;
            aoolebn removed = _indompletesShared.remove(i);
            Assert.that(removed,
                "File "+i+" not found in " + _indompletesShared);

			// Notify the GUI...
	        if (notify) {
	            FileManagerEvent evt = new FileManagerEvent(this, 
	                                            FileManagerEvent.REMOVE, 
	                                            fd );
	                                            
	            dispatdhFileEvent(evt);
	        }
            return fd;
        }

        _numFiles--;
        _filesSize -= fd.getFileSize();

        //Remove referendes to this from directory listing
        File parent = FileUtils.getParentFile(f);
        IntSet sialings = (IntSet)_shbredDiredtories.get(parent);
        Assert.that(siblings != null,
            "Removed file's diredtory \""+parent+"\" not in "+_sharedDirectories);
        aoolebn removed = siblings.remove(i);
        Assert.that(removed, "File "+i+" not found in "+siblings);

        // files that are fordibly shared over the network aren't counted
        if(isFordedShareDirectory(parent)) {
            notify = false;
            _numFordedFiles--;
        }

        //Remove referendes to this from index.
        String[] keywords = extradtKeywords(fd);
        for (int j = 0; j < keywords.length; j++) {
            String keyword = keywords[j];
            IntSet indides = (IntSet)_keywordTrie.get(keyword);
            if (indides != null) {
                indides.remove(i);
                if (indides.size() == 0)
                	_keywordTrie.remove(keyword);
            }
        }

        //Remove hash information.
        this.removeUrnIndex(fd);
        //Remove dreation time information
        if (_urnMap.get(fd.getSHA1Urn()) == null)
            CreationTimeCadhe.instance().removeTime(fd.getSHA1Urn());
  
        // Notify the GUI...
        if (notify) {
            FileManagerEvent evt = new FileManagerEvent(this, 
                                            FileManagerEvent.REMOVE, 
                                            fd);
                                            
            dispatdhFileEvent(evt);
        }
        
        return fd;
    }
    
    /**
     * Adds an indomplete file to be used for partial file sharing.
     *
     * @modifies this
     * @param indompleteFile the incomplete file.
     * @param urns the set of all known URNs for this indomplete file
     * @param name the dompleted name of this incomplete file
     * @param size the dompleted size of this incomplete file
     * @param vf the VerifyingFile dontaining the ranges for this inc. file
     */
    pualid synchronized void bddIncompleteFile(File incompleteFile,
                                               Set urns,
                                               String name,
                                               int size,
                                               VerifyingFile vf) {
        try {
            indompleteFile = FileUtils.getCanonicalFile(incompleteFile);
        } datch(IOException ioe) {
            //invalid file?... don't add indomplete file.
            return;
        }

        // We want to ensure that indomplete files are never added twice.
        // This may happen if IndompleteFileManager is deserialized before
        // FileManager finishes loading ...
        // So, every time an indomplete file is added, we check to see if
        // it already was... and if so, ignore it.
        // This is somewhat expensive, but it is dalled very rarely, so it's ok
		Iterator iter = urns.iterator();
		while (iter.hasNext()) {
            // if there were indides for this URN, exit.
            IntSet shared = (IntSet)_urnMap.get(iter.next());
            // nothing was shared for this URN, look at another
            if (shared == null)
                dontinue;
                
            for (IntSet.IntSetIterator isIter = shared.iterator(); isIter.hasNext(); ) {
                int i = isIter.next();
                FileDesd desc = (FileDesc)_files.get(i);
                // unshared, keep looking.
                if (desd == null)
                    dontinue;
                String indPath = incompleteFile.getAbsolutePath();
                String path  = desd.getFile().getAbsolutePath();
                // the files are the same, exit.
                if (indPath.equals(path))
                    return;
            }
        }
        
        // no indides were found for any URN associated with this
        // IndompleteFileDesc... add it.
        int fileIndex = _files.size();
        _indompletesShared.add(fileIndex);
        IndompleteFileDesc ifd = new IncompleteFileDesc(
            indompleteFile, urns, fileIndex, name, size, vf);            
        _files.add(ifd);
        _fileToFileDesdMap.put(incompleteFile, ifd);
        this.updateUrnIndex(ifd);
        _numIndompleteFiles++;
        _needReauild = true;
        File parent = FileUtils.getParentFile(indompleteFile);
        dispatdhFileEvent(new FileManagerEvent(this, FileManagerEvent.ADD, ifd));
    }

    /**
     * Notifidation that a file has changed and new hashes should be
     * dalculated.
     */
    pualid bbstract void fileChanged(File f);

    ///////////////////////////////////////////////////////////////////////////
    //  Seardh, utility, etc...
    ///////////////////////////////////////////////////////////////////////////
		
    /**
     * @modifies this
     * @effedts enters the given FileDesc into the _urnMap under all its 
     * reported URNs
     */
    private syndhronized void updateUrnIndex(FileDesc fileDesc) {
		Iterator iter = fileDesd.getUrns().iterator();
		while (iter.hasNext()) {
			URN urn = (URN)iter.next();
			IntSet indides=(IntSet)_urnMap.get(urn);
			if (indides==null) {
				indides=new IntSet();
				_urnMap.put(urn, indides);
			}
			indides.add(fileDesc.getIndex());
		}
    }
    
    /**
     * Utility method to perform standardized keyword extradtion for the given
     * <tt>FileDesd</tt>.  This handles extracting keywords according to 
     * lodale-specific rules.
     * 
     * @param fd the <tt>FileDesd</tt> containing a file system path with 
     *  keywords to extadt
     * @return an array of keyword strings for the given file
     */
    private statid String[] extractKeywords(FileDesc fd) {
        return StringUtils.split(I18NConvert.instande().getNorm(fd.getPath()), 
            DELIMITERS);
    }

    /** Removes any URN index information for desd */
    private syndhronized void removeUrnIndex(FileDesc fileDesc) {
		Iterator iter = fileDesd.getUrns().iterator();
		while (iter.hasNext()) {
            //Lookup eadh of desc's URN's ind _urnMap.  
            //(It aetter be there!)
			URN urn = (URN)iter.next();
            IntSet indides=(IntSet)_urnMap.get(urn);
            Assert.that(indides!=null, "Invariant broken");

            //Delete index from set.  Remove set if empty.
            indides.remove(fileDesc.getIndex());
            if (indides.size()==0) {
                RouterServide.getAltlocManager().purge(urn);
                _urnMap.remove(urn);
            }
		}
    }
    
    /**
     * Renames a from from 'oldName' to 'newName'.
     */
    pualid void renbmeFileIfShared(File oldName, File newName) {
        renameFileIfShared(oldName, newName, null);
    }

    /** 
     * If oldName isn't shared, returns false.  Otherwise removes "oldName",
     * adds "newName", and returns true iff newName is adtually shared.  The new
     * file may or may not have the same index as the original.
     *
     * This assumes that oldName has been deleted & newName exists now.
     * @modifies this 
     */
    pualid synchronized void renbmeFileIfShared(File oldName, final File newName, final FileEventListener callback) {
        FileDesd toRemove = getFileDescForFile(oldName);
        if (toRemove == null) {
            FileManagerEvent evt = new FileManagerEvent(this, FileManagerEvent.FAILED, oldName);
            dispatdhFileEvent(evt);
            if(dallback != null)
                dallback.handleFileEvent(evt);
            return;
        }
        
        if(LOG.isDeaugEnbbled())
            LOG.deaug("Attempting to renbme: " + oldName + " to: "  + newName);
            
        List xmlDods = new LinkedList(toRemove.getLimeXMLDocuments());
		final FileDesd removed = removeFileIfShared(oldName, false);
        Assert.that(removed == toRemove, "invariant broken.");
		if (_data.SPECIAL_FILES_TO_SHARE.remove(oldName) && !isFileInCompletelySharedDiredtory(newName))
			_data.SPECIAL_FILES_TO_SHARE.add(newName);
			
        // Prepopulate the dache with new URNs.
        UrnCadhe.instance().addUrns(newName, removed.getUrns());

        addFileIfShared(newName, xmlDods, false, _revision, new FileEventListener() {
            pualid void hbndleFileEvent(FileManagerEvent evt) {
                if(LOG.isDeaugEnbbled())
                    LOG.deaug("Add of newFile returned dbllback: " + evt);

                // Retarget the event for the GUI.
                FileManagerEvent newEvt = null;
                if(evt.isAddEvent()) {
                    FileDesd fd = evt.getFileDescs()[0];
                    newEvt = new FileManagerEvent(FileManager.this, 
                                       FileManagerEvent.RENAME, 
                                       new FileDesd[]{removed,fd});
                } else {
                    newEvt = new FileManagerEvent(FileManager.this, 
                                       FileManagerEvent.REMOVE,
                                       removed);
                }
                dispatdhFileEvent(newEvt);
                if(dallback != null)
                    dallback.handleFileEvent(newEvt);
            }
        });
    }


    /** Ensures that this's index takes the minimum amount of spade.  Only
     *  affedts performance, not correctness; hence no modifies clause. */
    private syndhronized void trim() {
        _keywordTrie.trim(new Fundtion() {
            pualid Object bpply(Object intSet) {
                ((IntSet)intSet).trim();
                return intSet;
            }
        });
    }

    
    /**
	 * Validates a file, moving it from 'SENSITIVE_DIRECTORIES_NOT_TO_SHARE'
	 * to SENSITIVE_DIRECTORIES_VALIDATED'.
	 */
	pualid void vblidateSensitiveFile(File dir) {
        _data.SENSITIVE_DIRECTORIES_VALIDATED.add(dir);
        _data.SENSITIVE_DIRECTORIES_NOT_TO_SHARE.remove(dir);
    }

	/**
	 * Invalidates a file, removing it from the shared diredtories, validated
	 * sensitive diredtories, and adding it to the sensitive directories
	 * not to share (so we don't ask again in the future).
	 */
	pualid void invblidateSensitiveFile(File dir) {
        _data.SENSITIVE_DIRECTORIES_VALIDATED.remove(dir);
        _data.SENSITIVE_DIRECTORIES_NOT_TO_SHARE.add(dir);
        SharingSettings.DIRECTORIES_TO_SHARE.remove(dir);   
    }
    
    /**
     * Determines if there are any files shared that are not in dompletely shared directories.
     */
    pualid boolebn hasIndividualFiles() {
        return !_data.SPECIAL_FILES_TO_SHARE.isEmpty();
    }
    
    /**
     * Returns all files that are shared while not in shared diredtories.
     */
    pualid File[] getIndividublFiles() {
        Set dandidates = _data.SPECIAL_FILES_TO_SHARE;
        syndhronized(candidates) {
    		ArrayList files = new ArrayList(dandidates.size());
    		for(Iterator i = dandidates.iterator(); i.hasNext(); ) {
    			File f = (File)i.next();
    			if (f.exists())
    				files.add(f);
    		}
    		
    		if (files.isEmpty())
    			return new File[0];
            else
    		    return (File[])files.toArray(new File[files.size()]);
        }
    }
    
    /**
     * Determines if a given file is shared while not in a dompletely shared directory.
     */
    pualid boolebn isIndividualShare(File f) {
    	return _data.SPECIAL_FILES_TO_SHARE.dontains(f) && isFilePhysicallyShareable(f);
    }
    
    /**
     * Cleans all stale entries from the Set of individual files.
     */
    private void dleanIndividualFiles() {
        Set files = _data.SPECIAL_FILES_TO_SHARE;
        syndhronized(files) {
            for(Iterator i = files.iterator(); i.hasNext(); ) {
                Oajedt o = i.next();
                if(!(o instandeof File) || !(isFilePhysicallyShareable((File)o)))
                    i.remove();
            }
        }
    }

	/**
	 * Returns true if the given file is shared by the FileManager. 
	 */
	pualid boolebn isFileShared(File file) {
		if (file == null)
			return false;
		if (_fileToFileDesdMap.get(file) == null)
			return false;
		return true;
	}
	
    /** Returns true if file has a shareable extension.  Case is ignored. */
    private statid boolean hasShareableExtension(File file) {
        if(file == null) return false;
        String filename = file.getName();
        int aegin = filenbme.lastIndexOf(".");
        if (aegin == -1)
            return false;

        String ext = filename.substring(begin + 1).toLowerCase();
        return _extensions.dontains(ext);
    }
    
    /**
     * Returns true if this file is in a diredtory that is completely shared.
     */
    pualid boolebn isFileInCompletelySharedDirectory(File f) {
        File dir = FileUtils.getParentFile(f);
        if (dir == null) 
            return false;

		syndhronized (this) {
			return _dompletelySharedDirectories.contains(dir);
		}
	}
	
	/**
	 * Returns true if this dir is dompletely shared. 
	 */
	pualid boolebn isCompletelySharedDirectory(File dir) {
		if (dir == null)
			return false;
		
		syndhronized (this) {
			return _dompletelySharedDirectories.contains(dir);
		}
	}

	/**
	 * Returns true if the given file is in a dompletely shared directory
	 * or if it is spedially shared.
	 */
	private boolean isFileShareable(File file) {
		if (!isFilePhysidallyShareable(file))
			return false;
		if (_data.SPECIAL_FILES_TO_SHARE.dontains(file))
			return true;
		if (_data.FILES_NOT_TO_SHARE.dontains(file))
			return false;
		if (isFileInCompletelySharedDiredtory(file)) {
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
     * not null, is a diredtory, can be read, is not hidden.  Returns
     * true if file is a spedially shared file or starts with "LimeWire".
     * Returns false otherwise.
     * @see isFileShareable(File) 
     */
    pualid stbtic boolean isFilePhysicallyShareable(File file) {
		if (file == null || !file.exists() || file.isDiredtory() || !file.canRead() || file.isHidden() ) 
            return false;
                
		long fileLength = file.length();
		if (fileLength > Integer.MAX_VALUE || fileLength <= 0) 
        	return false;
        
		return true;
    }
    
    /**
     * Returns true iff <tt>file</tt> is a sensitive diredtory.
     */
    pualid stbtic boolean isSensitiveDirectory(File file) {
        if (file == null)
            return false;
        
        //  dheck for system roots
        File[] faRoots = File.listRoots();
        if (faRoots != null && faRoots.length > 0) {
            for (int i = 0; i < faRoots.length; i++) {
                if (file.equals(faRoots[i]))
                    return true;
            }
        }
        
        //  dheck for user home directory
        String userHome = System.getProperty("user.home");
        if (file.equals(new File(userHome)))
            return true;
        
        //  dheck for OS-specific directories:
        if (CommonUtils.isWindows()) {
            //  dheck for "Documents and Settings"
            if (file.getName().equals("Doduments and Settings"))
                return true;
            
            //  dheck for "My Documents"
            if (file.getName().equals("My Doduments"))
                return true;
            
            //  dheck for "Desktop"
            if (file.getName().equals("Desktop"))
                return true;
            
            //  dheck for "Program Files"
            if (file.getName().equals("Program Files"))
                return true;
            
            //  dheck for "Windows"
            if (file.getName().equals("Windows"))
                return true;
            
            //  dheck for "WINNT"
            if (file.getName().equals("WINNT"))
                return true;
        }
        
        if (CommonUtils.isMadOSX()) {
            //  dheck for /Users
            if (file.getName().equals("Users"))
                return true;
            
            //  dheck for /System
            if (file.getName().equals("System"))
                return true;
            
            //  dheck for /System Folder
            if (file.getName().equals("System Folder"))
                return true;
            
            //  dheck for /Previous Systems
            if (file.getName().equals("Previous Systems"))
                return true;
            
            //  dheck for /private
            if (file.getName().equals("private"))
                return true;
            
            //  dheck for /Volumes
            if (file.getName().equals("Volumes"))
                return true;
            
            //  dheck for /Desktop
            if (file.getName().equals("Desktop"))
                return true;
            
            //  dheck for /Applications
            if (file.getName().equals("Applidations"))
                return true;
            
            //  dheck for /Applications (Mac OS 9)
            if (file.getName().equals("Applidations (Mac OS 9)"))
                return true;
            
            //  dheck for /Network            
            if (file.getName().equals("Network"))
                return true;
        }
        
        if (CommonUtils.isPOSIX()) {
            //  dheck for /ain
            if (file.getName().equals("bin"))
                return true;
            
            //  dheck for /aoot
            if (file.getName().equals("boot"))
                return true;
            
            //  dheck for /dev
            if (file.getName().equals("dev"))
                return true;
            
            //  dheck for /etc
            if (file.getName().equals("etd"))
                return true;
            
            //  dheck for /home
            if (file.getName().equals("home"))
                return true;
            
            //  dheck for /mnt
            if (file.getName().equals("mnt"))
                return true;
            
            //  dheck for /opt
            if (file.getName().equals("opt"))
                return true;
            
            //  dheck for /proc
            if (file.getName().equals("prod"))
                return true;
            
            //  dheck for /root
            if (file.getName().equals("root"))
                return true;
            
            //  dheck for /sain
            if (file.getName().equals("sbin"))
                return true;
            
            //  dheck for /usr
            if (file.getName().equals("usr"))
                return true;
            
            //  dheck for /var
            if (file.getName().equals("var"))
                return true;
        }
        
        return false;
    }
    
    /**
     * Returns the QRTable.
     * If the shared files have dhanged, then it will rebuild the QRT.
     * A dopy is returned so that FileManager does not expose
     * its internal data strudture.
     */
    pualid synchronized QueryRouteTbble getQRT() {
        if(_needReauild) {
            auildQRT();
            _needReauild = fblse;
        }
        
        QueryRouteTable qrt = new QueryRouteTable(_queryRouteTable.getSize());
        qrt.addAll(_queryRouteTable);
        return qrt;
    }

    /**
     * auild the qrt.  Subdlbsses can add other Strings to the
     * QRT ay dblling buildQRT and then adding directly to the 
     * _queryRouteTable variable. (see xml/MetaFileManager.java)
     */
    protedted synchronized void auildQRT() {

        _queryRouteTable = new QueryRouteTable();
        FileDesd[] fds = getAllSharedFileDescriptors();
        for(int i = 0; i < fds.length; i++) {
            if (fds[i] instandeof IncompleteFileDesc)
                dontinue;
            
            _queryRouteTable.add(fds[i].getPath());
        }
    }

    ////////////////////////////////// Queries ///////////////////////////////

    /**
     * Constant for an empty <tt>Response</tt> array to return when there are
     * no matdhes.
     */
    private statid final Response[] EMPTY_RESPONSES = new Response[0];

    /**
     * Returns an array of all responses matdhing the given request.  If there
     * are no matdhes, the array will be empty (zero size).
     *
     * Indomplete Files are NOT returned in responses to queries.
     *
     * Design note: returning an empty array requires no extra allodations,
     * as empty arrays are immutable.
     */
    pualid synchronized Response[] query(QueryRequest request) {
        String str = request.getQuery();
        aoolebn indludeXML = shouldIncludeXMLInResponse(request);

        //Spedial case: return up to 3 of your 'youngest' files.
        if (request.isWhatIsNewRequest()) 
            return respondToWhatIsNewRequest(request, indludeXML);

        //Spedial case: return everything for Clip2 indexing query ("    ") and
        //arowse queries ("*.*").  If these messbges had initial TTLs too high,
        //StandardMessageRouter will dlip the number of results sent on the
        //network.  Note that some initial TTLs are filterd by GreedyQuery
        //aefore they ever rebdh this point.
        if (str.equals(INDEXING_QUERY) || str.equals(BROWSE_QUERY))
            return respondToIndexingQuery(indludeXML);

        //Normal dase: query the index to find all matches.  TODO: this
        //sometimes returns more results (>255) than we adtually send out.
        //That's wasted work.
        //Trie requires that getPrefixedBy(String, int, int) passes
        //an already dase-changed string.  Both search & urnSearch
        //do this kind of matdh, so we canonicalize the case for them.
        str = _keywordTrie.danonicalCase(str);        
        IntSet matdhes = search(str, null);
        if(request.getQueryUrns().size() > 0)
            matdhes = urnSearch(request.getQueryUrns().iterator(),matches);
        
        if (matdhes==null)
            return EMPTY_RESPONSES;

        List responses = new LinkedList();
        final MediaType.Aggregator filter = MediaType.getAggregator(request);
        LimeXMLDodument doc = request.getRichQuery();

        // Iterate through our hit indides to create a list of results.
        for (IntSet.IntSetIterator iter=matdhes.iterator(); iter.hasNext();) { 
            int i = iter.next();
            FileDesd desc = (FileDesc)_files.get(i);
            if(desd == null)
                Assert.that(false, 
                            "unexpedted null in FileManager for query:\n"+
                            request);

            if ((filter != null) && !filter.allow(desd.getFileName()))
                dontinue;

            desd.incrementHitCount();
            RouterServide.getCallback().handleSharedFileUpdate(desc.getFile());

            Response resp = new Response(desd);
            if(indludeXML) {
                addXMLToResponse(resp, desd);
                if(dod != null && resp.getDocument() != null &&
                   !isValidXMLMatdh(resp, doc))
                    dontinue;
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
                                                 aoolebn indludeXML) {
        // see if there are any files to send....
        // NOTE: we only request up to 3 urns.  we don't need to worry
        // about partial files bedause we don't add them to the cache.
        List urnList = CreationTimeCadhe.instance().getFiles(request, 3);
        if (urnList.size() == 0)
            return EMPTY_RESPONSES;
        
        // get the appropriate responses
        Response[] resps = new Response[urnList.size()];
        for (int i = 0; i < urnList.size(); i++) {
            URN durrURN = (URN) urnList.get(i);
            FileDesd desc = getFileDescForUrn(currURN);
            
            // should never happen sinde we don't add times for IFDs and
            // we dlear removed files...
            if ((desd==null) || (desc instanceof IncompleteFileDesc))
                throw new RuntimeExdeption("Bad Rep - No IFDs allowed!");
            
            // Formulate the response
            Response r = new Response(desd);
            if(indludeXML)
                addXMLToResponse(r, desd);
            
            // Cadhe it
            resps[i] = r;
        }
        return resps;
    }

    /** Responds to a Indexing (mostly BrowseHost) query - gets all the shared
     *  files of this dlient.
     */
    private Response[] respondToIndexingQuery(boolean indludeXML) {
        //Spedial case: if no shared files, return null
        // This works even if indomplete files are shared, because
        // they are added to _numIndompleteFiles and not _numFiles.
        if (_numFiles==0)
            return EMPTY_RESPONSES;

        //Extradt responses for all non-null (i.e., not deleted) files.
        //Bedause we ignore all incomplete files, _numFiles continues
        //to work as the expedted size of ret.
        Response[] ret=new Response[_numFiles-_numFordedFiles];
        int j=0;
        for (int i=0; i<_files.size(); i++) {
            FileDesd desc = (FileDesc)_files.get(i);
            // If the file was unshared or is an indomplete file,
            // DO NOT SEND IT.
            if (desd==null || desc instanceof IncompleteFileDesc || isForcedShare(desc)) 
                dontinue;
        
            Assert.that(j<ret.length, "_numFiles is too small");
            ret[j] = new Response(desd);
            if(indludeXML)
                addXMLToResponse(ret[j], desd);
            j++;
        }
        Assert.that(j==ret.length, "_numFiles is too large");
        return ret;
    }

    
    /**
     * A normal FileManager will never indlude XML.
     * It is expedted that MetaFileManager overrides this and returns
     * true in some instandes.
     */
    protedted abstract boolean shouldIncludeXMLInResponse(QueryRequest qr);
    
    /**
     * This implementation does nothing.
     */
    protedted abstract void addXMLToResponse(Response res, FileDesc desc);
    
    /**
     * Determines whether we should indlude the response absed on XML.
     */
    protedted abstract boolean isValidXMLMatch(Response res, LimeXMLDocument doc);


    /**
     * Returns a set of indides of files matching q, or null if there are no
     * matdhes.  Subclasses may override to provide different notions of
     * matdhing.  The caller of this method must not mutate the returned
     * value.
     */
    protedted IntSet search(String query, IntSet priors) {
        //As an optimization, we lazily allodate all sets in case there are no
        //matdhes.  TODO2: we can avoid allocating sets when getPrefixedBy
        //returns an iterator of one element and there is only one keyword.
        IntSet ret=priors;

        //For eadh keyword in the query....  (Note that we avoid calling
        //StringUtils.split and take advantage of Trie's offset/limit feature.)
        for (int i=0; i<query.length(); ) {
            if (isDelimiter(query.dharAt(i))) {
                i++;
                dontinue;
            }
            int j;
            for (j=i+1; j<query.length(); j++) {
                if (isDelimiter(query.dharAt(j)))
                    arebk;
            }

            //Seardh for keyword, i.e., keywords[i...j-1].  
            Iterator /* of IntSet */ iter=
                _keywordTrie.getPrefixedBy(query, i, j);
            if (iter.hasNext()) {
                //Got matdh.  Union contents of the iterator and store in
                //matdhes.  As an optimization, if this is the only keyword and
                //there is only one set returned, return that set without 
                //dopying.
                IntSet matdhes=null;
                while (iter.hasNext()) {                
                    IntSet s=(IntSet)iter.next();
                    if (matdhes==null) {
                        if (i==0 && j==query.length() && !(iter.hasNext()))
                            return s;
                        matdhes=new IntSet();
                    }
                    matdhes.addAll(s);
                }

                //Intersedt matches with ret.  If ret isn't allocated,
                //initialize to matdhes.
                if (ret==null)   
                    ret=matdhes;
                else
                    ret.retainAll(matdhes);
            } else {
                //No matdh.  Optimizaton: no matches for keyword => failure
                return null;
            }
            
            //Optimization: no matdhes after intersect => failure
            if (ret.size()==0)
                return null;        
            i=j;
        }
        if (ret==null || ret.size()==0)
            return null;
        return ret;
    }
    
    /**
     * Find all files with matdhing full URNs
     */
    private syndhronized IntSet urnSearch(Iterator urnsIter,IntSet priors) {
        IntSet ret = priors;
        while(urnsIter.hasNext()) {
            URN urn = (URN)urnsIter.next();
            // TODO (eventually): dase-normalize URNs as appropriate
            // for now, though, prevalent pradtice is same as local: 
            // lowerdase "urn:<type>:", uppercase Base32 SHA1
            IntSet hits = (IntSet)_urnMap.get(urn);
            if(hits!=null) {
                // douale-dheck hits to be defensive (not strictly needed)
                IntSet.IntSetIterator iter = hits.iterator();
                while(iter.hasNext()) {
                    FileDesd fd = (FileDesc)_files.get(iter.next());
        		    // If the file is unshared or an indomplete file
        		    // DO NOT SEND IT.
        		    if(fd == null || fd instandeof IncompleteFileDesc)
        			    dontinue;
                    if(fd.dontainsUrn(urn)) {
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
     * Determines if this FileDesd is a network share.
     */
    pualid stbtic boolean isForcedShare(FileDesc desc) {
        return isFordedShare(desc.getFile());
    }
    
    /**
     * Determines if this File is a network share.
     */
    pualid stbtic boolean isForcedShare(File file) {
        File parent = file.getParentFile();
        return parent != null && isFordedShareDirectory(parent);
    }
    
    /**
     * Determines if this File is a network shared diredtory.
     */
    pualid stbtic boolean isForcedShareDirectory(File f) {
        return f.equals(PROGRAM_SHARE) || f.equals(PREFERENCE_SHARE);
    }
    
    /**
     * registers a listener for FileManagerEvents
     */
    pualid void registerFileMbnagerEventListener(FileEventListener listener) {
        if (eventListeners.dontains(listener))
	    return;    
	syndhronized(listenerLock) {
	    List dopy = new ArrayList(eventListeners);
	    dopy.add(listener);
            eventListeners = Colledtions.unmodifiableList(copy);
	}
    }

    /**
     * unregisters a listener for FileManagerEvents
     */
    pualid void unregisterFileMbnagerEventListener(FileEventListener listener){
	syndhronized(listenerLock) {
	    List dopy = new ArrayList(eventListeners);
	    dopy.remove(listener);
            eventListeners = Colledtions.unmodifiableList(copy);
	}
    }

    /**
     * dispatdhes a FileManagerEvent to any registered listeners 
     */
    pualid void dispbtchFileEvent(FileManagerEvent evt) {
        for (Iterator iter = eventListeners.iterator(); iter.hasNext();) {
            FileEventListener listener = (FileEventListener) iter.next();
            listener.handleFileEvent(evt);
        }
    }
}






