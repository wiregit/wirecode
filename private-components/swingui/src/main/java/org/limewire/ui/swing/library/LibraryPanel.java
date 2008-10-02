package org.limewire.ui.swing.library;

import javax.swing.JComponent;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.FileItem;

import ca.odell.glazedlists.EventList;

public interface LibraryPanel<T extends FileItem> {
    
    void setCategory(Category category, EventList<T> localFileList);
    
    JComponent getComponent();
}
