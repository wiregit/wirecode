package com.limegroup.gnutella;

import java.io.*;
import com.sun.java.util.collections.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.xml.*;
import com.limegroup.gnutella.downloader.IncompleteFileManager;
import com.limegroup.gnutella.downloader.VerifyingFile;

/**
 * The list of all shared files.  Provides operations to add and remove
 * individual files, directory, or sets of directories.  Provides a method to
 * efficiently query for files whose names contain certain keywords.<p>
 *
 * This class is thread-safe.
 */
public class FileManager {
    /** The string used by Clip2 reflectors to index hosts. */
    public static final String INDEXING_QUERY="    ";
    /** The string used by LimeWire to browse hosts. */
    public static final String BROWSE_QUERY="*.*";

    /**********************************************************************
     * LOCKING: obtain this' monitor before modifying this. The exception
     * is _loadThread, which is controlled by _loadThreadLock.
     **********************************************************************/

    /**
     * The total size of all files, in bytes.
     * INVARIANT: _size=sum of all size of the elements of _files,
     *   except IncompleteFileDescs, which may change size at any time.
     */
    private long _size;
    
    /**
     * The total number of files.  INVARIANT: _numFiles==number of
     * elements of _files that are not null and not IncompleteFileDescs.
     */
    private int _numFiles;
    
    /** 
     * The total number of files that are pending sharing.
     *  (ie: awaiting caching or being added)
     */
    private int _numPendingFiles;
    
    /**
     * The total number of incomplete shared files.
     * INVARIANT: _numFiles + _numIncompleteFiles == the number of
     *  elements of _files that are not null.
     */
    private int _numIncompleteFiles;
    
    /** 
     * The list of shareable files.  An entry is null if it is no longer
     *  shared.
     * INVARIANT: for all i, f[i]==null, or f[i].index==i and
     *  f[i]._path is in a shared directory with a shareable extension or
     *  f[i]._path is the incomplete directory if f is an IncompleteFileDesc.
     */
    private List /* of FileDesc */ _files;

    /**
     * An index mapping keywords in file names to the indices in _files.  A
     * keyword of a filename f is defined to be a maximal sequence of characters
     * without a character from DELIMETERS.  IncompleteFile keywords
     * are NOT stored in this index.  Retrieval is of IncompleteFiles are only
     * allowed by hash.
     *
     * INVARIANT: For all keys k in _index, for all i in _index.get(k), 
     * _files[i]._path.substring(k)!=-1.
     * Likewise for all i, for all k in _files[i]._path where _files[i]
     * is not an IncompleteFileDesc, _index.get(k) contains i.
     */
    private Trie /* String -> IntSet  */ _index;
    
    /**
     * An index mapping appropriately case-normalized URN strings to the
     * indices in _files.  Used to make query-by-hash faster.
     * INVARIANT: for all keys k in _urnIndex, for all i in _urnIndex.get(k),
     * _files[i].containsUrn(k).  Likewise for all i, for all k in
     * _files[i].getUrns(),  _urnIndex.get(k) contains i.
     */
    private Map /* URN -> IntSet  */ _urnIndex;
    
    /**
     * The set of extensions to share, sorted by StringComparator. 
     * INVARIANT: all extensions are lower case.
     */
    private static Set /* of String */ _extensions;
    
    /**
     * The list of shared directories and their contents.  More formally, a
     * mapping whose keys are shared directories and any subdirectories
     * reachable through those directories.  The value for any key is the set
     * of indices of all shared files in that directory.
     * INVARIANT: for any key k with value v in _sharedDirectories, 
     * for all i in v,
     *       _files[i]._path==k+_files[i]._name.
     *  Likewise, for all i s.t.
     *  _files[i]!=null and !(_files[i] instanceof IncompleteFileDesc),
     *       _sharedDirectories.get(
     *            _files[i]._path-_files[i]._name).contains(i).
     * Here "==" is shorthand for file path comparison and "a-b" is short for
     * string 'a' with suffix 'b' removed.  INVARIANT: all keys in this are
     * canonicalized files, sorted by a FileComparator.
     *
     * Incomplete shared files are NOT stored in this data structure, but are
     * instead in the _incompletesShared IntSet.
     */
    private Map /* of File -> IntSet */ _sharedDirectories;
    
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
     * others to retrieve all the incomplete-shared files, which is
     * a relatively useful feature.
     */
    private IntSet _incompletesShared;

    /**
     *  The thread responsisble for adding contents of _sharedDirectories to
     *  this, or null if no load has yet been triggered.  This is necessary
     *  because indexing files can be slow.  Interrupt this thread to stop the
     *  loading; it will periodically check its interrupted status. 
     *  LOCKING: obtain _loadThreadLock before modifying and before obtaining
     *  this (to prevent deadlock).
     */
    private Thread _loadThread;
    /**
     *  True if _loadThread.interrupt() was called.  This is needed because
     *  _loadThread.isInterrupted() does not behave as expected.  See
     *  http://developer.java.sun.com/developer/bugParade/bugs/4092438.html
     */
    private boolean _loadThreadInterrupted=false;
    
    /**
     * The lock for _loadThread.  Necessary to prevent deadlocks in
     * loadSettings.
     */
    private Object _loadThreadLock=new Object();
    
