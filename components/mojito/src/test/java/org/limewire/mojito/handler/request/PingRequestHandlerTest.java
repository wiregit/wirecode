package org.limewire.mojito.handler.request;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import junit.framework.TestSuite;

import org.limewire.mojito.MojitoFactory;
import org.limewire.mojito.MojitoTestCase;
import org.limewire.mojito.exceptions.DHTException;
import org.limewire.mojito.util.UnitTestUtils;
import org.limewire.mojito2.MojitoDHT;
import org.limewire.mojito2.entity.PingEntity;
import org.limewire.mojito2.settings.ContextSettings;
import org.limewire.mojito2.settings.NetworkSettings;
import org.limewire.mojito2.util.ExceptionUtils;
import org.limewire.mojito2.util.IoUtils;

public class PingRequestHandlerTest extends MojitoTestCase {
    
    public PingRequestHandlerTest(String name) {
        super(name);
    }

    public static TestSuite suite() {
        return buildTestSuite(PingRequestHandlerTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setLocalIsPrivate(false);
    }

    public void testPingRequest() throws Exception {
        final long waitForFutureDone = 250; // ms
        
        NetworkSettings.MAX_ERRORS.setValue(0);
        NetworkSettings.DEFAULT_TIMEOUT.setValue(250);
        
        MojitoDHT dht1 = null;
        MojitoDHT dht2 = null;
        
        try {
            
            dht1 = MojitoFactory.createDHT("DHT-1", 2000);
            dht2 = MojitoFactory.createDHT("DHT-2", 3000);
            
            // Regular Ping
            try {
                PingEntity entity = dht2.ping(
                        "localhost", 2000).get();
                assertNotNull(entity);
            } catch (ExecutionException err) {
                fail("Shouldn't have failed!", err);
            }
            
            // A Node that's bootstrapping should not respond to
            // Pings to prevent other Nodes from selecting it as
            // their initial bootstrap Node
            
            UnitTestUtils.setBooting(dht1, true);
            assertFalse(dht1.isReady());
            assertTrue(dht1.isBooting());
            
            try {
                PingEntity entity = dht2.ping("localhost", 2000).get();
                assertFalse(dht1.isReady());
                assertTrue(dht1.isBooting());
                fail("DHT-1 did respond to our request " + entity);
            } catch (ExecutionException expected) {
                assertTrue(ExceptionUtils.isCausedBy(
                        expected, TimeoutException.class));
            }
            
            // next collision Ping. Different Node IDs -> should not work (same as above)
            dht1.setContactAddress(new InetSocketAddress("localhost", 2000));
            
            // Disable local assertion to make sure the network level
            // collision ping checks work
            ContextSettings.ASSERT_COLLISION_PING.setValue(false);
            try {
                PingEntity entity = dht2.collisionPing(dht1.getLocalNode()).get();
                fail("DHT-1 did respond to our request " + entity);
            } catch (ExecutionException expected) {
                assertTrue(ExceptionUtils.isCausedBy(
                        expected, TimeoutException.class));
            } catch (IllegalArgumentException err) {
                fail("Should not have thrown an IllegalArgumentException", err);
            }
            
            // Re-Enable local assertion to make sure you can't create
            // malformed collision pings like above
            ContextSettings.ASSERT_COLLISION_PING.setValue(true);
            try {
                PingEntity entity = dht2.collisionPing(dht1.getLocalNode()).get();
                fail("DHT-1 did respond to our request " + entity);
            } catch (ExecutionException expected) {
                assertTrue(ExceptionUtils.isCausedBy(
                        expected, TimeoutException.class));
            } catch (IllegalArgumentException expected) {
            }
            
            // Set DHT-2's Node ID to DHT-1 and try again. This should work!
            dht2.setContactId(dht1.getLocalNodeID());
            try {
                PingEntity entity = dht2.collisionPing(dht1.getLocalNode()).get();
                assertNotNull(entity);
            } catch (ExecutionException err) {
                fail(err);
            }
            
            // Make sure DHT-1 is still bootstrapping!
            assertFalse(dht1.isReady());
            assertTrue(dht1.isBooting());
        } finally {
            IoUtils.closeAll(dht1, dht2);
        }
    }
}
