package com.limegroup.mojito;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import junit.framework.TestSuite;

import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.mojito.routing.Contact;
import com.limegroup.mojito.routing.RouteTable;
import com.limegroup.mojito.settings.KademliaSettings;

public class ContextTest extends BaseTestCase {
    
    /*static {
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
    }*/
    
    public ContextTest(String name) {
        super(name);
    }

    public static TestSuite suite() {
        return buildTestSuite(ContextTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testShutdown() throws Exception {
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        
        int m = KademliaSettings.SHUTDOWN_MULTIPLIER.getValue();
        int k = KademliaSettings.REPLICATION_PARAMETER.getValue();
        int expected = m*k;
        
        List<MojitoDHT> dhts = new ArrayList<MojitoDHT>();
        
        try {
            for (int i = 0; i < (2*expected); i++) {
                MojitoDHT dht = MojitoFactory.createDHT("DHT-" + i);
                dht.bind(new InetSocketAddress("localhost", 2000 + i));
                dht.start();
                
                if (i > 0) {
                    dht.bootstrap(new InetSocketAddress("localhost", 2000)).get();
                }
                dhts.add(dht);
            }
            dhts.get(0).bootstrap(new InetSocketAddress("localhost", 2000+1)).get();
            Thread.sleep(250);
            
            // Shutdown a random MojitoDHT instance
            Random generator = new Random();
            int index = generator.nextInt(dhts.size());
            MojitoDHT down = dhts.get(index);
            down.stop();
            
            // Check now if the expected number of Nodes
            // was notified about the shutdown
            int count = 0;
            for (int i = 0; i < dhts.size(); i++) {
                if (i != index) {
                    Context dht = (Context)dhts.get(i);
                    RouteTable routeTable = dht.getRouteTable();
                    List<Contact> nodes = routeTable.getContacts();
                    boolean flag = false;
                    for (Contact node : nodes) {
                        if (node.isShutdown()) {
                            assertFalse("There shouldn't be more than one Nodes in DOWN state per RouteTable", flag);
                            
                            count++;
                            flag = true;
                        }
                    }
                }
            }
            Thread.sleep(200);
            assertEquals(expected, count);
        } finally {
            for (MojitoDHT dht : dhts) {
                dht.stop();
            }
        }
    }
}
