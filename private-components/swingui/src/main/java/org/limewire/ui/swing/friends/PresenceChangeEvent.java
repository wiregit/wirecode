package org.limewire.ui.swing.friends;

import org.limewire.ui.swing.AbstractEDTEvent;
import org.limewire.xmpp.api.client.Presence.Mode;

public class PresenceChangeEvent extends AbstractEDTEvent {
    private final Mode mode;
    
    public PresenceChangeEvent(Mode mode) {
        this.mode = mode;
    }
    
    public Mode getNewMode() {
        return mode;
    }
}
