package org.limewire.ui.swing.library.table;

import java.io.File;
import java.util.List;

import org.limewire.core.api.library.LocalFileList;
import org.limewire.ui.swing.library.LibrarySelected;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class LibraryShowInListMenu extends ShowInListMenu {
    
    @Inject
    public LibraryShowInListMenu(@LibrarySelected Provider<List<File>> selectedFiles,
            final @LibrarySelected Provider<LocalFileList> selectedLocalFileList) {
        super(selectedFiles, selectedLocalFileList);
    }
}
