/*
 * auth: rsoule
 * file: FileScan.java
 * desc: At either run time or the first time
 *       the client is run, this class will scan
 *       the entire hard drive searching for files
 *       with particular file extensions (like .mp3).
 *       It will keep track of the directories in which
 *       those files exist, and will return the top 
 *       5 directories.  The top five are chosen
 *       by some combination of number of files and 
 *       memory.
 *
 */

package com.limegroup.gnutella;

import java.io.*;
// import com.sun.java.util.collections.*;

public class FileScan {

    private int MEM_WEIGHT = 1;
    private int NUM_WEIGHT = 1;
    private int MAX_DEPTH = 3;

    private String[] _extensions; 
    private String[] _filters;
    private LimitedList _list;
    
    public FileScan() {
        _list = new LimitedList();
        _filters = new String[0];
		_extensions = new String [] {"html","htm","xml","txt","pdf",
									 "ps","rtf","doc","tex","mp3",
									 "wav","au","aif","aiff","ra","ram",
									 "mpg","mpeg","asf","qt","mov","avi",
									 "mpe","gif","jpg","jpeg","jpe","png",
									 "tif","tiff","exe","zip","gz","gzip",
									 "hqx","tar","tgz","z"};
    }

    public void setExtensions(String[] e) {
        _extensions = e;
    }
    
    public void setFilters(String[] f) {
        _filters = f;
    }

    public boolean hasExtension(String filename) {
        int begin = filename.lastIndexOf(".");
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

    public boolean hasFilter(String pathname) {

        int length = _filters.length;
    
        for (int i = 0; i < length; i++) {
            if (pathname.indexOf(_filters[i]) != -1)
                return true;
        }
    
        return false;

    }

    public void print() {
        _list.print();
    
    }

    public void scan(String pathname) {
        scan(pathname, MAX_DEPTH);    
    }

    public void scan(String pathname, int depth) {
    
        if (depth == 0) 
            return;

        depth--;

        File file = new File(pathname);
        if (!file.isDirectory())
            return;
    
        File[] files = listFiles(file);
        int num_files = files.length;
    
        for( int i=0; i < num_files; i++ ) {
            File f = files[i];
            if ( f.isDirectory() ) {
                addDirectory(f.getAbsolutePath());
                scan(f.getAbsolutePath(), depth);
            }
        
        }   
    
    } 
    
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

    public void addDirectory(String pathname) {
        File dir = new File(pathname);
        if (!dir.isDirectory())
            return;
        File[] files = listFiles(dir);
        int num_files = files.length;
    
        int mem = 0;
        int num = 0;

        for (int i = 0; i < num_files; i++) {
            File f = files[i];
            String name = f.getName();
            if ( ( hasExtension(name) )
                 && (!hasFilter(pathname) ) ) {
                mem+=f.length();
                num++;
            }   


        }

        int key = calculateKey(num, mem);

        _list.add(new Pair(key, dir), key);
    
    }
    
    public int calculateKey(int num_files, int size_files) {
        int key = (num_files * NUM_WEIGHT) + (size_files * MEM_WEIGHT);
        return key;
    }

}
