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
    
    public void testUltrapeerConnection() throws Exception{
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);
        ApplicationSettings.AVERAGE_UPTIME.setValue(UltrapeerSettings.MIN_AVG_UPTIME.getValue() + 1);
        assertFalse(UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.getValue());

        mockery.checking(new Expectations(){{
            
            // say we have good bandwidth
            one(upTracker).measureBandwidth();
            one(downTracker).measureBandwidth();
            // connection manager is not mocked
            
            one(upTracker).getMeasuredBandwidth();
            will(returnValue(UltrapeerSettings.MIN_UPSTREAM_REQUIRED.getValue() + 2f));
            one(downTracker).getMeasuredBandwidth();
            will(returnValue(UltrapeerSettings.MIN_DOWNSTREAM_REQUIRED.getValue() + 2f));
            
            
            // set up some conditions for ultrapeer-ness
            one(connectionServices).isSupernode();
            will(returnValue(false));
            one(networkManager).isGUESSCapable();
            will(returnValue(true));
            one(networkManager).acceptedIncomingConnection();
            will(returnValue(true));
            one(searchServices).getLastQueryTime();
            will(returnValue(0l)); // long time ago
            atLeast(1).of(networkManager).getAddress();
            will(returnValue(new byte[4]));
            
            // we're currently not active and disabled
            exactly(2).of(dhtManager).getDHTMode();
            will(returnValue(DHTMode.INACTIVE));
            one(dhtManager).isEnabled();
            will(returnValue(false));
            
        }});
        
        assignerRunnable.run();
        assertTrue(UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.getValue());
        assertTrue(connectionManager.invoked.await(3, TimeUnit.SECONDS));
        assertEquals(4,connectionManager.demotes);
        mockery.assertIsSatisfied();
        
        // this should really be in a separate test
//        testAcceptor.setAcceptsUltrapeers(true);
//        connect();
//        assertTrue("should be an ultrapeer", connectionManager.isActiveSupernode());
//        assertTrue("should be passively connected to the DHT", dhtManager.isRunning() 
//                && (dhtManager.getDHTMode() != DHTMode.ACTIVE));
        
    }
    
    public void testLeafToUltrapeerPromotion() throws Exception{
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
    }
}
