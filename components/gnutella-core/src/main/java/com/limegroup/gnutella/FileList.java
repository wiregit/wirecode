package com.limegroup.gnutella;

import java.io.File;
import java.util.Iterator;
import java.util.List;

/**
 * A collection of FileDescs.
 */
public interface FileList {
    
    /**
     * Returns the name of this fileList
     */
    public String getName();
    
    /**
     * Given a non-null FileDesc, adds this FileDesc to this list. If FileDesc
     * is null, throws an IllegalArguementException. 
     * @param fileDesc - FileDesc to be added to this list
     */
    public boolean add(FileDesc fileDesc);
    
    /**
     * Removes the FileDesc from the list if it exists. If the value existed 
     * returns true, false if it did not exist and nothing was removed. This 
     * method simply removes this FileDesc from this list. If FileDesc is 
     * null, throws an IllegalArguementException. 
    */
    public boolean remove(FileDesc fileDesc);
    
    /**
     * Return true if this list contains this FileDesc, false otherwise.
     */
    public boolean contains(FileDesc fileDesc);
    
    /**
     * Returns an iterator over this list of FileDescs.
     * <p>
     * NOTE: This must be synchronized upon by the caller if accessed
     * in a multi-threaded way.
     */    
    public Iterator<FileDesc> iterator();
    
    /**
     * Returns a copy of a List containing all the FileDescs associated with 
     * this FileList.
     */
    public List<FileDesc> getAllFileDescs();

     /**
     * Returns the size of all files within this list, in <b>bytes</b>.  
     * <p>
     * NOTE: the largest value that can be returned is Integer.MAX_VALUE, 
     * i.e., ~2GB. If more bytes are being shared, returns this value.
     */
    public int getNumBytes();
    
    /**
     * Returns the number of files in this list. 
     */
    public int size();
    
    /**
     * Resets all values within this list.
     */
    public void clear();
    
    /**
     * Returns true if this file can be added to this list, false otherwise.
     */
    public boolean isFileAddable(File file);
    
    /**
     * Returns a list of all the file descriptors in this list that exist 
     * in the given directory, in any order.
     * 
     * Returns null if directory is not shared, or a zero-length array if it is
     * shared but contains no files.  This method is not recursive; files in 
     * any of the directory's children are not returned.
     * 
     * This operation is <b>not</b> efficient, and should not be done often.
     */
    public List<FileDesc> getFilesInDirectory(File directory);
    
    public int getNumForcedFiles();
       
    /**
     * Adds a listener to this list
     */
    public void addFileListListener(FileListListener listener);
    
    /**
     * Removes a listener from this list
     */
    public void removeFileListListener(FileListListener listener);
    
    /**
     * Returns an object which to lock on when iterating over this FileList. The
     * Lock should be used only during iteration, all other calls are thread safe
     * when used with SynchronizedFileList.
     */
    public Object getLock();
    
    /**
     * Removes any listeners this list might be holding prior to its destruction.
     */
    public void cleanupListeners();
        
    ///// BELOW for backwards compatibility with LW 4.x. Notion of an individual file ////
    /////   does not exist in 5.x  ////
    
    /**
     * Returns a copy of all the files in this list that are not located in 
     * a complete directory for this FileList type. This is a subset of files
     * returned by getAllFileDescs.
     */
    public File[] getIndividualFiles();
    
    /**
     * Returns true if this list contains at least one individual files, 
     * false otherwise.
     */
    public boolean hasIndividualFiles();
    
    /**
     * Returns the number of individual files in this FileList.
     */
    public int getNumIndividualFiles();
    
    /**
     * Returns true if this file exists in this FileList and is an individual
     * file in this FileList, false otherwise.
     */
    public boolean isIndividualFile(File file);
}
