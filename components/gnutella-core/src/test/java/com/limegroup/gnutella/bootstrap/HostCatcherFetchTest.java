package com.limegroup.gnutella.bootstrap;

import junit.framework.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.stubs.*;
import com.limegroup.gnutella.util.PrivilegedAccessor;

/**
 * Unit tests for the HostCatcher/BootstrapServerManager interface.
 */
public class HostCatcherFetchTest extends com.limegroup.gnutella.util.BaseTestCase {
    private HostCatcher hc;
    private RecordingBootstrapServerManager gWebCache;

    public HostCatcherFetchTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(HostCatcherFetchTest.class);
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void setUp() {
        SettingsManager.instance().setBannedIps(new String[0]);
        // we don't actually need the service, we just need it
        // to start up the other services.
        new RouterService( new ActivityCallbackStub() );

        //Create special TestBootstrapServerManager to record GWebCache hits.
        gWebCache=new RecordingBootstrapServerManager();

        hc=new HostCatcher();

        //Mutate HostCatcher to use this.
        try {
            PrivilegedAccessor.setValue(hc, "gWebCache", gWebCache);
        } catch (Exception e) {
            fail("Couldn't set up test host cache");
        }
    }
    
    /** Indirectly checks that the GWebCache is hit when there is no
     *  gnutella.net file. */
    public void testGetAnEndpoint_ImmediateFetch() {
        try {
            //Initially catcher is empty.  Calling getAnEndpoint will block, so
            //start thread to add a crap result.
            assertEquals("initial hostfiles not empty.", 0, gWebCache.hostfiles);
            
            Thread responder=new Thread() {
                public void run() {
                    //we must yield to ensure that getAnEndpoint
                    //is called before the endpoint is added to
                    //the hostCatcher, forcing a call to the gWebCache
                    yield();
                    hc.add(new Endpoint("1.1.1.1", 6346), false);
                }
            };
            responder.start();

            //Now make sure that exactly one fetch was issued.
            assertTrue("getAnEndpoint didn't return anything.", hc.getAnEndpoint()!=null);
            assertEquals("first look at hostfiles", 1, gWebCache.hostfiles);
            
            // now add something to the hostCatcher and make sure
            // it uses that instead of hitting the gWebCache for more.
            hc.add( new Endpoint("1.1.1.2", 6346), false);
            assertTrue("getAnEndpoint didn't return anything.", hc.getAnEndpoint()!=null);
            assertEquals("second look at hostfiles", 1, gWebCache.hostfiles);
            
            //and now that hostcatcher is empty, it should hit it again.
            responder = new Thread() {
                public void run() {
                    yield();
                    hc.add(new Endpoint("1.1.1.3", 6346), false);
                }
            };
            responder.start();
            assertTrue("getAnEndpoint didn't return anything.", hc.getAnEndpoint()!=null);
            assertEquals("third look at hostfiles", 2, gWebCache.hostfiles);
            
        } catch (InterruptedException e) {
            e.printStackTrace();
            fail("Interrupted!");
        }
    }

    /** Indirectly checks that the GWebCache isn't initially hit when there is a
     *  gnutella.net file. */
    public void testGetAnEndpoint_DelayedFetch() {
        try {
            //Fill up hc with crap pongs.
            for (int i=0; i<20; i++) 
                hc.add(new Endpoint("1.1.1."+i, 6346+i), false);
                
            //The first few calls (all those before GWEBCACHE_DELAY)
            //will be forced to use the stale pongs...
            //because it is bad to always hammer gWebCache's on startup
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