    /** The callback for adding shared directories and files, or null
     *  if this has no callback.
     */
    protected static ActivityCallback _callback;
    
    /**
     * The only ShareableFileFilter object that should be used.
     */
    public static FilenameFilter SHAREABLE_FILE_FILTER =
        new ShareableFileFilter();
        
    /**
     * The only DirectoryFilter object that should be used.
     */
    public static FilenameFilter DIRECTORY_FILTER = new DirectoryFilter();
        
    

    /**
     * Characters used to tokenize queries and file names.
     */
    public static final String DELIMETERS=" -._+/*()\\";    
    private static final boolean isDelimeter(char c) {
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
        _size = 0;
        _numFiles = 0;
        _numIncompleteFiles = 0;
        _numPendingFiles = 0;
        _files = new ArrayList();
        _index = new Trie(true);  //ignore case
        _urnIndex = new HashMap();
        _extensions = new TreeSet(new StringComparator());
        _sharedDirectories = new TreeMap(new FileComparator());
        _incompletesShared = new IntSet();
    }

    /** Asynchronously loads all files by calling loadSettings.  Sets this'
     *  callback to be "callback", and notifies "callback" of all file loads.
     *      @modifies this
     *      @see loadSettings */
    public void initialize() {
		this._callback = RouterService.getCallback();
		loadSettings(false);
    }

    ////////////////////////////// Accessors ///////////////////////////////

    
    /**
     * Returns the size of all files, in <b>bytes</b>.  Note that the largest
     *  value that can be returned is Integer.MAX_VALUE, i.e., ~2GB.  If more
     *  bytes are being shared, returns this value.
     */
    public int getSize() {return ByteOrder.long2int(_size);}

