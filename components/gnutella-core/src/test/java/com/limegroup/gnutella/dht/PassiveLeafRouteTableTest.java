package com.limegroup.gnutella.dht;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import junit.framework.Test;

import org.limewire.mojito.KUID;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.ContactFactory;
import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;
import org.limewire.mojito.settings.KademliaSettings;

public class PassiveLeafRouteTableTest extends DHTTestCase {
    
    public PassiveLeafRouteTableTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(PassiveLeafRouteTableTest.class);
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    /**
     * Test the PassiveLeafRouteTable's LRU properties
     */
    public void testPassiveLeafRouteTable() {
        final int k = KademliaSettings.REPLICATION_PARAMETER.getValue();
        
        PassiveLeafRouteTable routeTable = new PassiveLeafRouteTable(Vendor.UNKNOWN, Version.UNKNOWN);
        
        List<Contact> list = new ArrayList<Contact>();
        for (int i = 0; i < 2*k; i++) {
            Contact c = ContactFactory.createUnknownContact(
                    Vendor.UNKNOWN, Version.UNKNOWN, KUID.createRandomID(), 
                    new InetSocketAddress("localhost", 2000+i));
            
            list.add(c);
            routeTable.add(c);
        }
        
        // localhost + k Nodes
        assertEquals(k+1, routeTable.getActiveContacts().size());
        
        // Select k Nodes
        Collection<Contact> selected = routeTable.select(KUID.createRandomID(), k, true);
        assertEquals(k, selected.size());
        assertFalse(selected.contains(routeTable.getLocalNode()));
        
        // Should make no difference
        selected = routeTable.select(KUID.createRandomID(), k, false);
        assertEquals(k, selected.size());
        assertFalse(selected.contains(routeTable.getLocalNode()));
        
        // The first k elements should be gone and the last (newest) 
        // k elements should be there
        for (int i = 0; i < list.size(); i++) {
            Contact c = list.get(i);
            
            if (i < k) {
                assertNull(routeTable.get(c.getNodeID()));
            } else {
                assertEquals(c, routeTable.get(c.getNodeID()));
            }
        }
        
        // Test the actual LRU property. Touch the k+1 th element
        // which should move it to the end of the List
        routeTable.get(list.get(k+1).getNodeID());
        selected = routeTable.select(KUID.createRandomID(), k, true);
        
        Contact first = null;
        Contact last = null;
        for (Contact c : selected) {
            if (first == null) {
                first = c;
            }
            last = c;
        }
        
        // And check if it's at the end of the List
        assertEquals(list.get(k+1), last);
        
        // Now add a new Contact to the LRU List
        Contact c = ContactFactory.createUnknownContact(
                Vendor.UNKNOWN, Version.UNKNOWN, KUID.createRandomID(), 
                new InetSocketAddress("localhost", 3000));
        
        list.add(c);
        routeTable.add(c);
        
        // The first element should be gone now and the (previous) 
        // last element should be still there
        assertEquals(c, routeTable.get(c.getNodeID()));
        assertNull(routeTable.get(first.getNodeID()));
        assertEquals(last, routeTable.get(last.getNodeID()));
    }
}
