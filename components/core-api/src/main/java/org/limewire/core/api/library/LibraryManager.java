package org.limewire.core.api.library;

import java.util.Map;


public interface LibraryManager {
    
    void addLibraryLisListener(LibraryListListener libraryListener);
    
    void removeLibraryListener(LibraryListListener libraryListener);
    
    FileList getLibraryList();
    
    FileList getGnutellaList();

    Map<String, FileList> getAllBuddyLists();
    
    FileList getBuddy(String name);
    
    void addBuddy(String name);
    
    void removeBuddy(String name);
    
    boolean containsBuddy(String name);
    
    Map<String, FileList> getAllBuddyLibraries();
    
    FileList getBuddyLibrary(String name);
    
    void addBuddyLibrary(String name);
    
    void removeBuddyLibrary(String name);
    
    boolean containsBuddyLibrary(String name);
    
    void addBuddyShareListListener(BuddyShareListListener listener);
    
    void removeBuddyShareListListener(BuddyShareListListener listener);
}
