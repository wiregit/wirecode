package org.limewire.ui.swing.friends;

import org.limewire.xmpp.api.client.Presence.Mode;
/**
 * @author Mario Aquino, Object Computing, Inc.
 *
 */
public interface Friend {
    String getName();
    
    String getStatus();
    
    Mode getMode();
}
