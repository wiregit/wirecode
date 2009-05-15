package org.limewire.xmpp.api.client;

import org.limewire.listener.DefaultDataTypeEvent;
import org.limewire.core.api.friend.FriendPresence;

/**
 * This event is dispatched when a chat presence is added or updated
 */
public class PresenceEvent extends DefaultDataTypeEvent<FriendPresence, PresenceEvent.Type> {

    public static enum Type {
        /**
         * Indicates that this is the first time we're seeing this presence.
         */
        PRESENCE_NEW,

        /**
         * Indicates that this is an update to the presence. For the exact kind
         * of update, see the {@link org.limewire.xmpp.api.client.Presence.Type}.
         */
        PRESENCE_UPDATE
    }

    public PresenceEvent(FriendPresence data, Type event) {
        super(data, event);
    }
}
