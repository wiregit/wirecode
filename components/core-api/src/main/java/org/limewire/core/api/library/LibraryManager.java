package org.limewire.core.api.library;

import java.util.Map;

public interface LibraryManager {
    
    void addLibraryLisListener(LibraryListListener libraryListener);
    
    void removeLibraryListener(LibraryListListener libraryListener);
    
    FileList getGnutellaFileList();
    
    FileList getBuddiesFileList();
    
    Map<String, FileList> getUniqueLists();
}
