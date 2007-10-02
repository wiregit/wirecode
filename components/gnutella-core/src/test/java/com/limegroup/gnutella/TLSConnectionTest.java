package com.limegroup.gnutella;

import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.util.List;

import junit.framework.Test;

import com.google.inject.Injector;
import com.limegroup.gnutella.handshaking.HeadersFactory;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.PingRequestFactory;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.settings.UltrapeerSettings;
import com.limegroup.gnutella.util.EmptyResponder;
import com.limegroup.gnutella.util.LimeTestCase;
import com.limegroup.gnutella.util.SocketsManager.ConnectType;

public class TLSConnectionTest extends LimeTestCase {
    
    private static int PORT = 9999;

    private Injector injector;

    private ConnectionManager connectionManager;

    private ConnectionFactory connectionFactory;

    private HeadersFactory headersFactory;

    private PingRequestFactory pingRequestFactory;
   
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
   
    public void setUp() throws Exception {
        injector = LimeTestUtils.createInjector();
        
        PORT++; // TODO: Remove hack to override port
        
        setSettings();
        

        assertEquals("unexpected port", PORT, ConnectionSettings.PORT.getValue());
        injector.getInstance(LifecycleManager.class).start();

        
        connectionManager = injector.getInstance(ConnectionManager.class);
        connectionManager.connect();
        
        connectionFactory = injector.getInstance(ConnectionFactory.class);
        
        headersFactory = injector.getInstance(HeadersFactory.class);
        
        pingRequestFactory = injector.getInstance(PingRequestFactory.class);
    }

    public void testTLSConnectionBlockingConnect() throws Exception {
        Connection c = connectionFactory.createConnection("localhost", PORT, ConnectType.TLS);
        assertTrue(c.isTLSCapable());
        assertEquals(0, c.getBytesReceived());
        assertEquals(0, c.getBytesSent());
        assertEquals(0, c.getUncompressedBytesReceived());
        assertEquals(0, c.getUncompressedBytesSent());
        assertEquals(0.0f, c.getSentLostFromSSL());
        assertEquals(0.0f, c.getReadLostFromSSL());
        
        assertEquals(0, connectionManager.getNumConnections());
        c.initialize(headersFactory.createLeafHeaders("localhost"), new EmptyResponder(), 1000);
        drain(c);
        assertGreaterThan(0, c.getSentLostFromSSL());
        assertGreaterThan(0, c.getReadLostFromSSL());
        System.out.println("sent: " + c.getSentLostFromSSL() + ", read: " + c.getReadLostFromSSL());
        assertEquals(1, connectionManager.getNumConnections());
        
        ConnectionManager manager = connectionManager;
        List<ManagedConnection> l = manager.getConnections();
        assertEquals(1, l.size());
        ManagedConnection mc = l.get(0);
        assertTrue(mc.isTLSCapable());
        
        PingRequest pr = pingRequestFactory.createUDPPing();
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
        Connection c = connectionFactory.createConnection("localhost", PORT, ConnectType.TLS);
        assertTrue(c.isTLSCapable());
        assertEquals(0, c.getBytesReceived());
        assertEquals(0, c.getBytesSent());
        assertEquals(0, c.getUncompressedBytesReceived());
        assertEquals(0, c.getUncompressedBytesSent());
        assertEquals(0.0f, c.getSentLostFromSSL());
        assertEquals(0.0f, c.getReadLostFromSSL());
        
        assertEquals(0, connectionManager.getNumConnections());
        StubGnetConnectObserver connector = new StubGnetConnectObserver();
        c.initialize(headersFactory.createLeafHeaders("localhost"), new EmptyResponder(), 1000, connector);
        connector.waitForResponse(1000);
        assertTrue(connector.isConnect());
        drain(c);
        assertGreaterThan(0, c.getSentLostFromSSL());
        assertGreaterThan(0, c.getReadLostFromSSL());
        assertEquals(1, connectionManager.getNumConnections());
        
        ConnectionManager manager = connectionManager;
        List<ManagedConnection> l = manager.getConnections();
        assertEquals(1, l.size());
        ManagedConnection mc = l.get(0);
        assertTrue(mc.isTLSCapable());
        
        PingRequest pr = pingRequestFactory.createUDPPing();
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
