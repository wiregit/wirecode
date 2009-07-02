/**
 * 
 */
package org.limewire.ui.swing.friends.actions;

import javax.swing.JMenuItem;

import org.limewire.friend.api.FriendConnection;
import org.limewire.friend.api.FriendConnectionEvent;
import org.limewire.listener.EventBean;
import org.limewire.listener.EventUtils;

import com.google.inject.Inject;

public class FriendService extends JMenuItem {
    private final EventBean<FriendConnectionEvent> friendConnectionEventBean;

    @Inject
    public FriendService(EventBean<FriendConnectionEvent> friendConnectionEventBean) {
        this.friendConnectionEventBean = friendConnectionEventBean;
    }

    public void updateSignedInStatus() {
        FriendConnection friendConnection = EventUtils.getSource(friendConnectionEventBean);
        if(friendConnection != null && friendConnection.isLoggedIn()) {
            setText(friendConnection.getConfiguration().getUserInputLocalID());
        } else {
            setText("");
            setIcon(null);
        }
    }

}