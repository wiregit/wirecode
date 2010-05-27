package com.limegroup.gnutella.dht.db;

import java.net.UnknownHostException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.limewire.concurrent.FutureEvent;
import org.limewire.concurrent.FutureEvent.Type;
import org.limewire.core.settings.DHTSettings;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.concurrent.DHTValueFuture;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.PushEndpointCache;

public class PushEndpointManagerTest extends BaseTestCase {
    
    public PushEndpointManagerTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(PushEndpointManagerTest.class);
    }

    @Override
    public void setUp() {
        DHTSettings.ENABLE_PUSH_PROXY_QUERIES.setValue(true);
        DHTSettings.PUSH_ENDPOINT_PURGE_FREQUENCY.setTime(5L, TimeUnit.MILLISECONDS);
        DHTSettings.PUSH_ENDPOINT_CACHE_TIME.setTime(5L, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Call the {@link PushEndpointCache} first, followed by 
     * {@link PushEndpointService}.
     */
    public void tastInvokeChain() throws InterruptedException {
        final CountDownLatch cacheLatch = new CountDownLatch(1);
        PushEndpointCache cache = new PushEndpointCacheStub() {
            @Override
            public PushEndpoint getPushEndpoint(GUID guid) {
                cacheLatch.countDown();
                return super.getPushEndpoint(guid);
            }
        };
        
        final CountDownLatch serviceLatch = new CountDownLatch(1);
        PushEndpointService service = new PushEndpointService() {
            @Override
            public DHTFuture<PushEndpoint> findPushEndpoint(GUID guid) {
                serviceLatch.countDown();
                return new DHTValueFuture<PushEndpoint>(
                        new IllegalStateException("Ignore Me!"));
            }
        };
        
        PushEndpointManager manager = new PushEndpointManager(
                cache, service, 5L, TimeUnit.MILLISECONDS);
        manager.setCacheTime(5L, TimeUnit.MILLISECONDS);
        
        GUID guid = new GUID(GUID.makeGuid());
        DHTFuture<PushEndpoint> future = null;
        
        synchronized (manager) {
            assertEquals(0, manager.size());
            assertTrue(manager.isEmpty());
            
            future = manager.findPushEndpoint(guid);
            
            assertEquals(1, manager.size());
            assertFalse(manager.isEmpty());
        }
        
        // We call the PushEndpointCache 1st.
        if (!cacheLatch.await(10L, TimeUnit.MILLISECONDS)) {
            fail("Shouldn't have failed!");
        }
        
        // Followed by the PushEndpointService
        if (!serviceLatch.await(10L, TimeUnit.MILLISECONDS)) {
            fail("Shouldn't have failed!");
        }
    }
    
    /**
     * Everything we find needs to be added to the {@link PushEndpointCache}.
     */
    public void testNotifyCacheSuccess() 
            throws InterruptedException, UnknownHostException {
        
        final GUID guid = new GUID(GUID.makeGuid());
        final IpPort externalAddress = new IpPortImpl("localhost", 3000);
        
        final PushEndpoint enpoint = new PushEndpointStub() {
            @Override
            public byte[] getClientGUID() {
                return guid.bytes();
            }

            @Override
            public IpPort getValidExternalAddress() {
                return externalAddress;
            }
        };
        
        final CountDownLatch latch = new CountDownLatch(1);
        PushEndpointCache cache = new PushEndpointCacheStub() {
            @Override
            public void setAddr(byte[] g, IpPort addr) {
                latch.countDown();
            }
        };
        
        PushEndpointService service = new PushEndpointService() {
            @Override
            public DHTFuture<PushEndpoint> findPushEndpoint(GUID guid) {
                return new DHTValueFuture<PushEndpoint>(enpoint);
            }
        };
        
        PushEndpointManager manager = new PushEndpointManager(
                cache, service, 5L, TimeUnit.MILLISECONDS);
        
        
        DHTFuture<PushEndpoint> future 
            = manager.findPushEndpoint(guid);
        if (!latch.await(5L, TimeUnit.MILLISECONDS)) {
            fail("Shouldn't have failed!");
        }
    }
    
    /**
     * Make sure the {@link PushEndpointCache} is not being called
     * if a lookup for a {@link GUID} failed.
     */
    public void testNotifyCacheFailure() throws InterruptedException {
        
        PushEndpointCache cache = new PushEndpointCacheStub() {
            @Override
            public void setAddr(byte[] guid, IpPort addr) {
                fail("Shouldn't have been called!");
            }
        };
        
        PushEndpointService service = new PushEndpointService() {
            @Override
            public DHTFuture<PushEndpoint> findPushEndpoint(GUID guid) {
                return new DHTValueFuture<PushEndpoint>(
                        new IllegalStateException(
                            "Couldn't find PushEndpoint!"));
            }
        };
        
        final CountDownLatch latch = new CountDownLatch(1);
        PushEndpointManager manager = new PushEndpointManager(
                cache, service, 5L, TimeUnit.MILLISECONDS) {
            @Override
            protected void handlePushEndpoint(FutureEvent<PushEndpoint> event) {
                assertNotEquals(Type.SUCCESS, event.getType());
                latch.countDown();
                super.handlePushEndpoint(event);
            }
        };
        
        GUID guid = new GUID(GUID.makeGuid());
        DHTFuture<PushEndpoint> future 
            = manager.findPushEndpoint(guid);
        
        if (!latch.await(5L, TimeUnit.MILLISECONDS)) {
            fail("Shouldn't have failed!");
        }
    }
    
    /**
     * Make sure the {@link PushEndpointManager} is removing expired 
     * {@link DHTFuture}s from its internal cache.
     */
    public void testPurge() throws InterruptedException {
        
        PushEndpointCache cache = new PushEndpointCacheStub();
        
        PushEndpointService service = new PushEndpointService() {
            @Override
            public DHTFuture<PushEndpoint> findPushEndpoint(GUID guid) {
                return new DHTValueFuture<PushEndpoint>(
                        new IllegalStateException("Ignore Me!"));
            }
        };
        
        final CountDownLatch purgeLatch = new CountDownLatch(1);
        PushEndpointManager manager = new PushEndpointManager(
                cache, service, 5L, TimeUnit.MILLISECONDS) {
            @Override
            protected void purged(boolean done) {
                if (done) {
                    purgeLatch.countDown();
                }
            }
        };
        
        manager.setCacheTime(5L, TimeUnit.MILLISECONDS);
        
        GUID guid = new GUID(GUID.makeGuid());
        DHTFuture<PushEndpoint> future 
            = manager.findPushEndpoint(guid);
        
        if (!purgeLatch.await(10L, TimeUnit.SECONDS)) {
            fail("Shouldn't have failed!");
        }
        
        synchronized (manager) {
            assertEquals(0, manager.size());
            assertTrue(manager.isEmpty());
        }
    }
    
    /**
     * Two (or more) consequential calls with the same {@link GUID} should
     * always return the same {@link DHTFuture} instance unless a {@link DHTFuture}
     * has been purged.
     */
    public void testFutureCache() throws InterruptedException {
        PushEndpointCache cache = new PushEndpointCacheStub();
        
        PushEndpointService service = new PushEndpointService() {
            @Override
            public DHTFuture<PushEndpoint> findPushEndpoint(GUID guid) {
                return new DHTValueFuture<PushEndpoint>(
                        new IllegalStateException(
                            "Couldn't find PushEndpoint!"));
            }
        };
        
        final CountDownLatch latch = new CountDownLatch(1);
        PushEndpointManager manager = new PushEndpointManager(
                cache, service, 5L, TimeUnit.MILLISECONDS) {
            @Override
            protected void purged(boolean done) {
                if (done) {
                    latch.countDown();
                }
            }
        };
        
        manager.setCacheTime(5L, TimeUnit.MILLISECONDS);
        
        GUID guid = new GUID(GUID.makeGuid());
        DHTFuture<PushEndpoint> future1 = null;
        
        synchronized (manager) {
            future1 = manager.findPushEndpoint(guid);
            
            DHTFuture<PushEndpoint> future2 
                = manager.findPushEndpoint(guid);
            
            assertSame(future1, future2);
        }
        
        if (!latch.await(10L, TimeUnit.MILLISECONDS)) {
            fail("Shouldn't have failed!");
        }
        
        DHTFuture<PushEndpoint> future3 
            = manager.findPushEndpoint(guid);
        
        assertNotSame(future1, future3);
    }
}
