package org.limewire.ui.swing.library;

import javax.swing.JComponent;

import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.LocalFileItem;

import ca.odell.glazedlists.EventList;

public interface MyLibraryFactory {
    
    JComponent createMyLibrary(Friend friend, EventList<LocalFileItem> eventList);
}
