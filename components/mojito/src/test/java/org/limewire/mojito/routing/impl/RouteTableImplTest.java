package org.limewire.mojito.routing.impl;

import java.net.InetSocketAddress;
import java.text.MessageFormat;

import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoTestCase;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.ContactFactory;
import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;
import org.limewire.mojito.settings.KademliaSettings;
import org.limewire.mojito.settings.RouteTableSettings;

/**
 * Class with unit tests in addition to {@link RouteTableTest}. In this 
 * class each test method creates its own fixture to be more flexible.
 */
public class RouteTableImplTest extends MojitoTestCase {

    public RouteTableImplTest(String name) {
        super(name);
    }

    /**
     * Ensures that no contacts are added to the cache of a bucket that can
     * be split. If it can be split it should be split and no contacts should
     * be cached. This is true, since the the bucket's ability to split is an
     * invariant and will never change.
     */
    public void testAddWillNotCacheContactsInSplittableBucket() {
        RouteTableSettings.MAX_CACHE_SIZE.setValue(1);
        RouteTableSettings.MAX_CONTACTS_PER_NETWORK_CLASS_RATIO.setValue(0.001f);
        RouteTableSettings.DEPTH_LIMIT.setValue(2);
        
        
        KUID localNodeId = KUID.createRandomID();
        RouteTableImpl routeTable = new RouteTableImpl(localNodeId);
        
        // fill the local bucket with two nodes from the same class c network
        routeTable.add(createNode(KUID.createRandomID(), "192.168.0.1", 555));
        routeTable.add(createNode(KUID.createRandomID(), "192.168.0.2", 666));
        
        // fill the local bucked with replication_parameter - 1 other nodes, local node is already in there
        // last one should trigger the split
        for (int i = 0; i < KademliaSettings.REPLICATION_PARAMETER.getValue() - 1; i++) {
            // use different class c networks
            String address = MessageFormat.format("129.168.{0}.1", i);
            routeTable.add(createNode(KUID.createRandomID(), address, i + 1));
        }
    }

    private Contact createNode(KUID nodeID, String address, int port) {
        InetSocketAddress socketAddress = new InetSocketAddress(address, port);
        return ContactFactory.createLiveContact(socketAddress, Vendor.UNKNOWN, Version.ZERO, nodeID, socketAddress, 0, 0);
    }
}
