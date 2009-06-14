package org.limewire.ui.swing.library.table.menu.actions;

import java.awt.event.ActionEvent;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.friends.login.FriendActions;

/**
 * Menu Action that redirects to the friend login panel when pressed.
 */
public class DisabledFriendLoginAction extends AbstractAction {
    private final FriendActions friendActions;
    
    public DisabledFriendLoginAction(String menuText, FriendActions friendActions) {
        super(menuText);
        
        this.friendActions = friendActions;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        friendActions.signIn();
    }
}
