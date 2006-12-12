package com.limegroup.mojito.manager;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import junit.framework.TestSuite;

import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.MojitoDHT;
import com.limegroup.mojito.MojitoFactory;
import com.limegroup.mojito.exceptions.DHTTimeoutException;
import com.limegroup.mojito.routing.Contact;
import com.limegroup.mojito.settings.KademliaSettings;
import com.limegroup.mojito.settings.NetworkSettings;

public class PingManagerTest extends BaseTestCase {
    
    static {
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
    }
    
    public PingManagerTest(String name){
        super(name);
    }
    
    public static TestSuite suite() {
        return buildTestSuite(PingManagerTest.class);
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testParallelPings() throws Exception {
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        KademliaSettings.PARALLEL_PINGS.setValue(3);
        NetworkSettings.MAX_ERRORS.setValue(0);
        
        MojitoDHT dht1 = null, dht2 = null;
        
        try {
            dht1 = MojitoFactory.createDHT();
            dht1.bind(new InetSocketAddress(2000));
            dht1.start();
            
            dht2 = MojitoFactory.createDHT();
            dht2.bind(new InetSocketAddress(3000));
            dht2.start();
            
            Set<SocketAddress> hosts = new LinkedHashSet<SocketAddress>();
            hosts.add(new InetSocketAddress("www.apple.com", 80));
            hosts.add(new InetSocketAddress("www.microsoft.com", 80));
            hosts.add(new InetSocketAddress("www.google.com", 80));
            hosts.add(new InetSocketAddress("www.cnn.com", 80));
            assertEquals(4, hosts.size());
            
            try {
                ((Context)dht2).ping(hosts).get().getContact();
                fail("Ping should have failed");
            } catch (ExecutionException e) {
                assertTrue(e.getCause() instanceof DHTTimeoutException);
            }
            
            hosts.add(new InetSocketAddress("localhost", 2000));
            assertEquals(5, hosts.size());
            
            try {
                Contact node = ((Context)dht2).ping(hosts).get().getContact();
                assertEquals(dht1.getLocalNodeID(), node.getNodeID());
            } catch (ExecutionException e) {
                fail(e);
            }
        } finally {
            if (dht1 != null) { dht1.close(); }
            if (dht2 != null) { dht2.close(); }
        }
    }
}
