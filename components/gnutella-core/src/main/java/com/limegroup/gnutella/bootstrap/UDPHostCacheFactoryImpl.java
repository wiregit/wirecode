package com.limegroup.gnutella.bootstrap;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.UDPPinger;

@Singleton
public class UDPHostCacheFactoryImpl implements UDPHostCacheFactory {

    private final Provider<MessageRouter> messageRouter;

    @Inject
    public UDPHostCacheFactoryImpl(Provider<MessageRouter> messageRouter) {
        this.messageRouter = messageRouter;
    }
    
    public UDPHostCache createUDPHostCache(UDPPinger pinger) {
        return new UDPHostCache(pinger, messageRouter);
    }

    public UDPHostCache createUDPHostCache(long expiryTime,
            UDPPinger pinger) {
        return new UDPHostCache(expiryTime, pinger, messageRouter);
    }

}
