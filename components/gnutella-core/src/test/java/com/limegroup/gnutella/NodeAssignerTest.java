package com.limegroup.gnutella;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;

import junit.framework.Test;

import org.limewire.concurrent.ManagedThread;
import org.limewire.util.PrivilegedAccessor;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
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
import com.limegroup.gnutella.stubs.NetworkManagerStub;
import com.limegroup.gnutella.util.LimeTestCase;

public class NodeAssignerTest extends LimeTestCase {
    
    private final int TEST_PORT = 6667;
    
    private TestAcceptor testAcceptor;
    private Injector injector;
    private NodeAssigner nodeAssigner;
    private TestBandwidthTracker bandwidthTracker;

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
        injector = LimeTestUtils.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(BandwidthTracker.class).annotatedWith(Names.named("uploadTracker")).to(TestBandwidthTracker.class);
                bind(BandwidthTracker.class).annotatedWith(Names.named("downloadTracker")).to(TestBandwidthTracker.class);
                bind(NetworkManager.class).to(NetworkManagerStub.class);
            } 
        });
        nodeAssigner = injector.getInstance(NodeAssigner.class);
        bandwidthTracker = (TestBandwidthTracker)injector.getInstance(Key.get(BandwidthTracker.class, Names.named("uploadTracker")));
        assertSame(bandwidthTracker, injector.getInstance(Key.get(BandwidthTracker.class, Names.named("downloadTracker"))));
        NetworkManagerStub networkManager = (NetworkManagerStub)injector.getInstance(NetworkManager.class);
        networkManager.setAddress(new byte[] { 127, 0, 0, 1 } );
        networkManager.setPort(6346);
        networkManager.setSolicitedGUID(new GUID());
        
        testAcceptor = new TestAcceptor();
        new ManagedThread(testAcceptor, "TestAcceptor").start();    
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
        testAcceptor.shutdown();
        injector.getInstance(ConnectionServices.class).disconnect();
        Thread.sleep(1000);
    }
        
    private void setUltrapeerCapabilities() throws Exception{
        setHardcoreCapabilities();
        ApplicationSettings.AVERAGE_UPTIME.setValue(UltrapeerSettings.MIN_AVG_UPTIME.getValue());
        UltrapeerSettings.NEED_MIN_CONNECT_TIME.setValue(false);
    }
    
    private void setDHTCapabilities() throws Exception{
        setHardcoreCapabilities();
        ApplicationSettings.AVERAGE_CONNECTION_TIME.setValue(DHTSettings.MIN_ACTIVE_DHT_AVERAGE_UPTIME.getValue());
        PrivilegedAccessor.setValue(nodeAssigner,"_currentUptime",
                                    new Long(DHTSettings.MIN_ACTIVE_DHT_INITIAL_UPTIME.getValue()));
    }
    
    private void setHardcoreCapabilities() throws Exception{
        bandwidthTracker.setIsGoodBandwidth(true);
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);
        ConnectionSettings.CONNECTION_SPEED.setValue(SpeedConstants.MODEM_SPEED_INT + 1);
        NetworkManagerStub networkManager = (NetworkManagerStub)injector.getInstance(NetworkManager.class);
        networkManager.setGuessCapable(true);
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
        LifecycleManager lifecycleManager = injector.getInstance(LifecycleManager.class);
        ConnectionManager connectionManager = injector.getInstance(ConnectionManager.class);
        DHTManager dhtManager = injector.getInstance(DHTManager.class);
        
        assertFalse(UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.getValue());
        //set up an ultrapeer capable host:
        setUltrapeerCapabilities();
        lifecycleManager.start();
        Thread.sleep(1000);
        //the node assigner should have worked it's magic
        assertTrue(UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.getValue());
        testAcceptor.setAcceptsUltrapeers(true);
        connect();
        assertTrue("should be an ultrapeer", connectionManager.isActiveSupernode());
        assertTrue("should be passively connected to the DHT", dhtManager.isRunning() 
                && (dhtManager.getDHTMode() != DHTMode.ACTIVE));
        
        //make sure you can't be an active DHT node at the same time
        setDHTCapabilities();
        Thread.sleep(200);
        assertNotEquals(DHTMode.ACTIVE, dhtManager.getDHTMode());
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
        setUltrapeerCapabilities();
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
        
        setDHTCapabilities();
        lifecycleManager.start();
        Thread.sleep(1000);
        DHTSettings.SWITCH_TO_ULTRAPEER_PROBABILITY.setValue(0);
        testAcceptor.setAcceptsUltrapeers(false);
        connect();
        Thread.sleep(1000);
        assertFalse("should not be an ultrapeer", connectionServices.isSupernode());
        assertEquals(DHTMode.ACTIVE, dhtManager.getDHTMode());
        testAcceptor.setAcceptsUltrapeers(true);
        setUltrapeerCapabilities();
        networkManager.setAcceptedIncomingConnection(true);
        DHTSettings.SWITCH_TO_ULTRAPEER_PROBABILITY.setValue(1);
        PrivilegedAccessor.setValue(nodeAssigner, "_lastUltrapeerAttempt", 
                new Long(System.currentTimeMillis() - 24L*60L*60L*1000L));
        
        Thread.sleep(2 * NodeAssigner.TIMER_DELAY);
        
        assertNotEquals(DHTMode.ACTIVE, dhtManager.getDHTMode());
        assertTrue("should be an ultrapeer", connectionServices.isSupernode());
    }
    
    @Singleton
    private static class TestBandwidthTracker implements BandwidthTracker {
        
        private boolean isGoodBandwidth;
        
        public float getAverageBandwidth() {
            if(isGoodBandwidth) 
                return UltrapeerSettings.MIN_UPSTREAM_REQUIRED.getValue();
            else
                return (UltrapeerSettings.MIN_UPSTREAM_REQUIRED.getValue() -1);
        }

        public float getMeasuredBandwidth() throws InsufficientDataException {
            if(isGoodBandwidth) 
                return UltrapeerSettings.MIN_UPSTREAM_REQUIRED.getValue();
            else
                return (UltrapeerSettings.MIN_UPSTREAM_REQUIRED.getValue() - 1);
        }

        public void measureBandwidth() {}
        
        public void setIsGoodBandwidth(boolean good) {
            isGoodBandwidth = good;
        }
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
        private volatile Connection incomingConnection;
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
                    incomingConnection = injector.getInstance(ConnectionFactory.class).createConnection(socket);
                    if(acceptsUltrapeers) {
                        incomingConnection.initialize(null, new UltrapeerResponder());
                    } else {
                        incomingConnection.initialize(null, new NoUltrapeerResponder());
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
}
