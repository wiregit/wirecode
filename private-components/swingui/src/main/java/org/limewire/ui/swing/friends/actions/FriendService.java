package org.limewire.ui.swing.friends.actions;

import javax.swing.Icon;
import javax.swing.JMenuItem;

import org.jdesktop.application.Resource;
import org.limewire.friend.api.FriendConnection;
import org.limewire.friend.api.FriendConnectionEvent;
import org.limewire.listener.EventBean;
import org.limewire.listener.EventUtils;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;

public class FriendService extends JMenuItem {
    private final EventBean<FriendConnectionEvent> friendConnectionEventBean;

    @Resource
    private Icon gmailIcon;

    @Resource
    private Icon liveJournalIcon;

    @Resource
    private Icon facebookIcon;

    @Resource
    private Icon jabberIcon;

    @Inject
    public FriendService(EventBean<FriendConnectionEvent> friendConnectionEventBean) {
        this.friendConnectionEventBean = friendConnectionEventBean;
        GuiUtils.assignResources(this);
        setIconTextGap(4);
    }

    public void updateSignedInStatus() {
        FriendConnection friendConnection = EventUtils.getSource(friendConnectionEventBean);
        if (friendConnection != null && friendConnection.isLoggedIn()) {
            String friendName = friendConnection.getConfiguration().getUserInputLocalID();
            friendName = friendName == null ? "" : FontUtils.getTruncatedMessage(friendName, getFont(), 150);
            
            setText(friendName);
            String serviceName = friendConnection.getConfiguration().getServiceName();
            if(serviceName.contains("gmail")) {
                setIcon(gmailIcon);
            } else if(serviceName.contains("facebook")) {
                setIcon(facebookIcon);
            } else if(serviceName.contains("livejournal")) {
                setIcon(liveJournalIcon);
            } else {
                setIcon(jabberIcon);
            }
        } else {
            setText("");
            setIcon(null);
        }
    }

}