package org.limewire.ui.swing.friends;

import org.limewire.ui.swing.AbstractEDTEvent;
import org.limewire.xmpp.api.client.Presence.Mode;

public class SelfAvailabilityUpdateEvent extends AbstractEDTEvent {
    private final Mode mode;
    
    public SelfAvailabilityUpdateEvent(Mode mode) {
        this.mode = mode;
    }
    
    public Mode getNewMode() {
        return mode;
    }
}
