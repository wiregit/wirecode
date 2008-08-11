package org.limewire.core.api.library;

import java.util.Map;

import ca.odell.glazedlists.EventList;

public interface LibraryManager {
    
    void addLibraryLisListener(LibraryListListener libraryListener);
    
    void removeLibraryListener(LibraryListListener libraryListener);
    
    FileList getLibraryList();
    
    FileList getGnutellaList();
    
    FileList getAllBuddyList();

    Map<String, EventList<FileItem>> getUniqueLists();
    
    void addBuddy(String name);
    
    void removeBuddy(String name);
}
