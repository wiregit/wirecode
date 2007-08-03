package com.limegroup.gnutella.handshaking;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.NetworkManager;

@Singleton
public class HeadersFactoryImpl implements HeadersFactory {
    
    private final NetworkManager networkManager;
    
    @Inject
    public HeadersFactoryImpl(NetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.handshaking.HeadersFactory#createLeafHeaders(java.lang.String)
     */
    public LeafHeaders createLeafHeaders(String remoteIP) {
        return new LeafHeaders(remoteIP, networkManager);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.handshaking.HeadersFactory#createUltrapeerHeaders(java.lang.String)
     */
    public UltrapeerHeaders createUltrapeerHeaders(String remoteIP) {
        return new UltrapeerHeaders(remoteIP, networkManager);
    }

}
