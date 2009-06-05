package com.limegroup.gnutella.library;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.limewire.concurrent.ListeningFuture;

import com.limegroup.gnutella.xml.LimeXMLDocument;

/**
 * A mutable collection of FileDescs.
 * This is different from {@link FileView} in that it adds additional
 * operations to add or remove FileDescs from the collection.
 */
public interface FileCollection extends FileView {

    /** 
     * Adds the FileDesc to this list. 
     * @return true if the FileDesc could be added, false otherwise
     */
    boolean add(FileDesc fileDesc);

    /**
     * Adds all files from the folder that are manageable
     * according to the current set of managed categories.
     */
    ListeningFuture<List<ListeningFuture<FileDesc>>> addFolder(File folder);
    
    /**
     * Asynchronously adds a file to the list. The returned Future can be used
     * to retrieve the resulting FileDesc. If there is an error adding the file,
     * Future.get will throw an {@link ExecutionException}. If the FileDesc is
     * created but cannot be added to this list, Future.get will return null.
     */
    ListeningFuture<FileDesc> add(File file);

    /**
     * Asynchronously adds the specific file, using the given LimeXMLDocuments
     * as the default documents for that file. The returned Future can be used
     * to retrieve the resulting FileDesc. If there is an error adding the file,
     * Future.get will throw an {@link ExecutionException}. If the FileDesc is
     * created but cannot be added to this list, Future.get will also throw an
     * exception.
     */
    ListeningFuture<FileDesc> add(File file, List<? extends LimeXMLDocument> documents);

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
     * method simply removes this FileDesc from this list. If FileDesc is null,
     * throws an IllegalArguementException.
     */
    boolean remove(FileDesc fileDesc);

    /**
     * Resets all values within this list.
     */
    void clear();
}
