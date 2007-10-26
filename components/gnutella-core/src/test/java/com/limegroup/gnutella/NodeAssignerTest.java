package com.limegroup.gnutella;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.util.PrivilegedAccessor;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.name.Names;
import com.limegroup.gnutella.connection.BlockingConnection;
import com.limegroup.gnutella.connection.BlockingConnectionFactory;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.dht.DHTManager.DHTMode;
import com.limegroup.gnutella.handshaking.HandshakeResponder;
import com.limegroup.gnutella.handshaking.HandshakeResponse;
import com.limegroup.gnutella.handshaking.HeaderNames;
import com.limegroup.gnutella.handshaking.HeadersFactory;
import com.limegroup.gnutella.settings.ApplicationSettings;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.DHTSettings;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.settings.UltrapeerSettings;
import com.limegroup.gnutella.stubs.ConnectionManagerStub;
import com.limegroup.gnutella.stubs.NetworkManagerStub;
import com.limegroup.gnutella.stubs.ScheduledExecutorServiceStub;
import com.limegroup.gnutella.util.LimeTestCase;

public class NodeAssignerTest extends LimeTestCase {
    
    private final int TEST_PORT = 6667;
    
    private TestAcceptor testAcceptor;
    private Injector injector;
    private NodeAssigner nodeAssigner;
    private BandwidthTracker upTracker, downTracker;
    private DHTManager dhtManager;
    private Runnable assignerRunnable;
    private ConnectionServices connectionServices;
    private NetworkManager networkManager;
    private Mockery mockery;
    private MyConnectionManager connectionManager;
    private SearchServices searchServices;
    
    public NodeAssignerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(NodeAssignerTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    protected void setUp() throws Exception {
        setSettings();
        mockery = new Mockery();
        upTracker = mockery.mock(BandwidthTracker.class);
        downTracker = mockery.mock(BandwidthTracker.class);
        dhtManager = mockery.mock(DHTManager.class);
        connectionServices = mockery.mock(ConnectionServices.class);
        networkManager = mockery.mock(NetworkManager.class);
        connectionManager = new MyConnectionManager();
        searchServices = mockery.mock(SearchServices.class);
        
        final ScheduledExecutorService ses = new ScheduledExecutorServiceStub() {

            @Override
            public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
                if (delay == NodeAssigner.TIMER_DELAY && initialDelay == 0)
                    assignerRunnable = command;
                return null;
            }
            
        };
        injector = LimeTestUtils.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(BandwidthTracker.class).annotatedWith(Names.named("uploadTracker")).toInstance(upTracker);
                bind(BandwidthTracker.class).annotatedWith(Names.named("downloadTracker")).toInstance(downTracker);
                bind(DHTManager.class).toInstance(dhtManager);
                bind(NetworkManager.class).toInstance(networkManager);
                bind(ConnectionServices.class).toInstance(connectionServices);
                bind(ScheduledExecutorService.class).annotatedWith(Names.named("backgroundExecutor")).toInstance(ses);
                bind(ConnectionManager.class).toInstance(connectionManager);
                bind(SearchServices.class).toInstance(searchServices);
            } 
        });
        nodeAssigner = injector.getInstance(NodeAssigner.class);
        
        nodeAssigner.start();
        assertNotNull(assignerRunnable);
        
        // TODO: figure out what is this for and is it necessary
