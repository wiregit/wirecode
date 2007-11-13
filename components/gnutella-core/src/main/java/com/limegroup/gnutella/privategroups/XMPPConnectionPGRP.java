package com.limegroup.gnutella.privategroups;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;

public class XMPPConnectionPGRP extends XMPPConnection{

    public XMPPConnectionPGRP(ConnectionConfiguration config) {
        super(config);
    }
    
    public XMPPConnectionPGRP(String serviceName) {
        super(serviceName);
    }
    
    private void initConnection(){
        
        System.out.println("I'm here");
    }

}
