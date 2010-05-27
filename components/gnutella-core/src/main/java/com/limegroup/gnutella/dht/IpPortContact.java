package com.limegroup.gnutella.dht;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.limewire.io.IpPort;
import org.limewire.mojito.DHT;
import org.limewire.mojito.routing.Contact;

/**
 * A helper class to easily go back and forth from the {@link DHT}'s 
 * {@link Contact} to Gnutella's {@link IpPort}.
 */
public class IpPortContact implements IpPort {
    
    private final InetSocketAddress addr;
    
    public IpPortContact(Contact contact) {
        
        SocketAddress address = contact.getContactAddress();
        
        if(!(address instanceof InetSocketAddress)) {
            throw new IllegalArgumentException(
                    "Contact not instance of InetSocketAddress");
        }
        
        addr = (InetSocketAddress) address;
    }
    
    @Override 
    public String getAddress() {
        return getInetAddress().getHostAddress();
    }

    @Override 
    public InetAddress getInetAddress() {
        return addr.getAddress();
    }

    @Override 
    public int getPort() {
        return addr.getPort();
    }

    @Override 
    public InetSocketAddress getInetSocketAddress() {
        return addr;
    }
}