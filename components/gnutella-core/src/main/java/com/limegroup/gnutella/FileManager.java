/**
 * auth: rsoule
 * file: FileManager.java
 * desc: This class will keep track of all the files that
 *       may be shared through the client.  It keeps them
 *       in the list _files.  There are methods for adding
 *       one file, or a whole directory.
 */

package com.limegroup.gnutella;

import java.io.*;
import com.sun.java.util.collections.*;
import com.limegroup.gnutella.util.*;

public class FileManager {
    /** The string used by Clip2 reflectors to index hosts. */
    public static final String INDEXING_QUERY="    ";
    /** The string used by LimeWire to browse hosts. */
    public static final String BROWSE_QUERY="*.*";

    /** the total size of all files, in bytes.
     *  INVARIANT: _size=sum of all size of the elements of _files */
    private int _size;
    /** the total number of files.  INVARIANT: _numFiles==number of
     *  elements of _files that are not null. */
    private int _numFiles;
    /** the list of shareable files.  An entry is null if it is no longer
     *  shared.  INVARIANT: for all i, f[i]==null, or f[i].index==i and
     *  f[i]._path is in the shared folder with a shareable extension.
     *  LOCKING: obtain this before modifying. */
    private ArrayList /* of FileDesc */ _files;
    /** an index mapping keywords in file names to the indices in _files.  A
     * keyword of a filename f is defined to be a maximal sequence of characters
     * without a character from DELIMETERS.  INVARIANT: For all keys k in
     * _index, for all i in _index.get(k), _files[i]._path.substring(k)!=-1.
     * Likewise for all i, for all k in _files[i]._path, _index.get(k)
     * contains i. */
    private Trie /* String -> IntSet  */ _index;

    private String[] _extensions;
    private Set _sharedDirectories;

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
        _extensions = new String[0];
        _sharedDirectories = new HashSet();

