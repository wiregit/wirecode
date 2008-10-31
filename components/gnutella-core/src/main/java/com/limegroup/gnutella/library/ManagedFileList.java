package com.limegroup.gnutella.library;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;

import org.limewire.listener.EventListener;

import com.limegroup.gnutella.xml.LimeXMLDocument;

/**
 * The list of all files that this library is managing.
 * This list can include files that are shared, are not shared,
 * are files from the store, are shared with friends, are incomplete, etc...
 * 
 * Inclusion in this list means only that LimeWire knows about this file.
 */
public interface ManagedFileList extends FileList {
    
    void addManagedListStatusListener(EventListener<ManagedListStatusEvent> listener);
    void removeManagedListStatusListener(EventListener<ManagedListStatusEvent> listener);
  
    /** Returns true if the initial load of the library has finished. */
    boolean isLoadFinished();
    
    /** Informs the library that the file 'oldName' has been renamed to 'newName'. */
    Future<FileDesc> fileRenamed(File oldName, File newName);
    
    /** Informs the library that the file 'file' has changed. */
    Future<FileDesc> fileChanged(File file, List<? extends LimeXMLDocument> xmlDocs);
    
    /** Adds a directory that will not be managed. */
    void addDirectoryToExcludeFromManaging(File folder);
    
    /** Adds a directory that will be managed recursively. */
    void addDirectoryToManageRecursively(File folder);
    
    /** Returns a list of all directories that will be recursively managed. */
    List<File> getDirectoriesToManageRecursively();
    
    /** Gets the set of default managed extensions.  This is not the current set. */
    Collection<String> getDefaultManagedExtensions();
    
    /** Gets the current set of managed extensions. */
    Collection<String> getManagedExtensions();
    
    /** Sets a new collection of managed extensions. */
    void setManagedExtensions(Collection<String> extensions);
}
