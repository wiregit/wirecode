package org.limewire.ui.swing.friends.actions;

import java.awt.event.ActionEvent;

import org.limewire.friend.api.FriendConnection;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

class LogoutAction extends AbstractAction {

    private final FriendConnection friendConnection;
    
    @Inject
    public LogoutAction(FriendConnection friendConnection) {
        super(I18n.tr("Sign out"));
        this.friendConnection = friendConnection;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        friendConnection.logout();
    }
}