        // Launch a thread to asynchronously scan directories to load files
        Thread loadSettingsThread =
            new Thread()
            {
                public void run()
                {
                    loadSettings();
                }
            };
        // Not a daemon thread -- terminating early is fine
        loadSettingsThread.start();
    }

    public static FileManager instance() {
        return _instance;
    }

    /** Returns the size of all files, in <b>bytes</b>. */
    public int getSize() {return _size;}
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
     * Returns an array of all responses matching the given request, or null if
     * there are no responses.<p>
     *
     * Design note: this method returns null instead of an empty array to avoid
     * allocations in the common case of no matches.)
     */
    public Response[] query(QueryRequest request) {
        String str = request.getQuery();

        //Special case: return everything for Clip2 indexing query (" ") and
        //browse queries ("*.*").  If these messages had initial TTLs too high,
        //StandardMessageRouter will clip the number of results sent on the
        //network.  Note that some initial TTLs are filterd by GreedyQuery
        //before they ever reach this point.
        if (str.equals(INDEXING_QUERY) || str.equals(BROWSE_QUERY)) {
            synchronized (this) {   
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

    private boolean hasExtension(String filename) {
        int begin = filename.lastIndexOf(".");

        if (begin == -1)
            return false;

        String ext = filename.substring(begin + 1);

        int length = _extensions.length;
        for (int i = 0; i < length; i++) {
            if (ext.equalsIgnoreCase(_extensions[i])) {
                return true;
            }
        }
        return false;

    }

    /**
     * @modifies this
     * @effects adds the given file to this, if it exists
     *  and is of the proper extension.  <b>WARNING: this is a
     *  potential security hazard; caller must ensure the file
     *  is in the shared directory.</b>
     * This method should only be called from threads that hold this' monitor
     */
    private void addFile(String path) {
        File myFile = new File(path);

        if (!myFile.exists())
            return;

        String name = myFile.getName();     /* the name of the file */
        if (hasExtension(name)) {
            int n = (int)myFile.length();       /* the list, and increments */
            _size += n;                         /* the appropriate info */
            _files.add(new FileDesc(_files.size(), name, path,  n));
            _numFiles++;

            //For each keyword...
            int j=_files.size()-1;
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
     * @effects adds the given file to this, if it exists
     *  and if it is shared.
     *  <b>WARNING: this is a potential security hazard.</b>
     */
    public synchronized void addFileIfShared(String path) {

        Assert.that(_sharedDirectories != null);

        File f = new File(path);

        String parent = f.getParent();

        File dir = new File(parent);

        if (dir == null)
            return;

        String p;

        try {
            p = dir.getCanonicalPath();
        } catch (IOException e) {
            return;
        }
        if (!_sharedDirectories.contains(p))
            return;

        addFile(path);


    }

    /**
     * @modifies this
     * @effects ensures the given file is not shared.  Returns
     *  true iff the file was previously shared.  In this case,
     *  the file's index will not be assigned to any other files.
     *  Note that the file is not actually removed from disk.
     */
    public synchronized boolean removeFileIfShared(File file) {
        //Look for a file matching <file>...
        for (int i=0; i<_files.size(); i++) {
            FileDesc fd=(FileDesc)_files.get(i);
            if (fd==null)
                continue;
            File candidate=new File(fd._path);

            //Aha, it's shared. Unshare it by nulling it out.
            if (file.equals(candidate)) {
                _files.set(i,null);
                _numFiles--;
                _size-=fd._size;

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
     * Loads the extensions and directories settings and rebuilds the file
     * index.
     *
     * @modifies this
     * @effects ensures that this contains exactly the files recursively given
     *  directories and all their recursive subdirectories.  If dir_names
     *  contains duplicate directories, the duplicates will be ignored.  If
     *  the directories list contains files, they will be ignored. Note that
     *  some files in this before the call will not be in this after the call,
     *  or they may have a different index.
     * <b>WARNING: this is a potential security hazard.</b>
     * WARNING: This call could run for a long time. Only threads that
     * are prepared for that possibility should invoke it.  See the
     * FileManager constructor for an example.
     */
    public synchronized void loadSettings() {
        // Reset the file list info
        _size = 0;
        _numFiles = 0;
        _files=new ArrayList();
        _sharedDirectories = new HashSet();

        // Load the settings info
        String dir_names = SettingsManager.instance().getDirectories();
        dir_names.trim();
        String[] names = HTTPUtil.stringSplit(dir_names, ';');
        _extensions =  HTTPUtil.stringSplit(
            SettingsManager.instance().getExtensions(), ';');


        // need to see if there are duplicated directories...
        int size = names.length;

        File f;  // temporary file for testing existence
        String p;  // for the canonical path of a file
        String name;  //the semi colon deliminated string

        for (int i = 0; i < size; i++) {

            name = names[i];

            f = new File(name);

            if (!f.isDirectory())
                continue;
            try {
                p = f.getCanonicalPath();
            }
            catch (Exception e) {
                continue;
            }
            _sharedDirectories.add(p);
        }

        int hashsize = _sharedDirectories.size();

        String[] dirs = new String[hashsize];

        int j=0;

        for(Iterator iter = _sharedDirectories.iterator(); iter.hasNext() ;) {
            dirs[j++] = (String)iter.next();
        }

        for (int i=0; i < hashsize; i++) {
            addDirectory(dirs[i]);
        }        

//          System.out.println("Index loaded.");
//          System.out.println(_index.toString());   
    }

    /**
     *  Build the equivalent of the File.listFiles() utility in jdk1.2.2
     */
    private File[] listFiles(File dir)
    {
        String [] fnames   = dir.list();
        File   [] theFiles = new File[fnames.length];

        for ( int i = 0; i < fnames.length; i++ )
        {
            theFiles[i] = new File(dir, fnames[i]);
        }

        return theFiles;
    }

    /**
     * @modifies this
     * @effects adds the all the files with shared extensions
     *  in the given directory and its recursive children.
     *  If dir_name is actually a file, it will be added if it has
     *  a shared extension.  Entries in this before the call are unmodified.
     */
    public synchronized void addDirectory(String dir_name) {
        File myFile = new File(dir_name);
        if (!myFile.exists())
            return;
        File[] file_list = listFiles(myFile);   /* the files in a specified */
        int n = file_list.length;               /* directory */

        // go through file_list
        // get file name
        // se if it contains extention.
        // if yes, add to new list...


        for (int i=0; i < n; i++) {

            if (file_list[i].isDirectory())     /* the recursive call */
                addDirectory(file_list[i].getAbsolutePath());
            else                                /* add the file with the */
                addFile(file_list[i].getAbsolutePath());  /* addFile method */
        }

        //Compact the index.  Ideally this should be called just once on startup, not
        //once for each directory.
        _index.trim(new Function() {
            public Object apply(Object intSet) {
                ((IntSet)intSet).trim();
                return intSet;
            }
        });
    }

    ////////////////////////////////// Queries ///////////////////////////////

    /**
     * Returns a set of indices of files matching q, or null if there are no
     * matches.  Subclasses may override to provide different notions of
     * matching.  The caller of this method must not mutate the returned
     * value.
     */
    protected synchronized IntSet search(String query) {
        //TODO2: ideally this wouldn't be synchronized, a la ConnectionManager.
        //Doing so would allow multiple queries to proceed in parallel.  But
        //then you need to make _files volatile and work on a local reference,
        //i.e., "_files=this._files"

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

    /** Unit test--REQUIRES JAVA2 FOR USE OF CREATETEMPFILE */
    /*
    public static void main(String args[]) {
        //Test some of add/remove capability
        File f1=null;
        File f2=null;
        File f3=null;
        try {
            f1=createNewTestFile(1);
            System.out.println("Creating temporary files in "+f1.getParent());
            FileManager fman=FileManager.instance();
            fman.setExtensions("XYZ");
            fman.addDirectory(f1.getParent());
            f2=createNewTestFile(3);
            f3=createNewTestFile(11);

            //One file
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

            //Two files
            fman.addFile(f2.getAbsolutePath());
            Assert.that(fman.getNumFiles()==2, fman.getNumFiles()+"");
            Assert.that(fman.getSize()==4, fman.getSize()+"");
            responses=fman.query(new QueryRequest((byte)3,0,"unit"));
            Assert.that(responses[0].getIndex()!=responses[1].getIndex());
            for (int i=0; i<responses.length; i++) {
                Assert.that(responses[i].getIndex()==0
                               || responses[i].getIndex()==1);
            }

            //Remove file that's shared.  Back to 1 file.
            Assert.that(fman.removeFileIfShared(f2)==true);
            Assert.that(fman.getSize()==1);
            Assert.that(fman.getNumFiles()==1);
            responses=fman.query(new QueryRequest((byte)3,0,"unit"));
            Assert.that(responses.length==1);

            fman.addFile(f3.getAbsolutePath());
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

        } finally {
            if (f1!=null) f1.delete();
            if (f2!=null) f2.delete();
            if (f3!=null) f3.delete();
        }
    }

    static File createNewTestFile(int size) {
        try {
            File ret=File.createTempFile("FileManager_unit_test",".XYZ");
            OutputStream out=new FileOutputStream(ret);
            out.write(new byte[size]);
            out.flush();
            out.close();
            return ret;
        } catch (Exception e) {
            System.err.println("Couldn't run test");
            e.printStackTrace();
            System.exit(1);
            return null; //never executed
        }
    }
    */
}





