package org.limewire.core.api.library;

import java.io.File;
import java.util.List;

public interface LibraryData {
    
    void addDirectoryToExcludeFromManaging(File folder);
    
    void addDirectoryToManageRecursively(File folder);
    
    List<File> getDirectoriesToManageRecursively();
}
