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
import com.sun.java.util.collections.*;
import java.util.Properties;

public class Library {

    private HashMap _files;
    private static Library _lib;
 
    private Library() {
	_files = new HashMap();
    }

    /* for singleton */
    public static Library instance() {
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
	if (!f.isFile())
	    return false;
	Properties props = System.getProperties();
	String os = props.getProperty("os.name");
	if (os.indexOf("Windows") != -1) {
	    try {
		Runtime runtime = Runtime.getRuntime();
		String str = "cmd /c "+"\""+ checkChars(path)+"\"";
		
		//String st = "cmd /c \""+ path + "\"";
		//String [] str = {st};
		// str = checkChars(str);
		System.out.println(str);
		Process p1 = runtime.exec(str);
	    }catch (Exception e) {
		return false;
	    }
	    return true;
	}
	
	return false;
    }
    
    private static String checkChars(String str) {
	
	String escapeChars = "&()| ";
	
	char[] chars = str.toCharArray();
	int length = chars.length;

	char[] new_chars = new char[length*3];
	
	int index = 0;
	
	for (int i=0; i < length; i++) {
	    
	  //    if (escapeChars.indexOf(chars[i]) != -1 ) {
//  		// add escape char
//  		new_chars[index++] = '^';
//  		new_chars[index++] = chars[i];
//  	    }
//  	    if(chars[i] == ' ') {
	    if (escapeChars.indexOf(chars[i]) != -1 ) {
		new_chars[index++] = '"';
		new_chars[index++] = chars[i];
		new_chars[index++] = '"';
	    }
	    else {
		new_chars[index++] = chars[i];
	    }
		

	    
	    
	}

	String s = new String(new_chars);

	return s.trim();

    }
    
    
}
