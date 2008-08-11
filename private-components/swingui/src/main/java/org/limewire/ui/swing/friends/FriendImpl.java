package org.limewire.ui.swing.friends;

import org.jdesktop.beans.AbstractBean;
import org.limewire.xmpp.api.client.Presence.Mode;

/**
 * @author Mario Aquino, Object Computing, Inc.
 *
 */
public class FriendImpl extends AbstractBean implements Friend {
    private final String name;
    private String status;
    private Mode mode;
    
    public FriendImpl(String name, String status, Mode mode) {
        this.name = name;
        this.status = status;
        this.mode = mode;
    }

    @Override
    public Mode getMode() {
        return mode;
    }
    
//    @Override
    public void setMode(Mode mode) {
        Mode oldMode = getMode();
        this.mode = mode;
        firePropertyChange("mode", oldMode, mode);
    }

    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public String getStatus() {
        return status;
    }
    
//    @Override
    public void setStatus(String status) {
        String oldStatus = getStatus();
        this.status = status;
        firePropertyChange("status", oldStatus, status);
    }
}
