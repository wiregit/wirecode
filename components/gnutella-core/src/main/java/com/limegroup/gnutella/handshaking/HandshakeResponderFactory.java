package com.limegroup.gnutella.handshaking;

public interface HandshakeResponderFactory {

    public UltrapeerHandshakeResponder createUltrapeerHandshakeResponder(
            String host);

    public LeafHandshakeResponder createLeafHandshakeResponder(String host);

}