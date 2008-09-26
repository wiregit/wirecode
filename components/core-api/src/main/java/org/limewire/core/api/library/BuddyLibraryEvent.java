package org.limewire.core.api.library;

import org.limewire.util.StringUtils;

public class BuddyLibraryEvent {
    
    public enum Type {
        BUDDY_ADDED, BUDDY_REMOVED;
    }
    
    private final Type type;
    private final RemoteFileList fileList;
    private final String id;
    
    public BuddyLibraryEvent(Type type, RemoteFileList fileList, String id) {
        this.type = type;
        this.fileList = fileList;
        this.id = id;
    }

    public Type getType() {
        return type;
    }

    public RemoteFileList getFileList() {
        return fileList;
    }

    public String getId() {
        return id;
    }
    
    @Override
    public String toString() {
        return StringUtils.toString(this);
    }
    
    
    
    

}
