package org.limewire.ui.swing.friends.actions;

import java.awt.event.ActionEvent;

import org.limewire.friend.api.FriendConnection;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.friends.settings.FriendAccountConfigurationManager;
import org.limewire.ui.swing.util.I18n;

class LogoutAction extends AbstractAction {

    private final FriendConnection friendConnection;
    private final FriendAccountConfigurationManager accountManager;
    
    public LogoutAction(FriendConnection friendConnection,
            FriendAccountConfigurationManager accountManager) {
        super(I18n.tr("Sign out"));
        this.friendConnection = friendConnection;
        this.accountManager = accountManager;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        accountManager.setAutoLoginConfig(null);
        friendConnection.logout();
    }
}
