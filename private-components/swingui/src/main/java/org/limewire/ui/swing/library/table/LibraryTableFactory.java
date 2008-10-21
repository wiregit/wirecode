package org.limewire.ui.swing.library.table;

import org.limewire.core.api.Category;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.library.image.LibraryImagePanel;

import ca.odell.glazedlists.EventList;

public interface LibraryTableFactory {
    
    
    <T extends FileItem>LibraryTable<T> createTable(Category category,
            EventList<T> eventList, Friend friend);
    
    LibraryImagePanel createImagePanel(EventList<LocalFileItem> eventList);
}
