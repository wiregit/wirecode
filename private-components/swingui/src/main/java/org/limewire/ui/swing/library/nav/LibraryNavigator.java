package org.limewire.ui.swing.library.nav;

import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.Category;
import org.limewire.core.api.URN;
import org.limewire.core.api.friend.Friend;

/** Controls navigation of you & your friends libraries. */
public interface LibraryNavigator {
    
    /** Returns the renderable component. */
    JXPanel getComponent();
    
    /** Selects a friend's library. */
    void selectFriendLibrary(Friend friend);
    
    /** Selects a specific item in your library. */
    void selectInLibrary(URN urn, Category category);

}
