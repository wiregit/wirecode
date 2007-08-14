package com.limegroup.gnutella.dht;

import java.util.concurrent.ScheduledExecutorService;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.ConnectionServices;
import com.limegroup.gnutella.HostCatcher;
import com.limegroup.gnutella.UDPPinger;

@Singleton
public class DHTNodeFetcherFactoryImpl implements DHTNodeFetcherFactory {

    private final ConnectionServices connectionServices;
    private final ScheduledExecutorService backgroundExecutor;
    private final Provider<HostCatcher> hostCatcher;
    private final Provider<UDPPinger> udpPingerFactory;
    
    @Inject
    public DHTNodeFetcherFactoryImpl(ConnectionServices connectionServices,
            @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor,
            Provider<HostCatcher> hostCatcher,
            Provider<UDPPinger> udpPingerFactory) {
        this.connectionServices = connectionServices;
        this.backgroundExecutor = backgroundExecutor;
        this.hostCatcher = hostCatcher;
        this.udpPingerFactory = udpPingerFactory;
    }

    public DHTNodeFetcher createNodeFetcher(DHTBootstrapper dhtBootstrapper) {
        return new DHTNodeFetcher(dhtBootstrapper, connectionServices, hostCatcher, backgroundExecutor, udpPingerFactory);
    }

}
