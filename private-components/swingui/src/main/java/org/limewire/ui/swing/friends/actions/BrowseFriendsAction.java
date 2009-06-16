package org.limewire.ui.swing.friends.actions;

import java.awt.event.ActionEvent;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.friends.AllFriendsMediator;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.NavItem;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

public class BrowseFriendsAction extends AbstractAction {

    private final Navigator navigator;
    private final AllFriendsMediator allFriendsMediator;

    @Inject
    public BrowseFriendsAction(Navigator navigator, AllFriendsMediator allFriendsMediator) {
        super(I18n.tr("Browse Friends' Files"));
        this.navigator = navigator;
        this.allFriendsMediator = allFriendsMediator;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {        
        NavItem navItem = navigator.getNavItem(NavCategory.ALL_FRIENDS, AllFriendsMediator.NAME);
        if (navItem == null) {
            navItem = navigator.createNavItem(NavCategory.ALL_FRIENDS, AllFriendsMediator.NAME, allFriendsMediator);
        }
        navItem.select();
    }
}
