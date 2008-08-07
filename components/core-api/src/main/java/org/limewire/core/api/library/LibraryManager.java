package org.limewire.core.api.library;

import java.io.File;
import java.util.Map;

import ca.odell.glazedlists.EventList;

public interface LibraryManager {
    
    void addLibraryLisListener(LibraryListListener libraryListener);
    
    void removeLibraryListener(LibraryListListener libraryListener);
    
    EventList<FileItem> getAllFiles();
    
    EventList<FileItem> getGnutellaList();
    
    EventList<FileItem> getAllBuddyList();

    Map<String, EventList<FileItem>> getUniqueLists();
    
    void addBuddy(String name);
    
    void removeBuddy(String name);
    
    void addGnutellaFile(File file);
    
    void removeGnutellaFile(File file);
}