//        testAcceptor = new TestAcceptor();
//        new ManagedThread(testAcceptor, "TestAcceptor").start();    
    }
    
    
    private void setSettings() throws Exception {
        FilterSettings.BLACK_LISTED_IP_ADDRESSES.setValue(
            new String[] {"*.*.*.*"});
        //Set the local host to not be banned so pushes can go through
        String ip = InetAddress.getLocalHost().getHostAddress();
        FilterSettings.WHITE_LISTED_IP_ADDRESSES.setValue(
                new String[] {ip, "127.*.*.*"});
        ConnectionSettings.PORT.setValue(TEST_PORT);
        ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        ConnectionSettings.CONNECTION_SPEED.setValue(SpeedConstants.MODEM_SPEED_INT+1);
        //reset the node capabilities settings
        UltrapeerSettings.FORCE_ULTRAPEER_MODE.setValue(false);
        UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(false);
        UltrapeerSettings.NEED_MIN_CONNECT_TIME.setValue(true);
        UltrapeerSettings.DISABLE_ULTRAPEER_MODE.setValue(false);
        DHTSettings.DISABLE_DHT_USER.setValue(false);
        DHTSettings.DISABLE_DHT_NETWORK.setValue(false);
        DHTSettings.EXCLUDE_ULTRAPEERS.setValue(true);
        DHTSettings.FORCE_DHT_CONNECT.setValue(false);
        DHTSettings.ENABLE_PASSIVE_DHT_MODE.setValue(true);
        DHTSettings.ENABLE_PASSIVE_LEAF_DHT_MODE.setValue(true);
    }

    protected void tearDown() throws Exception {
        if(testAcceptor != null)
            testAcceptor.shutdown();
        Thread.sleep(1000);
    }
        
    
    public void connect() throws Exception {
        ConnectionServices connectionServices = injector.getInstance(ConnectionServices.class);
        HostCatcher hostCatcher = injector.getInstance(HostCatcher.class);
        hostCatcher.clear();
        connectionServices.connect();
        assertFalse("should not be connected", connectionServices.isConnected());
        hostCatcher.add(new Endpoint("localhost", TEST_PORT + 1), true);
        Thread.sleep(2000);
        assertTrue("should be connected", connectionServices.isConnected());
    }
    
    private Expectations buildBandwdithExpectations(final boolean good) throws Exception {
        return new Expectations() {{
            one(upTracker).measureBandwidth();
            one(downTracker).measureBandwidth();
            // connection manager is not mocked
            
            one(upTracker).getMeasuredBandwidth();
            will(returnValue(UltrapeerSettings.MIN_UPSTREAM_REQUIRED.getValue() + (good ? 2f : -2f)));
            one(downTracker).getMeasuredBandwidth();
            will(returnValue(UltrapeerSettings.MIN_DOWNSTREAM_REQUIRED.getValue() +(good ? 2f : -2f)));
            
        }};
    }
    
    /**
     * Builds an ultrapeer environment.
     * 
     * @param required if the checks are required or optional
     */
    private Expectations buildUltrapeerExpectations(
            final boolean GUESSCapable,
            final boolean acceptedIncoming,
            final long lastQueryTime,
            final boolean DHTEnabled,
            final DHTMode currentMode,
            boolean required
            ) throws Exception {
        
        final int invocations = required ? 1 : 0;
        
        return new Expectations(){{
            
            // this is a required call
            atLeast(1).of(connectionServices).isSupernode();
            will(returnValue(false));
            // annoying workaround for NetworkUtils.isPrivate
            atLeast(invocations).of(networkManager).getAddress();
            will(returnValue(new byte[4]));
            
            
            atLeast(invocations).of(networkManager).isGUESSCapable();
            will(returnValue(GUESSCapable));
            atLeast(invocations).of(networkManager).acceptedIncomingConnection();
            will(returnValue(acceptedIncoming));
            atLeast(invocations).of(searchServices).getLastQueryTime();
            will(returnValue(lastQueryTime)); 
            atLeast(invocations).of(dhtManager).getDHTMode();
            will(returnValue(currentMode));
            atLeast(invocations).of(dhtManager).isEnabled();
            will(returnValue(DHTEnabled));
        }};
    }
    
    public void testPromotesUltrapeer() throws Exception{
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);
        ApplicationSettings.AVERAGE_UPTIME.setValue(UltrapeerSettings.MIN_AVG_UPTIME.getValue() + 1);
        assertFalse(UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.getValue());

        // set up some conditions for ultrapeer-ness
        // all of them are required
        mockery.checking(buildBandwdithExpectations(true));
        mockery.checking(buildUltrapeerExpectations(true, true, 0l, false, DHTMode.INACTIVE , true));
        
        assignerRunnable.run();
        assertTrue(UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.getValue());
        assertTrue(connectionManager.invoked.await(1, TimeUnit.SECONDS));
        assertEquals(4,connectionManager.demotes);
        mockery.assertIsSatisfied();
    }
    
    public void testDoesNotPromoteIfSlow() throws Exception {
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);
        ApplicationSettings.AVERAGE_UPTIME.setValue(UltrapeerSettings.MIN_AVG_UPTIME.getValue() + 1);
        assertFalse(UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.getValue());
        
        // bandwidth not high enough
        mockery.checking(buildBandwdithExpectations(false));
        // all other conditions match, but not all will be checked
        mockery.checking(buildUltrapeerExpectations(true, true, 0l, false, DHTMode.INACTIVE , false));

        assignerRunnable.run();
        assertFalse(UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.getValue());
        assertFalse(connectionManager.invoked.await(1, TimeUnit.SECONDS));
        mockery.assertIsSatisfied();
    }

    public void testDoesNotPromoteIfNoUDP() throws Exception {
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);
        ApplicationSettings.AVERAGE_UPTIME.setValue(UltrapeerSettings.MIN_AVG_UPTIME.getValue() + 1);
        assertFalse(UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.getValue());
        
        mockery.checking(buildBandwdithExpectations(true));
        // no UDP support
        mockery.checking(buildUltrapeerExpectations(false, true, 0l, false, DHTMode.INACTIVE , false));
        assignerRunnable.run();
        assertFalse(UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.getValue());
        assertFalse(connectionManager.invoked.await(1, TimeUnit.SECONDS));
        mockery.assertIsSatisfied();
    }
    
    public void testDoesNotPromoteIfNoTCP() throws Exception {
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);
        ApplicationSettings.AVERAGE_UPTIME.setValue(UltrapeerSettings.MIN_AVG_UPTIME.getValue() + 1);
        assertFalse(UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.getValue());
        
        mockery.checking(buildBandwdithExpectations(true));
        // no tcp support
        mockery.checking(buildUltrapeerExpectations(true, false, 0l, false, DHTMode.INACTIVE, false));
        assignerRunnable.run();
        
        // we are ever_capable because of last time
        assertTrue(UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.getValue());
        assertFalse(connectionManager.invoked.await(1, TimeUnit.SECONDS));
        mockery.assertIsSatisfied();
    }
    
    public void testDoesNotPromoteIfQueryTooSoon() throws Exception {
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);
        ApplicationSettings.AVERAGE_UPTIME.setValue(UltrapeerSettings.MIN_AVG_UPTIME.getValue() + 1);
        assertFalse(UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.getValue());
        
        mockery.checking(buildBandwdithExpectations(true));
        // last query now
        mockery.checking(buildUltrapeerExpectations(true, true, System.currentTimeMillis(), false, DHTMode.INACTIVE, false));
        assignerRunnable.run();
        
        // we are ever_capable because of last time
        assertTrue(UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.getValue());
        assertFalse(connectionManager.invoked.await(1, TimeUnit.SECONDS));
        mockery.assertIsSatisfied();
    }
    
    public void testDoesNotPromoteIfAverageUptimeLow() throws Exception {
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);
        // uptime bad
        ApplicationSettings.AVERAGE_UPTIME.setValue(UltrapeerSettings.MIN_AVG_UPTIME.getValue() - 1);
        assertFalse(UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.getValue());
        
        mockery.checking(buildBandwdithExpectations(true));
        // last query now
        mockery.checking(buildUltrapeerExpectations(true, true, System.currentTimeMillis(), false, DHTMode.INACTIVE, false));
        assignerRunnable.run();

        // did not become capable this time either
        assertFalse(UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.getValue());
        assertFalse(connectionManager.invoked.await(1, TimeUnit.SECONDS));
        mockery.assertIsSatisfied();
    }
    
    public void testDoesNotPromoteIfNeverIncoming() throws Exception {
        // never incoming
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(false);
        ApplicationSettings.AVERAGE_UPTIME.setValue(UltrapeerSettings.MIN_AVG_UPTIME.getValue() + 1);
        assertFalse(UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.getValue());
        
        mockery.checking(buildBandwdithExpectations(true));
        // everything else is fine
        mockery.checking(buildUltrapeerExpectations(true, true, 0l, false, DHTMode.INACTIVE, false));
        assignerRunnable.run();

        // did not become capable this time either
        assertFalse(UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.getValue());
        assertFalse(connectionManager.invoked.await(1, TimeUnit.SECONDS));
        mockery.assertIsSatisfied();
    }
    
    public void testDoesNotPromoteIfDHTActive() throws Exception {
        // disallow switch to ultrapeer
        DHTSettings.SWITCH_TO_ULTRAPEER_PROBABILITY.setValue(0f);
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);
        ApplicationSettings.AVERAGE_UPTIME.setValue(UltrapeerSettings.MIN_AVG_UPTIME.getValue() + 1);
        assertFalse(UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.getValue());
        
        // pretend some time passed - the uptime counter in NodeAssigner is very hacky
        PrivilegedAccessor.setValue(nodeAssigner,"_currentUptime", new Long(DHTSettings.MIN_ACTIVE_DHT_INITIAL_UPTIME.getValue()));
        
        mockery.checking(buildBandwdithExpectations(true));
        // enabled and active DHT
        mockery.checking(buildUltrapeerExpectations(true, true, 0l, true, DHTMode.ACTIVE, false));
        
        // but we're not an active ultrapeer and can receive solicited
        mockery.checking(new Expectations(){{
            one(connectionServices).isActiveSuperNode();
            will(returnValue(false));
            one(networkManager).canReceiveSolicited();
            will(returnValue(true));
        }});
        
        // but we've been up long enough tob e active in the DHT
        connectionManager.avgUptime = DHTSettings.MIN_ACTIVE_DHT_AVERAGE_UPTIME.getValue() + 1;
        
        assignerRunnable.run();

        assertTrue(UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.getValue());
        assertFalse(connectionManager.invoked.await(1, TimeUnit.SECONDS));
        mockery.assertIsSatisfied();
    }
    
    public void testPromotesFromActiveDHTIfAllowed() throws Exception {
        // disallow switch to ultrapeer
        DHTSettings.SWITCH_TO_ULTRAPEER_PROBABILITY.setValue(1f);
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);
        ApplicationSettings.AVERAGE_UPTIME.setValue(UltrapeerSettings.MIN_AVG_UPTIME.getValue() + 1);
        assertFalse(UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.getValue());
        
        // pretend some time passed - the uptime counter in NodeAssigner is very hacky
        PrivilegedAccessor.setValue(nodeAssigner,"_currentUptime", new Long(DHTSettings.MIN_ACTIVE_DHT_INITIAL_UPTIME.getValue()));
        
        mockery.checking(buildBandwdithExpectations(true));
        // enabled and active DHT
        mockery.checking(buildUltrapeerExpectations(true, true, 0l, true, DHTMode.ACTIVE, false));
        
        // but we're not an active ultrapeer and can receive solicited
        mockery.checking(new Expectations(){{
            one(connectionServices).isActiveSuperNode();
            will(returnValue(false));
            one(networkManager).canReceiveSolicited();
            will(returnValue(true));
        }});
        
        
        
        // but we've been up long enough tob e active in the DHT
        connectionManager.avgUptime = DHTSettings.MIN_ACTIVE_DHT_AVERAGE_UPTIME.getValue() + 1;
        
        assignerRunnable.run();

        assertTrue(UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.getValue());
        assertTrue(connectionManager.invoked.await(1, TimeUnit.SECONDS));
        mockery.assertIsSatisfied();
    }
    
    
    public void testLeafToUltrapeerPromotion() throws Exception{
        fail("rewrite this");
        LifecycleManager lifecycleManager = injector.getInstance(LifecycleManager.class);
        ConnectionManager connectionManager = injector.getInstance(ConnectionManager.class);
        DHTManager dhtManager = injector.getInstance(DHTManager.class);
        NetworkManagerStub networkManager = (NetworkManagerStub)injector.getInstance(NetworkManager.class);
        
        lifecycleManager.start();
        connect();
        assertFalse("should be not be an ultrapeer", connectionManager.isSupernode());
        networkManager.setAcceptedIncomingConnection(true);
        DHTSettings.SWITCH_TO_ULTRAPEER_PROBABILITY.setValue(1);
        testAcceptor.setAcceptsUltrapeers(true);
        PrivilegedAccessor.setValue(nodeAssigner, "_lastUltrapeerAttempt", 
                new Long(System.currentTimeMillis() - 24L*60L*60L*1000L));
        
        Thread.sleep(2 * NodeAssigner.TIMER_DELAY);
        
        assertTrue("should be an ultrapeer", connectionManager.isActiveSupernode());
        assertNotEquals(DHTMode.ACTIVE, dhtManager.getDHTMode());
    }
    
    public void testDHTtoUltrapeerSwitch() throws Exception{
        fail("rewrite this");
        LifecycleManager lifecycleManager = injector.getInstance(LifecycleManager.class);
        DHTManager dhtManager = injector.getInstance(DHTManager.class);
        NetworkManagerStub networkManager = (NetworkManagerStub)injector.getInstance(NetworkManager.class);
        ConnectionServices connectionServices = injector.getInstance(ConnectionServices.class);
        
        lifecycleManager.start();
        Thread.sleep(1000);
        DHTSettings.SWITCH_TO_ULTRAPEER_PROBABILITY.setValue(0);
        testAcceptor.setAcceptsUltrapeers(false);
        connect();
        Thread.sleep(1000);
        assertFalse("should not be an ultrapeer", connectionServices.isSupernode());
        assertEquals(DHTMode.ACTIVE, dhtManager.getDHTMode());
        testAcceptor.setAcceptsUltrapeers(true);
        networkManager.setAcceptedIncomingConnection(true);
        DHTSettings.SWITCH_TO_ULTRAPEER_PROBABILITY.setValue(1);
        PrivilegedAccessor.setValue(nodeAssigner, "_lastUltrapeerAttempt", 
                new Long(System.currentTimeMillis() - 24L*60L*60L*1000L));
        
        Thread.sleep(2 * NodeAssigner.TIMER_DELAY);
        
        assertNotEquals(DHTMode.ACTIVE, dhtManager.getDHTMode());
        assertTrue("should be an ultrapeer", connectionServices.isSupernode());
    }
    
    private class UltrapeerResponder implements HandshakeResponder {
        public HandshakeResponse respond(HandshakeResponse response, 
                boolean outgoing)  {
            Properties props = injector.getInstance(HeadersFactory.class).createUltrapeerHeaders("localhost"); 
            props.put(HeaderNames.X_DEGREE, "42");           
            return HandshakeResponse.createResponse(props);
        }
        
        public void setLocalePreferencing(boolean b) {}
    }
    
    private class NoUltrapeerResponder implements HandshakeResponder {
        public HandshakeResponse respond(HandshakeResponse response, 
                boolean outgoing)  {
            Properties props= injector.getInstance(HeadersFactory.class).createUltrapeerHeaders("localhost");
            props.put(HeaderNames.X_ULTRAPEER_NEEDED, "false");
            return HandshakeResponse.createResponse(props);
        }
        
        public void setLocalePreferencing(boolean b) {}
    }
    
    private class TestAcceptor implements Runnable {
        private volatile boolean acceptsUltrapeers = true;        
        private volatile BlockingConnection incomingConnection;
        private volatile boolean keepRunning = true;
        private volatile ServerSocket serverSocket;
        
        public void run(){
            while(keepRunning) {
                try {
                    serverSocket=new ServerSocket(TEST_PORT+1);
                    serverSocket.setSoTimeout(10000);
                    Socket socket = serverSocket.accept();
                    serverSocket.close();
                    serverSocket = null;
                    socket.setSoTimeout(1000);
                    incomingConnection = injector.getInstance(BlockingConnectionFactory.class).createConnection(socket);
                    if(acceptsUltrapeers) {
                        incomingConnection.initialize(null, new UltrapeerResponder(), 0);
                    } else {
                        incomingConnection.initialize(null, new NoUltrapeerResponder(), 0);
                    }
                } catch (IOException e) {
                    if(keepRunning)
                        throw new RuntimeException(e);
                }
            }
        }
        
        public void setAcceptsUltrapeers(boolean acceptsUltrapeers) {
            this.acceptsUltrapeers = acceptsUltrapeers;
        }
        
        public void shutdown() throws Exception {
            keepRunning = false;
            if(serverSocket != null)
                serverSocket.close();
            if(incomingConnection != null)
                incomingConnection.close();
        }
    }
    
    private class MyConnectionManager extends ConnectionManagerStub {
        volatile int demotes = -1;
        volatile long avgUptime;
        private final CountDownLatch invoked = new CountDownLatch(1);
        public MyConnectionManager() {
            super(null, null, null, null, null,
                    null, null, null, null,
                    null, null, null, null,
                    null, null);
        }
        
        public void tryToBecomeAnUltrapeer(int demotes) {
            this.demotes = demotes;
            invoked.countDown();
        }
        
        public long getCurrentAverageUptime() {
            return avgUptime;
        }
    }
}
