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
import java.util.*;

public class FileManager{

    protected int _size;                   /* the total size of all files */ 
    protected int _numFiles;               /* the total number of files */
    public ArrayList _files;             /* the list of shareable files */
    private String[] _extensions;

    // Regular Expressions Stuff.
    private PatternMatcher matcher = new Perl5Matcher();
    private PatternCompiler compiler = new Perl5Compiler();
    private Pattern pattern;
    private PatternMatcherInput input;
    // For our enhancements of RegEx.
    private static final String EscapeChars = "./(){}\"\\";


    private static FileManager _myFileManager;

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

    public synchronized Response[] query(QueryRequest request) {
        String str = request.getQuery();
        ArrayList list = search(str);
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




    
    public synchronized void addFile(String path) { 
        File myFile = new File(path);  

        if (!myFile.exists())               
            return;                        
        /* the addFile method adds */ 
        /* just one single file to */
        String name = myFile.getName();     /* the name of the file */
        int n = (int)myFile.length();       /* the list, and increments */
        _size += n;                         /* the appropriate info */
        if (hasExtension(name)) {
            _files.add(new FileDesc(_numFiles, name, path,  n));
            _numFiles++;
        }
    }

    public void printFirstFive() {
    
        int size = 5;

        if (_files.size() < size)
            size = _files.size();

        System.out.println("printing " + size);

        for(int i =0; i < size; i++) {
        
            ((FileDesc)_files.get(i)).print();

        }
    
    }

    public synchronized void addDirectories(String dir_names) {
    
        String[] names = HTTPUtil.stringSplit(dir_names, ';');

		// need to see if there are duplicated directories...

		java.util.Hashtable hash = new java.util.Hashtable();
		
		int size = names.length;

		for (int i = 0; i < size; i++) {
			if (!hash.containsKey(names[i]))
				hash.put(names[i], names[i]);
		}
		
		int hashsize = hash.size();

		String[] dirs = new String[hashsize];

		int j=0;

		for(Enumeration e = hash.keys(); e.hasMoreElements() ;) {
			dirs[j++] = (String)e.nextElement();
		}
		

		// Collection c = hash.values();

		// String[] dirs = (String[])c.toArray();

        // int length = dirs.length;

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
    
    
    /** subclasses must override this method */
    protected ArrayList search(String q) {
        ArrayList response_list = new ArrayList();

        String query = wildcard2regexp(q).toLowerCase();
        try{
            pattern = compiler.compile(query);}
        catch(MalformedPatternException e){
            // use search w/o regEx in this case
            for(int i=0; i < _numFiles; i++) {
                FileDesc desc = (FileDesc)_files.get(i);
                //System.out.println("Rob:"+query);
                String fileName = desc._name;
                String file_name = fileName.toLowerCase();
                if (file_name.indexOf(query) != -1) 
                    response_list.add(_files.get(i));
            }
            return response_list;
        }
        for(int i=0; i < _numFiles; i++){
            FileDesc desc = (FileDesc)_files.get(i);//Adam will populate the list before calling query.
            //System.out.println("Sumeet: "+query);
            String fileName = desc._name;
            String file_name = fileName.toLowerCase();
            input = new PatternMatcherInput(file_name);
            if (matcher.contains(input,pattern))
                response_list.add(_files.get(i));
        }
        return response_list;
    }    
}





