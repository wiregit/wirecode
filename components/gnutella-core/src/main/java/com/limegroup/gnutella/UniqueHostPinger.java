package com.limegroup.gnutella;

import java.util.Set;
import java.util.TreeSet;

import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.util.IpPort;

public class UniqueHostPinger extends UDPPinger {

    /**
     * set of endpoints we pinged since last expiration
     */
    private final Set _recent = new TreeSet(IpPort.COMPARATOR);
    
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
        QUEUE.add(new Runnable(){
            public void run() {
                _recent.clear();
            }
        });
    }

}
