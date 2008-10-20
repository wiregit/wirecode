package org.limewire.ui.swing.sharing.actions;

import java.awt.event.ActionEvent;

import javax.swing.Action;

import org.limewire.core.api.Category;
import org.limewire.core.api.friend.Friend;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.NavItem;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.util.I18n;

public class GoToLibraryAction extends AbstractAction {
    
    private Friend friend;
    private final Navigator navigator;
    
    public GoToLibraryAction(Navigator navigator, Friend friend) {
        this.navigator = navigator;
        this.friend = friend;
        
        putValue(Action.NAME, I18n.tr("Library"));
    }
    
    public void setFriend(Friend friend) {
        this.friend = friend;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if(friend == null)
            return;
        //TODO: fix friend category
        NavItem navItem = navigator.getNavItem(NavCategory.LIBRARY, friend.getId() + "/" + Category.AUDIO);
        if(navItem != null)
            navItem.select();
    }
}
