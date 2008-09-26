package org.limewire.core.api.library;

import org.limewire.util.StringUtils;

public class BuddyLibraryEvent {
    
    public enum Type {
        BUDDY_ADDED, BUDDY_REMOVED;
    }
    
    private final Type type;
    private final RemoteFileList fileList;
    private final Buddy buddy;
    
    public BuddyLibraryEvent(Type type, RemoteFileList fileList, Buddy buddy) {
        this.type = type;
        this.fileList = fileList;
        this.buddy = buddy;
    }

    public Type getType() {
        return type;
    }

    public RemoteFileList getFileList() {
        return fileList;
    }

    public Buddy getBuddy() {
        return buddy;
    }
    
    @Override
    public String toString() {
        return StringUtils.toString(this);
    }
    
    
    
    

}
