/*
 * auth: rsoule
 * file: Library.java
 * desc: This class has two functions.  The first, it is a
 *       wrapper around a map that can keep track of files.
 *       So, if we want to keep track of all the files we've 
 *       downloaded, we could use this.
 *
 *       Also, there is the static execute method, which will
 *       run a program if the Os is of the Windows variety.
 * 
 */

package com.limegroup.gnutella;

import java.lang.*;
import java.io.*;
import java.util.*;

public class Library {

    private HashMap _files;
    private Library _lib;

    public Library() {
	_files = new HashMap();
    }

    /* for singleton */
    public Library instance() {
	if (_lib == null)
	    return new Library();
	else return _lib;
    }

    /* the number of files in the map */
    public int size() {return _files.size();}

    /* add a file to the map given its path */
    public void add(String path) {
	add(new File(path));
    }
    
    /* add a file to the map */
    public void add(File f) {
	if (!f.exists()) 
	    return;
	_files.put(f.getAbsolutePath(), f);
    }
    
    /*  get a file from the map given its path */ 
    public File getFile(String path) {
	return (File)_files.get(path);
    }
    
    /* get all of the files in the map in an array */
    public File[] getAllFiles() {
	Collection c = _files.values();
	return (File[])c.toArray();
    }
    
    /* execute the file of a given path if */
    /* the OS is windows */
    public static boolean execute(String path) {

	File f = new File(path);
	if (!f.exists())
	    return false;
	Properties props = System.getProperties();
	String os = props.getProperty("os.name");
	if (os.indexOf("Windows") != -1) {
	    try {
		Runtime runtime = Runtime.getRuntime();
		runtime.exec("cmd /c " + path);
	    }catch (Exception e) {
		return false;
	    }
	    return true;
	}
	
	return false;
    }

}
