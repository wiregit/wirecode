package com.limegroup.gnutella.library;

import java.io.File;
import java.util.List;

import org.limewire.listener.EventListener;

import com.limegroup.gnutella.xml.LimeXMLDocument;

public interface ManagedFileList extends FileList {
    
    void addManagedListStatusListener(EventListener<ManagedListStatusEvent> listener);
    void removeManagedListStatusListener(EventListener<ManagedListStatusEvent> listener);
    
    boolean isLoadFinished();
    
    void fileRenamed(File oldName, File newName);
    
    void fileChanged(File file, List<? extends LimeXMLDocument> xmlDocs);
    
    void validate(FileDesc fd);
    
    void addDirectoryToExcludeFromManaging(File folder);
    
    void addDirectoryToManageRecursively(File folder);
    
    List<File> getDirectoriesToManageRecursively();
}
