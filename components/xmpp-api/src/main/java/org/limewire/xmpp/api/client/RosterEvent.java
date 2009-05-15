package org.limewire.xmpp.api.client;

import org.limewire.listener.DefaultDataTypeEvent;
import org.limewire.core.api.friend.Friend;

public class RosterEvent extends DefaultDataTypeEvent<Friend, RosterEvent.Type> {

    public static enum Type {
        USER_ADDED,
        USER_UPDATED,
        USER_DELETED
    }

    public RosterEvent(Friend data, Type event) {
        super(data, event);
    }
}