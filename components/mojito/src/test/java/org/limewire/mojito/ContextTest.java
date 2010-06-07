package org.limewire.mojito;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import junit.framework.TestSuite;

import org.limewire.mojito.DHT;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.MojitoFactory;
import org.limewire.mojito.io.Transport;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.RouteTable;
import org.limewire.mojito.settings.ContextSettings;
import org.limewire.mojito.settings.KademliaSettings;
import org.limewire.mojito.util.IoUtils;

public class ContextTest extends MojitoTestCase {
    
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
    
    public void testUnbindWithoutBind() throws IOException {
        MojitoDHT dht = MojitoFactory.createDHT("DHT-0");
        
        try {
            dht.unbind();
        } finally {
            IoUtils.close(dht);
        }
    }
    
    public void testBind() throws IOException {
        MojitoDHT dht = MojitoFactory.createDHT("DHT-0");
        
        try {
            MojitoFactory.bind(dht, 2000);
        } finally {
            IoUtils.close(dht);
        }
    }
    
    public void testUnbind() throws IOException {
        MojitoDHT dht = MojitoFactory.createDHT("DHT-0");
        Transport transport = null;
        
        try {
            transport = MojitoFactory.bind(dht, 2000);
            dht.unbind();
            
        } finally {
            IoUtils.close(dht);
            IoUtils.close((Closeable)transport);
        }
    }
    
    public void testCloseWithoutBind() throws IOException {
        MojitoDHT dht = MojitoFactory.createDHT("DHT-0");
        dht.close();
    }
    
    public void testShutdown() throws Exception {
        setLocalIsPrivate(false);
        ContextSettings.SHUTDOWN_MESSAGES_MULTIPLIER.setValue(1);
        
        int m = ContextSettings.SHUTDOWN_MESSAGES_MULTIPLIER.getValue();
        int expected = m*KademliaSettings.K;
        
        List<MojitoDHT> dhts = new ArrayList<MojitoDHT>();
        
        try {
            for (int i = 0; i < (2*expected); i++) {
                
                int port = 2000 + i;
                
                MojitoDHT dht = MojitoFactory.createDHT("DHT-" + i, port);
                
                if (i > 0) {
                    // bootstrap from the node 0, but make sure node 0
                    // pings back to mark the new node as ALIVE.
                    dht.bootstrap("localhost", 2000).get();
                    dhts.get(0).ping("localhost", port).get();
                    
                    // make sure after the first k nodes bootstrap the rest have at least k nodes in the rt
                    if (i > KademliaSettings.K) {
                        assertGreaterThanOrEquals(KademliaSettings.K, 
                                dht.getRouteTable().getContacts().size());
                    }
                }
                
                dhts.add(dht);
            }
            
            dhts.get(0).bootstrap("localhost", 2000+1).get();
            Thread.sleep(250);
            
            // Shutdown a random MojitoDHT instance
            Random generator = new Random();
            int index = generator.nextInt(dhts.size() / 2) + KademliaSettings.K;
            MojitoDHT down = dhts.get(index);
            down.close();
            
            // Give everybody a bit time to process the 
            // shutdown message
            Thread.sleep(2000);
            
            // Check now if the expected number of Nodes
            // was notified about the shutdown
            int downCounter = 0;
            int locationCounter = 0;
            
            for (int i = 0; i < dhts.size(); i++) {
                if (i != index) {
                    MojitoDHT dht = dhts.get(i);
                    RouteTable routeTable = dht.getRouteTable();
                    Collection<Contact> nodes = routeTable.getContacts();
                    boolean flag = false;
                    for (Contact node : nodes) {
                        if (node.isShutdown()) {
                            assertFalse("There shouldn't be more than one Nodes in DOWN state per RouteTable", flag);
                            
                            downCounter++;
                            flag = true;
                        }
                        
                        if (node.getContactId().equals(down.getLocalhost().getContactId())) {
                            locationCounter++;
                        }
                    }
                }
            }
            
            assertGreaterThanOrEquals(expected, locationCounter);
            
            // Test is failing because we're not waiting for the
            // shutdown messages to be sent, received or processed.
            // It's essentially a network race-condition.
            assertEquals(expected, downCounter);
            
        } finally {
            IoUtils.closeAll(dhts);
        }
    }
}
