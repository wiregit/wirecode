package org.limewire.ui.swing.xmpp;

import org.limewire.ui.swing.AbstractEDTEvent;
import org.limewire.xmpp.api.client.Presence;
import org.limewire.xmpp.api.client.User;

public class PresenceUpdateEvent extends AbstractEDTEvent {
    private final User user;
    private final Presence presence;

    public PresenceUpdateEvent(User user, Presence presence) {
        this.presence = presence;
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    public Presence getPresence() {
        return presence;
    }
}
