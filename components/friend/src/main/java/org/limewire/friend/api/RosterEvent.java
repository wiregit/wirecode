package org.limewire.friend.api;

import org.limewire.listener.DefaultDataTypeEvent;

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