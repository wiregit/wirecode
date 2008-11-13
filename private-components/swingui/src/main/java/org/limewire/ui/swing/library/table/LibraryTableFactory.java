package org.limewire.ui.swing.library.table;

import javax.swing.JScrollPane;

import org.limewire.core.api.Category;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.ui.swing.library.image.LibraryImagePanel;

import ca.odell.glazedlists.EventList;

public interface LibraryTableFactory {
    
    <T extends FileItem>LibraryTable<T> createTable(Category category, EventList<T> eventList, Friend friend);
    
    LibraryImagePanel createImagePanel(EventList<LocalFileItem> eventList, JScrollPane scrollPane);
    
    <T extends FileItem>LibraryTable<T> createSharingTable(Category category, EventList<T> eventList, LocalFileList friendFileList);
}
