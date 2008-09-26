package org.limewire.ui.swing.sharing;

import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.NavItem;
import org.limewire.ui.swing.nav.Navigator;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class FriendSharingDisplayImpl implements FriendSharingDisplay {
    private final Navigator navigator;
    private final FriendSharePanel friendSharePanel;
    
    @Inject
    public FriendSharingDisplayImpl(Navigator navigator, FriendSharePanel friendSharePanel) {
        this.navigator = navigator;
        this.friendSharePanel = friendSharePanel;
    }
    
    @Override
    public void displaySharing() {
        displayNavigableItem(NavCategory.SHARING, FriendSharePanel.NAME);
    }

    private void displayNavigableItem(NavCategory navCategory, String name) {
        NavItem item = navigator.getNavItem(navCategory, name);
        item.select();
    }
    
    @Override
    public void selectFriendInFileSharingList(String friendId) {
        displaySharing();
        friendSharePanel.selectFriend(friendId);
    }

    @Override
    public void selectFriendLibrary(String friendName) {
        displayNavigableItem(NavCategory.LIBRARY, friendName);
    }
}
