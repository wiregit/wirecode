package com.limegroup.gnutella.handshaking;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.NetworkManager;

@Singleton
public class HandshakeResponderFactoryImpl implements HandshakeResponderFactory {

    private final HeadersFactory headersFactory;

    private final NetworkManager networkManager;

    private final Provider<ConnectionManager> connectionManager;

    @Inject
    public HandshakeResponderFactoryImpl(HeadersFactory headersFactory,
            NetworkManager networkManager, Provider<ConnectionManager> connectionManager) {
        this.headersFactory = headersFactory;
        this.networkManager = networkManager;
        this.connectionManager = connectionManager;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.handshaking.HandshakeResponderFactory#createUltrapeerHandshakeResponder(java.lang.String)
     */
    public UltrapeerHandshakeResponder createUltrapeerHandshakeResponder(
            String host) {
        return new UltrapeerHandshakeResponder(host, networkManager,
                headersFactory, connectionManager.get());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.handshaking.HandshakeResponderFactory#createLeafHandshakeResponder(java.lang.String)
     */
    public LeafHandshakeResponder createLeafHandshakeResponder(String host) {
        return new LeafHandshakeResponder(host, headersFactory, connectionManager.get());
    }

}
