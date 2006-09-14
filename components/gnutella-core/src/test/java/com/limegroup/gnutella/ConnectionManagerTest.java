package com.limegroup.gnutella;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Properties;

import junit.framework.Test;

import com.limegroup.gnutella.connection.OutputRunner;
import com.limegroup.gnutella.handshaking.HandshakeResponse;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.settings.ApplicationSettings;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.UltrapeerSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.util.PrivilegedAccessor;

/**
 * PARTIAL unit tests for ConnectionManager.  Makes sure HostCatcher is notified
 * of right events.  
 */
public class ConnectionManagerTest extends BaseTestCase {

    private static TestHostCatcher CATCHER;

    private static RouterService ROUTER_SERVICE;

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
    }
    
    private static void setSettings() throws Exception {
        ConnectionSettings.PORT.setValue(6346);
		ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
		ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);
		ConnectionSettings.USE_GWEBCACHE.setValue(false);
        UltrapeerSettings.FORCE_ULTRAPEER_MODE.setValue(true);
        UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(true);
        ConnectionSettings.PREFERENCING_ACTIVE.setValue(false);
        UltrapeerSettings.NEED_MIN_CONNECT_TIME.setValue(true);
        ApplicationSettings.TOTAL_CONNECTION_TIME.setValue(0);
        ApplicationSettings.AVERAGE_CONNECTION_TIME.setValue(0);
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
     * Tests the method for checking whether or not connections should be 
     * allowed.
     * 
     * @throws Exception if an error occurs
     */
 //   public void testAllowConnection() throws Exception {
        // NOTA BENE: you may have to turn on ConnectionSettings.PREFERENCING_ACTIVE.setValue(true);
    	// which is deactivated in this.setSettings()
 //   }
    
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
    public void testUnreachableHost() {
        CATCHER.endpoint = new ExtendedEndpoint("1.2.3.4", 5000);
        RouterService.connect();
        sleep(15000);
        assertEquals("unexpected successful connect", 0, CATCHER.connectSuccess);
        assertGreaterThan("should have received failures", 0, CATCHER.connectFailures);
    }

    /**
     * Test to make sure tests does not succeed with a host reporting
     * the wrong protocol.
     */
    public void testWrongProtocolHost() {
        CATCHER.endpoint = new ExtendedEndpoint("www.yahoo.com", 80);
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
        CATCHER.endpoint = new ExtendedEndpoint("localhost", Backend.BACKEND_PORT);
        
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
            new ExtendedEndpoint("localhost", Backend.REJECT_PORT);
        RouterService.connect();
        sleep();
        assertEquals("connect should have succeeded", 1, CATCHER.connectSuccess);
        assertEquals("connect should have failed", 0, CATCHER.connectFailures);
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
        sleep(2000);
        mgr.disconnect(false);
        long totalConnect = ApplicationSettings.TOTAL_CONNECTION_TIME.getValue();
        long averageTime = ApplicationSettings.AVERAGE_CONNECTION_TIME.getValue();
        assertEquals(totalConnect,
                averageTime);
        mgr.connect();
        sleep(6000);
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
        mgr.connect();
        sleep(2000);
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
        private volatile int connectSuccess=0;
        private volatile int connectFailures=0;
        
        TestHostCatcher() {
            super();
        }
        
        protected ExtendedEndpoint getAnEndpointInternal() {
            if(endpoint == null)
                return null;
            else {
                ExtendedEndpoint ret = endpoint;
                endpoint = null;
                return ret;
            }
        }
        
        public synchronized void doneWithConnect(Endpoint e, boolean success) {
            if (success)
                connectSuccess++;
            else
                connectFailures++;
        }
        
        //  Overridden because initialize() was previously overridden, but that missed
        //  setting up some required members.  Now, the code which was intended to be 
        //  skipped was moved into this function (on the base class)
        public void scheduleServices() {            
        }
    }

    private static class TestManagedConnection extends ManagedConnection {
        private boolean isOutgoing;
        private int sent;
        private int received;
        private static int lastHost = 0;

        public TestManagedConnection(boolean isOutgoing, int sent, int received) {
            super("1.2.3." + ++lastHost, 6346);
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
        
        protected void startOutputRunner() {
            // do nothing
        }
    }
    
    private static class ClientSupernode extends TestManagedConnection {
        
        final boolean isLime;
        
        ClientSupernode(boolean lime) {
            super(false, 0, 0);
            isLime = lime;
        }
        
        public boolean isClientSupernodeConnection() {
            return true;
        }
        public boolean isSupernodeClientConnection() {
            return false;
        }
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
        public boolean isClientSupernodeConnection() {
            return false;
        }
        public boolean isSupernodeClientConnection() {
            return true;
        }
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
        public boolean isSupernodeSupernodeConnection() {
            return false;
        }
        public boolean isClientSupernodeConnection() {
            return false;
        }
        public boolean isSupernodeClientConnection() {
            return false;
        }
        public boolean isLimeWire() {
            return isLime;
        }
    }
    
    private void pretendConnected() throws Exception {
        ConnectionManager mgr = RouterService.getConnectionManager();
        PrivilegedAccessor.setValue(mgr, "_disconnectTime", new Integer(0));
        PrivilegedAccessor.invokeMethod(mgr, "setPreferredConnections", null);
    }
    
    private class NullOutputRunner implements OutputRunner {
        //  Overridden methods do nothing
        public void send(Message m) {}
        public void shutdown() {}
    }
}
