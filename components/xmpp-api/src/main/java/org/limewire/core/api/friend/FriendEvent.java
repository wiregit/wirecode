package org.limewire.core.api.friend;

import org.limewire.listener.DefaultEvent;

public class FriendEvent extends DefaultEvent<Friend, FriendEvent.Type> {
    
    public static enum Type {
        ADDED, REMOVED;
    }

    public FriendEvent(Friend source, Type event) {
        super(source, event);
    }
    
}
