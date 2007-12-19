package com.limegroup.bittorrent;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.dht.DHTManager;

@Singleton
public class DHTPeerLocatorFactoryImpl implements PeerLocatorFactory {

    private final DHTManager          manager;
    private final ApplicationServices applicationServices;
    private final NetworkManager      networkManager;
    
    @Inject
    public DHTPeerLocatorFactoryImpl(DHTManager manager, ApplicationServices applicationServices, NetworkManager networkManager) {
        this.manager             = manager;
        this.applicationServices = applicationServices;
        this.networkManager      = networkManager;
    }    
    
    public PeerLocator create(ManagedTorrent torrent, BTMetaInfo torrentMeta) {

        return new DHTPeerLocator(manager, applicationServices, networkManager, 
                                    torrent, torrentMeta);
        
    }
}
