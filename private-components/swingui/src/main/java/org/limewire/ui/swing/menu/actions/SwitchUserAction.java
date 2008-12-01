package org.limewire.ui.swing.menu.actions;

import java.awt.event.ActionEvent;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.friends.login.FriendActions;
import org.limewire.ui.swing.friends.settings.XMPPAccountConfigurationManager;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

public class SwitchUserAction extends AbstractAction {

    private final FriendActions friendActions;
    private final XMPPAccountConfigurationManager accountManager;

    @Inject
    SwitchUserAction(FriendActions friendActions, XMPPAccountConfigurationManager accountManager) {
        super(I18n.tr("Switch user"));
        this.friendActions = friendActions;
        this.accountManager = accountManager;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        friendActions.signOut(true);
        accountManager.setAutoLoginConfig(null);
        friendActions.signIn();
    }
}