package com.limegroup.gnutella.library;

import java.util.Collection;

import org.limewire.listener.SourcedEvent;

public class SharedFileCollectionChangeEvent implements SourcedEvent<SharedFileCollection> {
    
    public static enum Type {
        COLLECTION_ADDED, COLLECTION_REMOVED, SHARE_ID_ADDED, SHARE_ID_REMOVED, SHARE_IDS_CHANGED;
    }
    
    private final Type type;
    private final SharedFileCollection list;
    private final String shareId;
    private final Collection<String> newIds;
    
    public SharedFileCollectionChangeEvent(Type type, SharedFileCollection list) {
        assert type == Type.COLLECTION_ADDED || type == Type.COLLECTION_REMOVED;        
        this.type = type;
        this.list = list;
        this.shareId = null;
        this.newIds = null;
    }
    
    public SharedFileCollectionChangeEvent(Type type, SharedFileCollection list, String id) {
        assert type == Type.SHARE_ID_ADDED || type == Type.SHARE_ID_REMOVED;
        this.type = type;
        this.list = list;
        this.shareId = id;
        this.newIds = null;
    }
    
    public SharedFileCollectionChangeEvent(Type type, SharedFileCollection list, Collection<String> newIds) {
        assert type == Type.SHARE_IDS_CHANGED;
        this.type = type;
        this.list = list;
        this.shareId = null;
        this.newIds = newIds;
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
    
    public Collection<String> getNewShareIds() {
        return newIds;
    }

}
