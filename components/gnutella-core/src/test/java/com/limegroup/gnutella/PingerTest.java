package com.limegroup.gnutella;

import junit.framework.Test;

import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.stubs.MessageRouterStub;
import com.limegroup.gnutella.util.LeafConnectionManager;
import com.limegroup.gnutella.util.LimeTestCase;
import com.limegroup.gnutella.util.UltrapeerConnectionManager;

/**
 * Tests the <tt>Pinger</tt> class that periodically sends pings to gather new
 * host data.
 */
public final class PingerTest extends LimeTestCase {


    public PingerTest(String name) {
        super(name);        
    }

    public static Test suite() {
        return buildTestSuite(PingerTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public static void globalSetUp() throws Exception {
        //ROUTER_SERVICE = new RouterService(new ActivityCallbackStub());
    }

    /**
     * Test to make sure that we're correctly sending out periodic pings
     * from the pinger as an Ultrapeer (we should not be sending these
     * periodic pings as a leaf).
     */
    public void testPeriodicUltrapeerPings() throws Exception {
        TestMessageRouter mr = new TestMessageRouter();
        @SuppressWarnings("all") // DPINJ: textfix
        ConnectionManager cm = new UltrapeerConnectionManager();
     //   PrivilegedAccessor.setValue(RouterService.class, "messageRouter", mr);
     //   PrivilegedAccessor.setValue(RouterService.class, "manager", cm);
        Pinger pinger = ProviderHacks.getPinger();
        pinger.start();

        synchronized(mr) {
            while(mr.getNumPings() < 2) {
                mr.wait(7000);
            }
        }
        
        // don't know exactly how many have been sent because of thread
        // scheduling, but just make sure they're going out
        assertTrue("unexpected number of ping sends: "+mr.getNumPings(), 
                   mr.getNumPings()>=2);
    }

    /**
     * Test to make sure that we're not sending out periodic pings as a 
     * leaf.
     */
    public void testPeriodicLeafPings() throws Exception {
        TestMessageRouter mr = new TestMessageRouter();
        @SuppressWarnings("all") // DPINJ: textfix
        ConnectionManager cm = new LeafConnectionManager();
      //  PrivilegedAccessor.setValue(RouterService.class, "messageRouter", mr);
     //   PrivilegedAccessor.setValue(RouterService.class, "manager", cm);
        Pinger pinger = ProviderHacks.getPinger();
        pinger.start();


        Thread.sleep(8000);

        
        
        // we should not have sent any out, since we're a leaf
        assertEquals("unexpected number of ping sends", 0, mr.getNumPings()); 
    }

    /**
     * Test class for making sure that <tt>MessageRouter</tt> receives the
     * expected calls.
     */
    private final class TestMessageRouter extends MessageRouterStub {

        private volatile int _numPings = 0;

        public void broadcastPingRequest(PingRequest ping) {
            _numPings++;
            
            // notify any waiting threads that we have received a ping request
            synchronized(this) {
                notifyAll();
            }
        }

        int getNumPings() {
            return _numPings;
        }
    }
}
