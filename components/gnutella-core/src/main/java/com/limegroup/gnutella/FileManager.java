/**
 * auth: rsoule
 * file: FileManager.java
 * desc: This class will keep track of all the files that
 *       may be shared through the client.  It keeps them
 *       in the list _files.  There are methods for adding
 *       one file, or a whole directory.
 *
 * Updated by Sumeet Thadani 8/17/2000. Changed the search method so that
 * searches are possible with Regular Expressions. Imported necessary package
 */

package com.limegroup.gnutella;

import java.io.*;
import com.sun.java.util.collections.*;
import com.oroinc.text.regex.*;
// import java.util.*;
import java.util.Enumeration;

public class FileManager{
    /** the total size of all files, in bytes. 
     *  INVARIANT: _size=sum of all size of the elements of _files */
    private int _size;                  
    /** the total number of files.  INVARIANT: _numFiles==number of 
     *  elements of _files that are not null. */
    private int _numFiles;      
    /** the list of shareable files.  An entry is null if it is no longer
     *  shared.  INVARIANT: for all i, f[i]==null, or f[i].index==i and
     *  f[i]._path is in the shared folder with the shareable extension. */
    private ArrayList /* of FileDesc */ _files;             
    private String[] _extensions;

    // Regular Expressions Stuff.
    private PatternMatcher matcher = new Perl5Matcher();
    private PatternCompiler compiler = new Perl5Compiler();
    private Pattern pattern;
    private PatternMatcherInput input;
    // For our enhancements of RegEx.
    private static final String EscapeChars = "./(){}\"\\";

    private static FileManager _myFileManager;

    java.util.Hashtable _sharedHash;

    public FileManager() {               /* the constructor initializes */
        _size = 0;                       /* all the provate variables */
        _numFiles = 0;
        _files = new ArrayList();
        _extensions = new String[0];

    }

    public static synchronized FileManager getFileManager() {
        if(_myFileManager == null)
            _myFileManager = new FileManager();
        return _myFileManager;

    }

    /** Returns the size of all files, in <b>bytes</b>. */
    public int getSize() {return _size;}
    public int getNumFiles() {return _numFiles;}

    public void reset() {
        _size = 0;
        _numFiles = 0;
        _files = new ArrayList();
        // _extensions = new String[0];
    }

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
    public synchronized Response[] query(QueryRequest request) {
        String str = request.getQuery();
        ArrayList list = search(str);
        if (list==null)
            return null;

        int size = list.size();
        Response[] response = new Response[size];
        FileDesc desc;
        for(int j=0; j < size; j++) {
            desc = (FileDesc)list.get(j);
            response[j] =
            new Response(desc._index, desc._size, desc._name);
        }
        return response;
    }

    public void setExtensions(String str) {
        /* recieves a semi-colon separated list of extensions */
        _extensions =  HTTPUtil.stringSplit(str, ';');
        // int length = _extensions.length;
    }


