package org.limewire.ui.swing.friends;

import org.limewire.ui.swing.AbstractEDTEvent;

public class XMPPConnectionEstablishedEvent extends AbstractEDTEvent {
    private final String id;

    XMPPConnectionEstablishedEvent(String id) {
        this.id = id;
    }

    public String getID() {
        return id;
    }
}
