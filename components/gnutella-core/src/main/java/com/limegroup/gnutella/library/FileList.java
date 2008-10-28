package com.limegroup.gnutella.library;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;

import org.limewire.listener.EventListener;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.xml.LimeXMLDocument;


/**
 * A collection of FileDescs.
 */
public interface FileList extends Iterable<FileDesc> {

    /**
     * Returns the <tt>FileDesc</tt> that is wrapping this <tt>File</tt> or
     * null if the file is not shared or not a store file.
     */
    FileDesc getFileDesc(File f);

    /**
     * Returns the <tt>FileDesc</tt> for the specified URN. This only returns
     * one <tt>FileDesc</tt>, even though multiple indices are possible with
     * HUGE v. 0.93.
     * 
     * @param urn the urn for the file
     * @return the <tt>FileDesc</tt> corresponding to the requested urn, or
     *         <tt>null</tt> if no matching <tt>FileDesc</tt> could be found
     */
    FileDesc getFileDesc(URN urn);
    
    /**
     * Returns all FileDescs that match this URN.
     */
    List<FileDesc> getFileDescsMatching(URN urn);

    /**
     * Returns the FileDesc at the given index.
     * This returns the FileDesc for which FileDesc.getIndex == index.
     * This is supported as an optimization so that classes can efficiently
     * locate matches.
     */
    FileDesc getFileDescForIndex(int index);
    
    /**
     * Adds a folder to the FileList.
     * Depending on the kind of FileList, this may either add all
     * current contents of the folder, or the folder itself (allowing
     * for future items in the folder to be added).
     */
    void addFolder(File folder);
    
    /**
     * Adds a file to the list.
     */
    Future<FileDesc> add(File file);
    
    /**
     * Adds the specific file, using the given LimeXMLDocuments as the default
     * documents for that file.
     */
    Future<FileDesc> add(File file, List<? extends LimeXMLDocument> documents);
    
    /**
     * Removes the File from this list if there exists a FileDesc wrapper for
     * that file. If the value existed returns true, false if it did not exist
     * and nothing was removed. This method simply removes this FileDesc from
     * this list. If FileDesc is null, throws an IllegalArguementException.
     */
    boolean remove(File file);
    
    /**
     * Removes the FileDesc from the list if it exists. If the value existed 
     * returns true, false if it did not exist and nothing was removed. This 
     * method simply removes this FileDesc from this list. If FileDesc is 
     * null, throws an IllegalArguementException. 
    */
    boolean remove(FileDesc fileDesc);
    
    /** Returns true if this list contains a FileDesc for the given file. */
    boolean contains(File file);
    
    /**
     * Return true if this list contains this FileDesc, false otherwise.
     */
    boolean contains(FileDesc fileDesc);
    
    /**
     * Returns an iterator over all FileDescs.
     * The returned iterator is *NOT* thread safe.
     * You must lock on FileList while using it.
     */
    Iterator<FileDesc> iterator();
    
    /**
     * Returns an iterable that is thread-safe and can
     * be used over a period of time (iterating through
     * it piecemeal, with time lapses).  The returned
     * iterable is much slower and more inefficient than the
     * default iterator, though, so only use it if absolutely
     * necessary.
     */
    Iterable<FileDesc> pausableIterator();
    
    /**
     * Returns the number of files in this list. 
     */
    int size();
    
    /**
     * Resets all values within this list.
     */
    void clear();
    
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
    List<FileDesc> getFilesInDirectory(File directory);
       
    /**
     * Adds a listener to this list
     */
    void addFileListListener(EventListener<FileListChangedEvent> listener);
    
    /**
     * Removes a listener from this list
     */
    void removeFileListListener(EventListener<FileListChangedEvent> listener);
    
    /** Returns a lock to use when iterating over this FileList. */
    Lock getReadLock();
}
