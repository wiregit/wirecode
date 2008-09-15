package org.limewire.ui.swing.sharing;

import org.limewire.ui.swing.nav.NavItem;
import org.limewire.ui.swing.nav.NavigableTree;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.nav.Navigator.NavCategory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class BuddySharingDisplayImpl implements BuddySharingDisplay {
    private final NavigableTree navTree;
    private final BuddySharePanel buddySharePanel;
    
    @Inject
    public BuddySharingDisplayImpl(NavigableTree navTree, BuddySharePanel buddySharePanel) {
        this.navTree = navTree;
        this.buddySharePanel = buddySharePanel;
    }
    
    @Override
    public void displaySharing() {
        displayNavigableItem(Navigator.NavCategory.SHARING, BuddySharePanel.NAME);
    }

    private void displayNavigableItem(NavCategory navCategory, String name) {
        NavItem item = navTree.getNavigableItemByName(navCategory, name);
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
