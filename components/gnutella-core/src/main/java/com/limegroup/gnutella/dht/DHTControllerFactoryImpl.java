package com.limegroup.gnutella.dht;

import org.limewire.mojito.io.MessageDispatcherFactory;
import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.dht.db.AltLocValueFactory;
import com.limegroup.gnutella.dht.db.PushProxiesValueFactory;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.util.EventDispatcher;

@Singleton
public class DHTControllerFactoryImpl implements DHTControllerFactory {
    
    private final NetworkManager networkManager;
    private final AltLocValueFactory altLocValueFactory;
    private final PushProxiesValueFactory pushProxiesValueFactory;
    private final MessageDispatcherFactory messageDispatcherFactory;
    private final Provider<ConnectionManager> connectionManager;
    private final Provider<IPFilter> ipFilter;
    
    @Inject
    public DHTControllerFactoryImpl(NetworkManager networkManager,
            AltLocValueFactory altLocValueFactory,
            PushProxiesValueFactory pushProxiesValueFactory,
            MessageDispatcherFactory messageDispatcherFactory,
            Provider<ConnectionManager> connectionManager,
            Provider<IPFilter> ipFilter) {
        this.networkManager = networkManager;
        this.altLocValueFactory = altLocValueFactory;
        this.pushProxiesValueFactory = pushProxiesValueFactory;
        this.messageDispatcherFactory = messageDispatcherFactory;
        this.connectionManager = connectionManager;
        this.ipFilter = ipFilter;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.dht.DHTControllerFactory#createActiveDHTNodeController(org.limewire.mojito.routing.Vendor, org.limewire.mojito.routing.Version, com.limegroup.gnutella.util.EventDispatcher)
     */
    public ActiveDHTNodeController createActiveDHTNodeController(
            Vendor vendor, Version version,
            EventDispatcher<DHTEvent, DHTEventListener> dispatcher) {
        return new ActiveDHTNodeController(vendor, version, dispatcher,
                networkManager, altLocValueFactory, pushProxiesValueFactory,
                messageDispatcherFactory, connectionManager, ipFilter);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.dht.DHTControllerFactory#createPassiveDHTNodeController(org.limewire.mojito.routing.Vendor, org.limewire.mojito.routing.Version, com.limegroup.gnutella.util.EventDispatcher)
     */
    public PassiveDHTNodeController createPassiveDHTNodeController(
            Vendor vendor, Version version,
            EventDispatcher<DHTEvent, DHTEventListener> dispatcher) {
        return new PassiveDHTNodeController(vendor, version, dispatcher,
                networkManager, altLocValueFactory, pushProxiesValueFactory,
                messageDispatcherFactory, connectionManager, ipFilter);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.dht.DHTControllerFactory#createPassiveLeafController(org.limewire.mojito.routing.Vendor, org.limewire.mojito.routing.Version, com.limegroup.gnutella.util.EventDispatcher)
     */
    public PassiveLeafController createPassiveLeafController(
            Vendor vendor, Version version,
            EventDispatcher<DHTEvent, DHTEventListener> dispatcher) {
        return new PassiveLeafController(vendor, version, dispatcher,
                networkManager, altLocValueFactory, pushProxiesValueFactory,
                messageDispatcherFactory, connectionManager, ipFilter);
    }

}
