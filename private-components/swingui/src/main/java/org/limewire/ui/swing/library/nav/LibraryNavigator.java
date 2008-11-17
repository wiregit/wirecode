package org.limewire.ui.swing.library.nav;

import java.awt.Component;

import org.limewire.core.api.Category;
import org.limewire.core.api.URN;
import org.limewire.core.api.friend.Friend;

/** Controls navigation of you & your friends libraries. */
public interface LibraryNavigator {
    
    /** Returns the renderable component. */
    Component getComponent();
   
    /** Selects a friend's sharelist. */
    void selectFriendShareList(Friend friend);
    
    /** Selects a friend's library. */
    void selectFriendLibrary(Friend friend);
    
    /** Selects a specific item in your library. */
    void selectInLibrary(URN urn, Category category);

}
