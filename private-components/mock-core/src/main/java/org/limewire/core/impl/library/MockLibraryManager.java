package org.limewire.core.impl.library;

import java.util.Collections;
import java.util.Map;

import org.limewire.core.api.library.FileList;
import org.limewire.core.api.library.LibraryListListener;
import org.limewire.core.api.library.LibraryManager;


public class MockLibraryManager implements LibraryManager {

    @Override
    public void addLibraryLisListener(LibraryListListener libraryListener) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public FileList getBuddiesFileList() {
        return new MockFileList(3);
    }

    @Override
    public FileList getGnutellaFileList() {
        return new MockFileList(3);
    }

    @Override
    public Map<String, FileList> getUniqueLists() {
        return Collections.emptyMap();
    }

    @Override
    public void removeLibraryListener(LibraryListListener libraryListener) {
        // TODO Auto-generated method stub
        
    }
    
    
    

}
