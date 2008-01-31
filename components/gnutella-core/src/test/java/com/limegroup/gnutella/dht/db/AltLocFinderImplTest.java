package com.limegroup.gnutella.dht.db;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.io.IpPortImpl;
import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.util.MojitoUtils;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.PushEndpointFactory;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.altlocs.AlternateLocationFactory;
import com.limegroup.gnutella.altlocs.PushAltLoc;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.dht.DHTTestCase;
import com.limegroup.gnutella.dht.util.KUIDUtils;
import com.limegroup.gnutella.security.TigerTree;
import com.limegroup.gnutella.stubs.NetworkManagerStub;
import com.limegroup.gnutella.util.MockUtils;

public class AltLocFinderImplTest extends DHTTestCase {

    private Mockery context;
    private MojitoDHT mojitoDHT;
    private List<MojitoDHT> dhts = Collections.emptyList();
    private Injector injector;
    private AltLocValueFactory altLocValueFactory;
    private AltLocFinder altLocFinder;
    private DHTManager dhtManager;
    private AlternateLocationFactory alternateLocationFactory;
    private NetworkManagerStub networkManager;
    private PushProxiesValueFactory pushProxiesValueFactory;
    private PushEndpointFactory pushEndpointFactory;

    public AltLocFinderImplTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(AltLocFinderImplTest.class);
    }
    
    @Override
    protected void setUp() throws Exception {
        setSettings();
        setLocalIsPrivate(false);
        
        context = new Mockery();
        dhtManager = context.mock(DHTManager.class);
        dhts = MojitoUtils.createBootStrappedDHTs(1);
        
        mojitoDHT = dhts.get(0);
        context.checking(new Expectations() {{
            allowing(dhtManager).getMojitoDHT();
            will(returnValue(mojitoDHT));
        }});
        assertTrue(dhtManager.getMojitoDHT().isBootstrapped());

        networkManager = new NetworkManagerStub();

        // to have non-empty push proxies to send
        final ConnectionManager connectionManager = MockUtils.createConnectionManagerWithPushProxies(context);
        
        injector = LimeTestUtils.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(DHTManager.class).toInstance(dhtManager);
                bind(NetworkManager.class).toInstance(networkManager);
                bind(ConnectionManager.class).toInstance(connectionManager);
            }
        });
        altLocValueFactory = injector.getInstance(AltLocValueFactory.class);
        altLocFinder = injector.getInstance(AltLocFinder.class);
        alternateLocationFactory = injector.getInstance(AlternateLocationFactory.class);
        pushProxiesValueFactory = injector.getInstance(PushProxiesValueFactory.class);
        pushEndpointFactory = injector.getInstance(PushEndpointFactory.class);
        
        // register necessary factories
        mojitoDHT.getDHTValueFactoryManager().addValueFactory(AbstractAltLocValue.ALT_LOC, altLocValueFactory);
        mojitoDHT.getDHTValueFactoryManager().addValueFactory(AbstractPushProxiesValue.PUSH_PROXIES, pushProxiesValueFactory);
    }
    
    @Override
    protected void tearDown() throws Exception {
        for (MojitoDHT dht : dhts) {
            dht.close();
        }
    }
    
    public void testAltLocListenerIsNotifiedOfNonFirewalledLocations() throws Exception {
        testAltLocListenerNonFirewalledLocations(true);
    }
    
    public void testAltLocListenerIsNotNotifiedOfNonFirewalledLocations() throws Exception {
        testAltLocListenerNonFirewalledLocations(false);
    }
    
    private URN publishDirectAltLoc(boolean publish) throws Exception {
        // set to non-firewalled, so created altloc value for self is not firewalled
        networkManager.setAcceptedIncomingConnection(true);
        // publish an alternate location in the DHT
        URN urn = URN.createSHA1Urn("urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB");
        AltLocValue value = altLocValueFactory.createAltLocValueForSelf(5555, new byte[TigerTree.HASHSIZE]);
        assertFalse(value.isFirewalled());
        KUID kuid = KUIDUtils.toKUID(urn);

        // publish altloc value manually for successful case
        if (publish) {
            mojitoDHT.put(kuid, value).get();
        }
        return urn;
    }
    
    private void testAltLocListenerNonFirewalledLocations(boolean successful) throws Exception {
        URN urn = publishDirectAltLoc(successful);
        // expected alternate location
        final AlternateLocation expectedAltLoc = alternateLocationFactory.createDirectDHTAltLoc(new IpPortImpl(networkManager.getAddress(), networkManager.getPort()), urn, 5555, new byte[TigerTree.HASHSIZE]);
        AltLocSearchHandler listener = new AltLocSearchHandler();
        // initialize with other value to see if it is actually called
        listener.success = !successful;
        
        altLocFinder.findAltLocs(urn, listener);
        listener.doneLatch.await(500, TimeUnit.MILLISECONDS);
        listener.alternateLocationLatch.await(500, TimeUnit.MILLISECONDS);

        if (successful) {
            assertEquals(expectedAltLoc, listener.altLoc);
            assertTrue(listener.success);
        } else {
            assertNull(listener.altLoc);
            assertFalse(listener.success);
        }
    }
    
    public void testAltLocListenerIsNotifedOfFirewalledLocations() throws Exception {
        testAltLocListenerFirewalledLocations(true);
    }
    
    public void testAltLocListenerIsNotNotifedOfFirewalledLocations() throws Exception {
        testAltLocListenerFirewalledLocations(false);
    }
    
    private URN publishPushAltLoc(boolean publish) throws Exception {
        networkManager.setExternalAddress(new byte[] { 127, 0, 0, 1 });
        
        URN urn = URN.createSHA1Urn("urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB");
        AltLocValue value = altLocValueFactory.createAltLocValueForSelf(5555, new byte[TigerTree.HASHSIZE]);
        assertTrue(value.isFirewalled());
        KUID kuid = KUIDUtils.toKUID(urn);

        // publish altloc value manually
        mojitoDHT.put(kuid, value).get();
        // publish push proxy manually
        PushProxiesValue pushProxiesValue = pushProxiesValueFactory.createDHTValueForSelf();
        if (publish) {
            mojitoDHT.put(KUIDUtils.toKUID(new GUID(pushProxiesValue.getGUID())), pushProxiesValue).get();
        }
        return urn;
    }
    
    private void testAltLocListenerFirewalledLocations(boolean successful) throws Exception {
        URN urn = publishPushAltLoc(successful);
        
        AltLocSearchHandler listener = new AltLocSearchHandler();
        // initialize with opposite value to see if it is actually set
        listener.success = !successful;
        
        altLocFinder.findAltLocs(urn, listener);
        listener.doneLatch.await(500, TimeUnit.MILLISECONDS);
        listener.alternateLocationLatch.await(500, TimeUnit.MILLISECONDS);

        // expected alternate location
        AlternateLocation expectedAltLoc = alternateLocationFactory.createPushAltLoc(pushEndpointFactory.createForSelf(), urn);
        
        if (successful) {
            assertEquals(expectedAltLoc, listener.altLoc);
            assertTrue(listener.success);
        } else {
            assertNull(listener.altLoc);
            assertFalse(listener.success);
        }
    }
    
    public void testGetGUIDAlternateLocation() throws Exception {
        URN urn = publishPushAltLoc(true);
        PushAltLoc expectedAltLoc = (PushAltLoc) alternateLocationFactory.createPushAltLoc(pushEndpointFactory.createForSelf(), urn);
        AlternateLocation result = altLocFinder.getAlternateLocation(new GUID(expectedAltLoc.getPushAddress().getClientGUID()), urn);
        assertEquals(expectedAltLoc, result);
    }
    
    public void testGetUnavailableGUIDAlternateLocation() throws Exception {
        URN urn = URN.createSHA1Urn("urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB");
        AlternateLocation result = altLocFinder.getAlternateLocation(new GUID(), urn);
        assertNull(result);
    }
    
    private static class AltLocSearchHandler implements AltLocSearchListener {

        CountDownLatch doneLatch = new CountDownLatch(1);
        CountDownLatch alternateLocationLatch = new CountDownLatch(1);
        volatile AlternateLocation altLoc;
        volatile boolean success;
        
        public void handleAltLocSearchDone(boolean success) {
            this.success = success;
            doneLatch.countDown();
        }

        public void handleAlternateLocation(AlternateLocation alternateLocation) {
            altLoc = alternateLocation;
            alternateLocationLatch.countDown();
        }
    };

}
