package com.limegroup.gnutella.dht.db;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.Test;

import org.limewire.concurrent.FutureEvent;
import org.limewire.concurrent.FutureEvent.Type;
import org.limewire.io.GUID;
import org.limewire.io.IpPortImpl;
import org.limewire.listener.EventListener;
import org.limewire.mojito2.KUID;
import org.limewire.mojito2.concurrent.DHTFuture;

import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.dht.util.KUIDUtils;
import com.limegroup.gnutella.security.MerkleTree;

public class AltLocFinderImplTest extends DHTFinderTestCase {

    private AltLocFinder altLocFinder;

    public AltLocFinderImplTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(AltLocFinderImplTest.class);
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        altLocFinder = injector.getInstance(AltLocFinder.class);
    }
    
    private URN publishDirectAltLoc(boolean publish) throws IOException, 
            InterruptedException, ExecutionException {
        ApplicationServices applicationServices 
            = injector.getInstance(ApplicationServices.class);
        
        // set to non-firewalled, so created altloc value for self is not firewalled
        networkManager.setAcceptedIncomingConnection(true);
        // publish an alternate location in the DHT
        URN urn = URN.createSHA1Urn("urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB");
        
        DefaultAltLocValue value = new DefaultAltLocValue(
                5555, new byte[MerkleTree.HASHSIZE],
                networkManager, applicationServices);
        
        assertFalse(value.isFirewalled());
        KUID kuid = KUIDUtils.toKUID(urn);

        // publish altloc value manually for successful case
        if (publish) {
            mojitoDHT.put(kuid, value.serialize()).get();
        }
        return urn;
    }
    
    public void testAltLocListenerIsNotifiedOfNonFirewalledLocations() throws Exception {
        testAltLocListenerNonFirewalledLocations(true);
    }
    
    public void testAltLocListenerIsNotNotifiedOfNonFirewalledLocations() throws Exception {
        testAltLocListenerNonFirewalledLocations(false);
    }
    
    private void testAltLocListenerNonFirewalledLocations(boolean successful) throws Exception {
        URN urn = publishDirectAltLoc(successful);
        
        // expected alternate location
        final AlternateLocation expectedAltLoc 
            = alternateLocationFactory.createDirectDHTAltLoc(
                    new IpPortImpl(networkManager.getAddress(), 
                            networkManager.getPort()), 
                            urn, 5555, new byte[MerkleTree.HASHSIZE]);
        
        final AtomicReference<AlternateLocation[]> locationsRef 
            = new AtomicReference<AlternateLocation[]>();
        final CountDownLatch latch = new CountDownLatch(1);
        
        DHTFuture<AlternateLocation[]> futures 
            = altLocFinder.findAltLocs(urn);
        futures.addFutureListener(new EventListener<FutureEvent<AlternateLocation[]>>() {
            @Override
            public void handleEvent(FutureEvent<AlternateLocation[]> event) {
                try {
                    if (event.getType() == Type.SUCCESS) {
                        locationsRef.set(event.getResult());
                    }
                } finally {
                    latch.countDown();
                }
            }
        });
        
        if (!latch.await(5, TimeUnit.SECONDS)) {
            fail("Shouldn't have failed!");
        }

        AlternateLocation[] locations = locationsRef.get();
        
        if (successful) {
            assertNotNull(locations);
            assertEquals(1, locations.length);
            assertEquals(expectedAltLoc, locations[0]);
        } else {
            assertNull(locations);
        }
    }
    
    public void testAltLocListenerIsNotifedOfFirewalledLocations() throws Exception {
        testAltLocListenerFirewalledLocations(true);
    }
    
    public void testAltLocListenerIsNotNotifedOfFirewalledLocations() throws Exception {
        testAltLocListenerFirewalledLocations(false);
    }
    
    private URN publishPushAltLoc(boolean publish) throws IOException, 
            InterruptedException, ExecutionException {
        
        ApplicationServices applicationServices 
            = injector.getInstance(ApplicationServices.class);
        
        networkManager.setExternalAddress(new byte[] { 127, 0, 0, 1 });
        
        URN urn = URN.createSHA1Urn("urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB");
        DefaultAltLocValue value = new DefaultAltLocValue(
                5555, new byte[MerkleTree.HASHSIZE], 
                networkManager, applicationServices);
        
        assertTrue(value.isFirewalled());
        KUID kuid = KUIDUtils.toKUID(urn);

        // publish altloc value manually
        mojitoDHT.put(kuid, value.serialize()).get();
        
        
        // publish push proxy manually
        DefaultPushProxiesValue pushProxiesValue 
            = new DefaultPushProxiesValue(networkManager, 
                    applicationServices, pushEndpointFactory);
        
        if (publish) {
            KUID key = KUIDUtils.toKUID(new GUID(pushProxiesValue.getGUID()));
            mojitoDHT.put(key, pushProxiesValue.serialize()).get();
        }
        return urn;
    }
    
    private void testAltLocListenerFirewalledLocations(boolean successful) throws Exception {
        URN urn = publishPushAltLoc(successful);
        
        DHTFuture<AlternateLocation[]> future 
            = altLocFinder.findAltLocs(urn);

        final AtomicReference<AlternateLocation[]> locationsRef 
            = new AtomicReference<AlternateLocation[]>();
        final CountDownLatch latch = new CountDownLatch(1);
        future.addFutureListener(new EventListener<FutureEvent<AlternateLocation[]>>() {
            @Override
            public void handleEvent(FutureEvent<AlternateLocation[]> event) {
                try {
                    if (event.getType() == Type.SUCCESS) {
                        locationsRef.set(event.getResult());
                    }
                } finally {
                    latch.countDown();
                }
            }
        });
        
        if (!latch.await(5, TimeUnit.SECONDS)) {
            fail("Shouldn't have failed!");
        }
        
        AlternateLocation[] locations = locationsRef.get();
        
        // expected alternate location
        AlternateLocation expectedAltLoc 
            = alternateLocationFactory.createPushAltLoc(
                    pushEndpointFactory.createForSelf(), urn);
        
        if (successful) {
            assertNotNull(locations);
            assertEquals(1, locations.length);
            assertEquals(expectedAltLoc, locations[0]);
        } else {
            assertNull(locations);
        }
    }
}
