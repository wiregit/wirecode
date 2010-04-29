package org.limewire.mojito;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import junit.framework.TestSuite;

import org.limewire.mojito2.Context;
import org.limewire.mojito2.DHT;
import org.limewire.mojito2.io.Transport;
import org.limewire.mojito2.routing.Contact;
import org.limewire.mojito2.routing.RouteTable;
import org.limewire.mojito2.settings.ContextSettings;
import org.limewire.mojito2.settings.KademliaSettings;
import org.limewire.mojito2.util.IoUtils;

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
        DHT dht = MojitoFactory.createDHT("DHT-0");
        
        try {
            dht.unbind();
        } finally {
            IoUtils.close(dht);
        }
    }
    
    public void testBind() throws IOException {
        DHT dht = MojitoFactory.createDHT("DHT-0");
        
        try {
            MojitoFactory.bind(dht, 2000);
        } finally {
            IoUtils.close(dht);
        }
    }
    
    public void testUnbind() throws IOException {
        DHT dht = MojitoFactory.createDHT("DHT-0");
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
        DHT dht = MojitoFactory.createDHT("DHT-0");
        dht.close();
    }
    
    public void testShutdown() throws Exception {
        setLocalIsPrivate(false);
        ContextSettings.SHUTDOWN_MESSAGES_MULTIPLIER.setValue(1);
        
        int m = ContextSettings.SHUTDOWN_MESSAGES_MULTIPLIER.getValue();
        int k = KademliaSettings.REPLICATION_PARAMETER.getValue();
        int expected = m*k;
        
        List<DHT> dhts = new ArrayList<DHT>();
        
        try {
            for (int i = 0; i < (2*expected); i++) {
                
                int port = 2000 + i;
                
                DHT dht = MojitoFactory.createDHT("DHT-" + i, port);
                InetSocketAddress addr = new InetSocketAddress(
                        "localhost", port); 
                
                if (i > 0) {
                    // bootstrap from the node 0, but make sure node 0
                    // pings back to mark the new node as ALIVE.
                    dht.bootstrap(new InetSocketAddress("localhost", 2000)).get();
                    dhts.get(0).ping(addr).get();
                    
                    // make sure after the first k nodes bootstrap the rest have at least k nodes in the rt
                    if (i > k)
                        assertGreaterThanOrEquals(k, dht.getRouteTable().getContacts().size());
                }
                dhts.add(dht);
            }
            dhts.get(0).bootstrap(new InetSocketAddress("localhost", 2000+1)).get();
            Thread.sleep(250);
            
            // Shutdown a random MojitoDHT instance
            Random generator = new Random();
            int index = generator.nextInt(dhts.size() / 2) + k;
            DHT down = dhts.get(index);
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
                    Context dht = (Context)dhts.get(i);
                    RouteTable routeTable = dht.getRouteTable();
                    Collection<Contact> nodes = routeTable.getContacts();
                    boolean flag = false;
                    for (Contact node : nodes) {
                        if (node.isShutdown()) {
                            assertFalse("There shouldn't be more than one Nodes in DOWN state per RouteTable", flag);
                            
                            downCounter++;
                            flag = true;
                        }
                        
                        if (node.getNodeID().equals(down.getLocalNode().getNodeID())) {
                            locationCounter++;
                        }
                    }
                }
            }
            
            assertGreaterThanOrEquals(expected, locationCounter);
            assertEquals(expected, downCounter);
            
        } finally {
            IoUtils.closeAll(dhts);
        }
    }
}
