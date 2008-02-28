package com.limegroup.gnutella.dht;

import com.limegroup.gnutella.util.EventDispatcher;

public class DHTEventDispatcherStub implements EventDispatcher<DHTEvent, DHTEventListener>{
    
    public void addEventListener(DHTEventListener listener) {
    }

    public void dispatchEvent(DHTEvent event) {
        System.out.println("torrentmanaged");
    }

    public void removeEventListener(DHTEventListener listener) {
    }
}
