package com.limegroup.gnutella;

import java.io.*;
import com.sun.java.util.collections.*;
import com.limegroup.gnutella.util.*;


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

    /******  LOCKING: obtain this' monitor before modifying this *******/

    /** the total size of all files, in bytes.
     *  INVARIANT: _size=sum of all size of the elements of _files */
    private int _size;
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

    /** The thread responsisble for adding contents of _sharedDirectories to
     *  this, or null if no load has yet been triggered.  This is necessary
     *  because indexing files can be slow.  Interrupt this thread to stop the
     *  loading; it will periodically check its interrupted status. */
    private Thread _loadThread;

    /** The single instance of FileManager.  (Singleton pattern.) */
    private static FileManager _instance = new FileManager();

    /** Characters used to tokenize queries and file names. */
    static final String DELIMETERS=" -._+/*()\\";
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

    private FileManager() {
        // We'll initialize all the instance variables so that the FileManager
        // is ready once the constructor completes, even though the
        // thread launched at the end of the constructor will immediately
        // overwrite all these variables
        _size = 0;
        _numFiles = 0;
        _files = new ArrayList();
        _index = new Trie(true);  //ignore case
        _extensions = new TreeSet(new StringComparator());
        _sharedDirectories = new TreeMap(new FileComparator());
    }

    
    /** Returns the single instance of the FileManager.  The FileManager
     *  has no files until loadSettings() is called.  */
    public static FileManager instance() {
        return _instance;
    }

    /** Returns the size of all files, in <b>bytes</b>. */
    public int getSize() {return _size;}

    /** Returns the number of files. */
    public int getNumFiles() {return _numFiles;}


    /**
     * Returns the file descriptor with the given index.  Throws
     * IndexOutOfBoundsException if the index is not valid, either because the
     * file was never shared or was "unshared".<p>
     *
     * Design note: this is slightly unusual use of NoSuchElementException.  For
     * example, get(0) and get(2) may throw an exception but get(1) may not.
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
     * Returns a list of all shared files in the given directory, in any order.
     * Returns null if directory is not shared, or a zero-length array if it is
     * shared but contains no files.  This method is not recursive; files in any
     * of the directory's children are not returned.  
     */
    public synchronized File[] getSharedFiles(File directory) {        
        //Remove case, trailing separators, etc.
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
    }


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
     * @modifies this 
     */
    public synchronized void loadSettings() {
        // If settings are already being loaded, interrupt and wait for them to
        // complete.  TODO: the call to join would block if the call to
        // File.list called by listFiles called by addDirectory blocks.  If this
        // is the case, we need to spawn a thread before join'ing.
        if (_loadThread!=null) {
            _loadThread.interrupt();
            try {
                _loadThread.join();
            } catch (InterruptedException e) {
                return;
            }
        }

        // Reset the file list info
        _size = 0;
        _numFiles = 0;
        _files=new ArrayList();
        _index=new Trie(true); //maintain invariant
        _extensions = new TreeSet(new StringComparator());
        _sharedDirectories = new TreeMap(new FileComparator());
        
        // Load the extensions.
        String[] extensions = HTTPUtil.stringSplit(
            SettingsManager.instance().getExtensions().trim(),
            ';');
        for (int i=0; i<extensions.length; i++)
            _extensions.add(extensions[i].toLowerCase());
                      

        // Load the shared directories and their files asynchonously.
        // Duplicates in the directories list will be ignored.  Note that the
        // runner thread only obtain this' monitor when adding individual files.
        final String[] directories = HTTPUtil.stringSplit(
            SettingsManager.instance().getDirectories().trim(),
            ';');
        _loadThread = new Thread("FileManager.loadSettings") {
            public void run() {
                // Add each directory as long as we're not interrupted.
                int i=0;
                while (i<directories.length && !_loadThread.isInterrupted()) {
                    addDirectory(new File(directories[i]), null);      
                    i++;
                }

                // Compact the index once.  As an optimization, we skip this if
                // loadSettings has subsequently been called.
                if (! _loadThread.isInterrupted()) {
                    synchronized (FileManager.this) {
                        _index.trim(new Function() {
                            public Object apply(Object intSet) {
                                ((IntSet)intSet).trim();
                                return intSet;
                            }
                        });
                    }
                }
            }
        };
        _loadThread.start();
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
            if (!directory.exists() || !directory.isDirectory())
                return;       
        } catch (IOException e) {
            return;  //doesn't exist?
        }

        synchronized (this) {
            if (_sharedDirectories.get(directory)!=null)
                //directory already added.  Don't re-add.
                return;
            _sharedDirectories.put(directory, new IntSet());
            //_callback.addSharedDirectory(directory, parent);
        }

        File[] file_list = listFiles(directory);    /* the files in a specified */
        int n = file_list.length;                   /* directory */
       
        // go through file_list
        // get file name
        // see if it contains extention.
        // if yes, add to new list...
        //   TODO: add all files before directories so IntSet's become
        //   less fragmented
        for (int i=0; i<n && !_loadThread.isInterrupted(); i++) {
            if (file_list[i].isDirectory())     /* the recursive call */
                addDirectory((File)file_list[i], directory);
            else                                /* add the file with the */
                addFile((File)file_list[i]); 
        }
    }


    /**
     * @modifies this
     * @effects adds the given file to this, if it exists in a shared 
     *  directory and has a shared extension.  <b>WARNING: this is a potential
     *  security hazard.</b> 
     *
     * Design note: this method takes a String as an argument for compatibility
     * with HTTPDownloader.  For consistency, it should take a File.  
     */
    public synchronized void addFileIfShared(String path) {
        //Make sure capitals are resolved properly, etc.
        File f = null;
        try {
            f=getCanonicalFile(new File(path));
            if (!f.exists())
                return;
        } catch (IOException e) {
            return;
        }

        File dir = getParentFile(f);
        if (dir==null)
            return;

        //TODO: if overwriting an existing, take special care.
        if (_sharedDirectories.containsKey(dir))
            addFile(f);
    }


    /**
     * @requires the given file exists and is in a shared directory
     * @modifies this
     * @effects adds the given file to this if it is of the proper 
     *  extension.  <b>WARNING: this is a potential security hazard; 
     *  caller must ensure the file is in the shared directory.</b>
     */
    private synchronized void addFile(File file) {
        String path = file.getAbsolutePath();   //TODO: right method?
        String name = file.getName();    
        if (hasExtension(name)) {
            int n = (int)file.length();  
            _size += n;                    
            _files.add(new FileDesc(_files.size(), name, path,  n));
            _numFiles++;
            int j=_files.size()-1;              //this' index

            //Register this file with its parent directory.
            File parent=getParentFile(file);
            Assert.that(parent!=null, "Null parent to \""+file+"\"");
            IntSet siblings=(IntSet)_sharedDirectories.get(parent);
            Assert.that(siblings!=null,
                "Add directory \""+parent+"\" not in "+_sharedDirectories);
            boolean added=siblings.add(j);
            Assert.that(added, "File "+j+" already found in "+siblings);
            //_callback.addSharedFile(file, parent);

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
                //Add j to the set.
                indices.add(j);
            }
        }
    }


    /**
     * @modifies this
     * @effects ensures the given file is not shared.  Returns
     *  true iff the file was previously shared.  In this case,
     *  the file's index will not be assigned to any other files.
     *  Note that the file is not actually removed from disk.
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


    /** Returns true if filename has a shared extension.  Case is ignored. */
    private boolean hasExtension(String filename) {
        int begin = filename.lastIndexOf(".");

        if (begin == -1)
            return false;

        String ext = filename.substring(begin + 1).toLowerCase();
        return _extensions.contains(ext);
    }

    /** Same as f.getParentFile() in JDK1.3. */
    public static File getParentFile(File f) {
        //Strip off any trailing "\"'s to work around limitations in JDK1.1.8.
        //This isn't actually needed in this file, because f is always
        //canonicalized if a directory, but it can't hurt.f
        while (true) {
            String name=f.getAbsolutePath();
            if (! name.endsWith(File.separator))
                break;
            f=new File(name.substring(0, name.length()-1));
        }
        
        //Now list contents of directory.
        String name=f.getParent();
        if (name==null)
            return null;
        else 
            return new File(name);
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
                Response r=new Response(desc._index, desc._size, desc._name);
                ret[j]=r;
                j++;
            }
            Assert.that(j==ret.length,
                        "_numFiles is too large");
            return ret;
        }

        //Normal case: query the index to find all matches.  TODO: this
        //sometimes returns more results (>255) than we actually send out.
        //That's wasted work.
        IntSet matches = search(str);
        if (matches==null)
            return null;

        Response[] response = new Response[matches.size()];
        int j=0;
        for (IntSet.IntSetIterator iter=matches.iterator(); 
                 iter.hasNext(); 
                 j++) {            
            int i=iter.next();
            FileDesc desc = (FileDesc)_files.get(i);
            response[j] = new Response(desc._index, desc._size, desc._name);
        }
        return response;
    }


    /**
     * Returns a set of indices of files matching q, or null if there are no
     * matches.  Subclasses may override to provide different notions of
     * matching.  The caller of this method must not mutate the returned
     * value.
     */
    protected IntSet search(String query) {
        //As an optimization, we lazily allocate all sets in case there are no
        //matches.  TODO2: we can avoid allocating sets when getPrefixedBy
        //returns an iterator of one element and there is only one keyword.
        IntSet ret=null;

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

    /** Unit test.  REQUIRES JAVA2 FOR createTempFile */
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
            fman.loadSettings();
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
            fman.addFileIfShared(f2.getAbsolutePath());
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
            Assert.that(files[1].equals(f2), files[0]+" differs from "+f2);

            //Remove file that's shared.  Back to 1 file.
            Assert.that(fman.removeFileIfShared(f2)==true);
            Assert.that(fman.getSize()==1);
            Assert.that(fman.getNumFiles()==1);
            responses=fman.query(new QueryRequest((byte)3,0,"unit"));
            Assert.that(responses.length==1);
            files=fman.getSharedFiles(directory);
            Assert.that(files.length==1);
            Assert.that(files[0].equals(f1), files[0]+" differs from "+f1);

            //Add a new second file, with new index.
            fman.addFileIfShared(f3.getAbsolutePath());
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
            Assert.that(files[1].equals(f3), files[0]+" differs from "+f3);

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
    */
}





