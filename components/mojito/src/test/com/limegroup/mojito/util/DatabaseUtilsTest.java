package com.limegroup.mojito.util;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import junit.framework.TestSuite;

import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.db.DHTValue;
import com.limegroup.mojito.db.DHTValueFactory;
import com.limegroup.mojito.db.DHTValue.ValueType;
import com.limegroup.mojito.routing.Contact;
import com.limegroup.mojito.routing.ContactFactory;
import com.limegroup.mojito.routing.RouteTable;
import com.limegroup.mojito.routing.impl.RouteTableImpl;
import com.limegroup.mojito.settings.DatabaseSettings;
import com.limegroup.mojito.settings.KademliaSettings;

public class DatabaseUtilsTest extends BaseTestCase {
    
    public static final String LOCAL_NODE_ID = "8A82F518E1CD6E7D56F965D65CE5FCAA6261DEA4";
    
    /*static {
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
    }*/
    
    public DatabaseUtilsTest(String name) {
        super(name);
    }

    public static TestSuite suite() {
        return buildTestSuite(DatabaseUtilsTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testExpirationTime() {
        assertEquals(20, KademliaSettings.REPLICATION_PARAMETER.getValue());
        
        RouteTable routeTable = new RouteTableImpl(LOCAL_NODE_ID);
        
        for (int i = 0; i < 15; i++) {
            KUID nodeId = KUID.createRandomID();
            SocketAddress addr = new InetSocketAddress("localhost", 5000 + i);
            routeTable.add(ContactFactory.createLiveContact(addr, 0, 0, nodeId, addr, 0, Contact.DEFAULT_FLAG));
        }
        
        assertEquals(16, routeTable.size());
        
        Contact originator = routeTable.getLocalNode();
        KUID valueId = originator.getNodeID().invert();
        DHTValue value = DHTValueFactory.createRemoteValue(
                originator, originator, ValueType.TEST, valueId, "Hello World".getBytes());
        
        long expectedExpiresAt = value.getCreationTime() + DatabaseSettings.VALUE_EXPIRATION_TIME.getValue();
        assertEquals(expectedExpiresAt, DatabaseUtils.getExpirationTime(routeTable, value));
        
        for (int i = 0; i < 4; i++) {
            KUID nodeId = KUID.createRandomID();
            SocketAddress addr = new InetSocketAddress("localhost", 6000 + i);
            routeTable.add(ContactFactory.createLiveContact(addr, 0, 0, nodeId, addr, 0, Contact.DEFAULT_FLAG));
            
            assertEquals(expectedExpiresAt, DatabaseUtils.getExpirationTime(routeTable, value));
        }
        
        assertEquals(20, routeTable.size());
        
        KUID nodeId = KUID.createRandomID();
        SocketAddress addr = new InetSocketAddress("localhost", 7000);
        routeTable.add(ContactFactory.createLiveContact(addr, 0, 0, nodeId, addr, 0, Contact.DEFAULT_FLAG));
        
        long expiresAt = DatabaseUtils.getExpirationTime(routeTable, value);
        assertLessThan(expectedExpiresAt, expiresAt);
    }
}
