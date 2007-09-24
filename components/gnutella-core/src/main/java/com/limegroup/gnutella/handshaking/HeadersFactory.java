package com.limegroup.gnutella.handshaking;

public interface HeadersFactory {

    public LeafHeaders createLeafHeaders(String remoteIP);

    public UltrapeerHeaders createUltrapeerHeaders(String remoteIP);

}