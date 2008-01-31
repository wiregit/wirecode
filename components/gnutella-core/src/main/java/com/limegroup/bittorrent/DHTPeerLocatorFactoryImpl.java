package com.limegroup.bittorrent;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.dht.DHTManager;

@Singleton
public class DHTPeerLocatorFactoryImpl implements PeerLocatorFactory {

    private final DHTManager          MANAGER;
    private final ApplicationServices APPLICATION_SERVICES;
    private final NetworkManager      NETWORK_MANAGER;
    
    @Inject
    public DHTPeerLocatorFactoryImpl(DHTManager manager, ApplicationServices applicationServices, NetworkManager networkManager) {
        this.MANAGER             = manager;
        this.APPLICATION_SERVICES = applicationServices;
        this.NETWORK_MANAGER      = networkManager;
    }    
    
    public PeerLocator create(ManagedTorrent torrent, BTMetaInfo torrentMeta) {

        return new DHTPeerLocator(MANAGER, APPLICATION_SERVICES, NETWORK_MANAGER, 
                                    torrent, torrentMeta);
        
    }
}