    public boolean hasExtension(String filename) {
        int begin = filename.lastIndexOf(".") + 1;

        if (begin == -1)
            return false;

        int end = filename.length();
        String ext = filename.substring(begin, end);

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
     */
    private synchronized void addFile(String path) {
        File myFile = new File(path);

        if (!myFile.exists())
            return;

        String name = myFile.getName();     /* the name of the file */
        if (hasExtension(name)) {
            int n = (int)myFile.length();       /* the list, and increments */
            _size += n;                         /* the appropriate info */
            _files.add(new FileDesc(_files.size(), name, path,  n));
            _numFiles++;
        }
    }

    /**
     * @modifies this
     * @effects adds the given file to this, if it exists
     *  and if it is shared.
     *  <b>WARNING: this is a potential security hazard.</b>
     */
    public synchronized void addFileIfShared(String path) {

        if (_sharedHash == null)
            return;

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
        if (!_sharedHash.containsKey(p))
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
                return true;  //No more files in list will match this.
            }                
        }
        return false;
    }

    /**
     * @modifies this
     * @effects recursively adds the following directories
     *  to this.  <b>WARNING: this is a potential security hazard.</b>
     */
    public synchronized void addDirectories(String dir_names) {
		
		dir_names.trim();
        String[] names = HTTPUtil.stringSplit(dir_names, ';');

        // need to see if there are duplicated directories...

        _sharedHash = new java.util.Hashtable();

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
            if (!_sharedHash.containsKey(p)) {
                _sharedHash.put(p, p);
            }
        }

        int hashsize = _sharedHash.size();

        String[] dirs = new String[hashsize];

        int j=0;

        for(Enumeration e = _sharedHash.keys(); e.hasMoreElements() ;) {
            dirs[j++] = (String)e.nextElement();
        }

        for (int i=0; i < hashsize; i++) {
            addDirectory(dirs[i]);
        }

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

    public synchronized void addDirectory(String dir_name) { /* the addDirectory method */
        File myFile = new File(dir_name);       /* recursively adds all of */
        if (!myFile.exists())
            return;
        //File[] file_list = myFile.listFiles();  /* This is JDK1.2 specific */
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
    }

    ////////////////////////////////// Queries ///////////////////////////////


    /** Returns true if s is lowercase and without regular expressions. */
    private static boolean isSimpleQuery(String s) {
        for (int i=0; i<s.length(); i++) {
            char c=s.charAt(i);
            if (Character.isUpperCase(c))
                return false;
            switch (c) {
                case '*':
                case '+':
                case ' ':
                    return false;
            }
        }
        return true;
    }

    /**
     * Translates the given wildcard search into a Perl5 regular
     * expression.  For example, "*.mp3" gets translated into the
     * regexp ".*\.mp3".
     */
    static String wildcard2regexp(String q) {
        StringBuffer sb = new StringBuffer();
        for(int i=0; i<q.length();i++){
            if (EscapeChars.indexOf(q.charAt(i)) < 0){
                //the particular char is an normal except for *
                if(q.charAt(i)=='*'||q.charAt(i)=='+'||q.charAt(i)==' '){ //special cases
                    sb = sb.append(".");
                    sb = sb.append("*"); //+ and * replaced with .*
                }
                else //normal character
                    sb = sb.append(q.charAt(i));
            }
            else{//escape character
                sb = sb.append("\\");
                sb = sb.append(q.charAt(i));
            }
        }//for
        return sb.toString();
    }


    /** 
     * Returns a list of FileDesc matching q, or null if there are no matches.
     * Subclasses may override to provide different notions of matching.  
     */
    protected ArrayList search(String q) {
        //Assumption: most queries will pass this test, and
        //the cost of the test is negligible.  Alternatively,
        //we could integrate the test with searchFast, but that's
        //more complicated and requires writing our own string
        //matching algorithm--potentially tricky to do right.
        if (isSimpleQuery(q))
            return searchFast(q);

        //Can't use simple query.  Fancy query follows.
        q=q.toLowerCase();
        String query = wildcard2regexp(q);
        try {
            pattern = compiler.compile(query);
        } catch(MalformedPatternException e) {
            return searchFast(q);
        }

        ArrayList response_list = null;  //Don't allocate until needed.
        for(int i=0; i < _files.size(); i++) {
            FileDesc desc = (FileDesc)_files.get(i);
            if (desc==null)
                continue;
            String file_name = desc._nameLowerCase;            
            input = new PatternMatcherInput(file_name);
            if (matcher.contains(input,pattern)) {
                if (response_list==null)
                    response_list=new ArrayList();
                response_list.add(_files.get(i));
            }
        }
        return response_list;
    }

    /**
     * @requires q is a lower-case string without regular expressions.
     * @effects exactly likely like search(query), but optimized for the
     *  common case.  (See precondition!)  This is called from search(query).
     */
    protected ArrayList searchFast(String query) {
        //Don't allocate until needed
        ArrayList response_list=null;

        for(int i=0; i < _files.size(); i++) {
            FileDesc desc = (FileDesc)_files.get(i);
            if (desc==null)
                continue;
            String file_name = desc._nameLowerCase;
            if (file_name.indexOf(query) != -1) {
                if (response_list==null)
                    response_list=new ArrayList();
                response_list.add(_files.get(i));
            }
        }
        return response_list;               
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
            FileManager fman=FileManager.getFileManager();
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





