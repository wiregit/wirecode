package com.limegroup.gnutella;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import junit.framework.Test;

import org.limewire.util.PrivilegedAccessor;

import com.limegroup.gnutella.HostCatcher.EndpointObserver;
import com.limegroup.gnutella.connection.ConnectionLifecycleEvent;
import com.limegroup.gnutella.connection.ConnectionLifecycleListener;
import com.limegroup.gnutella.connection.OutputRunner;
import com.limegroup.gnutella.handshaking.HandshakeResponse;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.settings.ApplicationSettings;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.SSLSettings;
import com.limegroup.gnutella.settings.UltrapeerSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.LimeTestCase;
import com.limegroup.gnutella.util.SocketsManager.ConnectType;

/**
 * PARTIAL unit tests for ConnectionManager.  Makes sure HostCatcher is notified
 * of right events.  
 */
@SuppressWarnings("all")
public class ConnectionManagerTest extends LimeTestCase {

    private static TestHostCatcher CATCHER;
    private static RouterService ROUTER_SERVICE;
    private static ConnectionListener LISTENER;

    public ConnectionManagerTest(String name) {
        super(name);        
    }

    public static Test suite() {
        return buildTestSuite(ConnectionManagerTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public static void globalSetUp() throws Exception {
        ROUTER_SERVICE =
            new RouterService(new ActivityCallbackStub());
        CATCHER = new TestHostCatcher();
        LISTENER = new ConnectionListener();
        
        RouterService.getConnectionManager().addEventListener(LISTENER);
        
        setSettings();
        launchAllBackends();
                
        PrivilegedAccessor.setValue(ROUTER_SERVICE,"catcher",CATCHER);       

        PrivilegedAccessor.setValue(RouterService.getConnectionManager(),
                                    "_catcher",CATCHER);
             
        ROUTER_SERVICE.start();
        RouterService.clearHostCatcher();
    }

    public void setUp() throws Exception {
        setSettings();
        
        //  Currently, there are no default EVIL_HOSTS useragents.  to test this, we need
        //      to pick on someone, so it will be Morpheus =)
        String [] agents = {"morpheus"};
        ConnectionSettings.EVIL_HOSTS.setValue( agents );
        
        CATCHER.resetLatches();
        CATCHER.endpoint = null;
    }
    
    private static void setSettings() throws Exception {
        ConnectionSettings.PORT.setValue(6346);
		ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
		ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);
        UltrapeerSettings.FORCE_ULTRAPEER_MODE.setValue(true);
        UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(true);
        ConnectionSettings.PREFERENCING_ACTIVE.setValue(false);
        UltrapeerSettings.NEED_MIN_CONNECT_TIME.setValue(true);
        ApplicationSettings.TOTAL_CONNECTION_TIME.setValue(0);
        ApplicationSettings.AVERAGE_CONNECTION_TIME.setValue(0);
    }

    public void tearDown() throws Exception {
        //Kill all connections
        RouterService.disconnect();
        RouterService.clearHostCatcher();
        Thread.sleep(500);
    }
    
    /**
     * Tests the method for allowing ultrapeer 2 ultrapeer connections.
     * 
     * @throws Exception if an error occurs
     */
    public void testAllowUltrapeer2UltrapeerConnection() throws Exception {
        Method m = PrivilegedAccessor.getMethod(ConnectionManager.class,
            "allowUltrapeer2UltrapeerConnection", 
            new Class[] {HandshakeResponse.class});
        
        
        HandshakeResponse hr = createTestResponse("Morpheus 3.3");
        Object[] params = new Object[] {hr};
        boolean allow =
            ((Boolean)m.invoke(ConnectionManager.class, params)).booleanValue();
        
        assertFalse("connection should not have been allowed", allow);
        
        hr = createTestResponse("Bearshare 3.3");
        params[0] = hr;
        allow =
            ((Boolean)m.invoke(ConnectionManager.class, params)).booleanValue();
        
        assertTrue("connection should have been allowed", allow);
        
        hr = createTestResponse("LimeWire 3.3");
        params[0] = hr;
        allow =
            ((Boolean)m.invoke(ConnectionManager.class, params)).booleanValue();
        
        assertTrue("connection should have been allowed", allow);
        
        hr = createTestResponse("Shareaza 3.3");
        params[0] = hr;
        allow =
            ((Boolean)m.invoke(ConnectionManager.class, params)).booleanValue();
        
        assertTrue("connection should have been allowed", allow);
    }
    
    /**
     * Tests the method for allowing ultrapeer 2 leaf connections.
     * 
     * @throws Exception if an error occurs
     */
    public void testAllowUltrapeer2LeafConnection() throws Exception {
        Method m = PrivilegedAccessor.getMethod(ConnectionManager.class,
            "allowUltrapeer2LeafConnection", 
            new Class[] {HandshakeResponse.class});
        
        
        HandshakeResponse hr = createTestResponse("Morpheus 3.3");
        Object[] params = new Object[] {hr};
        boolean allow =
            ((Boolean)m.invoke(ConnectionManager.class, params)).booleanValue();
        
        assertFalse("connection should not have been allowed", allow);
        
        hr = createTestResponse("Bearshare 3.3");
        params[0] = hr;
        allow =
            ((Boolean)m.invoke(ConnectionManager.class, params)).booleanValue();
        
        assertTrue("connection should have been allowed", allow);
        
        hr = createTestResponse("LimeWire 3.3");
        params[0] = hr;
        allow =
            ((Boolean)m.invoke(ConnectionManager.class, params)).booleanValue();
        
        assertTrue("connection should have been allowed", allow);
        
        hr = createTestResponse("Shareaza 3.3");
        params[0] = hr;
        allow =
            ((Boolean)m.invoke(ConnectionManager.class, params)).booleanValue();
        
        assertTrue("connection should have been allowed", allow);
    }    

    /**
     * Utility method for creating a set of headers with the specified user
     * agent value.
     * 
     * @param userAgent the User-Agent to include in the headers
     * @return a new <tt>HandshakeResponse</tt> with the specified user agent
     * @throws IOException if an error occurs
     */
    private static HandshakeResponse createTestResponse(String userAgent) 
        throws IOException {
        Properties headers = new Properties();
        headers.put("User-Agent", userAgent);
        return HandshakeResponse.createResponse(headers);
    }
    
    public void testSupernodeStatus() throws Exception {
        UltrapeerSettings.FORCE_ULTRAPEER_MODE.setValue(false);
        UltrapeerSettings.NEED_MIN_CONNECT_TIME.setValue(true);
        
        ConnectionManager mgr = RouterService.getConnectionManager();
        
        // test preconditions
        assertTrue("should not start as supernode", !mgr.isSupernode());
        assertTrue("should not be a shielded leaf", !mgr.isShieldedLeaf());
        UltrapeerSettings.MIN_CONNECT_TIME.setValue(0);
        setConnectTime();
        assertTrue("should start as supernode", mgr.isSupernode());
        assertTrue("should not be leaf", !mgr.isShieldedLeaf());        
        
        // construct peers
        // u ==> i should be ultrapeer
        // l ==> i should be leaf
        ManagedConnection u1, l1, l2;
        u1 = new SupernodeClient(true);
        l1 = new ClientSupernode(true);
        l2 = new ClientSupernode(true);
        
        // add a supernode => client connection
        initializeStart(u1);
        assertTrue("should still be supernode", mgr.isSupernode());
        assertTrue("should still not be leaf", !mgr.isShieldedLeaf());
        initializeDone(u1);
        assertTrue("should still be supernode", mgr.isSupernode());
        assertTrue("should still not be leaf", !mgr.isShieldedLeaf());
        mgr.remove(u1);
        assertTrue("should still be supernode", mgr.isSupernode());
        assertTrue("should still not be leaf", !mgr.isShieldedLeaf());
        
        
        // add a leaf -> supernode connection
        initializeStart(l1);
        assertTrue("should still be supernode", mgr.isSupernode());
        assertTrue("should still not be leaf", !mgr.isShieldedLeaf());
        initializeDone(l1);
        assertTrue("should not be supernode", !mgr.isSupernode());
        assertTrue("should be leaf", mgr.isShieldedLeaf());
        mgr.remove(l1);
        assertTrue("should be supernode", mgr.isSupernode());
        assertTrue("should not be leaf", !mgr.isShieldedLeaf());
        
        // test a strange condition
        // (two leaves start, second finishes then removes,
        //  third removes then finishes)
        initializeStart(l1);
        initializeStart(l2);
        initializeDone(l2);
        assertTrue("should not be supernode", !mgr.isSupernode());
        assertTrue("should be leaf", mgr.isShieldedLeaf());
        mgr.remove(l2);
        assertTrue("should be supernode", mgr.isSupernode());
        assertTrue("should not be leaf", !mgr.isShieldedLeaf());
        mgr.remove(l1);
        assertTrue("should be supernode", mgr.isSupernode());
        assertTrue("should not be leaf", !mgr.isShieldedLeaf());
        initializeDone(l1);
        assertTrue("should be supernode", mgr.isSupernode());
        assertTrue("should not be leaf", !mgr.isShieldedLeaf());
    }
    
    /**
     * Tests the various connection-counting methods
     */
    public void testGetNumberOfConnections() throws Exception {
        ManagedConnection[] limeLeaves = new ManagedConnection[30];
        ManagedConnection[] nonLimeLeaves = new ManagedConnection[30];
        ManagedConnection[] limePeers = new ManagedConnection[32];
        ManagedConnection[] nonLimePeers = new ManagedConnection[32];
        
        for(int i = 0; i < limeLeaves.length; i++)
            limeLeaves[i] = new SupernodeClient(true);
        for(int i = 0; i < nonLimeLeaves.length; i++)
            nonLimeLeaves[i] = new SupernodeClient(false);
        for(int i = 0; i < limePeers.length; i++)
            limePeers[i] = new SupernodeSupernode(true);
        for(int i = 0; i < nonLimePeers.length; i++)
            nonLimePeers[i] = new SupernodeSupernode(false);
            
        UltrapeerSettings.FORCE_ULTRAPEER_MODE.setValue(true);
        ConnectionManager mgr = RouterService.getConnectionManager();
        setConnectTime();
        // test preconditions
        assertTrue("should start as supernode", mgr.isSupernode());
        assertTrue("should not be leaf", !mgr.isShieldedLeaf());
        pretendConnected();
        
        assertEquals(32, mgr.getNumFreeNonLeafSlots());
        assertEquals(27, mgr.getNumFreeLimeWireNonLeafSlots());
        assertEquals(30, mgr.getNumFreeLeafSlots());
        assertEquals(28, mgr.getNumFreeLimeWireLeafSlots());
        
        // Add 10 limewire leaves and 5 limewire peers.
        for(int i = 0; i < 10; i++) {
            initializeStart(limeLeaves[i]);
            initializeDone(limeLeaves[i]);
        }
        for(int i = 0; i < 5; i++) {
            initializeStart(limePeers[i]);
            initializeDone(limePeers[i]);
        }
        // Are now 10 lime leaves & 5 lime peers.
        // Equaling: 10 leaves & 5 peers.
        assertEquals(20, mgr.getNumFreeLeafSlots());
        assertEquals(18, mgr.getNumFreeLimeWireLeafSlots());
        assertEquals(27, mgr.getNumFreeNonLeafSlots());
        assertEquals(24, mgr.getNumFreeLimeWireNonLeafSlots());
        
        // Add 1 non lime leaf and 2 non limewire peers.
        for(int i = 0; i < 1; i++) {
            initializeStart(nonLimeLeaves[i]);
            initializeDone(nonLimeLeaves[i]);
        }
        for(int i = 0; i < 2; i++) {
            initializeStart(nonLimePeers[i]);
            initializeDone(nonLimePeers[i]);
        }
        // Are now 10 lime leaves, 1 non lime leaf, 5 lime peers, 2 non lime peers
        // Equaling: 11 leaves & 7 peers
        assertEquals(19, mgr.getNumFreeLeafSlots());
        assertEquals(18, mgr.getNumFreeLimeWireLeafSlots());
        assertEquals(25, mgr.getNumFreeNonLeafSlots());
        assertEquals(24, mgr.getNumFreeLimeWireNonLeafSlots());
        
        // Add a bunch of non lime peers & leaves.
        for(int i = 1; i < 15; i++) {
            initializeStart(nonLimeLeaves[i]);
            initializeDone(nonLimeLeaves[i]);
        }
        for(int i = 2; i < 15; i++) {
            initializeStart(nonLimePeers[i]);
            initializeDone(nonLimePeers[i]);
        }
        // There are now 15 non lime leaves & non lime peers connected,
        // 10 lime leaves, and 5 lime peers.
        // Equaling: 25 leaves and 20 peers.
        assertEquals(5, mgr.getNumFreeLeafSlots());
        assertEquals(5, mgr.getNumFreeLimeWireLeafSlots());
        assertEquals(12, mgr.getNumFreeNonLeafSlots());
        assertEquals(12, mgr.getNumFreeLimeWireNonLeafSlots());
        
        // Add 5 lime leaves and 12 lime peers
        for(int i = 10; i < 15; i++) {
            initializeStart(limeLeaves[i]);
            initializeDone(limeLeaves[i]);
        }
        for(int i = 5; i < 17; i++) {
            initializeStart(limePeers[i]);
            initializeDone(limePeers[i]);
        }
        // There are now 15 non lime leaves & non lime peers,
        // 15 lime leaves, and 17 lime peers.
        // Equaling: 30 leaves and 32 peers.
        assertEquals(0, mgr.getNumFreeLeafSlots());
        assertEquals(0, mgr.getNumFreeLimeWireLeafSlots());
        assertEquals(0, mgr.getNumFreeNonLeafSlots());
        assertEquals(0, mgr.getNumFreeLimeWireNonLeafSlots());
        
        // Now kill all but 2 of the non lime leaves
        // and all but 3 of non lime peers.
        for(int i = 14; i >= 2; i--)
            mgr.remove(nonLimeLeaves[i]);
        for(int i = 14; i >= 3; i--)
            mgr.remove(nonLimePeers[i]);
        // There are now 2 non lime leaves and 3 non lime peers,
        // and 15 lime leaves and 17 lime peers.
        // Equaling: 17 leaves and 20 peers
        assertEquals(13, mgr.getNumFreeLeafSlots());
        assertEquals(13, mgr.getNumFreeLimeWireLeafSlots());
        assertEquals(12, mgr.getNumFreeNonLeafSlots());
        assertEquals(12, mgr.getNumFreeLimeWireNonLeafSlots());
        
        // Now add 13 lime leaves and 12 lime peers.
        for(int i = 15; i < 28; i++) {
            initializeStart(limeLeaves[i]);
            initializeDone(limeLeaves[i]);
        }
        for(int i = 17; i < 29; i++) {
            initializeStart(limePeers[i]);
            initializeDone(limePeers[i]);
        }
        // There are now 2 non lime leaves and 3 non lime peers,
        // and 28 lime leaves and 29 lime peers
        // Equaling: 30 leaves and 32 peers
        assertEquals(0, mgr.getNumFreeLeafSlots());
        assertEquals(0, mgr.getNumFreeLimeWireLeafSlots());
        assertEquals(0, mgr.getNumFreeNonLeafSlots());
        assertEquals(0, mgr.getNumFreeLimeWireNonLeafSlots());
        
        // Now kill off the rest of the non lime peers and leaves.
        for(int i = 1; i >= 0; i--)
            mgr.remove(nonLimeLeaves[i]);
        for(int i = 2; i >= 0; i--)
            mgr.remove(nonLimePeers[i]);
        // There are now no non lime leaves and no non lime peers,
        // and 28 lime leaves and 29 lime peers
        // Equaling: 28 leaves and 29 peers
        assertEquals(2, mgr.getNumFreeLeafSlots());
        assertEquals(0, mgr.getNumFreeLimeWireLeafSlots());
        assertEquals(3, mgr.getNumFreeNonLeafSlots());
        assertEquals(0, mgr.getNumFreeLimeWireNonLeafSlots());
    }
        
        
    
    /**
     * Tests to make sure that a connection does not succeed with an
     * unreachable host.
     */
    public void testUnreachableHost() throws Exception {
        CATCHER.endpoint = new ExtendedEndpoint("1.2.3.4", 5000);
        RouterService.connect();
        assertTrue(CATCHER.waitForFailure(10000));
        assertEquals(0, CATCHER.getSuccessCount());
    }

    /**
     * Test to make sure tests does not succeed with a host reporting
     * the wrong protocol.
     */
    public void testWrongProtocolHost() throws Exception {
        CATCHER.endpoint = new ExtendedEndpoint("www.yahoo.com", 80);
        RouterService.connect();
        assertTrue(CATCHER.waitForFailure(10000));
        assertEquals(0, CATCHER.getSuccessCount());
    }

    /**
     * Test to make sure that a good host is successfully connected to.
     */
    public void testGoodHost() throws Exception {
        CATCHER.endpoint = new ExtendedEndpoint("localhost", Backend.BACKEND_PORT);        
        RouterService.connect();
        assertTrue(CATCHER.waitForSuccess(5000));
        assertEquals(0, CATCHER.getFailureCount());
        
        Thread.sleep(1000); // let capVM send
        
        assertEquals(1, RouterService.getConnectionManager().getInitializedConnections().size());
        ManagedConnection mc = RouterService.getConnectionManager().getInitializedConnections().get(0);
        assertTrue(mc.isTLSCapable());
        assertFalse(mc.isTLSEncoded());
    }
    
    public void testGoodTLSHost() throws Exception {
        SSLSettings.TLS_OUTGOING.setValue(true);
        CATCHER.endpoint = new ExtendedEndpoint("localhost", Backend.BACKEND_PORT);
        CATCHER.endpoint.setTLSCapable(true);        
        RouterService.connect();
        assertTrue(CATCHER.waitForSuccess(5000));
        assertEquals(0, CATCHER.getFailureCount());

        Thread.sleep(1000); // let capVM send
        
        assertEquals(1, RouterService.getConnectionManager().getInitializedConnections().size());
        ManagedConnection mc = RouterService.getConnectionManager().getInitializedConnections().get(0);
        assertTrue(mc.isTLSCapable());
        assertTrue(mc.isTLSEncoded());
    }
    
    public void testGoodTLSHostNotUsedIfNoSetting() throws Exception {
        SSLSettings.TLS_OUTGOING.setValue(false);
        CATCHER.endpoint = new ExtendedEndpoint("localhost", Backend.BACKEND_PORT);
        CATCHER.endpoint.setTLSCapable(true);        
        RouterService.connect();
        assertTrue(CATCHER.waitForSuccess(5000));
        assertEquals(0, CATCHER.getFailureCount());

        Thread.sleep(1000); // let capVM send
        
        assertEquals(1, RouterService.getConnectionManager().getInitializedConnections().size());
        ManagedConnection mc = RouterService.getConnectionManager().getInitializedConnections().get(0);
        assertTrue(mc.isTLSCapable());
        assertFalse(mc.isTLSEncoded());
    }


    /**
     * Tests to make sure that a host is still added to the host
     * catcher as a connection that was made (at least temporarily) even
     * if the server sent a 503.
     */
    public void testRejectHost() throws Exception {
        CATCHER.endpoint =  new ExtendedEndpoint("localhost", Backend.REJECT_PORT);
        RouterService.connect();
        assertTrue(CATCHER.waitForSuccess(5000));
        assertEquals(0, CATCHER.getFailureCount());
    }
    
    public void testRecordConnectionTime() throws Exception{
        ApplicationSettings.AVERAGE_CONNECTION_TIME.setValue(0);
        ApplicationSettings.TOTAL_CONNECTION_TIME.setValue(0);
        ApplicationSettings.TOTAL_CONNECTIONS.setValue(0);
        ConnectionManager mgr = RouterService.getConnectionManager();
        assertFalse(mgr.isConnected());
        CATCHER.endpoint = new ExtendedEndpoint("localhost", Backend.BACKEND_PORT);
        //try simple connect-disconnect
        mgr.connect();
        LISTENER.getLock().lock();
        try {
            mgr.connect();
            LISTENER.waitForInitialized(2000);
        } finally {
            LISTENER.getLock().unlock();
        }
        mgr.disconnect(false);
        long totalConnect = ApplicationSettings.TOTAL_CONNECTION_TIME.getValue();
        long averageTime = ApplicationSettings.AVERAGE_CONNECTION_TIME.getValue();
        assertEquals(totalConnect,
                averageTime);
        mgr.connect();
        Thread.sleep(6000);
        mgr.disconnect(false);
        assertGreaterThan(totalConnect+5800,
                ApplicationSettings.TOTAL_CONNECTION_TIME.getValue());
        assertLessThan(totalConnect+6500,
                ApplicationSettings.TOTAL_CONNECTION_TIME.getValue());
        assertGreaterThan(averageTime, ApplicationSettings.AVERAGE_CONNECTION_TIME.getValue());
        assertGreaterThan((totalConnect+5800)/2,
                ApplicationSettings.AVERAGE_CONNECTION_TIME.getValue());
        assertLessThan((totalConnect+6500)/2,
                ApplicationSettings.AVERAGE_CONNECTION_TIME.getValue());
        //try disconnecting twice in a row
        mgr.disconnect(false);
        assertGreaterThan(totalConnect+5800,
                ApplicationSettings.TOTAL_CONNECTION_TIME.getValue());
        assertLessThan(totalConnect+6500,
                ApplicationSettings.TOTAL_CONNECTION_TIME.getValue());
        assertGreaterThan(averageTime, ApplicationSettings.AVERAGE_CONNECTION_TIME.getValue());
        //test time changed during session
        long now = System.currentTimeMillis();
        PrivilegedAccessor.setValue(RouterService.getConnectionManager(), 
                "_connectTime", new Long(now+(60L*60L*1000L)));
        mgr.disconnect(false);
        assertGreaterThan(totalConnect+5800,
                ApplicationSettings.TOTAL_CONNECTION_TIME.getValue());
        assertLessThan(totalConnect+6500,
                ApplicationSettings.TOTAL_CONNECTION_TIME.getValue());
        assertGreaterThan((totalConnect+5800)/2,
                ApplicationSettings.AVERAGE_CONNECTION_TIME.getValue());
        assertLessThan((totalConnect+6500)/2,
                ApplicationSettings.AVERAGE_CONNECTION_TIME.getValue());
    }
    
    public void testGetCurrentAverageUptime() throws Exception{
        ApplicationSettings.AVERAGE_CONNECTION_TIME.setValue(30L*60L*1000L);
        ApplicationSettings.TOTAL_CONNECTION_TIME.setValue(60L*60L*1000L);
        ApplicationSettings.TOTAL_CONNECTIONS.setValue(2);
        ConnectionManager mgr = RouterService.getConnectionManager();
        assertFalse(mgr.isConnected());
        CATCHER.endpoint = new ExtendedEndpoint("localhost", Backend.BACKEND_PORT);
        //try simple connect-disconnect
        LISTENER.getLock().lock();
        try {
            mgr.connect();
            LISTENER.waitForInitialized(2000);
        } finally {
            LISTENER.getLock().unlock();
        }
        
        if(!mgr.isConnected())
            fail("couldn't connect!");
        
        Thread.sleep(2000);
        //this should have lowered average time
        assertLessThan((30L*60L*1000L), mgr.getCurrentAverageUptime());
        assertGreaterThan((((60L*60L*1000L)+2L*1000L)/3L)-1, mgr.getCurrentAverageUptime());
        assertEquals(2,ApplicationSettings.TOTAL_CONNECTIONS.getValue());
        mgr.disconnect(false);
        long avgtime = ApplicationSettings.AVERAGE_CONNECTION_TIME.getValue();
        assertLessThan((30L*60L*1000L), avgtime);
        assertGreaterThan((60L*60L*1000L)+(2L*1000L), 
                ApplicationSettings.TOTAL_CONNECTION_TIME.getValue());
        assertEquals(3, ApplicationSettings.TOTAL_CONNECTIONS.getValue());
        
        //this should not change anything:
        long x = mgr.getCurrentAverageUptime();
        assertEquals(x, mgr.getCurrentAverageUptime());
        assertEquals(avgtime, ApplicationSettings.AVERAGE_CONNECTION_TIME.getValue());
    }
    
    public void testClassCFiltering() throws Exception {
        ServerSocket s = new ServerSocket(10000);
        try {
            ConnectionSettings.FILTER_CLASS_C.setValue(true);
            TestHostCatcher2 catcher = new TestHostCatcher2();
            TestConnectionObserver observer = new TestConnectionObserver();
            ConnectionManager cm = RouterService.getConnectionManager();
            PrivilegedAccessor.setValue(cm,"_catcher",catcher);
            cm.addEventListener(observer);
            cm.connect();
            EndpointObserver eo = catcher.observers.poll(1000, TimeUnit.MILLISECONDS);

            // a connection should be initialized.
            eo.handleEndpoint(new Endpoint("127.0.0.1", 10000));
            ConnectionLifecycleEvent event;
            do {
                event = observer.evts.poll(1000, TimeUnit.MILLISECONDS);
            } while( !event.isConnectionInitializingEvent());
            // we should be still looking for more connections
            eo = catcher.observers.poll(1000, TimeUnit.MILLISECONDS);
            // adding a new endpoint from the same class C network should not trigger a connection
            Endpoint e2 = new Endpoint("127.0.0.2", 10000);
            eo.handleEndpoint(e2);
            assertNull(observer.evts.poll(1000, TimeUnit.MILLISECONDS));
            // we should be still looking for more connections
            eo = catcher.observers.poll(1000, TimeUnit.MILLISECONDS);
            // this is a different class C - it should trigger an event.
            eo.handleEndpoint(new Endpoint("127.0.1.1", 20000));
            do {
                event = observer.evts.poll(1000, TimeUnit.MILLISECONDS);
            } while( !event.isConnectionInitializingEvent());

            // after the connection gets closed we should get the endpoint back to 
            // hostcatcher
            s.accept().close();
            assertSame(e2,catcher.endpoints.poll(1000,TimeUnit.MILLISECONDS));
            // two closed events - one for .1 and one for 1.1
            do {
                event = observer.evts.poll(2000, TimeUnit.MILLISECONDS);
            } while( !event.isConnectionClosedEvent());
            do {
                event = observer.evts.poll(2000, TimeUnit.MILLISECONDS);
            } while( !event.isConnectionClosedEvent());

            // now we should be able to establish a second connection
            eo.handleEndpoint(new Endpoint("127.0.0.2", 10000));
            do {
                event = observer.evts.poll(1000, TimeUnit.MILLISECONDS);
            } while( !event.isConnectionInitializingEvent());

            // if we allow more than one connection per class C, we'll be able to
            // add a second connection to the same class
            ConnectionSettings.FILTER_CLASS_C.setValue(false);
            eo.handleEndpoint(new Endpoint("127.0.0.1", 10000));
            do {
                event = observer.evts.poll(1000, TimeUnit.MILLISECONDS);
            } while( !event.isConnectionInitializingEvent());
            assertEquals(2,cm.getNumConnections());

            cm.removeEventListener(observer);
        } finally {
            s.close();
        }
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
    
    private void setConnectTime() throws Exception {
        PrivilegedAccessor.setValue(RouterService.getConnectionManager(), 
                "_connectTime", new Integer(0));
    }
    
    private void initializeStart(ManagedConnection c) throws Exception {
        //  Need to setup the _outputRunner member of c as well...
        PrivilegedAccessor.setValue(c, "_outputRunner", new NullOutputRunner() );
        
        PrivilegedAccessor.invokeMethod( RouterService.getConnectionManager(),
            "connectionInitializingIncoming",
            new Object[] { c },
            new Class[] { ManagedConnection.class} );
    }
    
    private void initializeDone(ManagedConnection c) throws Exception {
        PrivilegedAccessor.invokeMethod( RouterService.getConnectionManager(),
            "connectionInitialized",
            new Object[] { c },
            new Class[] { ManagedConnection.class} );            
    }

    /**
     * Test host catcher that allows us to return endpoints that we 
     * specify when our test framework requests endpoints to connect
     * to.
     */
    private static class TestHostCatcher extends HostCatcher {
        private volatile ExtendedEndpoint endpoint;
        private volatile CountDownLatch successLatch;
        private volatile CountDownLatch failureLatch;
        private volatile AtomicInteger successes;
        private volatile AtomicInteger failures;
        
        TestHostCatcher() {
            super();
            resetLatches();
        }
        
        void resetLatches() {
            successLatch = new CountDownLatch(1);
            failureLatch = new CountDownLatch(1);
            successes = new AtomicInteger();
            failures = new AtomicInteger();
        }
        
        @Override
        protected ExtendedEndpoint getAnEndpointInternal() {
            if(endpoint == null)
                return null;
            else {
                ExtendedEndpoint ret = endpoint;
                endpoint = null;
                return ret;
            }
        }
        
        @Override
        public void doneWithConnect(Endpoint e, boolean success) {
            if (success) {
                successLatch.countDown();
                successes.incrementAndGet();
            } else {
                failureLatch.countDown();
                failures.incrementAndGet();
            }
        }
        
        boolean waitForSuccess(int millis) throws Exception {
            return successLatch.await(millis, TimeUnit.MILLISECONDS);
        }
        
        boolean waitForFailure(int millis) throws Exception {
            return failureLatch.await(millis, TimeUnit.MILLISECONDS);
        }
        
        long getSuccessCount() {
            return successes.get();
        }
        
        long getFailureCount() {
            return failures.get();
        }
        
        //  Overridden because initialize() was previously overridden, but that missed
        //  setting up some required members.  Now, the code which was intended to be 
        //  skipped was moved into this function (on the base class)
        @Override
        public void scheduleServices() {            
        }
    }
    
    private static class TestHostCatcher2 extends TestHostCatcher {
        final BlockingQueue<EndpointObserver> observers = 
            new ArrayBlockingQueue<EndpointObserver>(100);
        final BlockingQueue<Endpoint> endpoints = 
            new ArrayBlockingQueue<Endpoint>(100);
        
        @Override
        public void getAnEndpoint(EndpointObserver observer) {
            assertTrue(observers.offer(observer));
        }
        
        @Override
        public boolean add(Endpoint e, boolean asdf) {
            assertTrue(endpoints.offer(e));
            return false;
        }
    }
    
    private static class TestConnectionObserver implements ConnectionLifecycleListener {
        final BlockingQueue<ConnectionLifecycleEvent> evts = 
            new ArrayBlockingQueue<ConnectionLifecycleEvent>(100);
        public void handleConnectionLifecycleEvent(ConnectionLifecycleEvent evt) {
            assertTrue(evts.offer(evt));
        }
    }

    private static class TestManagedConnection extends ManagedConnection {
        private boolean isOutgoing;
        private int sent;
        private int received;
        private static int lastHost = 0;

        public TestManagedConnection(boolean isOutgoing, int sent, int received) {
            super("1.2.3." + ++lastHost, 6346, ConnectType.PLAIN, RouterService.getConnectionManager(), ProviderHacks.getNetworkManager(), ProviderHacks.getQueryRequestFactory(), ProviderHacks.getHeadersFactory(), ProviderHacks.getHandshakeResponderFactory() );
            this.isOutgoing=isOutgoing;
            this.sent=sent;
            this.received=received;
        }

        @Override
        public boolean isOutgoing() {
            return isOutgoing;
        }

        @Override
        public int getNumMessagesSent() {
            return sent;
        }
        
        @Override
        public int getNumMessagesReceived() {
            return received;
        }
    }
    
    private static class ClientSupernode extends TestManagedConnection {
        
        final boolean isLime;
        
        ClientSupernode(boolean lime) {
            super(false, 0, 0);
            isLime = lime;
        }
        
        @Override
        public boolean isClientSupernodeConnection() {
            return true;
        }
        @Override
        public boolean isSupernodeClientConnection() {
            return false;
        }
        @Override
        public boolean isLimeWire() {
            return isLime;
        }
    }
    
    private static class SupernodeClient extends TestManagedConnection {
        final boolean isLime;
        
        SupernodeClient(boolean lime) {
            super(false, 0, 0);
            isLime = lime;
        }
        @Override
        public boolean isClientSupernodeConnection() {
            return false;
        }
        @Override
        public boolean isSupernodeClientConnection() {
            return true;
        }
        @Override
        public boolean isLimeWire() {
            return isLime;
        }
    }
    
    private static class SupernodeSupernode extends TestManagedConnection {
        final boolean isLime;
        
        SupernodeSupernode(boolean lime) {
            super(false, 0, 0);
            isLime = lime;
        }
        @Override
        public boolean isSupernodeSupernodeConnection() {
            return false;
        }
        @Override
        public boolean isClientSupernodeConnection() {
            return false;
        }
        @Override
        public boolean isSupernodeClientConnection() {
            return false;
        }
        @Override
        public boolean isLimeWire() {
            return isLime;
        }
    }
    
    private void pretendConnected() throws Exception {
        ConnectionManager mgr = RouterService.getConnectionManager();
        PrivilegedAccessor.setValue(mgr, "_disconnectTime", new Integer(0));
        PrivilegedAccessor.invokeMethod(mgr, "setPreferredConnections");
    }
    
    private class NullOutputRunner implements OutputRunner {
        //  Overridden methods do nothing
        public void send(Message m) {}
        public void shutdown() {}
    }
    
    private static class ConnectionListener implements ConnectionLifecycleListener {
        private final Lock lock = new ReentrantLock();
        private final Condition connected = lock.newCondition();
        private final Condition connecting = lock.newCondition();
        private final Condition capabilities = lock.newCondition();
        private final Condition closed = lock.newCondition();
        private final Condition initialized = lock.newCondition();
        private final Condition initializing = lock.newCondition();
        private final Condition disconnected = lock.newCondition();
        private final Condition noInternet = lock.newCondition();
        
        public void handleConnectionLifecycleEvent(ConnectionLifecycleEvent evt) {
            try {
                lock.lock();
                switch(evt.getType()) {
                case CONNECTED: connected.signal(); break;
                case CONNECTING:  connecting.signal(); break;
                case CONNECTION_CAPABILITIES: capabilities.signal(); break;
                case CONNECTION_CLOSED: closed.signal(); break;
                case CONNECTION_INITIALIZED: initialized.signal(); break;
                case CONNECTION_INITIALIZING: initializing.signal(); break;
                case DISCONNECTED: disconnected.signal(); break;
                case NO_INTERNET: noInternet.signal(); break;
                    
                default:
                    throw new IllegalStateException("invalid type: " + evt.getType());
                    
                }
            } finally {
                lock.unlock();
            }
        }
        
        boolean waitForConnected(int millis) throws Exception {
            return await(connected, millis);
        }
        
        boolean waitForConnecting(int millis) throws Exception {
            return await(connecting, millis);
        }
        
        boolean waitForCapabilities(int millis) throws Exception {
            return await(capabilities, millis);
        }
        
        boolean waitForClosed(int millis) throws Exception {
            return await(closed, millis);
        }
        
        boolean waitForInitialized(int millis) throws Exception {
            return await(initialized, millis);
        }
        
        boolean waitForInitializing(int millis) throws Exception {
            return await(initializing, millis);
        }
        
        boolean waitForDisconnected(int millis) throws Exception {
            return await(disconnected, millis);
        }
        
        boolean waitForNoInternet(int millis) throws Exception {
            return await(noInternet, millis);
        }
        
        private boolean await(Condition x, int millis) throws Exception {
            return x.await(millis, TimeUnit.MILLISECONDS);
        }
        
        Lock getLock() {
            return lock;
        }
        
    }
}
