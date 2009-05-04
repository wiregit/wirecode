package org.limewire.xmpp.api.client;

import org.limewire.listener.DefaultDataTypeEvent;

public class RosterEvent extends DefaultDataTypeEvent<XMPPFriend, RosterEvent.Type> {

    public static enum Type {
        USER_ADDED,
        USER_UPDATED,
        USER_DELETED
    }

    public RosterEvent(XMPPFriend data, Type event) {
        super(data, event);
    }
}