package com.limegroup.gnutella.bootstrap;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.UDPPinger;
import com.limegroup.gnutella.messages.PingRequestFactory;

@Singleton
public class UDPHostCacheFactoryImpl implements UDPHostCacheFactory {

    private final Provider<MessageRouter> messageRouter;
    private final PingRequestFactory pingRequestFactory;

    @Inject
    public UDPHostCacheFactoryImpl(Provider<MessageRouter> messageRouter,
            PingRequestFactory pingRequestFactory) {
        this.messageRouter = messageRouter;
        this.pingRequestFactory = pingRequestFactory;
    }
    
    public UDPHostCache createUDPHostCache(UDPPinger pinger) {
        return new UDPHostCache(pinger, messageRouter, pingRequestFactory);
    }

    public UDPHostCache createUDPHostCache(long expiryTime,
            UDPPinger pinger) {
        return new UDPHostCache(expiryTime, pinger, messageRouter, pingRequestFactory);
    }

}
