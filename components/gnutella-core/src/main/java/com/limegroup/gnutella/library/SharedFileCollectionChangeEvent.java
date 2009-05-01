package com.limegroup.gnutella.library;

import org.limewire.listener.SourcedEvent;

public class SharedFileCollectionChangeEvent implements SourcedEvent<SharedFileCollection> {
    
    public static enum Type {
        COLLECTION_ADDED, COLLECTION_REMOVED, SHARE_ID_ADDED, SHARE_ID_REMOVED;
    }
    
    private final Type type;
    private final SharedFileCollection list;
    private final String shareId;
    
    public SharedFileCollectionChangeEvent(Type type, SharedFileCollection list, String shareId) {
        this.type = type;
        this.list = list;
        this.shareId = shareId;
    }

    public Type getType() {
        return type;
    }

    public String getShareId() {
        return shareId;
    }

    @Override
    public SharedFileCollection getSource() {
        return list;
    }

}
