package com.limegroup.mojito.handler.request;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;

import junit.framework.TestSuite;

import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.MojitoDHT;
import com.limegroup.mojito.MojitoFactory;
import com.limegroup.mojito.exceptions.DHTException;
import com.limegroup.mojito.result.PingResult;
import com.limegroup.mojito.settings.NetworkSettings;
import com.limegroup.mojito.util.UnitTestUtils;

public class PingRequestHandlerTest extends BaseTestCase {
    
    public PingRequestHandlerTest(String name) {
        super(name);
    }

    public static TestSuite suite() {
        return buildTestSuite(PingRequestHandlerTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testPingRequest() throws Exception {
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        NetworkSettings.MAX_ERRORS.setValue(0);
        NetworkSettings.TIMEOUT.setValue(250);
        
        MojitoDHT dht1 = null;
        MojitoDHT dht2 = null;
        
        try {
            
            dht1 = MojitoFactory.createDHT();
            dht1.bind(2000);
            dht1.start();
            
            dht2 = MojitoFactory.createDHT();
            dht2.bind(3000);
            dht2.start();
            
            // Regular Ping
            try {
                PingResult result = dht2.ping(new InetSocketAddress("localhost", 2000)).get();
                assertNotNull(result);
            } catch (ExecutionException err) {
                fail(err);
            }
            
            // A Node that's bootstrapping should not respond to
            // Pings to prevent other Nodes from seleting it as
            // their initial bootstrap Node
            Context context1 = (Context)dht1;
            UnitTestUtils.setBootstrapping(dht1, true);            
            assertFalse(dht1.isBootstrapped());
            assertTrue(context1.isBootstrapping());
            
            try {
                PingResult result = dht2.ping(new InetSocketAddress("localhost", 2000)).get();
                fail("DHT-1 did respond to our request " + result);
            } catch (ExecutionException expected) {
                assertTrue(expected.getCause() instanceof DHTException);
            }
            
            // Mext collision Ping. Different Node IDs -> should not work (same as above)
            context1.setContactAddress(new InetSocketAddress("localhost", 2000));
            Context context2 = (Context)dht2;
            try {
                PingResult result = context2.collisionPing(dht1.getLocalNode()).get();
                fail("DHT-1 did respond to our request " + result);
            } catch (ExecutionException expected) {
                assertTrue(expected.getCause() instanceof DHTException);
            }
            
            // Set DHT-2's Node ID to DHT-1 and try again. This should work!
            UnitTestUtils.setNodeID(dht2, dht1.getLocalNodeID());
            try {
                PingResult result = context2.collisionPing(dht1.getLocalNode()).get();
                assertNotNull(result);
            } catch (ExecutionException err) {
                fail(err);
            }
            
            // Make sure DHT-1 is still bootstrapping!
            assertFalse(dht1.isBootstrapped());
            assertTrue(context1.isBootstrapping());
        } finally {
            if (dht1 != null) { dht1.stop(); }
            if (dht2 != null) { dht2.stop(); }
        }
    }
}
