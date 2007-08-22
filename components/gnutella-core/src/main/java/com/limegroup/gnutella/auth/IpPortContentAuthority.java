package com.limegroup.gnutella.auth;


import java.net.UnknownHostException;

import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;

import com.limegroup.gnutella.ProviderHacks;
import com.limegroup.gnutella.messages.Message;

/** A ContentAuthority that sends to a single IpPort. */
public class IpPortContentAuthority implements ContentAuthority {
    
    private IpPort authority;
    
    /** host/port to store which'll be used when initializing, if necessary */
    private String host;
    private int port;

    /** Constructs the authority with the given IpPort. */
    public IpPortContentAuthority(IpPort host) {
        this.authority = host;
        this.host = host.getAddress();
        this.port = host.getPort();
    }
    
    /**
     * Constructs the authority with the given host/port.
     * You must call initialize prior to sending a message.
     */
    public IpPortContentAuthority(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /** Sends a message to the authority. */
    public void send(Message m) {
        ProviderHacks.getUdpService().send(m, authority);
    }

    /** Constructs the authority from the host/port if necessary */
    public boolean initialize() {
        if (authority == null) {
            try {
                authority = new IpPortImpl(host, port);
            } catch (UnknownHostException uhe) {
                return false;
                // ignored.
            }
        }
        
        return true;
    }
    
    public IpPort getIpPort() {
        return authority;
    }

}