    /**
     * Returns the number of files.
     */
    public int getNumFiles() {return _numFiles;}
    
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
	 * Returns the <tt>FileDesc</tt> for the specified URN.  This only returns 
	 * one <tt>FileDesc</tt>, even though multiple indeces are possible with 
	 * HUGE v. 0.93.
	 *
	 * @param urn the urn for the file
	 * @return the <tt>FileDesc</tt> corresponding to the requested urn, or
	 *  <tt>null</tt> if no matching <tt>FileDesc</tt> could be found
	 */
	public synchronized FileDesc getFileDescForUrn(final URN urn) {
		IntSet indeces = (IntSet)_urnIndex.get(urn);
		if(indeces == null) return null;

		IntSet.IntSetIterator iter = indeces.iterator();
		
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
     * Returns the FileDesc matching the passed-in path and size, if any,
     * null otherwise. Kind of silly, definitely inefficient, but only
     * needed rarely, from library view, because there's no sharing of
     * data structures for local files
     */
    public synchronized FileDesc getFileDescMatching(File file) {
		// linear probe. thankfully it's rare
		Iterator iter = _files.iterator();
		while(iter.hasNext()) {
			FileDesc candidate = (FileDesc)iter.next();
			if (candidate==null) continue;
			if(file.equals(candidate.getFile())) {
				return candidate;
			}
		}
        // none found
        return null;
	}
	
	/**
	 * Returns a list of all shared incomplete file descriptors.
	 */
	public FileDesc[] getIncompleteFileDescriptors() {
        if (_incompletesShared == null) {
            return null;
        }
        
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
     * Returns a list of all shared files in the given directory, in any order.
     * Returns null if directory is not shared, or a zero-length array if it is
     * shared but contains no files.  This method is not recursive; files in 
     * any of the directory's children are not returned.   
     * <p>
     * If directory is null, returns all shared files.
     */
    public File[] getSharedFiles(File directory) {
        return (File[])getSharedFilesImpl(false, new File[0], directory);
    }

    /**
     * Returns a list of all shared file descriptors in the given directory,
     * in any order.
     * Returns null if directory is not shared, or a zero-length array if it is
     * shared but contains no files.  This method is not recursive; files in 
     * any of the directory's children are not returned.   
     * <p>
     * If directory is null, returns all shared file descriptors.
     */    
    public FileDesc[] getSharedFileDescriptors(File directory) {
        return (FileDesc[])getSharedFilesImpl(true,
                             new FileDesc[0], directory);
    }
    
    /**
     * The implementation of the code to scan through files.
     * Returns true if the directory was shared, false otherwise.
     * @param descs If true, the returned array will be FileDescs,
     *    If false, the returned array will be Files.
     * @param type An array whose type should be either FileDesc
     *    or File, to be filled with the appropriate object.
     * @param directory The directory you want to search shared files for.
     * @return An array (FileDesc or File) of the shared files.
     */
    private synchronized Object[] getSharedFilesImpl(boolean descs,
                                                 Object[] type,
                                                 File directory) {
        if(directory!=null){
            // a. Remove case, trailing separators, etc.
            try {
                directory=getCanonicalFile(directory);
            } catch (IOException e) {
                return null;
            }
            
            //Lookup indices of files in the given directory...
            IntSet indices=(IntSet)_sharedDirectories.get(directory);
            if (indices==null) {
                return null;
            }
            //...and pack them into an array.
            if (type.length < indices.size())
                type = (Object[])java.lang.reflect.Array.newInstance(
                                type.getClass().getComponentType(), 
                                indices.size());
            IntSet.IntSetIterator iter=indices.iterator(); 
            for (int i=0; iter.hasNext(); i++) {
                FileDesc fd=(FileDesc)_files.get(iter.next());
                Assert.that(fd!=null, "Directory has null entry");
                if (descs)
                    type[i]=fd;
                else
                    type[i]=fd.getFile();
            }
            return type;
        } else {
            // b. Filter out unshared entries.
            ArrayList buf=new ArrayList(_files.size());
            for (int i=0; i<_files.size(); i++) {
                FileDesc fd=(FileDesc)_files.get(i);
                if (fd!=null) {
                    if ( descs )
                        buf.add(fd);                
                    else
                        buf.add(fd.getFile());
                }
            }
            Object[] ret = buf.toArray(type);
            Assert.that(ret.length==buf.size(), 
                "Couldn't fit list in returned value");
            return ret;
        }
    }        

    /**
     * @param directory Gets all files under this directory RECURSIVELY.
     * @param filter If null, then returns all files.  Else, only returns files
     * extensions in the filter array.
     * @return A Array of Files recursively obtained from the directory,
     * according to the filter.
     */
    public static File[] getFilesRecursive(File directory,
                                           String[] filter) {

        debug("FileManager.getFilesRecursive(): entered.");
        ArrayList dirs = new ArrayList();
        // the return array of files...
        ArrayList retFileArray = new ArrayList();
        File[] retArray = null;

        // bootstrap the process
        if (directory.exists() && directory.isDirectory())
            dirs.add(directory);

        // while i have dirs to process
        while (dirs.size() > 0) {
            File currDir = (File) dirs.remove(0);
            debug("FileManager.getFilesRecursive(): currDir = " +
                  currDir);
            String[] listedFiles = currDir.list();
            for (int i = 0; i < listedFiles.length; i++) {

                File currFile = new File(currDir,listedFiles[i]);
                if (currFile.isDirectory()) // to be dealt with later
                    dirs.add(currFile);
                else if (currFile.isFile()) { // we have a 'file'....

                    boolean shouldAdd = false;
                    if (filter == null)
                        shouldAdd = true;
                    else {
                        String ext = getFileExtension(currFile);
                        for (int j = 0; 
                             (j < filter.length) && (ext != null); 
                             j++)
                            if (ext.equalsIgnoreCase(filter[j]))
                                shouldAdd = true;
                    }

                    if (shouldAdd)
                        retFileArray.add(currFile);
                }
            }
        }        

        if (!retFileArray.isEmpty()) {
            retArray = new File[retFileArray.size()];
            for (int i = 0; i < retArray.length; i++)
                retArray[i] = (File) retFileArray.get(i);
        }

        debug("FileManager.getFilesRecursive(): returning.");
        return retArray;
    }


    // simply gets whatever is after the "." in a filename, or the first thing
    // after the first "." in the filename
    private static String getFileExtension(File f) {
        String retString = null;

        java.util.StringTokenizer st = 
        new java.util.StringTokenizer(f.getName(), ".");
        if (st.countTokens() > 1) {
            st.nextToken();
            retString = st.nextToken();
        }
        return retString;
    }

    private static boolean debugOn = false;
    public static void debug(String out) {
        if (debugOn)
            System.out.println(out);
    }


    ///////////////////////////// Mutators ////////////////////////////////////   

    /**
     * Ensures this contains exactly the files specified by the
     * EXTENSIONS_TO_SEARCH_FOR and DIRECTORIES_TO_SEARCH_FOR_FILES properties.
     * That is, clears this and loads all files with the given extensions in the
     * given directories <i>and their children</i>.  Note that some files in
     * this before the call will not be in this after the call, or they may have
     * a different index.  If DIRECTORIES_TO_SEARCH_FOR_FILES contains duplicate
     * directories, the duplicates will be ignored.  If it contains files, they
     * will be ignored.<p>
	 *
     * This method is thread-safe but non-blocking.  When the method returns,
     * the directory and extension settings used by addFileIfShared() are
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
            //by listFiles called by updateDirectories blocks.  If this is the case,
            //we need to spawn a thread before join'ing.
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
            _loadThread = new Thread("FileManager.loadSettingsBlocking") {
                public void run() {
					try {
						loadSettingsBlocking(notifyOnClearFinal);
						_callback.fileManagerLoaded();
					} catch(Throwable t) {
						ErrorService.error(t);
					}
                }
            };
            _loadThread.start();
        } 
    }

    /** Returns true if the load thread has been interrupted an this should stop
     *  loading files. */
    protected boolean loadThreadInterrupted() {
        return _loadThreadInterrupted;
    }

    /** Clears this', reloads this' extensions, generates an array of
     *  directories, and then indexes the generated directories files.
     *  NOTE TO SUBCLASSES: extend this method as needed, it shall be
     *  threaded and run asynchronously as to not slow down the main
     *  thread (the GUI).
     *  @modifies this */
    protected void loadSettingsBlocking(boolean notifyOnClear) {
        File[] tempDirVar;
        synchronized (this) {
            // Reset the file list info
            _size = 0;
            _numFiles = 0;
            _numIncompleteFiles = 0;
            _numPendingFiles = 0;
            _files=new ArrayList();
            _index=new Trie(true);   //maintain invariant
            _urnIndex=new HashMap(); //maintain invariant
            _extensions = new TreeSet(new StringComparator());
            _sharedDirectories = new TreeMap(new FileComparator());
            _incompletesShared = new IntSet();

            // Load the extensions.
            String[] extensions = 
            StringUtils.split(SettingsManager.instance().getExtensions().trim(),
                              ';');
            for (int i=0; 
                 (i<extensions.length) && !loadThreadInterrupted();
                 i++)
                _extensions.add(extensions[i].toLowerCase());

            //Ideally we'd like to ensure that "C:\dir\" is loaded BEFORE
            //C:\dir\subdir.  Although this isn't needed for correctness, it may
            //help the GUI show "subdir" as a subdirectory of "dir".  One way of
            //doing this is to do a full topological sort, but that's a lot of work.
            //So we just approximate this by sorting by filename length, from
            //smallest to largest.  Unless directories are specified as
            //"C:\dir\..\dir\..\dir", this will do the right thing.
			final File[] directories = SettingsManager.instance().getDirectories();

            Arrays.sort(directories, new Comparator() {
                public int compare(Object a, Object b) {
                    return (a.toString()).length()-(b.toString()).length();
                }
            });
                
            tempDirVar = directories;
        }

        //clear this, list of directories retreived
        final File[] directories = tempDirVar;
        if (notifyOnClear) 
            _callback.clearSharedFiles();
        
        //Load the shared directories and their files.
        //Duplicates in the directories list will be ignored.  Note that the
        //runner thread only obtain this' monitor when adding individual
        //files.
        {
            // Add each directory as long as we're not interrupted.
            List added = new LinkedList();
            for(int i=0; i<directories.length&&!loadThreadInterrupted(); i++) {
                added.addAll(updateDirectories(directories[i], null));
            }
            // Add the files that were just marked as being shareable.
            if ( !loadThreadInterrupted() )
                updateSharedFiles(added);
            
            // Compact the index once.  As an optimization, we skip this
            // if loadSettings has subsequently been called.
            if (! loadThreadInterrupted())
                trim();                    
        }
        
        // Tell the download manager to notify us of incomplete files.
		if (! loadThreadInterrupted())
		    RouterService.getDownloadManager().getIncompleteFileManager().
		        registerAllIncompleteFiles();
		    
		// write out the cache of URNs
		UrnCache.instance().persistCache();
    }
    
    /**
     * Recursively adds this directory and all subdirectories
     * to the shared directories and updated the number of pending
     * shared files. If directory doesn't exist, isn't a 
     * directory, or has already been added, does nothing.
     * This method is thread-safe.  It acquires locks on a per-directory basis.
     * If the _loadThread is interrupted while scanning the contents of
     * directory, it returns immediately.
     * @requires directory is part of DIRECTORIES_TO_SEARCH_FOR_FILES or one of
     *  its children, and parent is directory's shared parent or null if
     *  directory's parent is not shared.
     * @modifies this
     * @return A List of directories that were added. The list
     *         in the form of a KeyValue pair, the key being
     *         the directory, and the value being an array of
     *         shareable files in that directory.
     */
    private List updateDirectories(File directory, File parent) {
        //We have to get the canonical path to make sure "D:\dir" and "d:\DIR"
        //are the same on Windows but different on Unix.
        try {
            directory=getCanonicalFile(directory);
        } catch (IOException e) {
            return new LinkedList();  //doesn't exist?
        }
        
        //STEP 1:
        // Scan subdirectory for the amount of shared files.
        File[] dir_list = listFiles(directory, DIRECTORY_FILTER);
        File[] file_list = listFiles(directory, SHAREABLE_FILE_FILTER);
        
        // no shared files or subdirs
        if ( dir_list == null && file_list == null )
            return new LinkedList();

        int numShareable = file_list.length;
        int numSubDirs = dir_list.length;
            
        //STEP 2:
        // Tell the GUI that this file is being shared and update
        // the amount of pending shared files.
        synchronized (this) {
            // if it was already added, ignore.
            if ( _sharedDirectories.get(directory) != null)
                return new LinkedList();
                
            _sharedDirectories.put(directory, new IntSet());
            if( _callback != null )
                _callback.addSharedDirectory(directory, parent);
                
            _numPendingFiles += numShareable;
        }
        
        //STEP 3:
        // Recursively add subdirectories.
        // This has the effect of ensuring that the number of pending files
        // is closer to correct number.
        List added = new LinkedList();
        added.add(new KeyValue(directory, file_list));
        for(int i = 0; i < numSubDirs && !loadThreadInterrupted(); i++) {
            added.addAll(updateDirectories(dir_list[i], directory));
        }
        
        return added;
    }
    
    /**
     * Updates the shared files with the list of shareable files.
     * This method is thread-safe.  It acquires locks on a per-file basis.
     * If the _loadThread is interrupted while adding the contents of the
     * directory, it returns immediately.     
     *
     * @param toShare a list of KeyValue objects, the key being the directory
     *  the files are in, and the value being an array of the shareable
     *  files.
     */
     private void updateSharedFiles(List toShare) {
        for(Iterator i = toShare.iterator();
          i.hasNext() && !loadThreadInterrupted(); ) {
            KeyValue info = (KeyValue)i.next();
            File[] shareables = (File[])info.getValue();
            for(int j=0; j<shareables.length && !loadThreadInterrupted();j++) {
                addFile(shareables[j]);
                synchronized(this) { _numPendingFiles--; }
            }
            // let the gc clean up the array of shareables.
            info.setValue(null);
            
        }
    }
    
    /**
     * @modifies this
     * @effects adds the given file to this, if it exists in a shared 
     *  directory and has a shared extension.  Returns true iff the file
     *  was actually added.  <b>WARNING: this is a potential security 
     *  hazard.</b> 
     */
	public boolean addFileIfShared(File file) {
        //Make sure capitals are resolved properly, etc.
        File f = null;
        try {
            f=getCanonicalFile(file);
            if (!f.exists()) 
                return false;
        } catch (IOException e) {
            return false;
		}
        File dir = getParentFile(file);
        if (dir==null) 
            return false;

        //TODO: if overwriting an existing, take special care.
        boolean directoryShared;
        synchronized (this) {
            directoryShared=_sharedDirectories.containsKey(dir);
            _numPendingFiles++;
        }
        boolean retval;
        if (directoryShared)
            retval = addFile(file);
        else 
            retval = false;
        synchronized(this) { _numPendingFiles--; }
        return retval;
	}

    /**
     * @modifies this
     * @effects calls addFileIfShared(file), then optionally stores any metadata
     *  in the given XML documents.  metadata may be null if there is no data.
     *  Returns the value from addFileIfShared. <b>WARNING: this is a potential
     *  security hazard.</b> 
     */
	public boolean addFileIfShared(File file,
                                   LimeXMLDocument[] metadata) {
        return addFileIfShared(file);
        //This implementation does nothing with metadata.  See MetaFileManager.
    }

    /**
     * @requires the given file exists and is in a shared directory
     * @modifies this
     * @effects adds the given file to this if it is of the proper extension and
     *  not too big (>~2GB).  Returns true iff the file was actually added.
     *  <b>WARNING: this is a potential security hazard; caller must ensure the
     *  file is in the shared directory.</b> 
     */
    private boolean addFile(File file) {
        repOk();
        long fileLength = file.length();
        if( !isFileShareable(file, fileLength) )
            return false;
        
        //Calculate hash OUTSIDE of lock.
        
        Set urns = null;
        try {
            urns = FileDesc.calculateAndCacheURN(file);  
        } catch(IOException e) {
            // there was an IO error calculating the hash, so we can't
            // add the file
            return false;
        } catch(InterruptedException e) {
            // the hash calculation was interrupted, so we can't add
            // the file -- should get reloaded
            return false;
        }
        if (loadThreadInterrupted()) 
            return false;
        
        if(urns.size() == 0) {
            // the URN was not calculated correctly for some reason
            return false;
        }

        synchronized (this) {
            _size += fileLength;
            int fileIndex = _files.size();
            FileDesc fileDesc = new FileDesc(file, urns, fileIndex);
            _files.add(fileDesc);
            _numFiles++;
		
            //Register this file with its parent directory.
            File parent=getParentFile(file);
            Assert.that(parent!=null, "Null parent to \""+file+"\"");
            IntSet siblings=(IntSet)_sharedDirectories.get(parent);
            Assert.that(siblings!=null,
                "Add directory \""+parent+"\" not in "+_sharedDirectories);
            boolean added=siblings.add(fileIndex);
            Assert.that(added, "File "+fileIndex+" already found in "+siblings);
            if (_callback!=null)
                _callback.addSharedFile(fileDesc, parent);
		
            //Index the filename.  For each keyword...
            String[] keywords=StringUtils.split(fileDesc.getPath(), DELIMETERS);
            for (int i=0; i<keywords.length; i++) {
                String keyword=keywords[i];
                //Ensure there _index has a set of indices associated with
                //keyword.
                IntSet indices=(IntSet)_index.get(keyword);
                if (indices==null) {
                    indices=new IntSet();
                    _index.add(keyword, indices);
                }
                //Add fileIndex to the set.
                indices.add(fileIndex);
            }
		
            // Ensure file can be found by URN lookups
            this.updateUrnIndex(fileDesc);
		
            repOk();
            return true;
        }
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
        // We want to ensure that incomplete files are never added twice.
        // This may happen if IncompleteFileManager is deserialized before
        // FileManager finishes loading ...
        // So, every time an incomplete file is added, we check to see if
        // it already was... and if so, ignore it.
        // This is somewhat expensive, but it is called very rarely, so it's ok
		Iterator iter = urns.iterator();
		while (iter.hasNext()) {
            // if there were indices for this URN, exit.
            IntSet shared = (IntSet)_urnIndex.get(iter.next());
            // nothing was shared for this URN, look at another
            if( shared == null )
                continue;
                
            IntSet.IntSetIterator isIter = shared.iterator();
            for ( ; isIter.hasNext(); ) {
                int i = isIter.next();
                FileDesc desc = (FileDesc)_files.get(i);
                // unshared, keep looking.
                if(desc == null)
                    continue;
                String incPath = incompleteFile.getAbsolutePath();
                String path  = desc.getFile().getAbsolutePath();
                // the files are the same, exit.
                if( incPath.equals(path) )
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
        this.updateUrnIndex(ifd);
        _numIncompleteFiles++;
        if (_callback != null) {
            File parent = getParentFile(incompleteFile);
            _callback.addSharedFile(ifd, parent);
        }
    }

    /**
     * @modifies this
     * @effects enters the given FileDesc into the _urnIndex under all its 
     * reported URNs
     */
    private synchronized void updateUrnIndex(FileDesc fileDesc) {
		Iterator iter = fileDesc.getUrns().iterator();
		while (iter.hasNext()) {
			URN urn = (URN)iter.next();
			IntSet indices=(IntSet)_urnIndex.get(urn);
			if (indices==null) {
				indices=new IntSet();
				_urnIndex.put(urn, indices);
			}
			indices.add(fileDesc.getIndex());
		}
    }
    

    /**
     * @modifies this
     * @effects ensures the first instance of the given file is not
     *  shared.  Returns true iff the file was previously shared.  
     *  In this case, the file's index will not be assigned to any 
     *  other files.  Note that the file is not actually removed from
     *  disk.
     */
    public synchronized boolean removeFileIfShared(File f) {
        repOk();
        //Take care of case, etc.
        try {
            f=getCanonicalFile(f);
        } catch (IOException e) {
            repOk();
            return false;
        }

        //Look for a file matching <file>...
        for (int i=0; i<_files.size(); i++) {
            FileDesc fd=(FileDesc)_files.get(i);
            if (fd==null)
                continue;
            File candidate = fd.getFile();

            //Aha, it's shared. Unshare it by nulling it out.
            if (f.equals(candidate)) {
                _files.set(i,null);
                
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
                    return false;
                }
                
                _numFiles--;
                _size-=fd.getSize();

                //Remove references to this from directory listing
                File parent=getParentFile(f);
                IntSet siblings=(IntSet)_sharedDirectories.get(parent);
                Assert.that(siblings!=null,
                    "Rem directory \""+parent+"\" not in "+_sharedDirectories);
                boolean removed=siblings.remove(i);
                Assert.that(removed, "File "+i+" not found in "+siblings);

                //Remove references to this from index.
                String[] keywords=StringUtils.split(fd.getPath(),
                                                    DELIMETERS);
                for (int j=0; j<keywords.length; j++) {
                    String keyword=keywords[j];
                    IntSet indices=(IntSet)_index.get(keyword);
                    if (indices!=null) {
                        indices.remove(i);
                        //TODO2: prune tree if possible.  call
                        //_index.remove(keyword) if indices.size()==0.
                    }
                }

                //Remove hash information.
                this.removeUrnIndex(fd);

                repOk();
                return true;  //No more files in list will match this.
            }
        }
        repOk();
        return false;
    }

    /** Removes any URN index information for desc */
    private synchronized void removeUrnIndex(FileDesc fileDesc) {
		Iterator iter = fileDesc.getUrns().iterator();
		while (iter.hasNext()) {
            //Lookup each of desc's URN's ind _urnIndex.  
            //(It better be there!)
			URN urn = (URN)iter.next();
            IntSet indices=(IntSet)_urnIndex.get(urn);
            Assert.that(indices!=null, "Invariant broken");

            //Delete index from set.  Remove set if empty.
            indices.remove(fileDesc.getIndex());
            if (indices.size()==0)
                _urnIndex.remove(urn);
		}
    }

    /** 
     * If oldName isn't shared, returns false.  Otherwise removes "oldName",
     * adds "newName", and returns true iff newName is actually shared.  The new
     * file may or may not have the same index as the original.<p>
     * 
     * This method does not call the addSharedFile callback method.  It exists
     * to make it easier for the GUI to rename a file without getting a
     * confusing callback.  Note that this doesn't affect the disk.
     *
     * @modifies this 
     */
    public synchronized boolean renameFileIfShared(File oldName,
                                                   File newName) {
        //As a temporary hack, we just disable callbacks, remove old,
        //re-add new, and restore callback.
        ActivityCallback savedCallback=_callback;
        _callback=null;
        try {
            boolean removed=removeFileIfShared(oldName);
            if (! removed)
                return false;
            boolean added=addFileIfShared(newName);
            if (! added)
                return false;
            return true;
        } finally {
            _callback=savedCallback;
        }
    }


    /** Ensures that this' index takes the minimum amount of space.  Only
     *  affects performance, not correctness; hence no modifies clause. */
    private synchronized void trim() {
        _index.trim(new Function() {
            public Object apply(Object intSet) {
                ((IntSet)intSet).trim();
                return intSet;
            }
        });
    }

    /** Returns true if filename has a shared extension.  Case is ignored. */
    private static boolean hasExtension(String filename) {
        int begin = filename.lastIndexOf(".");

        if (begin == -1)
            return false;

        String ext = filename.substring(begin + 1).toLowerCase();
        return _extensions.contains(ext);
    }
    
    /**
     * Returns true if this file is sharable.
     */
    public static boolean isFileShareable(File file, long fileLength) {
        if (fileLength>Integer.MAX_VALUE || fileLength<=0) 
        	return false;
        
        // we don't check for hidden files because this feature was not
        // supported in 1.1.8
        if (file.isDirectory() || !file.canRead() ) 
            return false;        
        if (!file.getName().toUpperCase().startsWith("LIMEWIRE") && 
            !hasExtension(file.getName())) {
        	return false;
        }        
        return true;
    }

    /** 
	 * Same as f.getParentFile() in JDK1.3. 
	 * @requires the File parameter must be a File object constructed
	 *  with the canonical path.
	 */
    public static File getParentFile(File f) {
		// slight changes to get around getParent bug on Mac
		String name=f.getParent();
		if(name == null) return null;
		try {
			return getCanonicalFile(new File(name));
		} catch(IOException ioe) {
  			//if the exception occurs, simply return null
  			return null;
  		}
    }
    
    /** Same as the f.listFiles() in JDK1.3. */
    public static File[] listFiles(File f) {
        String[] children=f.list();
        if (children==null)
            return null;

        File[] ret = new File[children.length];
        for (int i=0; i<children.length; i++) {
            ret[i] = new File(f, children[i]);
        }

        return ret;
    }
    
    /**
     * Same as f.listFiles(FileNameFilter) in JDK1.2
     */
    public static File[] listFiles(File f, FilenameFilter filter) {
        String[] children=f.list(filter);
        if(children == null)
            return null;
        File[] ret = new File[children.length];
        for (int i=0; i<children.length; i++) {
            ret[i] = new File(f, children[i]);
        }

        return ret;
    }    
    
    /**
     * called when a query route table has to be made. The current 
     * implementaion just takes all the file names and they are split
     * internally when added the QRT
     */
    public List getKeyWords(){
        File[] files = getSharedFiles(null);
        ArrayList retList = new ArrayList();
        for(int i=0;i<files.length;i++)
            retList.add(files[i].getAbsolutePath());
        return retList;
    }
    

    /** @return A List of KeyWords from the FS that one does NOT want broken
     *  upon hashing into a QRT.  Initially being used for schema hashing.
     *  We add all the hashes of the files we share so queries with hashes
     *  can be checked for potential positives against a QRT.
     */
    public List getIndivisibleKeyWords() {
        // Took out the code that added URNs to the returned List since we are
        // not recognizing queries with hashes anymore.  If you need the code
        // check CVS for it ;) .
        // Just return an empty list.
        return new ArrayList();
    }

    /** Same as f.getCanonicalFile() in JDK1.3. */
    public static File getCanonicalFile(File f) throws IOException {
        return new File(f.getCanonicalPath());
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

        //Special case: return everything for Clip2 indexing query ("    ") and
        //browse queries ("*.*").  If these messages had initial TTLs too high,
        //StandardMessageRouter will clip the number of results sent on the
        //network.  Note that some initial TTLs are filterd by GreedyQuery
        //before they ever reach this point.
        if (str.equals(INDEXING_QUERY) || str.equals(BROWSE_QUERY)) {
            //Special case: if no shared files, return null
            // This works even if incomplete files are shared, because
            // they are added to _numIncompleteFiles and not _numFiles.
            if (_numFiles==0)
                return EMPTY_RESPONSES;
            //Extract responses for all non-null (i.e., not deleted) files.
            //Because we ignore all incomplete files, _numFiles continues
            //to work as the expected size of ret.
            Response[] ret=new Response[_numFiles];
            int j=0;
            for (int i=0; i<_files.size(); i++) {
                FileDesc desc = (FileDesc)_files.get(i);
        		// If the file was unshared or is an incomplete file,
        		// DO NOT SEND IT.
                if (desc==null || desc instanceof IncompleteFileDesc) 
                    continue;    
                Assert.that(j<ret.length,
                            "_numFiles is too small");
                ret[j] = new Response(desc);
                j++;
            }
            Assert.that(j==ret.length,
                        "_numFiles is too large");
            return ret;
        }

        //Normal case: query the index to find all matches.  TODO: this
        //sometimes returns more results (>255) than we actually send out.
        //That's wasted work.
        IntSet matches = null;
        //Trie requires that getPrefixedBy(String, int, int) passes
        //an already case-changed string.  Both search & urnSearch
        //do thise kind of match, so we canonicalise the case for them.
        str = _index.canonicalCase(str);        
        matches = search( str,
                          matches);
        if(request.getQueryUrns().size() > 0) {
            matches = urnSearch(request.getQueryUrns().iterator(),matches);
        }
        
        if (matches==null) {
            return EMPTY_RESPONSES;
		}

        List responses = new LinkedList();

        boolean busy = 
            RouterService.getUploadManager().isBusy() &&
            RouterService.getUploadManager().isQueueFull();
        for (IntSet.IntSetIterator iter=matches.iterator(); 
             iter.hasNext();) { 
            int i = iter.next();
            FileDesc desc = (FileDesc)_files.get(i);
            if(desc == null) {
                Assert.that(false, 
                            "unexpected null in FileManager for query:\n"+
                            request);
            } if(!busy || 
                 desc.getNumberOfAlternateLocations() < 10) {
                desc.incrementHitCount();
                if ( _callback != null )
                    _callback.handleSharedFileUpdate(desc.getFile());
                responses.add(new Response(desc));
            } 
        }
        Response[] retArray = new Response[responses.size()];
        return (Response[])responses.toArray(retArray);
    }

    public synchronized FileDesc file2index(String fullName) {  
        // TODO1: precompute and store in table.
        for (int i=0; i<_files.size(); i++) {
            FileDesc fd=(FileDesc)_files.get(i);
            if (fd==null)  file://unshared
            continue;
            else if (fd.getPath().equals(fullName))
                return fd;
        }
        return null;//The file with this name was not found.
    }
    

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
            if (isDelimeter(query.charAt(i))) {
                i++;
                continue;
            }
            int j;
            for (j=i+1; j<query.length(); j++) {
                if (isDelimeter(query.charAt(j)))
                    break;
            }

            //Search for keyword, i.e., keywords[i...j-1].  
            Iterator /* of IntSet */ iter=
                _index.getPrefixedBy(query, i, j);
            if (iter.hasNext()) {
                //Got match.  Union contents of the iterator and store in
                //matches.  As an optimization, if this is the only keyword and
                //there is only one set returned, return that set without copying.
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
        else 
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
            IntSet hits = (IntSet)_urnIndex.get(urn);
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
     * A filter for listing all shared files.
     */
    private static class ShareableFileFilter implements FilenameFilter {
        public boolean accept(File dir, String name) {
            File f = new File(dir, name);
            return isFileShareable(f, f.length());
        }
    }
    
    /**
     * A filter for listing subdirectory only.
     */
    private static class DirectoryFilter implements FilenameFilter {
        public boolean accept(File dir, String name) {
            File f = new File(dir, name);
            return f.isDirectory();
        }
    }
    


    ///////////////////////////////////// Testing //////////////////////////////

    /** Checks this' rep. invariants.  VERY expensive. */
    private boolean DEBUG=false;
    protected synchronized void repOk() {
        if (!DEBUG)
            return;
        System.err.println("WARNING: running repOk()");

        //Verify index.  Get the set of indices in the _index....
        IntSet indices=new IntSet();
        for (Iterator iter=_index.getPrefixedBy(""); iter.hasNext(); ) {
            IntSet set=(IntSet)iter.next();
            indices.addAll(set);
        }
        //...and make sure all indices are in _files. 
        //(Note that we don't check filenames; I'm lazy)
        for (IntSet.IntSetIterator iter=indices.iterator(); iter.hasNext(); ) {
            int i=iter.next();
            FileDesc desc=(FileDesc)_files.get(i);
            Assert.that(desc!=null,
                        "Null entry for index value "+i);
        }

        //Make sure all FileDesc named in _urnIndex exist.
        for (Iterator iter=_urnIndex.keySet().iterator(); iter.hasNext(); ) {
            URN urn=(URN)iter.next();
            IntSet indices2=(IntSet)_urnIndex.get(urn);
            for (IntSet.IntSetIterator iter2=indices2.iterator(); 
                     iter2.hasNext(); ) {
                int i=iter2.next();
                FileDesc fd=(FileDesc)_files.get(i);
                Assert.that(fd!=null, "Missing file for urn");
                Assert.that(fd.containsUrn(urn), "URN mismatch");
            }
        }

        //Verify directory listing.  Make sure directory only contains 
        //legal values.
        Iterator iter=_sharedDirectories.keySet().iterator();
        while (iter.hasNext()) {
            File directory=(File)iter.next();
            IntSet children=(IntSet)_sharedDirectories.get(directory);
            
            IntSet.IntSetIterator iter2=children.iterator();
            while (iter2.hasNext()) {
                int i=iter2.next();
                Assert.that(i>=0 && i<_files.size(),
                            "Bad index "+i+" in directory");
                FileDesc fd=(FileDesc)_files.get(i);
                Assert.that(fd!=null, "Directory listing points to empty file");
            }
        }

        //For all files...
        int numFilesCount=0;
        int sizeFilesCount=0;
        for (int i=0; i<_files.size(); i++) {
            if (_files.get(i)==null)
                continue;
            FileDesc desc=(FileDesc)_files.get(i);
            numFilesCount++;
            sizeFilesCount+=desc.getSize();

            //a) Ensure is has the right index.
            Assert.that(desc.getIndex()==i,
                        "Bad index value.  Got "+desc.getIndex()+" not "+i);
            //b) Ensured name indexed indexed. 
            //   (Note we don't check filenames; I'm lazy.)
            Assert.that(indices.contains(i),
                        "Index does not contain entry for "+i);
            //c) Ensure properly listed in directory
            try {
                IntSet siblings=(IntSet)_sharedDirectories.get(
                    getCanonicalFile(getParentFile(desc.getFile())));
                Assert.that(siblings!=null, 
                    "Directory for "+desc.getPath()+" isn't shared");
                Assert.that(siblings.contains(i),
                    "Index "+i+" not in directory");
            } catch (IOException e) {
                Assert.that(false);
            }
            //d) Ensure URNs listed.
            for (iter=desc.getUrns().iterator(); iter.hasNext(); ) {
                URN urn=(URN)iter.next();
                IntSet indices2=(IntSet)_urnIndex.get(urn);
                Assert.that(indices2!=null, "Urn not found");
                Assert.that(indices2.contains(desc.getIndex()));
            }
        }   
        Assert.that(_numFiles==numFilesCount,
                    _numFiles+" should be "+numFilesCount);
        Assert.that(_size==sizeFilesCount,
                    _size+" should be "+sizeFilesCount);
    }

    //Unit tests: tests/com/limegroup/gnutella/FileManagerTest.java
    //            core/com/limegroup/gnutella/tests/UrnRequestTest.java
}
