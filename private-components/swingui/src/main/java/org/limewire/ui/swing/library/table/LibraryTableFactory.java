package org.limewire.ui.swing.library.table;

import org.limewire.core.api.Category;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.FileItem;

import ca.odell.glazedlists.EventList;

public interface LibraryTableFactory {
    
    
    <T extends FileItem>LibraryTable<T> createTable(Category category,
            EventList<T> eventList, Friend friend);
}
