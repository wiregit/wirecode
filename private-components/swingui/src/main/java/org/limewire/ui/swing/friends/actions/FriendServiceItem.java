package org.limewire.ui.swing.friends.actions;

import javax.swing.JLabel;

import org.limewire.friend.api.FriendConnection;
import org.limewire.friend.api.FriendConnectionEvent;
import org.limewire.listener.EventBean;
import org.limewire.listener.EventUtils;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;

public class FriendServiceItem extends JLabel {

    @Inject
    public FriendServiceItem(EventBean<FriendConnectionEvent> friendConnectionEventBean) {
        
        FriendConnection friendConnection = EventUtils.getSource(friendConnectionEventBean);
        if (friendConnection != null && friendConnection.isLoggedIn()) {
            GuiUtils.assignResources(this);
            setIconTextGap(4);
            
            String friendName = friendConnection.getConfiguration().getUserInputLocalID();
            friendName = friendName == null ? "" : FontUtils.getTruncatedMessage(friendName, getFont(), 150);
            
            setText(friendName);
            setIcon(friendConnection.getConfiguration().getIcon());
        } else {
            setVisible(false);
        }
    }
}