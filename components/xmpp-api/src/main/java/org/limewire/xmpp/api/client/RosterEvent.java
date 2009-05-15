package org.limewire.xmpp.api.client;

import org.limewire.listener.DefaultDataTypeEvent;
import org.limewire.xmpp.client.impl.XMPPFriendImpl;

public class RosterEvent extends DefaultDataTypeEvent<XMPPFriendImpl, RosterEvent.Type> {

    public static enum Type {
        USER_ADDED,
        USER_UPDATED,
        USER_DELETED
    }

    public RosterEvent(XMPPFriendImpl data, Type event) {
        super(data, event);
    }
}