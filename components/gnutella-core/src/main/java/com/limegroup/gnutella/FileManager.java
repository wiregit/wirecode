package com.limegroup.gnutella;

import java.io.*;
import com.sun.java.util.collections.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.xml.*;

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

    /** the total size of all files, in bytes.
     *  INVARIANT: _size=sum of all size of the elements of _files */
    private long _size;
    /** the total number of files.  INVARIANT: _numFiles==number of
     *  elements of _files that are not null. */
    private int _numFiles;
    /** the list of shareable files.  An entry is null if it is no longer
     *  shared.  INVARIANT: for all i, f[i]==null, or f[i].index==i and
     *  f[i]._path is in a shared directory with a shareable extension. */
    private ArrayList /* of FileDesc */ _files;

    /** an index mapping keywords in file names to the indices in _files.  A
     * keyword of a filename f is defined to be a maximal sequence of characters
     * without a character from DELIMETERS.  INVARIANT: For all keys k in
     * _index, for all i in _index.get(k), _files[i]._path.substring(k)!=-1.
     * Likewise for all i, for all k in _files[i]._path, _index.get(k)
     * contains i. */
    private Trie /* String -> IntSet  */ _index;
    /** an index mapping appropriately case-normalized URN strings to
     * the indices in _files.  */

    private Hashtable /* String -> IntSet  */ _urnIndex;
    
    /** The set of extensions to share, sorted by StringComparator. 
     *  INVARIANT: all extensions are lower case. */
    private Set /* of String */ _extensions;
    /** The list of shared directories and their contents.  More formally, a
     *  mapping whose keys are shared directories and any subdirectories
     *  reachable through those directories.  The value for any key is the set
     *  of indices of all shared files in that directory.  INVARIANT: for any
     *  key k with value v in _sharedDirectories, for all i in v,
     *       _files[i]._path==k+_files[i]._name.
     *  Likewise, for all i s.t. _files[i]!=null,
     *       _sharedDirectories.get(
     *            _files[i]._path-_files[i]._name).contains(i).
     * Here "==" is shorthand for file path comparison and "a-b" is short for
     * string 'a' with suffix 'b' removed.  INVARIANT: all keys in this are
     * canonicalized files, sorted by a FileComparator. */
    private Map /* of File -> IntSet */ _sharedDirectories;

	/**
	 * Constant member for the <tt>FileDescLoader</tt> instance that handles
	 * the creation and loading of <tt>FileDesc</tt> instances.
	 */
	private final FileDescLoader _fileDescLoader;

    /** The thread responsisble for adding contents of _sharedDirectories to
     *  this, or null if no load has yet been triggered.  This is necessary
     *  because indexing files can be slow.  Interrupt this thread to stop the
     *  loading; it will periodically check its interrupted status. 
     *  LOCKING: obtain _loadThreadLock before modifying and before obtaining
     *  this (to prevent deadlock). */
    private Thread _loadThread;
    /** The lock for _loadThread.  Necessary to prevent deadlocks in
     *  loadSettings. */
    private Object _loadThreadLock=new Object();
    
    /** The callback for adding shared directories and files, or null
     *  if this has no callback.  */
    private static ActivityCallback _callback;

    /** Characters used to tokenize queries and file names. */
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

    public FileManager() {
        // We'll initialize all the instance variables so that the FileManager
        // is ready once the constructor completes, even though the
        // thread launched at the end of the constructor will immediately
        // overwrite all these variables
        _size = 0;
        _numFiles = 0;
        _files = new ArrayList();
        _index = new Trie(true);  //ignore case
        _urnIndex = new Hashtable();
        _extensions = new TreeSet(new StringComparator());
        _sharedDirectories = new TreeMap(new FileComparator());
		_fileDescLoader = new FileDescLoader(this);
    }

    /** Asynchronously loads all files by calling loadSettings.  Sets this'
     *  callback to be "callback", and notifies "callback" of all file loads.
     *      @modifies this
     *      @see loadSettings */
    public void initialize(ActivityCallback callback) {
        this._callback=callback;
        loadSettings(false);
    }

    ////////////////////////////// Accessors ///////////////////////////////

    
    /** Returns the size of all files, in <b>bytes</b>.  Note that the largest
     *  value that can be returned is Integer.MAX_VALUE, i.e., ~2GB.  If more
     *  bytes are being shared, returns this value. */
    public int getSize() {return ByteOrder.long2int(_size);}

    /** Returns the number of files. */
    public int getNumFiles() {return _numFiles;}


    /**
     * Returns the file descriptor with the given index.  Throws
     * IndexOutOfBoundsException if the index is not valid, either because the
     * file was never shared or was "unshared".<p>
     *
     * Design note: this is slightly unusual use of IndexOutOfBoundsException.
     * For example, get(0) and get(2) may throw an exception but get(1) may not.
     * NoSuchElementException was considered as an alernative, but this can
     * create ambiguity problems between java.util and com.sun.java.util.  
     */
    public FileDesc get(int i) throws IndexOutOfBoundsException {
        FileDesc ret=(FileDesc)_files.get(i);
        if (ret==null)
            throw new IndexOutOfBoundsException();
        return ret;
    }

	/**
	 * Returns the file index for the specified URN.  This only returns 
	 * one index, even though multiple indeces are possible with 
	 * HUGE v. 0.93.
	 *
	 * @param urn the urn for the file
	 * @return the index corresponding to the requested urn, or
	 *  -1 if not matching index could be found
	 */
	public int getFileIndexForUrn(final URN urn) {
        Iterator iter = _files.iterator();
		int count = 0;
        while(iter.hasNext()) {
            FileDesc candidate = (FileDesc)iter.next();
            if (candidate==null) continue;
            if (candidate.containsUrn(urn)) {
                return count;
            }
			count++;
        }
        // none found
        return -1;
	}

	/**
	 * Returns the <tt>FileDesc</tt> for the specified URN.  This only returns 
	 * one <tt>FileDesc</tt>, even though multiple indeces are possible with 
	 * HUGE v. 0.93.
	 *
	 * @param urn the urn for the file
	 * @return the <tt>FileDesc</tt> corresponding to the requested urn, or
	 *  <tt>null</tt> if not matching <tt>FileDesc</tt> could be found
	 */
	public FileDesc getFileDescForUrn(final URN urn) {
        Iterator iter = _files.iterator();
        while(iter.hasNext()) {
            FileDesc candidate = (FileDesc)iter.next();
            if (candidate==null) continue;
            if (candidate.containsUrn(urn)) {
                return candidate;
            }
        }
        // none found
        return null;
	}

   /**
     * Returns the FileDesc matching the passed-in path and size, if any,
     * null otherwise. Kind of silly, definitely inefficient, but only
     * needed rarely, from library view, because there's no sharing of
     * data structures for local files
     */	
    public FileDesc getFileDescMatching(String path, int size) {
        // linear probe. thankfully it's rare
        Iterator iter = _files.iterator();
        while(iter.hasNext()) {
            FileDesc candidate = (FileDesc)iter.next();
            if (candidate==null) continue;
            // do quicker check first
            if (size!=candidate._size) continue;
            if (path.equals(candidate._path)) {
                // bingo
                return candidate;
            }
        }
        // none found
        return null;
	}
	


    /**
     * Returns a list of all shared files in the given directory, in any order.
     * Returns null if directory is not shared, or a zero-length array if it is
     * shared but contains no files.  This method is not recursive; files in 
     * any of the directory's children are not returned.   
     * <p>
     * If directory is null, returns all shared files.
     */
    public synchronized File[] getSharedFiles(File directory) {
        if(directory!=null){
            // a. Remove case, trailing separators, etc.
            try {
                directory=getCanonicalFile(directory);
            } catch (IOException e) {
                return null;
            }
            
            //Lookup indices of files in the given directory...
            IntSet indices=(IntSet)_sharedDirectories.get(directory);
            if (indices==null)
                return null;
            //...and pack them into an array.
            File[] ret=new File[indices.size()];
            IntSet.IntSetIterator iter=indices.iterator(); 
            for (int i=0; iter.hasNext(); i++) {
                FileDesc fd=(FileDesc)_files.get(iter.next());
                Assert.that(fd!=null, "Directory has null entry");
                ret[i]=new File(fd._path);
            }
            return ret;
        } else {
            // b. Filter out unshared entries.
            ArrayList buf=new ArrayList(_files.size());
            for (int i=0; i<_files.size(); i++) {
                FileDesc fd=(FileDesc)_files.get(i);
                if (fd!=null)
                    buf.add(new File(fd._path));                
            }
            File[] ret=new File[buf.size()];
            Object[] out=buf.toArray(ret);
            Assert.that(out==ret, "Couldn't fit list in returned value");
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

    /*
      public static void main(String argv[]) {
      String[] filter = {"mp3"};
      File[] toPrint = FileManager.getFilesRecursive(new File(argv[0]), 
      filter);
      for (int i = 0; 
      (toPrint != null) && (i < toPrint.length); 
      i++)
      System.out.println(""+toPrint[i]);
      }
    */

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
            //by listFiles called by addDirectory blocks.  If this is the case,
            //we need to spawn a thread before join'ing.
            if (_loadThread!=null) {
                _loadThread.interrupt();
                try {
                    _loadThread.join();
                } catch (InterruptedException e) {
                    return;
                }
            }

            final boolean notifyOnClearFinal = notifyOnClear;
            _loadThread = new Thread("FileManager.loadSettingsBlocking") {
                public void run() {
                    loadSettingsBlocking(notifyOnClearFinal);
                }
            };
            _loadThread.start();
        } 
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
            _files=new ArrayList();
            _index=new Trie(true); //maintain invariant
            _extensions = new TreeSet(new StringComparator());
            _sharedDirectories = new TreeMap(new FileComparator());
            
            if (_loadThread.isInterrupted())
                return;

            // Load the extensions.
            String[] extensions = 
            StringUtils.split(SettingsManager.instance().getExtensions().trim(),
                              ';');
            for (int i=0; 
                 (i<extensions.length) && !_loadThread.isInterrupted();
                 i++)
                _extensions.add(extensions[i].toLowerCase());
            
            if (_loadThread.isInterrupted())
                return;

            //Ideally we'd like to ensure that "C:\dir\" is loaded BEFORE
            //C:\dir\subdir.  Although this isn't needed for correctness, it may
            //help the GUI show "subdir" as a subdirectory of "dir".  One way of
            //doing this is to do a full topological sort, but that's a lot of work.
            //So we just approximate this by sorting by filename length, from
            //smallest to largest.  Unless directories are specified as
            //"C:\dir\..\dir\..\dir", this will do the right thing.
            //final String[] directories = 
            //StringUtils.split(SettingsManager.instance().getDirectories().trim(),
			//                ';');

			final File[] directories = SettingsManager.instance().getDirectories();

            if (_loadThread.isInterrupted())
                return;

            Arrays.sort(directories, new Comparator() {
                public int compare(Object a, Object b) {
                    return (a.toString()).length()-(b.toString()).length();
                }
            });

            //Arrays.sort(directories, new Comparator() {
			//  public int compare(Object a, Object b) {
			//      return ((String)a).length()-((String)b).length();
			//  }
			// });
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
            int i=0;
            while (i<directories.length && !_loadThread.isInterrupted()) {
                addDirectory(directories[i], null);      
                i++;
            }
            
            // Compact the index once.  As an optimization, we skip this
            // if loadSettings has subsequently been called.
            if (! _loadThread.isInterrupted())
                trim();                    
        }
    }


    /**
     * @requires directory is part of DIRECTORIES_TO_SEARCH_FOR_FILES or one of
     *  its children, and parent is directory's shared parent or null if
     *  directory's parent is not shared.
     * @modifies this
     * @effects adds all files with shared extensions in directory and its
     *  (recursive) children to this.  If directory doesn't exist, isn't a 
     *  directory, or has already been added, does nothing.  Entries in this 
     *  before the call are unmodified.
     *     This method is thread-safe.  It acquires locks on a per-file basis.
     *  If the _loadThread is interrupted while adding the contents of the
     *  directory, it returns immediately.  */
    private void addDirectory(File directory, File parent) {
        //We have to get the canonical path to make sure "D:\dir" and "d:\DIR"
        //are the same on Windows but different on Unix.
        try {
            directory=getCanonicalFile(directory);
        } catch (IOException e) {
            return;  //doesn't exist?
        }

        //List contents of directory.
        File[] file_list = listFiles(directory);
        if (file_list == null) 
            return; // directory doesn't exist or isn't a directory...
        int n = file_list.length;                   /* directory */

        //Register this directory with list of share directories.
        synchronized (this) {
            if (_sharedDirectories.get(directory)!=null)
                //directory already added.  Don't re-add.
                return;
            _sharedDirectories.put(directory, new IntSet());
            if (_callback!=null)
                _callback.addSharedDirectory(directory, parent);
        }
      
        //First add all files.  We'll add the directories later to smooth out
        //what the user sees.  It also decreases the size of the IntSet values
        //in _sharedDirectories.  Again, this is not strictly necessary for
        //correctness.
        List /* of File */ directories=new ArrayList();
        for (int i=0; i<n && !_loadThread.isInterrupted(); i++) {
            if (file_list[i].isDirectory())     /* the recursive call */
                directories.add(file_list[i]);
            else                                /* add the file with the */
                addFile((File)file_list[i]); 
        }
        //Now add directories discovered in previous pass.
        Iterator iter=directories.iterator();
        while (iter.hasNext() && !_loadThread.isInterrupted())
            addDirectory((File)iter.next(), directory);
    }

    /**
     * @modifies this
     * @effects adds the given file to this, if it exists in a shared 
     *  directory and has a shared extension.  Returns true iff the file
     *  was actually added.  <b>WARNING: this is a potential security 
     *  hazard.</b> 
     */
	public synchronized boolean addFileIfShared(File file) {
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
        if (_sharedDirectories.containsKey(dir))
            return addFile(file);
        else
            return false;
	}

    /**
     * @modifies this
     * @effects calls addFileIfShared(file), then optionally stores any metadata
     *  in the given XML documents.  metadata may be null if there is no data.
     *  Returns the value from addFileIfShared. <b>WARNING: this is a potential
     *  security hazard.</b> 
     */
	public synchronized boolean addFileIfShared(File file,
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
    private synchronized boolean addFile(File file) {
        if (!hasExtension(file.getName())) {
			return false;
		}
		long fileLength = file.length();  
		if (fileLength>Integer.MAX_VALUE || fileLength<0)
			return false;
		_size += fileLength;
		int fileIndex = _files.size();
		FileDesc fileDesc = _fileDescLoader.createFileDesc(file, fileIndex);
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
			_callback.addSharedFile(file, parent);
		
		String path = file.getAbsolutePath();
		//Index the filename.  For each keyword...
		String[] keywords=StringUtils.split(path, DELIMETERS);
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
		
		return true;
    }

    /**
     * @modifies this
     * @effects enters the given FileDesc into the _urnIndex under all its 
     * reported URNs
     */
    public synchronized void updateUrnIndex(FileDesc fileDesc) {
		Iterator iter = fileDesc.getUrns().iterator();
		while (iter.hasNext()) {
			URN urn = (URN)iter.next();
			IntSet indices=(IntSet)_urnIndex.get(urn);
			if (indices==null) {
				indices=new IntSet();
				_urnIndex.put(urn, indices);
			}

			// hmmnn...suspicious line.
			indices.add(fileDesc._index);
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
        //Take care of case, etc.
        try {
            f=getCanonicalFile(f);
        } catch (IOException e) {
            return false;
        }

        //Look for a file matching <file>...
        for (int i=0; i<_files.size(); i++) {
            FileDesc fd=(FileDesc)_files.get(i);
            if (fd==null)
                continue;
            File candidate=new File(fd._path);

            //Aha, it's shared. Unshare it by nulling it out.
            if (f.equals(candidate)) {
                _files.set(i,null);
                _numFiles--;
                _size-=fd._size;

                //Remove references to this from directory listing
                File parent=getParentFile(f);
                IntSet siblings=(IntSet)_sharedDirectories.get(parent);
                Assert.that(siblings!=null,
                    "Rem directory \""+parent+"\" not in "+_sharedDirectories);
                boolean removed=siblings.remove(i);
                Assert.that(removed, "File "+i+" not found in "+siblings);

                //Remove references to this from index.
                String[] keywords=StringUtils.split(fd._path,
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
                return true;  //No more files in list will match this.
            }
        }
        return false;
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
    private boolean hasExtension(String filename) {
        int begin = filename.lastIndexOf(".");

        if (begin == -1)
            return false;

        String ext = filename.substring(begin + 1).toLowerCase();
        return _extensions.contains(ext);
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
     */
    public List getIndivisibleKeyWords() {
        return new ArrayList();
    }

    /** Same as f.getCanonicalFile() in JDK1.3. */
    public static File getCanonicalFile(File f) throws IOException {
        return new File(f.getCanonicalPath());
    }


    ////////////////////////////////// Queries ///////////////////////////////

    /**
     * Returns an array of all responses matching the given request, or null if
     * there are no responses.<p>
     *
     * Design note: this method returns null instead of an empty array to avoid
     * allocations in the common case of no matches.)
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
            if (_numFiles==0)
                return null;
            //Extract responses for all non-null (i.e., not deleted) files.
            Response[] ret=new Response[_numFiles];
            int j=0;
            for (int i=0; i<_files.size(); i++) {
                FileDesc desc = (FileDesc)_files.get(i);
                if (desc==null) 
                    continue;                    
                Assert.that(j<ret.length,
                            "_numFiles is too small");
                //Response r= desc.responseFor(request);
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
        matches = search( str,
                          matches);
        if ( request.getQueryUrns()!=null ) {
            matches = urnSearch(request.getQueryUrns().iterator(),matches);
        }
        
        if (matches==null)
            return null;

        Response[] response = new Response[matches.size()];
        int j=0;
        for (IntSet.IntSetIterator iter=matches.iterator(); 
                 iter.hasNext(); 
                 j++) {            
            int i=iter.next();
            FileDesc desc = (FileDesc)_files.get(i);
            response[j] = new Response(desc);//desc.responseFor(request);
        }
        return response;
    }

    public FileDesc file2index(String fullName) {  
        // TODO1: precompute and store in table.
        for (int i=0; i<_files.size(); i++) {
            FileDesc fd=(FileDesc)_files.get(i);
            if (fd==null)  file://unshared
            continue;
            else if (fd._path.equals(fullName))
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
    protected IntSet urnSearch(Iterator urnsIter,IntSet priors) {
        IntSet ret = priors;
        while(urnsIter.hasNext()) {
            URN urn = (URN)urnsIter.next();
            // TODO (eventually): case-normalize URNs as appropriate
            // for now, though, prevalent practice is same as local: 
            // lowercase "urn:<type>:", uppercase Base32 SHA1
            IntSet hits = (IntSet)_urnIndex.get(urn);
            if(hits!=null) {
                // double-check hits to ensure they're still valid
                IntSet.IntSetIterator iter = hits.iterator();
                while(iter.hasNext()) {
                    FileDesc fd = (FileDesc)_files.get(iter.next());
                    if(fd.containsUrn(urn)) {
                        // still valid
                        if(ret==null) ret = new IntSet();
                        ret.add(fd._index);
                    } else {
                        // was invalid; consider rehashing
                        if(!fd.hasSHA1Urn()) {
							//backgroundCalculateAndUpdate(fd);
							_fileDescLoader.loadFileDesc(fd);
                        }
                    }
                }
            }
        }
        return ret;
    }
    


    ///////////////////////////////////// Testing //////////////////////////////

    /** Checks this' rep. invariants.  VERY expensive. */
    /*
    protected synchronized void repOk() {
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
            sizeFilesCount+=desc._size;

            //a) Ensure is has the right index.
            Assert.that(desc._index==i,
                        "Bad index value.  Got "+desc._index+" not "+i);
            //b) Ensured name indexed indexed. 
            //   (Note we don't check filenames; I'm lazy.)
            Assert.that(indices.contains(i),
                        "Index does not contain entry for "+i);
            //c) Ensure properly listed in directory
            try {
                IntSet siblings=(IntSet)_sharedDirectories.get(
                    getCanonicalFile(getParentFile(new File(desc._path))));
                Assert.that(siblings!=null, 
                    "Directory for "+desc._path+" isn't shared");
                Assert.that(siblings.contains(i),
                    "Index "+i+" not in directory");
            } catch (IOException e) {
                Assert.that(false);
            }
        }   
        Assert.that(_numFiles==numFilesCount,
                    _numFiles+" should be "+numFilesCount);
        Assert.that(_size==sizeFilesCount,
                    _size+" should be "+sizeFilesCount);
    }
    */

    /** Unit test.  REQUIRES JAVA2 FOR createTempFile Note that many tests are
     *  STRONGER than required by the specifications for simplicity.  For
     *  example, we assume an order on the values returned by getSharedFiles. */
    /*
    public static void main(String args[]) {
        //Test some of add/remove capability
        File f1=null;
        File f2=null;
        File f3=null;
        try {
            f1=createNewTestFile(1);
            File directory=getParentFile(f1);
            System.out.println("Creating temporary files in "+f1.getParent());
            FileManager fman=FileManager.instance();
            File[] files=fman.getSharedFiles(directory);
            Assert.that(files==null);

            //One file
            SettingsManager settings=SettingsManager.instance();
            settings.setExtensions("XYZ");
            settings.setDirectories(directory.getAbsolutePath());
            //Since we don't have a non-blocking loadSettings method, we just
            //wait a little time and cross our fingers.
            fman.loadSettings(false);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) { }
            f2=createNewTestFile(3);
            f3=createNewTestFile(11);

            Assert.that(fman.getNumFiles()==1, fman.getNumFiles()+"");
            Assert.that(fman.getSize()==1, fman.getSize()+"");
            Response[] responses=fman.query(new QueryRequest((byte)3,0,"unit"));
            Assert.that(responses.length==1);
            Assert.that(fman.removeFileIfShared(f3)==false);
            responses=fman.query(new QueryRequest((byte)3,0,"unit"));
            Assert.that(responses.length==1);
            Assert.that(fman.getSize()==1);
            Assert.that(fman.getNumFiles()==1);
            fman.get(0);
            files=fman.getSharedFiles(directory);
            Assert.that(files.length==1);
            Assert.that(files[0].equals(f1), files[0]+" differs from "+f1);
            files=fman.getSharedFiles(getParentFile(directory));
            Assert.that(files==null);

            //Two files
            Assert.that(fman.addFileIfShared(new File("C:\\bad.ABCDEF"))==false);
            Assert.that(fman.addFileIfShared(f2.getAbsolutePath())==true);
            Assert.that(fman.getNumFiles()==2, fman.getNumFiles()+"");
            Assert.that(fman.getSize()==4, fman.getSize()+"");
            responses=fman.query(new QueryRequest((byte)3,0,"unit"));
            Assert.that(responses[0].getIndex()!=responses[1].getIndex());
            for (int i=0; i<responses.length; i++) {
                Assert.that(responses[i].getIndex()==0
                               || responses[i].getIndex()==1);
            }
            files=fman.getSharedFiles(directory);
            Assert.that(files.length==2);
            Assert.that(files[0].equals(f1), files[0]+" differs from "+f1);
            Assert.that(files[1].equals(f2), files[1]+" differs from "+f2);

            //Remove file that's shared.  Back to 1 file.                        
            Assert.that(fman.removeFileIfShared(f3)==false);
            Assert.that(fman.removeFileIfShared(f2)==true);
            Assert.that(fman.getSize()==1);
            Assert.that(fman.getNumFiles()==1);
            responses=fman.query(new QueryRequest((byte)3,0,"unit"));
            Assert.that(responses.length==1);
            files=fman.getSharedFiles(directory);
            Assert.that(files.length==1);
            Assert.that(files[0].equals(f1), files[0]+" differs from "+f1);

            //Add a new second file, with new index.
            Assert.that(fman.addFileIfShared(f3.getAbsolutePath())==true);
            Assert.that(fman.getSize()==12, "size of files: "+fman.getSize());
            Assert.that(fman.getNumFiles()==2, "# files: "+fman.getNumFiles());
            responses=fman.query(new QueryRequest((byte)3,0,"unit"));
            Assert.that(responses.length==2, "response: "+responses.length);
            Assert.that(responses[0].getIndex()!=1);
            Assert.that(responses[1].getIndex()!=1);
            fman.get(0);
            fman.get(2);
            try {
                fman.get(1);
                Assert.that(false);
            } catch (IndexOutOfBoundsException e) { }

            responses=fman.query(new QueryRequest((byte)3,0,"*unit*"));
            Assert.that(responses.length==2, "response: "+responses.length);

            files=fman.getSharedFiles(directory);
            Assert.that(files.length==2);
            Assert.that(files[0].equals(f1), files[0]+" differs from "+f1);
            Assert.that(files[1].equals(f3), files[1]+" differs from "+f3);
            files=fman.getSharedFiles(null);
            Assert.that(files.length==2);
            Assert.that(files[0].equals(f1), files[0]+" differs from "+f1);
            Assert.that(files[1].equals(f3), files[1]+" differs from "+f3);


            //Rename files
            Assert.that(fman.renameFileIfShared(f2, f2)==false);
            Assert.that(fman.renameFileIfShared(f1, f2)==true);
            files=fman.getSharedFiles(directory);
            Assert.that(files.length==2);
            Assert.that(files[0].equals(f3));
            Assert.that(files[1].equals(f2));
            Assert.that(
                fman.renameFileIfShared(f2,
                                        new File("C\\garbage.XSADF"))==false);
            files=fman.getSharedFiles(directory);
            Assert.that(files.length==1);
            Assert.that(files[0].equals(f3));

            //Try to add a huge file.  (It will be ignored.)
            File f4=new HugeFakeFile(directory, "big.XYZ", Integer.MAX_VALUE+1l);
            Assert.that(fman.addFile(f4)==false);
            Assert.that(fman.getNumFiles()==1);
            Assert.that(fman.getSize()==11);
            //Test getSize().  Note that we have to use addFile instead of
            //addFileIfShared since the file doesn't exist.
            f4=new HugeFakeFile(directory, "big.XYZ", Integer.MAX_VALUE-1);
            File f5=new HugeFakeFile(directory, "big2.XYZ",
                                     Integer.MAX_VALUE);
            Assert.that(fman.addFile(f4)==true);
            Assert.that(fman.addFile(f5)==true);
            Assert.that(fman.getNumFiles()==3);
            Assert.that(fman.getSize()==Integer.MAX_VALUE);
            responses=fman.query(new QueryRequest((byte)3, (byte)0, "*.*"));
            Assert.that(responses.length==3);
            Assert.that(responses[0].getName().equals(f3.getName()));
            Assert.that(responses[1].getName().equals(f4.getName()));
            Assert.that(responses[2].getName().equals(f5.getName()));
        } finally {
            if (f1!=null) f1.delete();
            if (f2!=null) f2.delete();
            if (f3!=null) f3.delete();
        }
    }

    static File createNewTestFile(int size) {
        try {
            File file=File.createTempFile("FileManager_unit_test",".XYZ");
            OutputStream out=new FileOutputStream(file);
            out.write(new byte[size]);
            out.flush();
            out.close();
            //Needed for comparisons between "C:\Progra~1" and "C:\Program Files".
            return getCanonicalFile(file);
        } catch (Exception e) {
            System.err.println("Couldn't run test");
            e.printStackTrace();
            System.exit(1);
            return null; //never executed
        }
    }

    private static class HugeFakeFile extends File {
        long length;

        public HugeFakeFile(File directory, String name, long length) {
            super(directory, name);
            this.length=length;
        }

        public long length() {
            return length;
        }
    }
    */
}
