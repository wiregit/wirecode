package com.limegroup.gnutella.util;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class IpPortImpl implements IpPort {
    
    private final InetAddress addr;
    private final String addrString;
    private final int port;
    
    /**
     * Constructs a new IpPort using the given addr, host & port.
     */
    public IpPortImpl(InetAddress addr, String host, int port) {
        this.addr = addr;
        this.addrString = host;
        this.port = port;
    }
    
    /**
     * Constructs a new IpPort using the given host & port.
     */
    public IpPortImpl(String host, int port) throws UnknownHostException {
        this(InetAddress.getByName(host), host, port);
    }
    
    public InetAddress getInetAddress() {
        return addr;
    }
    
    public String getAddress() {
        return addrString;
    }
    
    public int getPort() {
        return port;
    }
}