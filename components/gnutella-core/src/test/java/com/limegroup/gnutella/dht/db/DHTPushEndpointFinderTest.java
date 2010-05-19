package com.limegroup.gnutella.dht.db;

import java.util.concurrent.ExecutionException;

import junit.framework.Test;

import org.limewire.io.GUID;

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

    /*public void testGetPushEndPoint() throws Exception {
        // publish push proxy manually
        PushProxiesValue pushProxiesValue = pushProxiesValueFactory.createDHTValueForSelf();
        mojitoDHT.put(KUIDUtils.toKUID(new GUID(pushProxiesValue.getGUID())), pushProxiesValue).get();

        PushEndpoint pushEndpoint = pushEndpointFinder.getPushEndpoint(new GUID(pushProxiesValue.getGUID()));
        // only compare values, so we don't have to recreate the push endpoint
        assertEquals(pushProxiesValue.getGUID(), pushEndpoint.getClientGUID());
        assertEquals(pushProxiesValue.getPushProxies(), pushEndpoint.getProxies());
        assertEquals(pushProxiesValue.getFeatures(), pushEndpoint.getFeatures());
        assertEquals(pushProxiesValue.getFwtVersion(), pushEndpoint.getFWTVersion());
    }*/
    
    public void testGetUnavailablePushEndpoint() {
        try {
            pushEndpointFinder.findPushEndpoint(new GUID()).get();
            fail("Should have failed!");
        } catch (InterruptedException e) {
            fail(e);
        } catch (ExecutionException expected) {
            expected.printStackTrace();
        }
    }
    
}
