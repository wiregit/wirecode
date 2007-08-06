package com.limegroup.gnutella.dht;

import org.limewire.mojito.Context;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.MojitoFactory;
import org.limewire.mojito.io.MessageDispatcherFactory;
import org.limewire.mojito.routing.RouteTable;
import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;

import com.google.inject.Provider;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.dht.DHTManager.DHTMode;
import com.limegroup.gnutella.dht.db.AltLocValueFactory;
import com.limegroup.gnutella.dht.db.PushProxiesValueFactory;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.util.EventDispatcher;

class PassiveLeafController extends AbstractDHTController {

    private RouteTable routeTable;
    
    PassiveLeafController(Vendor vendor, Version version,
            EventDispatcher<DHTEvent, DHTEventListener> dispatcher,
            NetworkManager networkManager,
            AltLocValueFactory altLocValueFactory,
            PushProxiesValueFactory pushProxiesValueFactory,
            MessageDispatcherFactory messageDispatcherFactory,
            Provider<ConnectionManager> connectionManager,
            Provider<IPFilter> ipFilter) {
        super(vendor, version, dispatcher, DHTMode.PASSIVE_LEAF,
                networkManager, altLocValueFactory, pushProxiesValueFactory,
                messageDispatcherFactory, connectionManager, ipFilter);
    }

    @Override
    protected MojitoDHT createMojitoDHT(Vendor vendor, Version version) {
        MojitoDHT dht = MojitoFactory.createFirewalledDHT("PassiveLeafDHT", vendor, version);
        
        ((Context)dht).setBootstrapped(true);
        ((Context)dht).setBucketRefresherDisabled(true);
        
        routeTable = new PassiveLeafRouteTable(vendor, version);
        dht.setRouteTable(routeTable);
        assert (dht.isFirewalled());
        
        return dht;
    }

    @Override
    public void start() {
        super.start();
        
        if (isRunning()) {
            sendUpdatedCapabilities();
        }
    }
}
