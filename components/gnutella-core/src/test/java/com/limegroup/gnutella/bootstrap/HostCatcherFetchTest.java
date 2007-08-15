package com.limegroup.gnutella.bootstrap;

import org.limewire.util.PrivilegedAccessor;

import junit.framework.Test;

import com.limegroup.gnutella.Endpoint;
import com.limegroup.gnutella.HostCatcher;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.LimeTestCase;

/**
 * Unit tests for the HostCatcher/BootstrapServerManager interface.
 */
public class HostCatcherFetchTest extends LimeTestCase {
    private HostCatcher hc;
    private RecordingBootstrapServerManager gWebCache;

    public HostCatcherFetchTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(HostCatcherFetchTest.class);
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public static void globalSetUp() throws Exception {
        //we don't actually need the service, we just need it
        // to start up the other services.
        new RouterService( new ActivityCallbackStub() );
    }

    public void setUp() throws Exception {
        FilterSettings.BLACK_LISTED_IP_ADDRESSES.setValue(new String[0]);

        //Create special TestBootstrapServerManager to record GWebCache hits.
        gWebCache=new RecordingBootstrapServerManager();

        hc=new HostCatcher();
        hc.initialize();

        //Mutate HostCatcher to use this.
        PrivilegedAccessor.setValue(hc, "gWebCache", gWebCache);
    }
    
    /** Indirectly checks that the GWebCache is hit when there is no
     *  gnutella.net file. */
    public void testGetAnEndpoint_ImmediateFetch() throws Exception {
        //Initially catcher is empty.  Calling getAnEndpoint will block, so
        //start thread to add a crap result.
        assertEquals("initial hostfiles not empty.", 0, gWebCache.hostfiles);

        //Now make sure that exactly one fetch was issued.
        try {
            interrupt(10);
            assertNotNull("getAnEndpoint didn't return anything.", hc.getAnEndpoint());
            fail("didn't get interrupted exception");
        } catch(InterruptedException expected) {}
        assertEquals("first look at hostfiles", 1, gWebCache.hostfiles);
        
        // now add something to the hostCatcher and make sure
        // it uses that instead of hitting the gWebCache for more.
        hc.add( new Endpoint("1.1.1.2", 6346), false);
        assertNotNull("getAnEndpoint didn't return anything.", hc.getAnEndpoint());
        assertEquals("second look at hostfiles", 1, gWebCache.hostfiles);
        
        // make sure we tell the scheduled fetcher it's okay to fetch again
        hc.recoverHosts();
        try {
            interrupt(10);
            assertNotNull("getAnEndpoint didn't return anything.", hc.getAnEndpoint());
            fail("no exception");
        } catch(InterruptedException expected) {}
        assertEquals("third look at hostfiles", 2, gWebCache.hostfiles);
    }

    /** Indirectly checks that the GWebCache isn't initially hit when there is a
     *  gnutella.net file. */
    public void testGetAnEndpoint_DelayedFetch() throws Exception {
        //Fill up hc with crap pongs.
        for (int i=0; i<20; i++) 
            hc.add(new Endpoint("1.1.1."+i, 6346+i), false);
            
        //The first few calls (all those before GWEBCACHE_DELAY)
        //will be forced to use the stale pongs...
        //because it is bad to always hammer gWebCache's on startup
        assertNotNull(hc.getAnEndpoint());
        assertNotNull(hc.getAnEndpoint());
        assertEquals(0, gWebCache.hostfiles);
        

        //Make sure we use up all our endpoints before we
        //hit a gwebcache, because it's always bad to hit a gwebcache
        sleep(15 * 1000);
        assertNotNull(hc.getAnEndpoint());
        assertEquals(0, gWebCache.hostfiles);
        assertNotNull(hc.getAnEndpoint());
        assertEquals(0, gWebCache.hostfiles);
    
        //Same after another few seconds.
        sleep(15 * 1000);
        assertNotNull(hc.getAnEndpoint());
        assertEquals(0, gWebCache.hostfiles);
        assertNotNull(hc.getAnEndpoint());
        assertEquals(0, gWebCache.hostfiles);
        
        //get the other endpoints.
        for(int i = 6; i < 20; i++)
            assertNotNull(hc.getAnEndpoint());
            
        // now we should hit the gwebcache.
        try {
            hc.recoverHosts();
            interrupt(10);
            hc.getAnEndpoint();
            fail("got an endpoint");
        } catch(InterruptedException expected) {}
        assertEquals(1, gWebCache.hostfiles);
    }
    
    private void interrupt(final int secs) {
        final Thread thisThread = Thread.currentThread();
        Thread responder=new Thread() {
            public void run() {
                // sleep & then interrupt the endpoint get.
                try {
                    sleep(secs * 1000);
                } catch(InterruptedException ignored) {}
                thisThread.interrupt();
            }
        };
        responder.start();
    }

    private void sleep(int msecs) throws Exception {
        Thread.sleep(msecs);
    }
}

/** Doesn't actually connect; just records HostCatcher's attempts. */
class RecordingBootstrapServerManager extends BootstrapServerManager {
    int urlfiles=0;
    int hostfiles=0;
    int updates=0;
    
    public RecordingBootstrapServerManager() {
        super();
    }

    public synchronized void fetchBootstrapServersAsync() { urlfiles++; }
    public synchronized int fetchEndpointsAsync() {
            hostfiles++;
            return FETCH_SCHEDULED;
    }
    public synchronized void sendUpdatesAsync(Endpoint myIP) { updates++; }
}
