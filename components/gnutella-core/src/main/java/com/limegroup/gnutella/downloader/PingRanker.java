package com.limegroup.gnutella.downloader;

import com.limegroup.gnutella.MessageListener;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.UDPPinger;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.util.IpPort;

public class PingRanker extends SourceRanker implements MessageListener {

    public PingRanker() {
        this(new UDPPinger());
    }
    
    PingRanker(UDPPinger pinger){}
    
    public void addToPool(IpPort host){}
    
    public IpPort getBest(){return null;}
    
    public boolean hasMore() {return false;}
    
    public void processMessage(Message m, ReplyHandler handler) {
        // TODO Auto-generated method stub
        
    }

    public void registered(byte[] guid) {
        // TODO Auto-generated method stub
        
    }

    public void unregistered(byte[] guid) {
        // TODO Auto-generated method stub
        
    }

}
