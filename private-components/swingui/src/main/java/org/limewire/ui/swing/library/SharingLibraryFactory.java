package org.limewire.ui.swing.library;

import javax.swing.JComponent;

import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;

import ca.odell.glazedlists.EventList;

public interface SharingLibraryFactory {

    JComponent createSharingLibrary(BaseLibraryPanel basePanel, Friend friend, EventList<LocalFileItem> eventList, LocalFileList localFileList);
}
