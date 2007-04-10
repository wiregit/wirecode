package com.limegroup.gnutella;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;

import junit.framework.Test;

import org.limewire.service.ErrorService;
import org.limewire.util.PrivilegedAccessor;

import com.limegroup.gnutella.dht.DHTManager.DHTMode;
import com.limegroup.gnutella.handshaking.HandshakeResponder;
import com.limegroup.gnutella.handshaking.HandshakeResponse;
import com.limegroup.gnutella.handshaking.HeaderNames;
import com.limegroup.gnutella.handshaking.UltrapeerHeaders;
import com.limegroup.gnutella.settings.ApplicationSettings;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.DHTSettings;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.settings.UltrapeerSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.LimeTestCase;

public class NodeAssignerTest extends LimeTestCase {
    
    private static NodeAssigner ASSIGNER;
    
    private static RouterService ROUTER_SERVICE;
    
    private static TestUltrapeer ULTRAPEER = new TestUltrapeer();
    
    private static Thread upThread = new Thread(ULTRAPEER);
    
    private static int TEST_PORT = 6667;
    
    private static final TestBandwidthTracker BW = new TestBandwidthTracker();

    public NodeAssignerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(NodeAssignerTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public static void globalSetUp() throws Exception {
        ROUTER_SERVICE =
            new RouterService(new ActivityCallbackStub());
        
        setSettings();
        launchAllBackends();
        
        //start the listening ultrapeer
        upThread.start();

    }

    protected void setUp() throws Exception {
        setSettings();
        ASSIGNER = new NodeAssigner(BW, BW, RouterService.getConnectionManager());

    }
    
    private static void setSettings() throws Exception {
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
        ConnectionSettings.USE_GWEBCACHE.setValue(false);
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
        ULTRAPEER.shutdown();
        RouterService.disconnect();
        sleep();
    }
    
    private void sleep() {
        try {Thread.sleep(1000);}catch(InterruptedException e) {}
    }
    
    private void sleep(long millis) {
        try {Thread.sleep(millis);}catch(InterruptedException e) {}
    }
    
    private void setUltrapeerCapabilities() throws Exception{
        setHardcoreCapabilities();
        ApplicationSettings.AVERAGE_UPTIME.setValue(UltrapeerSettings.MIN_AVG_UPTIME.getValue());
        UltrapeerSettings.NEED_MIN_CONNECT_TIME.setValue(false);
    }
    
    private void setDHTCapabilities() throws Exception{
        setHardcoreCapabilities();
        ApplicationSettings.AVERAGE_CONNECTION_TIME.setValue(DHTSettings.MIN_ACTIVE_DHT_AVERAGE_UPTIME.getValue());
        PrivilegedAccessor.setValue(ASSIGNER,"_currentUptime",
                                    new Long(DHTSettings.MIN_ACTIVE_DHT_INITIAL_UPTIME.getValue()));
    }
    
    private void setHardcoreCapabilities() throws Exception{
        BW.setIsGoodBandwidth(true);
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);
        ConnectionSettings.CONNECTION_SPEED.setValue(SpeedConstants.MODEM_SPEED_INT + 1);
        PrivilegedAccessor.setValue(RouterService.getUdpService(),"_acceptedSolicitedIncoming",new Boolean(true));
        PrivilegedAccessor.setValue(RouterService.getUdpService(),"_acceptedUnsolicitedIncoming",new Boolean(true));
       
        PrivilegedAccessor.setValue(ROUTER_SERVICE,"nodeAssigner",ASSIGNER);
    }
    
    public void connect() throws Exception {
        //now try to connect
        RouterService.clearHostCatcher();
        RouterService.connect();
        assertFalse("should not be connected", RouterService.isConnected());
        RouterService.getHostCatcher().add(new Endpoint("localhost", TEST_PORT + 1), true);
        sleep();
        sleep();
        assertTrue("should be connected", RouterService.isConnected());
    }
    
    public void testUltrapeerConnection() throws Exception{
        assertFalse(UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.getValue());
        //set up an ultrapeer capable host:
        setUltrapeerCapabilities();
        ROUTER_SERVICE.start();
        sleep();
        //the node assigner should have worked it's magic
        assertTrue(UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.getValue());
        ULTRAPEER.setAcceptsUltrapeers(true);
        connect();
        assertTrue("should be an ultrapeer", RouterService.getConnectionManager().isActiveSupernode());
        assertTrue("should be passively connected to the DHT", RouterService.isDHTNode() 
                && (RouterService.getDHTMode() != DHTMode.ACTIVE));
        
        //make sure you can't be an active DHT node at the same time
        setDHTCapabilities();
        sleep(200);
        assertFalse(RouterService.getDHTMode().isActive());
    }
    
