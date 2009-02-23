package org.limewire.ui.swing.library.table;

import java.io.File;

import javax.swing.JScrollPane;

import org.limewire.core.api.Category;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.ui.swing.library.SharingMatchingEditor;
import org.limewire.ui.swing.library.image.LibraryImagePanel;
import org.limewire.ui.swing.library.sharing.ShareWidget;

import ca.odell.glazedlists.EventList;

public interface LibraryTableFactory {
    
    /**
     * Creates a table for MyLibrary
     */
    <T extends LocalFileItem> LibraryTable<T> createMyTable(Category category, EventList<T> eventList, SharingMatchingEditor sharingMatchingEditor);
    
    
    /**
     * Creates an image list for My Library
     */
    LibraryImagePanel createMyImagePanel(EventList<LocalFileItem> eventList, JScrollPane scrollPane, ShareWidget<File> sharePanel, SharingMatchingEditor sharingMatchingEditor);
    
    /**
     * Creates a table for Friends
     */
    <T extends RemoteFileItem> LibraryTable<T> createFriendTable(Category category, EventList<T> eventList, Friend friend);
}
