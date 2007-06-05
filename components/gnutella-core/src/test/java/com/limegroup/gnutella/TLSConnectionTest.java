package com.limegroup.gnutella;

import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.util.List;

import junit.framework.Test;

import com.limegroup.gnutella.handshaking.LeafHeaders;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.settings.UltrapeerSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.EmptyResponder;
import com.limegroup.gnutella.util.LimeTestCase;
import com.limegroup.gnutella.util.Sockets.ConnectType;

public class TLSConnectionTest extends LimeTestCase {
    
    private static final int PORT = 9999;
    private static RouterService ROUTER;
    
    public TLSConnectionTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(TLSConnectionTest.class);
    }    

    public static void main(String argv[]) {
        junit.textui.TestRunner.run(suite());
    }
    
    private static void setSettings() throws Exception {
        String localIP = InetAddress.getLocalHost().getHostAddress();
        FilterSettings.BLACK_LISTED_IP_ADDRESSES.setValue(new String[] {"*.*.*.*"});
        FilterSettings.WHITE_LISTED_IP_ADDRESSES.setValue(new String[] {"127.*.*.*", "192.168.*.*", "10.254.*.*", localIP});
        ConnectionSettings.PORT.setValue(PORT);
        ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
        UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(true);
        UltrapeerSettings.DISABLE_ULTRAPEER_MODE.setValue(false);
        UltrapeerSettings.FORCE_ULTRAPEER_MODE.setValue(true);
        UltrapeerSettings.MAX_LEAVES.setValue(33);
        ConnectionSettings.NUM_CONNECTIONS.setValue(33);
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        ConnectionSettings.WATCHDOG_ACTIVE.setValue(false);
        UltrapeerSettings.NEED_MIN_CONNECT_TIME.setValue(false);
    }
    
    public static void globalSetUp() throws Exception {
        setSettings();
        ROUTER = new RouterService(new ActivityCallbackStub());
        assertEquals("unexpected port", PORT, ConnectionSettings.PORT.getValue());
        ROUTER.start();
    }
    
    public void setUp() throws Exception {
        setSettings();
        RouterService.clearHostCatcher();
        RouterService.getConnectionManager().disconnect(false);
        RouterService.getConnectionManager().connect();
    }

    public void testTLSConnectionBlockingConnect() throws Exception {
        Connection c = new Connection("localhost", PORT, ConnectType.TLS);
        assertTrue(c.isTLSCapable());
        assertEquals(0, c.getBytesReceived());
        assertEquals(0, c.getBytesSent());
        assertEquals(0, c.getUncompressedBytesReceived());
        assertEquals(0, c.getUncompressedBytesSent());
        assertEquals(0.0f, c.getSentLostFromSSL());
        assertEquals(0.0f, c.getReadLostFromSSL());
        
        assertEquals(0, RouterService.getConnectionManager().getNumConnections());
        c.initialize(new LeafHeaders("localhost"), new EmptyResponder(), 1000);
        drain(c);
        assertGreaterThan(0, c.getSentLostFromSSL());
        assertGreaterThan(0, c.getReadLostFromSSL());
        System.out.println("sent: " + c.getSentLostFromSSL() + ", read: " + c.getReadLostFromSSL());
        assertEquals(1, RouterService.getConnectionManager().getNumConnections());
        
        ConnectionManager manager = RouterService.getConnectionManager();
        List<ManagedConnection> l = manager.getConnections();
        assertEquals(1, l.size());
        ManagedConnection mc = l.get(0);
        assertTrue(mc.isTLSCapable());
        
        PingRequest pr = PingRequest.createUDPPing();
        mc.send(pr);
        mc.flush();
        Message readPr = c.receive();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        pr.write(out);
        byte[] expected = out.toByteArray();
        out.reset();
        readPr.write(out);
        assertEquals(expected, out.toByteArray());
        
        assertGreaterThan(0, mc.getSentLostFromSSL());
        assertGreaterThan(0, mc.getReadLostFromSSL());
    }
    
    public void testTLSConnectionNonBlockingConnect() throws Exception {
        Connection c = new Connection("localhost", PORT, ConnectType.TLS);
        assertTrue(c.isTLSCapable());
        assertEquals(0, c.getBytesReceived());
        assertEquals(0, c.getBytesSent());
        assertEquals(0, c.getUncompressedBytesReceived());
        assertEquals(0, c.getUncompressedBytesSent());
        assertEquals(0.0f, c.getSentLostFromSSL());
        assertEquals(0.0f, c.getReadLostFromSSL());
        
        assertEquals(0, RouterService.getConnectionManager().getNumConnections());
        StubGnetConnectObserver connector = new StubGnetConnectObserver();
        c.initialize(new LeafHeaders("localhost"), new EmptyResponder(), 1000, connector);
        connector.waitForResponse(1000);
        assertTrue(connector.isConnect());
        drain(c);
        assertGreaterThan(0, c.getSentLostFromSSL());
        assertGreaterThan(0, c.getReadLostFromSSL());
        assertEquals(1, RouterService.getConnectionManager().getNumConnections());
        
        ConnectionManager manager = RouterService.getConnectionManager();
        List<ManagedConnection> l = manager.getConnections();
        assertEquals(1, l.size());
        ManagedConnection mc = l.get(0);
        assertTrue(mc.isTLSCapable());
        
        PingRequest pr = PingRequest.createUDPPing();
        mc.send(pr);
        mc.flush();
        Message readPr = c.receive();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        pr.write(out);
        byte[] expected = out.toByteArray();
        out.reset();
        readPr.write(out);
        assertEquals(expected, out.toByteArray());
        
        assertGreaterThan(0, mc.getSentLostFromSSL());
        assertGreaterThan(0, mc.getReadLostFromSSL());
    }    
    
}
