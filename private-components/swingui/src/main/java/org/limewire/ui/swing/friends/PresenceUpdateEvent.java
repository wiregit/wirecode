package org.limewire.ui.swing.friends;

import org.limewire.ui.swing.event.AbstractEDTEvent;
import org.limewire.xmpp.api.client.Presence;
import org.limewire.xmpp.api.client.User;

public class PresenceUpdateEvent extends AbstractEDTEvent {
    private static final String TOPIC_PREFIX = "presence-";
    private final Presence presence;
    private final Presence.EventType presenceEventType;

    PresenceUpdateEvent(Presence presence, Presence.EventType presenceEventType) {
        this.presence = presence;
        this.presenceEventType = presenceEventType;
    }

    public User getUser() {
        return presence.getUser();
    }

    public Presence getPresence() {
        return presence;
    }
    
    public static String buildTopic(String friendId) {
        return TOPIC_PREFIX + friendId;
    }

    public boolean isNewPresence() {
        return presenceEventType == Presence.EventType.PRESENCE_NEW;
    }

    @Override
    public void publish() {
        super.publish(buildTopic(getUser().getId()));
    }
}
