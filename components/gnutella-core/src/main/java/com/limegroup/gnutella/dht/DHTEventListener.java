package com.limegroup.gnutella.dht;

import java.util.EventListener;

public interface DHTEventListener extends EventListener{
    
    public void handleDHTEvent(DHTEvent evt);

}
