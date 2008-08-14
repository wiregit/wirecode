package org.limewire.ui.swing.friends;

import org.jdesktop.beans.AbstractBean;
import org.limewire.xmpp.api.client.MessageReader;
import org.limewire.xmpp.api.client.MessageWriter;
import org.limewire.xmpp.api.client.Presence;
import org.limewire.xmpp.api.client.User;
import org.limewire.xmpp.api.client.Presence.Mode;

/**
 * @author Mario Aquino, Object Computing, Inc.
 *
 */
public class FriendImpl extends AbstractBean implements Friend {
    private final User user;
    private final Presence presence;
    private String status;
    private Mode mode;
    
    FriendImpl(User user, Presence presence) {
        this.user = user;
        this.presence = presence;
        this.status = presence.getStatus();
        this.mode = presence.getMode();
    }
    
    @Override
    public String getId() {
        return user.getId();
    }

    @Override
    public Mode getMode() {
        return mode;
    }
    
    public void setMode(Mode mode) {
        Mode oldMode = getMode();
        this.mode = mode;
        firePropertyChange("mode", oldMode, mode);
    }

    @Override
    public String getName() {
        return user.getName();
    }
    
    @Override
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        String oldStatus = getStatus();
        this.status = status;
        firePropertyChange("status", oldStatus, status);
    }

    @Override
    public MessageWriter createChat(MessageReader reader) {
        return presence.createChat(reader);
    }
}
