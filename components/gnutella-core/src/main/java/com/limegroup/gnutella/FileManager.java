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
import java.util.*;
import com.oroinc.text.regex.*;

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
	int length = _extensions.length;
    }

    public boolean hasExtension(String filename) {
	int length = _extensions.length;
	for (int i = 0; i < length; i++) {
	    if (filename.indexOf(_extensions[i]) != -1)
		return true;
	}
	
	return false;

    }
    
    public synchronized void addFile(String path) { /* the addFile method adds */ 
	File myFile = new File(path);  /* just one single file to */
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
	
	String[] dirs = HTTPUtil.stringSplit(dir_names, ';');

	int length = dirs.length;

	for (int i=0; i < length; i++) {
	    addDirectory(dirs[i]);
	}
	
    }

    public synchronized void addDirectory(String dir_name) { /* the addDirectory method */
	File myFile = new File(dir_name);       /* recursively adds all of */
	File[] file_list = myFile.listFiles();  /* the files in a specified */
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
    /** subclasses must override this method */
    protected ArrayList search(String query) {
	ArrayList response_list = new ArrayList();
	try{
	    try{
		pattern = compiler.compile(query);}
	    catch(MalformedPatternException e){
		//If we catch and expression we seacrh without regular expressions
		for(int i=0; i < _numFiles; i++) {
		    FileDesc desc = (FileDesc)_files.get(i);
		    //System.out.println("Rob:"+query);
		    String file_name = desc._name;
		    if (file_name.indexOf(query) != -1) 
			response_list.add(_files.get(i));
		}
		return response_list;
	    }
	    for(int i=0; i < _numFiles; i++){
		FileDesc desc = (FileDesc)_files.get(i);//Adam will populate the list before calling query.
		//System.out.println("Sumeet: "+query);
		String file_name = desc._name;
		input = new PatternMatcherInput(file_name);
		if (matcher.contains(input,pattern))
		    response_list.add(_files.get(i));
	    }
	}
	catch (Exception e){
	    e.printStackTrace();
	}
	return response_list;
    }    
}





