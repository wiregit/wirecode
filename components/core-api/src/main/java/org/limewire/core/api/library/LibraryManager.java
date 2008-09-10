package org.limewire.core.api.library;

import java.util.Map;


public interface LibraryManager {
    
    void addLibraryLisListener(LibraryListListener libraryListener);
    
    void removeLibraryListener(LibraryListListener libraryListener);
    
    LocalFileList getLibraryList();
    
    LocalFileList getGnutellaList();

    Map<String, LocalFileList> getAllBuddyLists();
    
    LocalFileList getBuddy(String name);
    
    void addBuddy(String name);
    
    void removeBuddy(String name);
    
    boolean containsBuddy(String name);
    
    Map<String, RemoteFileList> getAllBuddyLibraries();
    
    RemoteFileList getBuddyLibrary(String name);
    
    void addBuddyLibrary(String name);
    
    void removeBuddyLibrary(String name);
    
    boolean containsBuddyLibrary(String name);
    
    void addBuddyShareListListener(BuddyShareListListener listener);
    
    void removeBuddyShareListListener(BuddyShareListListener listener);
}
