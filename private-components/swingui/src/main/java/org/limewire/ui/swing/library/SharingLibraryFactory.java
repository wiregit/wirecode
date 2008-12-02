package org.limewire.ui.swing.library;

import javax.swing.JComponent;

import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.FriendFileList;
import org.limewire.core.api.library.LocalFileItem;

import ca.odell.glazedlists.EventList;

/**
 * A factory for creating {@link SharingLibraryPanel}s.
 */
public interface SharingLibraryFactory {

    /**
     * Creates a new {@link SharingLibraryPanel}.
     * @param basePanel The library panel to go back to. 
     * @param friend The friend this is for.
     * @param eventList The list of all items in the library.
     * @param friendFileList The FileList for the particular friend.
     */
    JComponent createSharingLibrary(BaseLibraryMediator basePanel, 
            Friend friend,
            EventList<LocalFileItem> eventList,
            FriendFileList friendFileList);
}
