/**
 * auth: rsoule
 * file: FileManager.java
 * desc: This class will keep track of all the files that
 *       may be shared through the client.  It keeps them 
 *       in the list _files.  There are methods for adding
 *       one file, or a whole directory.
 *
 */

//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
package com.limegroup.gnutella;

import java.io.*;
import java.util.*;

//import com.limegroup.gnutella.*;

public class FileManager {

    private int _size;                   /* the total size of all files */ 
    private int _numFiles;               /* the total number of files */
    public ArrayList _files;             /* the list of shareable files */
    private String[] _extensions;

    private static FileManager _myFileManager;

    public FileManager() {               /* the constructor initializes */ 
	_size = 0;                       /* all the provate variables */
	_numFiles = 0;
	_files = new ArrayList();
	_extensions = null;
	
    }
    
    public static synchronized FileManager getFileManager() {
	if(_myFileManager == null)
	    _myFileManager = new FileManager();;
	return _myFileManager;

    }

    public int getSize() {return _size;}
    public int getNumFiles() {return _numFiles;}

    public void reset() {
	_size = 0;
	_numFiles = 0;
	_files = new ArrayList();
	_extensions = null;
    }

    public Response[] query(QueryRequest request) {
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

    public ArrayList search(String query) {     /* the search method */
	                                        /* looks for a particular */
	ArrayList response_list = new ArrayList(); /* file name and returns */
	                                        /* all of the matches in an */
	for(int i=0; i < _numFiles; i++) {      /* array */
	    FileDesc desc = (FileDesc)_files.get(i);
	    String file_name = desc._name;
	    if (file_name.indexOf(query) != -1) /* we have a match */
		response_list.add(_files.get(i));
	}
	
	return response_list;
    }
    
}





