package com.limegroup.gnutella;


import java.util.Set;

import org.limewire.io.IpPort;
import org.limewire.io.IpPortSet;

import com.limegroup.gnutella.messages.Message;

public class UniqueHostPinger extends UDPPinger {

    /**
     * set of endpoints we pinged since last expiration
     */
    private final Set<IpPort> _recent = new IpPortSet();
    
    public UniqueHostPinger() {
        super();
    }
    

    protected void sendSingleMessage(IpPort host, Message m) {
        if (_recent.contains(host))
            return;
        
        _recent.add(host);
        super.sendSingleMessage(host,m);
    }
    
    /**
     * clears the list of Endpoints we pinged since the last reset,
     * after sending all currently queued messages.
     */
    void resetData() {
        QUEUE.execute(new Runnable(){
            public void run() {
                _recent.clear();
            }
        });
    }

}
