package com.limegroup.gnutella.bootstrap;

import junit.framework.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.stubs.*;
import com.limegroup.gnutella.util.PrivilegedAccessor;

/**
 * Unit tests for the HostCatcher/BootstrapServerManager interface.
 */
public class HostCatcherFetchTest extends TestCase {
    private HostCatcher hc;
    private RecordingBootstrapServerManager gWebCache;

    public HostCatcherFetchTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(HostCatcherFetchTest.class);
    }

    public void setUp() {
        //Create special TestBootstrapServerManager to record GWebCache hits.
        //Mutate HostCatcher to use this.
        hc=new HostCatcher(new ActivityCallbackStub());
        gWebCache=new RecordingBootstrapServerManager();
        try {
            PrivilegedAccessor.setValue(hc, "gWebCache", gWebCache);
        } catch (Exception e) {
            fail("Couldn't set up test host cache");
        }
        hc.initialize(new AcceptorStub(),
                      new ConnectionManager(null, null),
                      new RouterService(null, null, null, null));
    }
    
    /** Indirectly checks that the GWebCache is hit when there is no
     *  gnutella.net file. */
    public void testGetAnEndpoint_ImmediateFetch() {
        try {
            //Initially catcher is empty.  Calling getAnEndpoint will block, so
            //start thread to add a crap result.
            assertEquals(0, gWebCache.hostfiles);
            Thread responder=new Thread() {
                public void run() {
                    hc.add(new Endpoint("1.1.1.1", 6346), false);
                    hc.add(new Endpoint("1.1.1.2", 6346), false);
                    hc.add(new Endpoint("1.1.1.3", 6346), false);
                }
            };
            responder.start();
            //Now make sure that exactly one fetch was issued.
            assertTrue(hc.getAnEndpoint()!=null);
            assertEquals(1, gWebCache.hostfiles);
            //And no more are done.
            assertTrue(hc.getAnEndpoint()!=null);
            assertEquals(2, gWebCache.hostfiles);
        } catch (InterruptedException e) {
            e.printStackTrace();
            fail("Interrupted!");
        }
    }

    /** Indirectly checks that the GWebCache isn't initially hit when there is a
     *  gnutella.net file. */
    public void testGetAnEndpoint_DelayedFetch() {        
        try {
            //Fill up hc with crap pongs.  No fetches are allowed initially.
            for (int i=0; i<20; i++) 
                hc.add(new Endpoint("1.1.1."+i, 6346+i), false);
            assertTrue(hc.getAnEndpoint()!=null);
            assertTrue(hc.getAnEndpoint()!=null);
            assertEquals(0, gWebCache.hostfiles);

            //But after a few seconds, we allow another fetch because there are
            //no good pongs.
            final int FUDGE_FACTOR=200;
            sleep(HostCatcher.GWEBCACHE_DELAY+FUDGE_FACTOR);
            assertTrue(hc.getAnEndpoint()!=null);
            assertEquals(1, gWebCache.hostfiles);
            assertTrue(hc.getAnEndpoint()!=null);
            assertEquals(1, gWebCache.hostfiles);
        
            //Same after another few seconds.
            sleep(HostCatcher.GWEBCACHE_DELAY+FUDGE_FACTOR);
            assertTrue(hc.getAnEndpoint()!=null);
            assertEquals(2, gWebCache.hostfiles);
            assertTrue(hc.getAnEndpoint()!=null);
            assertEquals(2, gWebCache.hostfiles);
        } catch (InterruptedException e) {
            e.printStackTrace();
            fail("Interrupted!");
        }
    }

    private void sleep(int msecs) {
        try {
            Thread.sleep(msecs);
        } catch (InterruptedException e) {
            fail("Interrupted!");
        }
    }
}

/** Doesn't actually connect; just records HostCatcher's attempts. */
class RecordingBootstrapServerManager extends BootstrapServerManager {
    int urlfiles=0;
    int hostfiles=0;
    int updates=0;
    
    public RecordingBootstrapServerManager() {
        super(null);
    }

    public synchronized void fetchBootstrapServersAsync() { urlfiles++; }
    public synchronized void fetchEndpointsAsync() { hostfiles++; }
    public synchronized void sendUpdatesAsync(Endpoint myIP) { updates++; }
}
