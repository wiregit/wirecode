package org.limewire.ui.swing.sharing;

import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.NavItem;
import org.limewire.ui.swing.nav.Navigator;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class BuddySharingDisplayImpl implements BuddySharingDisplay {
    private final Navigator navigator;
    private final BuddySharePanel buddySharePanel;
    
    @Inject
    public BuddySharingDisplayImpl(Navigator navigator, BuddySharePanel buddySharePanel) {
        this.navigator = navigator;
        this.buddySharePanel = buddySharePanel;
    }
    
    @Override
    public void displaySharing() {
        displayNavigableItem(NavCategory.SHARING, BuddySharePanel.NAME);
    }

    private void displayNavigableItem(NavCategory navCategory, String name) {
        NavItem item = navigator.getNavItem(navCategory, name);
        item.select();
    }
    
    @Override
    public void selectBuddyInFileSharingList(String buddyId) {
        displaySharing();
        buddySharePanel.selectBuddy(buddyId);
    }

    @Override
    public void selectBuddyLibrary(String buddyName) {
        displayNavigableItem(NavCategory.LIBRARY, buddyName);
    }
}
