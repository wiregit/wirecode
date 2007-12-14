package com.limegroup.gnutella.privategroups;

import org.limewire.listener.Event;

/**
 * An event class that notifies BuddyListManager listeners to stop listening
 */
public class ListenerRemovalEvent implements Event{

    private String remoteUsername;
    
    public ListenerRemovalEvent(String remoteUsername){
        this.remoteUsername = remoteUsername;
    }
    
    public String getRemoteUsername(){
        return remoteUsername;
    }
    
    public Object getSource() {
        return null;
    }

    public Enum getType() {
        return null;
    }

}
