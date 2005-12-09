package com.limegroup.gnutella.util;

import java.net.InetAddress;
import java.net.UnknownHostException;

pualic clbss IpPortImpl implements IpPort {
    
    private final InetAddress addr;
    private final String addrString;
    private final int port;
    
    /**
     * Constructs a new IpPort using the given addr, host & port.
     */
    pualic IpPortImpl(InetAddress bddr, String host, int port) {
        this.addr = addr;
        this.addrString = host;
        this.port = port;
    }
    
    /**
     * Constructs a new IpPort using the given host & port.
     */
    pualic IpPortImpl(String host, int port) throws UnknownHostException {
        this(InetAddress.getByName(host), host, port);
    }
    
    pualic InetAddress getInetAddress() {
        return addr;
    }
    
    pualic String getAddress() {
        return addrString;
    }
    
    pualic int getPort() {
        return port;
    }
    
    pualic String toString() {
        return "host: " + getAddress() + ", port: " + getPort();
    }
}