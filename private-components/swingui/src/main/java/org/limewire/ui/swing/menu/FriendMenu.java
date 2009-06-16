package org.limewire.ui.swing.menu;

import org.limewire.ui.swing.action.MnemonicMenu;
import org.limewire.ui.swing.action.StatusActions;
import org.limewire.ui.swing.menu.actions.ChatAction;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.NavItem;
import org.limewire.ui.swing.nav.NavSelectable;
import org.limewire.ui.swing.nav.NavigationListener;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

public class FriendMenu extends MnemonicMenu {
    
    private final ChatAction chatAction;
    
  
    @Inject
    public FriendMenu(ChatAction chatAction, StatusActions statusActions) {
        super(I18n.tr("F&riend"));
        this.chatAction = chatAction;
        add(chatAction);
        addSeparator();
        add(statusActions.getAvailableMenuItem());
        add(statusActions.getDnDMenuItem());
        addSeparator();
    }

    @Inject void register(Navigator navigator) {
        // listen for changes in the selected friend
        navigator.addNavigationListener(new NavigationListener() {
            @Override
            public void itemSelected(NavCategory category, NavItem navItem,
                    NavSelectable selectable, org.limewire.ui.swing.nav.NavMediator navMediator) {
//                chatAction.setFriend(libraryNavigator.getSelectedFriend());
            }
            
            @Override public void itemAdded(NavCategory category, NavItem navItem) {}
            @Override public void itemRemoved(NavCategory category, NavItem navItem) {}
            @Override public void categoryAdded(NavCategory category) {}
            @Override public void categoryRemoved(NavCategory category) {}
        });
    }
}