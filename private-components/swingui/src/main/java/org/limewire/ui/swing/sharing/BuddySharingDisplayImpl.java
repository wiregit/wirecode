package org.limewire.ui.swing.sharing;

import org.limewire.ui.swing.nav.NavItem;
import org.limewire.ui.swing.nav.NavigableTree;
import org.limewire.ui.swing.nav.Navigator;

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
        NavItem item = navTree.getNavigableItemByName(Navigator.NavCategory.SHARING, BuddySharePanel.NAME);
        item.select();
    }
    
    @Override
    public void selectBuddy(String buddyId) {
        displaySharing();
        buddySharePanel.selectBuddy(buddyId);
    }
}
