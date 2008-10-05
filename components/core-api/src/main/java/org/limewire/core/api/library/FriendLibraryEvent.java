package org.limewire.core.api.library;

import org.limewire.util.StringUtils;

public class FriendLibraryEvent {
    
    public enum Type {
        LIBRARY_ADDED, LIBRARY_REMOVED
    }
    
    private final Type type;
    private final FriendLibrary friendLibrary;
    
    public FriendLibraryEvent(Type type, FriendLibrary friendLibrary) {
        this.type = type;
        this.friendLibrary = friendLibrary;
    }

    public Type getType() {
        return type;
    }

    public FriendLibrary getFriendLibrary() {
        return friendLibrary;
    }
    
    @Override
    public String toString() {
        return StringUtils.toString(this);
    }
    
    
    
    

}
