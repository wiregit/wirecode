package com.limegroup.gnutella.library;

import java.io.File;
import java.util.List;

import org.limewire.listener.EventListener;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.xml.LimeXMLDocument;


/**
 * A collection of FileDescs.
 */
public interface FileList {

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
    void add(File file);
    
    /**
     * Adds the specific file, using the given LimeXMLDocuments as the default
     * documents for that file.
     */
    void add(File file, List<LimeXMLDocument> documents);    

    /**
     * Adds this FileList just for this session.
     */
    void addForSession(File file);
    
    /**
     * Given a non-null FileDesc, adds this FileDesc to this list. If FileDesc
     * is null, throws an IllegalArguementException. 
     * @param fileDesc - FileDesc to be added to this list
     */
    boolean add(FileDesc fileDesc);
    
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
     * Returns an iterable that returns iterator().
     * The returned Iterable is *NOT* thread safe.
     * You must lock on FileList while using it.
     */
    Iterable<FileDesc> iterable();
    
    /**
     * Returns an iterable that is thread-safe, albeit
     * slower and more inefficient than the non-thread-safe variety.
     * Only use this if you must iterate in passes.
     */
    Iterable<FileDesc> threadSafeIterable();

     /**
     * Returns the size of all files within this list, in <b>bytes</b>.  
     * <p>
     * NOTE: the largest value that can be returned is Integer.MAX_VALUE, 
     * i.e., ~2GB. If more bytes are being shared, returns this value.
     */
    int getNumBytes();
    
    /**
     * Returns the number of files in this list. 
     */
    int size();
    
    /**
     * Resets all values within this list.
     */
    void clear();
    
    /**
     * Returns true if this file can be added to this list, false otherwise.
     */
    boolean isFileAddable(File file);
    
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
    
    int getNumForcedFiles();
       
    /**
     * Adds a listener to this list
     */
    void addFileListListener(EventListener<FileListChangedEvent> listener);
    
    /**
     * Removes a listener from this list
     */
    void removeFileListListener(EventListener<FileListChangedEvent> listener);
    
    /**
     * Returns an object which to lock on when iterating over this FileList. The
     * Lock should be used only during iteration, all other calls are thread safe
     * when used with SynchronizedFileList.
     */
    Object getLock();
    
    /**
     * Removes any listeners this list might be holding prior to its destruction.
     */
    void cleanupListeners();
    
    /**
     * Changes the smart sharing value for images. If true, all new images added to
     * the library will be shared with this list, if false, new images added to 
     * the library will not be automatically shared with this list but current images
     * will not be removed.
     */
    void setAddNewImageAlways(boolean value);
    
    /**
     * Returns true if image files are being smartly shraed with this friend, false otherwise.
     */
    boolean isAddNewImageAlways();
    
    /**
     * Changes the smart sharing value for audio files. If true, all new audio files added to
     * the library will be shared with this list, if false, new audio files added to 
     * the library will not be automatically shared with this list but current audio files
     * will not be removed.
     */
    void setAddNewAudioAlways(boolean value);
    
    /**
     * Returns true if audio files are being smartly shared with this friend, false otherwise.
     */
    boolean isAddNewAudioAlways();
    
    /**
     * Changes the smart sharing value for videos. If true, all new videos added to
     * the library will be shared with this list, if false, new videos added to 
     * the library will not be automatically shared with this list but current videos
     * will not be removed.
     */
    void setAddNewVideoAlways(boolean value);
    
    /**
     * Returns true if videos are being smartly shared with this friend, false otherwise.
     */
    boolean isAddNewVideoAlways();
        
    ///// BELOW for backwards compatibility with LW 4.x. Notion of an individual file ////
    /////   does not exist in 5.x  ////
    
    /**
     * Returns a copy of all the files in this list that are not located in 
     * a complete directory for this FileList type. This is a subset of files
     * returned by getAllFileDescs.
     */
    File[] getIndividualFiles();
    
    /**
     * Returns the number of individual files in this FileList.
     */
    int getNumIndividualFiles();
    
    /**
     * Returns true if this file exists in this FileList and is an individual
     * file in this FileList, false otherwise.
     */
    boolean isIndividualFile(File file);
}
