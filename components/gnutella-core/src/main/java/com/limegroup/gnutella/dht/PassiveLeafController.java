package com.limegroup.gnutella.dht;

import org.limewire.mojito.Context;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.MojitoFactory;
import org.limewire.mojito.routing.RouteTable;
import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;

import com.limegroup.gnutella.dht.DHTManager.DHTMode;
import com.limegroup.gnutella.util.EventDispatcher;

class PassiveLeafController extends AbstractDHTController {

    private RouteTable routeTable;
    
    public PassiveLeafController(Vendor vendor, Version version, 
            EventDispatcher<DHTEvent, DHTEventListener> dispatcher) {
        super(vendor, version, dispatcher, DHTMode.PASSIVE_LEAF);
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
