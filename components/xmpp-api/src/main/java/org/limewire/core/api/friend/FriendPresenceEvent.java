package org.limewire.core.api.friend;

import org.limewire.listener.DefaultEvent;

public class FriendPresenceEvent extends DefaultEvent<FriendPresence, FriendPresenceEvent.Type> {
    
    public static enum Type {
        ADDED, REMOVED;
    }

    public FriendPresenceEvent(FriendPresence source, Type event) {
        super(source, event);
    }
    
}
