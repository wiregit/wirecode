package org.limewire.ui.swing.friends.actions;

import java.awt.event.ActionEvent;

import javax.swing.Action;

import org.limewire.friend.api.FriendConnection;
import org.limewire.friend.api.FriendConnectionEvent;
import org.limewire.listener.EventBean;
import org.limewire.listener.EventUtils;
import org.limewire.ui.swing.action.AbstractAction;

import com.google.inject.Inject;

public class SignInOutAction extends AbstractAction {

    private final EventBean<FriendConnectionEvent> friendConnectionEventBean;

    private final LoginAction loginAction;

    private final LogoutAction logoutAction;

    @Inject
    public SignInOutAction(EventBean<FriendConnectionEvent> friendConnectionEventBean,
            LoginAction loginAction, LogoutAction logoutAction) {
        this.friendConnectionEventBean = friendConnectionEventBean;
        this.loginAction = loginAction;
        this.logoutAction = logoutAction;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        FriendConnection friendConnection = EventUtils.getSource(friendConnectionEventBean);
        if (friendConnection != null && friendConnection.isLoggedIn()) {
            logoutAction.actionPerformed(e);
        } else {
            loginAction.actionPerformed(e);
        }
    }

    public void updateSignedInStatus(boolean signedIn) {
        if (signedIn) {
            putValue(Action.NAME, logoutAction.getValue(Action.NAME));
        } else {
            putValue(Action.NAME, loginAction.getValue(Action.NAME));
        }
    }
}
