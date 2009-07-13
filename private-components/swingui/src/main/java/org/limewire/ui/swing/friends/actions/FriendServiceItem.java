package org.limewire.ui.swing.friends.actions;

import javax.swing.BorderFactory;
import javax.swing.JLabel;

import org.limewire.friend.api.FriendConnection;
import org.limewire.friend.api.FriendConnectionEvent;
import org.limewire.listener.EventBean;
import org.limewire.listener.EventUtils;

import com.google.inject.Inject;

public class FriendServiceItem extends JLabel {

    @Inject
    public FriendServiceItem(EventBean<FriendConnectionEvent> friendConnectionEventBean) {
        
        FriendConnection friendConnection = EventUtils.getSource(friendConnectionEventBean);
        if (friendConnection != null && friendConnection.isLoggedIn()) {
            setBorder(BorderFactory.createEmptyBorder(0,10,0,0));
            setIconTextGap(4);
            
            setText(friendConnection.getConfiguration().getUserInputLocalID());
            setIcon(friendConnection.getConfiguration().getIcon());
        } else {
            setVisible(false);
        }
    }
}