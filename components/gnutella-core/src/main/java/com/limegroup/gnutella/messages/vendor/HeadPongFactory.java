package com.limegroup.gnutella.messages.vendor;

import com.limegroup.gnutella.messages.BadPacketException;

public interface HeadPongFactory {

    public HeadPong createFromNetwork(byte[] guid, byte ttl, byte hops,
            int version, byte[] payload) throws BadPacketException;

    public HeadPong create(HeadPongRequestor ping);

}