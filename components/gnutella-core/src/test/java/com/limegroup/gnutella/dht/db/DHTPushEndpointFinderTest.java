package com.limegroup.gnutella.dht.db;

import java.util.Set;
import java.util.concurrent.ExecutionException;

import junit.framework.Test;

import org.limewire.io.GUID;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.io.IpPortSet;
import org.limewire.mojito2.KUID;

import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.dht.util.KUIDUtils;

public class DHTPushEndpointFinderTest extends DHTFinderTestCase {

    private DHTPushEndpointFinder pushEndpointFinder;

    public DHTPushEndpointFinderTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(DHTPushEndpointFinderTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        pushEndpointFinder = injector.getInstance(DHTPushEndpointFinder.class);
    }

    public void testGetPushEndPoint() throws Exception {
        // publish push proxy manually
        
        byte[] guid = GUID.makeGuid();
        byte features = 0;
        int fwtVersion = 0;
        int port = 6969;
        Set<? extends IpPort> proxies = new IpPortSet(
                new IpPortImpl("localhost", 1234));
        
        PushProxiesValue value = new DefaultPushProxiesValue(
                PushProxiesValue.VERSION,
                guid, features, fwtVersion, port, proxies);
        
        KUID key = KUIDUtils.toKUID(new GUID(value.getGUID()));
        
        mojitoDHT.put(key, value.serialize()).get();

        PushEndpoint pushEndpoint = pushEndpointFinder.findPushEndpoint(
                new GUID(value.getGUID())).get();
        
        // only compare values, so we don't have to recreate the push endpoint
        assertEquals(value.getGUID(), pushEndpoint.getClientGUID());
        assertEquals(value.getPushProxies(), pushEndpoint.getProxies());
        assertEquals(value.getFeatures(), pushEndpoint.getFeatures());
        assertEquals(value.getFwtVersion(), pushEndpoint.getFWTVersion());
    }
    
    public void testGetUnavailablePushEndpoint() {
        try {
            pushEndpointFinder.findPushEndpoint(new GUID()).get();
            fail("Should have failed!");
        } catch (InterruptedException e) {
            fail(e);
        } catch (ExecutionException expected) {
        }
    }
}
