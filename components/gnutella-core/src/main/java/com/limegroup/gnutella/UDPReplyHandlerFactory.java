package com.limegroup.gnutella;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import com.google.inject.Singleton;
import com.limegroup.gnutella.filters.SpamFilter;

@Singleton
public interface UDPReplyHandlerFactory {

    public UDPReplyHandler createUDPReplyHandler(InetSocketAddress addr, SpamFilter personalFilter);

    public UDPReplyHandler createUDPReplyHandler(InetAddress addr, int port, SpamFilter personalFilter);
    
}