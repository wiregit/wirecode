package org.limewire.ui.swing.sharing;

import org.limewire.core.api.friend.Friend;
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
    public void displaySharing() { throw new IllegalStateException("Implement me again");
//        displayNavigableItem(NavCategory.SHARING, FriendSharePanel.NAME);
    }

    private void displayNavigableItem(NavCategory navCategory, String id) {
        NavItem item = navigator.getNavItem(navCategory, id);
        item.select();
    }
    
    @Override
    public void selectFriendInFileSharingList(Friend friend) {
        displaySharing();
        friendSharePanel.selectFriend(friend);
    }

    @Override
    public void selectFriendLibrary(Friend friend) {
        displayNavigableItem(NavCategory.LIBRARY, friend.getId());
    }
}
