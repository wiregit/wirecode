package org.limewire.mojito.util;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import junit.framework.TestSuite;

import org.limewire.mojito.MojitoTestCase;
import org.limewire.mojito2.KUID;
import org.limewire.mojito2.routing.Contact;
import org.limewire.mojito2.routing.ContactFactory;
import org.limewire.mojito2.routing.RouteTable;
import org.limewire.mojito2.routing.RouteTableImpl;
import org.limewire.mojito2.routing.Vendor;
import org.limewire.mojito2.routing.Version;
import org.limewire.mojito2.settings.DatabaseSettings;
import org.limewire.mojito2.settings.KademliaSettings;
import org.limewire.mojito2.storage.DHTValueEntity;
import org.limewire.mojito2.storage.DHTValueImpl;
import org.limewire.mojito2.storage.DHTValueType;
import org.limewire.mojito2.util.DatabaseUtils;
import org.limewire.util.StringUtils;


public class DatabaseUtilsTest extends MojitoTestCase {
    
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
        assertEquals(20, KademliaSettings.K);
        
        RouteTable routeTable = new RouteTableImpl(LOCAL_NODE_ID);
        
        for (int i = 0; i < 15; i++) {
            KUID nodeId = KUID.createRandomID();
            SocketAddress addr = new InetSocketAddress("localhost", 5000 + i);
            
            routeTable.add(ContactFactory.createLiveContact(addr, Vendor.UNKNOWN, Version.ZERO, 
                    nodeId, addr, 0, Contact.DEFAULT_FLAG));
        }
        
        assertEquals(16, routeTable.size());
        
        Contact creator = routeTable.getLocalNode();
        KUID valueId = creator.getNodeID().invert();
        DHTValueEntity value = DHTValueEntity.createFromRemote(creator, creator, valueId, 
															   new DHTValueImpl(DHTValueType.TEST, Version.ZERO, StringUtils.toUTF8Bytes("Hello World")));
        
        long expectedExpiresAt = value.getCreationTime() 
            + DatabaseSettings.VALUE_EXPIRATION_TIME.getTimeInMillis();
        assertEquals(expectedExpiresAt, DatabaseUtils.getExpirationTime(routeTable, value));
        
        for (int i = 0; i < 4; i++) {
            KUID nodeId = KUID.createRandomID();
            SocketAddress addr = new InetSocketAddress("localhost", 6000 + i);
            
            routeTable.add(ContactFactory.createLiveContact(addr, Vendor.UNKNOWN, Version.ZERO, 
                    nodeId, addr, 0, Contact.DEFAULT_FLAG));
            
            assertEquals(expectedExpiresAt, DatabaseUtils.getExpirationTime(routeTable, value));
        }
        
        assertEquals(20, routeTable.size());
        
        KUID nodeId = KUID.createRandomID();
        SocketAddress addr = new InetSocketAddress("localhost", 7000);
        
        routeTable.add(ContactFactory.createLiveContact(addr, Vendor.UNKNOWN, Version.ZERO, 
                nodeId, addr, 0, Contact.DEFAULT_FLAG));
        
        long expiresAt = DatabaseUtils.getExpirationTime(routeTable, value);
        assertLessThan(expectedExpiresAt, expiresAt);
    }
}
