package com.limegroup.gnutella.privategroups;

import org.limewire.listener.Event;

/**
 * An event class that tells the gui to start a new chat window
 */
public class ChatWindowEvent implements Event{

    private String remoteUser;
    private String localUser;
    
    public ChatWindowEvent(String remoteUser, String localUser){
        this.remoteUser = remoteUser;
        this.localUser = localUser;
    }
    
    public String getRemoteUser(){
        return remoteUser;
    }
    
    public String getLocalUser(){
        return localUser;
    }
    
    public Object getSource() {
        return null;
    }

    public Enum getType() {
        return null;
    }
}
