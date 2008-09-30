package org.limewire.ui.swing.sharing.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.limewire.core.api.friend.Friend;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.NavItem;
import org.limewire.ui.swing.nav.Navigator;

public class GoToLibraryAction extends AbstractAction {
    
    private final Friend friend;
    private final Navigator navigator;
    
    public GoToLibraryAction(Navigator navigator, Friend friend) {
        this.navigator = navigator;
        this.friend = friend;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        NavItem navItem = navigator.getNavItem(NavCategory.LIBRARY, friend.getId());
        navItem.select();
    }
}
