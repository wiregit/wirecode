package com.limegroup.gnutella.dht;

import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;

import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.util.EventDispatcher;

public class DHTControllerFactoryImpl implements DHTControllerFactory {
    
    private final NetworkManager networkManager;
    
    public DHTControllerFactoryImpl(NetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.dht.DHTControllerFactory#createActiveDHTNodeController(org.limewire.mojito.routing.Vendor, org.limewire.mojito.routing.Version, com.limegroup.gnutella.util.EventDispatcher)
     */
    public ActiveDHTNodeController createActiveDHTNodeController(
            Vendor vendor, Version version,
            EventDispatcher<DHTEvent, DHTEventListener> dispatcher) {
        return new ActiveDHTNodeController(vendor, version, dispatcher, networkManager);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.dht.DHTControllerFactory#createPassiveDHTNodeController(org.limewire.mojito.routing.Vendor, org.limewire.mojito.routing.Version, com.limegroup.gnutella.util.EventDispatcher)
     */
    public PassiveDHTNodeController createPassiveDHTNodeController(
            Vendor vendor, Version version,
            EventDispatcher<DHTEvent, DHTEventListener> dispatcher) {
        return new PassiveDHTNodeController(vendor, version, dispatcher, networkManager);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.dht.DHTControllerFactory#createPassiveLeafController(org.limewire.mojito.routing.Vendor, org.limewire.mojito.routing.Version, com.limegroup.gnutella.util.EventDispatcher)
     */
    public PassiveLeafController createPassiveLeafController(
            Vendor vendor, Version version,
            EventDispatcher<DHTEvent, DHTEventListener> dispatcher) {
        return new PassiveLeafController(vendor, version, dispatcher, networkManager);
    }

}
