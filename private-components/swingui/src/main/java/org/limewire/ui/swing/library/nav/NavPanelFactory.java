package org.limewire.ui.swing.library.nav;

import javax.swing.Action;

import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.LibraryState;
import org.limewire.ui.swing.library.FriendLibraryMediator;

interface NavPanelFactory {
    
    NavPanel createNavPanel(Action action,
            Friend friend,
            FriendLibraryMediator libraryPanel,
            LibraryState libraryState);

}
