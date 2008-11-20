package org.limewire.ui.swing.library;

import javax.swing.JComponent;

import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.FriendFileList;
import org.limewire.core.api.library.LocalFileItem;

import ca.odell.glazedlists.EventList;

public interface SharingLibraryFactory {

    JComponent createSharingLibrary(BaseLibraryMediator basePanel, Friend friend, EventList<LocalFileItem> eventList, FriendFileList friendFileList);
}
