package com.limegroup.gnutella;

import junit.framework.*;
import java.util.Properties;
import com.limegroup.gnutella.handshaking.*;
import com.limegroup.gnutella.settings.*;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.security.DummyAuthenticator;
import com.limegroup.gnutella.stubs.MessageRouterStub;
import com.limegroup.gnutella.MiniAcceptor;
import com.limegroup.gnutella.util.*;
import com.sun.java.util.collections.*;

/**
 * PARTIAL unit tests for ConnectionManager.  Makes sure HostCatcher is notified
 * of right events.  VERY slow--involves lots of timeouts--so not part of the
 * standard test suite.  
 */
public class ConnectionManagerTest extends com.limegroup.gnutella.util.BaseTestCase {

    private static final TestHostCatcher CATCHER = new TestHostCatcher();

    private static final RouterService ROUTER_SERVICE =
        new RouterService(new ActivityCallbackStub());

    public ConnectionManagerTest(String name) {
        super(name);        
    }

    public static Test suite() {
        return buildTestSuite(ConnectionManagerTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void setUp() throws Exception {
        if(ROUTER_SERVICE.isStarted()) return;
        launchAllBackends();
        SettingsManager.instance().setPort(6346);
		ConnectionSettings.KEEP_ALIVE.setValue(1);
		ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
		ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);
		ConnectionSettings.USE_GWEBCACHE.setValue(false);
        UltrapeerSettings.FORCE_ULTRAPEER_MODE.setValue(true);
        UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(true);	


        try {
            PrivilegedAccessor.setValue(ROUTER_SERVICE,"catcher",CATCHER);
        } catch(Exception e) {
            fail("could not initialize test", e);
        }

        try {
            PrivilegedAccessor.setValue(ROUTER_SERVICE.getConnectionManager(),
                                        "_catcher",CATCHER);
        } catch(Exception e) {
            fail("could not initialize test", e);
        }

        ROUTER_SERVICE.start();
        RouterService.clearHostCatcher();
    }

    private void failWithServerMessage(Exception e) {
        fail("You must run this test with servers running --\n"+
             "use the test6301 ant target to run LimeWire servers "+
             "on ports 6300 and 6301.\n\n"+
             "Type ant -D\"class=ConnectionManagerTest\" test6301\n\n", e);        
    }

    public void tearDown() {
        //Ensure no more threads.
        RouterService.disconnect();
        RouterService.clearHostCatcher();
        CATCHER.connectSuccess = 0;
        CATCHER.connectFailures = 0;
        CATCHER.endpoint = null;
        sleep();
    }
    
    /**
     * Tests to make sure that a connection does not succeed with an
     * unreachable host.
     */
    public void testUnreachableHost() {
        CATCHER.endpoint = new Endpoint("1.2.3.4", 5000);
        RouterService.connect();
        sleep(10000);
        assertEquals("unexpected successful connect", 0, CATCHER.connectSuccess);
        assertGreaterThan("should have received failures", 0, CATCHER.connectFailures);
    }

    /**
     * Test to make sure tests does not succeed with a host reporting
     * the wrong protocol.
     */
    public void testWrongProtocolHost() {
        CATCHER.endpoint = new Endpoint("www.yahoo.com", 80);
        RouterService.connect();
        sleep();
        assertEquals("unexpected successful connect", 0, CATCHER.connectSuccess);
        assertGreaterThan("should have received failures", 0, CATCHER.connectFailures);
        //assertEquals("should have received failure", 1, CATCHER.connectFailures);
    }

    /**
     * Test to make sure that a good host is successfully connected to.
     */
    public void testGoodHost() {
        CATCHER.endpoint = new Endpoint("localhost", Backend.PORT);
        
        RouterService.connect();
        sleep();
        assertEquals("connect should have succeeded", 1, CATCHER.connectSuccess);
        assertEquals("connect should have failed", 0, CATCHER.connectFailures);
    }


    /**
     * Tests to make sure that a host is still added to the host
     * catcher as a connection that was made (at least temporarily) even
     * if the server sent a 503.
     */
    public void testRejectHost() {
        CATCHER.endpoint = 
            new Endpoint("localhost", Backend.REJECT_PORT);
        RouterService.connect();
        sleep();
        assertEquals("connect should have succeeded", 1, CATCHER.connectSuccess);
        assertEquals("connect should have failed", 0, CATCHER.connectFailures);
    }

    private void sleep() {
        sleep(5000);
    }

    private void sleep(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
        }
    }
    
   public void testComparator() throws Exception {
        TestManagedConnection i13=new TestManagedConnection(false, 1, 3);
        TestManagedConnection o13=new TestManagedConnection(true, 1, 3);
        TestManagedConnection i10=new TestManagedConnection(false, 1, 0);
        TestManagedConnection o10=new TestManagedConnection(true, 1, 0);
        
        List l=new ArrayList(); l.add(i13); l.add(o13); l.add(i10); l.add(o10);
        Collections.sort(l, newManagedConnectionComparator());
        assertSame(o10, l.get(0));
        assertSame(o13, l.get(1));
        assertSame(i10, l.get(2));
        assertSame(i13, l.get(3));
    }
    
    private static Comparator newManagedConnectionComparator() throws Exception {
        Class mcc = PrivilegedAccessor.getClass(
            ConnectionManager.class, "ManagedConnectionComparator" );
        return (Comparator)PrivilegedAccessor.invokeConstructor(
            mcc, null );
    }

    /**
     * Test host catcher that allows us to return endpoints that we 
     * specify when our test framework requests endpoints to connect
     * to.
     */
    private static class TestHostCatcher extends HostCatcher {
        private volatile Endpoint endpoint;
        private volatile int connectSuccess=0;
        private volatile int connectFailures=0;
        
        TestHostCatcher() {
            super();
        }
        
        public synchronized Endpoint getAnEndpoint() throws InterruptedException {
            if (endpoint==null)
                throw new InterruptedException("no endpoint");
            else {
                Endpoint ret=endpoint;
                endpoint=null;
                return ret;
            }
        }
        
        public synchronized void doneWithConnect(Endpoint e, boolean success) {
            if (success)
                connectSuccess++;
            else
                connectFailures++;
        }
        
        // overridden so we ignore gnutella.net for this test
        public void initialize() {        
        }
    }

    private static class TestManagedConnection extends ManagedConnection {
        private boolean isOutgoing;
        private int sent;
        private int received;

        public TestManagedConnection(boolean isOutgoing, int sent, int received) {
            super("1.2.3.4", 6346);
            this.isOutgoing=isOutgoing;
            this.sent=sent;
            this.received=received;
        }

        public boolean isOutgoing() {
            return isOutgoing;
        }

        public int getNumMessagesSent() {
            return sent;
        }
        
        public int getNumMessagesReceived() {
            return received;
        }        
    }    
}
