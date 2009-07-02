package org.limewire.ui.swing.friends.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.limewire.friend.api.FriendConnection;
import org.limewire.friend.api.FriendConnectionEvent;
import org.limewire.listener.EventBean;
import org.limewire.listener.EventUtils;

import com.google.inject.Inject;

public class BrowseOrLoginAction extends AbstractAction {
    private final LoginAction loginAction;

    private final BrowseFriendsAction browseFriendAction;

    private final EventBean<FriendConnectionEvent> friendConnectionEventBean;

    @Inject
    private BrowseOrLoginAction(LoginAction loginAction,
            BrowseFriendsAction browseFriendAction,
            EventBean<FriendConnectionEvent> friendConnectionEventBean) {
        this.loginAction = loginAction;
        this.browseFriendAction = browseFriendAction;
        this.friendConnectionEventBean = friendConnectionEventBean;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        FriendConnection friendConnection = EventUtils.getSource(friendConnectionEventBean);
        if(friendConnection != null && friendConnection.isLoggedIn()) {
            browseFriendAction.actionPerformed(e);
        } else {
            loginAction.actionPerformed(e);
        }
    }
}