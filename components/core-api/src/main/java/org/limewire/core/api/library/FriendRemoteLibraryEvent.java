package org.limewire.core.api.library;

import org.limewire.core.api.friend.Friend;
import org.limewire.util.StringUtils;

public class FriendRemoteLibraryEvent {
    
    public enum Type {
        FRIEND_LIBRARY_ADDED, FRIEND_LIBRARY_REMOVED;
    }
    
    private final Type type;
    private final RemoteFileList fileList;
    private final Friend friend;
    
    public FriendRemoteLibraryEvent(Type type, RemoteFileList fileList, Friend friend) {
        this.type = type;
        this.fileList = fileList;
        this.friend = friend;
    }

    public Type getType() {
        return type;
    }

    public RemoteFileList getFileList() {
        return fileList;
    }

    public Friend getFriend() {
        return friend;
    }
    
    @Override
    public String toString() {
        return StringUtils.toString(this);
    }
    
    
    
    

}