    public void testLeafToUltrapeerPromotion() throws Exception{
          ROUTER_SERVICE.start();
        connect();
        assertFalse("should be not be an ultrapeer", RouterService.isSupernode());
        PrivilegedAccessor.setValue(RouterService.getAcceptor(),"_acceptedIncoming",new Boolean(true));
        setUltrapeerCapabilities();
        DHTSettings.DHT_TO_ULTRAPEER_PROBABILITY.setValue(1);
        ULTRAPEER.setAcceptsUltrapeers(true);
        PrivilegedAccessor.setValue(ASSIGNER, "_lastUltrapeerAttempt", 
                new Long(System.currentTimeMillis() - 24*3600*1000));
        sleep(1000);
        assertTrue("should be an ultrapeer", RouterService.getConnectionManager().isActiveSupernode());
        assertNotEquals(DHTMode.ACTIVE, RouterService.getDHTMode());
    }
    
    public void testDHTtoUltrapeerSwitch() throws Exception{
        setDHTCapabilities();
        ROUTER_SERVICE.start();
        sleep();
        DHTSettings.DHT_TO_ULTRAPEER_PROBABILITY.setValue(0);
        ULTRAPEER.setAcceptsUltrapeers(false);
        connect();
        sleep();
        assertFalse("should not be an ultrapeer", RouterService.isSupernode());
        assertEquals(DHTMode.ACTIVE, RouterService.getDHTMode());
        ULTRAPEER.setAcceptsUltrapeers(true);
        setUltrapeerCapabilities();
        PrivilegedAccessor.setValue(RouterService.getAcceptor(),"_acceptedIncoming",new Boolean(true));
        DHTSettings.DHT_TO_ULTRAPEER_PROBABILITY.setValue(1);
        PrivilegedAccessor.setValue(ASSIGNER, "_lastUltrapeerAttempt", 
                new Long(System.currentTimeMillis() - 24*3600*1000));
        sleep(2000);
        
        assertNotEquals(DHTMode.ACTIVE, RouterService.getDHTMode());
        assertTrue("should be an ultrapeer", RouterService.isSupernode());
    }
    
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
    
    private static class UltrapeerResponder implements HandshakeResponder {
        public HandshakeResponse respond(HandshakeResponse response, 
                boolean outgoing)  {
            Properties props = new UltrapeerHeaders("localhost"); 
            props.put(HeaderNames.X_DEGREE, "42");           
            return HandshakeResponse.createResponse(props);
        }
        
        public void setLocalePreferencing(boolean b) {}
    }
    
    private static class NoUltrapeerResponder implements HandshakeResponder {
        public HandshakeResponse respond(HandshakeResponse response, 
                boolean outgoing)  {
            Properties props= new UltrapeerHeaders("localhost");
            props.put(HeaderNames.X_ULTRAPEER_NEEDED, "false");
            return HandshakeResponse.createResponse(props);
        }
        
        public void setLocalePreferencing(boolean b) {}
    }
    
    private static class TestUltrapeer implements Runnable{
    
        private boolean acceptsUltrapeers = true;
        
        Connection UP;
        
        private Socket socket;
        
        public void run(){
            while(true) {
                try {
                    ServerSocket ss=new ServerSocket(TEST_PORT+1);
                    socket = ss.accept();
                    ss.close();
                    socket.setSoTimeout(1000);
                    UP = new Connection(socket);
                    if(acceptsUltrapeers) {
                        UP.initialize(null, new UltrapeerResponder());
                    } else {
                        UP.initialize(null, new NoUltrapeerResponder());
                    }
                } catch (Exception e) {
                    ErrorService.error(e);
                }
            }
        }
        
        public void setAcceptsUltrapeers(boolean acceptsUltrapeers) {
            this.acceptsUltrapeers = acceptsUltrapeers;
        }
        
        public void shutdown() throws Exception{
            if(UP != null){
                UP.close();
            }
        }
        
    }
}
