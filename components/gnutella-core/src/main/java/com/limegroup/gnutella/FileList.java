package com.limegroup.gnutella;

import java.io.File;
import java.util.List;

import org.limewire.collection.IntSet;

public interface FileList {
    
    /**
     * For backwards compatibility with the old fileManager. Clears all
     * the saved value and returns them to their default
     */
    public void resetVariables();
    
    /**
     * Returns the number of files in this list. This value may not reflect the length 
     * of the list. This value represents the number of non-null values in the list.
     */
    public int getNumFiles();
    
    /**
    * Returns the size of all files, in <b>bytes</b>.  Note that the largest
    *  value that can be returned is Integer.MAX_VALUE, i.e., ~2GB.  If more
    *  bytes are being shared, returns this value.
    */
    public int getNumBytes();
    
    /**
     * Returns the size of the list which may include null values.
     * NOTE: this may be different than the getNumFiles as some 
     * indices may be null if the file was unshared
     */
    public int getListLength();
    
    /**
     * Returns set of indices of {@link FileDesc file descs} that have URN <code>
     * urn</code> or null if there are none.
     */    
    public IntSet getIndicesForUrn(URN urn);
    
    /**
     * Determines whether or not the specified index is valid.  The index
     * is valid if it is within range of the number of files shared, i.e.,
     * if:<p>
     *
     * i >= 0 && i < _files.size() <p>
     *
     * @param i the index to check
     * @return <tt>true</tt> if the index is within range of our shared
     *  file data structure, otherwise <tt>false</tt>
     */
    public boolean isValidSharedIndex(int i);

     /**
     * Returns the <tt>FileDesc</tt> for the specified URN. This only returns 
     * one <tt>FileDesc</tt>, even though multiple indices are possible with 
     * HUGE v. 0.93.
     *
     * @param urn the urn for the file
     * @return the <tt>FileDesc</tt> corresponding to the requested urn, or
     *  <tt>null</tt> if no matching shared <tt>FileDesc</tt> could be found
     */
    public FileDesc getFileDesc(URN urn);
    
    /**
     * Returns the <tt>FileDesc</tt> associated with this file
     * if it exists, null otherwise
     */
    public FileDesc getFileDesc(File file);
    
    /**
     * Returns the file descriptor with the given index.  Throws
     * IndexOutOfBoundsException if the index is out of range.  It is also
     * possible for the index to be within range, but for this method to
     * return <tt>null</tt>, such as the case where the file has been
     * unshared.
     *
     * @param i the index of the <tt>FileDesc</tt> to access
     * @throws <tt>IndexOutOfBoundsException</tt> if the index is out of 
     *  range
     * @return the <tt>FileDesc</tt> at the specified index, which may
     *  be <tt>null</tt>
     */
    public FileDesc get(int i);
    
    /**
     * Returns a list of all FileDesc associated with this FileList
     * 
     * TODO: convert to an Iterator in the future?? 
     */
    public List<FileDesc> getAllFileDescs();
    
    /**
     * Adds this file and FileDesc to this FileList. 
     */
    public void addFile(File file, FileDesc fileDesc);
       
    /**
     * Removes the FileDesc from the list if it exists
     */
    public void remove(FileDesc fileDesc);
    
    /**
     * Removes the IncompleteFileDesc from the list if it exists
     */
    public void removeIncomplete(IncompleteFileDesc fileDesc);
    
    /**
     * Removes this URN from the list, NOTE: this does NOT 
     * remove files and fileDescs associated with this urn
     */
    public void remove(URN urn);
    
    /**
     * Return true if this list contains this file, false otherwise
     */
    public boolean contains(File file);
    
    /**
     * Return true if this list contains this FileDesc, false otherwise
     */
    public boolean contains(FileDesc fileDesc);
    
    /**
     * Return true if this list contains this URN, false otherwise
     */
    public boolean contains(URN urn);
    
    /**
     * Returns a list of all file descriptors in the given directory,
     * in any order.
     * Returns null if directory is not shared, or a zero-length array if it is
     * shared but contains no files.  This method is not recursive; files in 
     * any of the directory's children are not returned.
     */
    public List<FileDesc> getFilesInDirectory(File directory);
    
    public void updateUrnIndex(FileDesc fileDesc);
    
    public void addIncompleteFile(File incompleteFile, IncompleteFileDesc incompleteFileDesc);
    
    public int getNumForcedFiles();
    
    public int getNumIncompleteFiles();
}
