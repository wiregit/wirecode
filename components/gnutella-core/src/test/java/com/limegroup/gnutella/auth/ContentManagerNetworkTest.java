package com.limegroup.gnutella.auth;

import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import junit.framework.Test;

import com.limegroup.gnutella.Acceptor;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.StandardMessageRouter;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.messages.vendor.ContentRequest;
import com.limegroup.gnutella.messages.vendor.ContentResponse;
import com.limegroup.gnutella.settings.ContentSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.gnutella.util.IpPortImpl;
 
public class ContentManagerNetworkTest extends BaseTestCase {
    
    private static final String S_URN_1 = "urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB";
    
    private static URN URN_1;
    
    private ContentManager mgr;
    private ContentResponse crOne;
    private Observer one;
    
    private static final int LISTEN_PORT = 9172;
    private static final int SEND_PORT = 9876;
    
    
    public ContentManagerNetworkTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(ContentManagerNetworkTest.class);
    }
    
    public static void globalSetUp() throws Exception {
        new RouterService(new ActivityCallbackStub(), new StandardMessageRouter());
        RouterService.getMessageRouter().initialize();
        
        new Acceptor().setListeningPort(LISTEN_PORT);
        UDPService.instance().start();
        
        URN_1 = URN.createSHA1Urn(S_URN_1);
    }
    
    public void setUp() throws Exception {
        ContentSettings.CONTENT_MANAGEMENT_ACTIVE.setValue(true);
        ContentSettings.USER_WANTS_MANAGEMENTS.setValue(true);
        mgr = new ContentManager();
        crOne = new ContentResponse(URN_1, true, "True");
        one = new Observer();
        assertNull(mgr.getResponse(URN_1));
        assertNull(one.urn);
        assertNull(one.response);
    }
    
    public void teardown() throws Exception {
        mgr.shutdown();
    }
    
    public void testMessageSent() throws Exception {
        InetSocketAddress addr = new InetSocketAddress(SEND_PORT);
        DatagramSocket socket = new DatagramSocket(addr);
        socket.setReuseAddress(true);
        socket.setSoTimeout(5000);
        
        mgr.setContentAuthority(new IpPortContentAuthority(new IpPortImpl("127.0.0.1", socket.getLocalPort())));
        mgr.request(URN_1, one, 2000);
        
        DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
        socket.receive(packet);
        byte[] read = packet.getData();
        
        ContentRequest expectSentMsg = new ContentRequest(URN_1);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        expectSentMsg.write(out);
        byte[] expectSentBytes = out.toByteArray();
        
        assertEquals(expectSentBytes.length, packet.getLength());
        
        // start at 16, because less than that is the GUID which is random.
        for(int i = 16; i < expectSentBytes.length; i++)
            assertEquals("byte[" + i + "] wrong. ", expectSentBytes[i], read[i]);
        
        socket.close();
    }
    
    public void testDelayedRequestSent() throws Exception {
        InetSocketAddress addr = new InetSocketAddress(SEND_PORT);
        final DatagramSocket socket = new DatagramSocket(addr);
        socket.setReuseAddress(true);
        socket.setSoTimeout(5000);
        final IpPort authority = new IpPortImpl("127.0.0.1", socket.getLocalPort());
        
        mgr.shutdown();
        mgr = new ContentManager() {
            protected ContentAuthority getDefaultContentAuthority() {
                return new IpPortContentAuthority(authority);
            }
        };
        mgr.request(URN_1, one, 2000);
        mgr.initialize();
        
        DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
        socket.receive(packet);
        byte[] read = packet.getData();
        
        ContentRequest expectSentMsg = new ContentRequest(URN_1);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        expectSentMsg.write(out);
        byte[] expectSentBytes = out.toByteArray();
        
        assertGreaterThan(30, expectSentBytes.length);
        assertEquals(expectSentBytes.length, packet.getLength());
        // start at 16, because less than that is the GUID which is random.
        for(int i = 16; i < expectSentBytes.length; i++)
            assertEquals("byte[" + i + "] wrong. ", expectSentBytes[i], read[i]);
        
        socket.close();
    }
    
    public void testResponseReceived() throws Exception {        
        mgr.shutdown();
        mgr = RouterService.getContentManager();
        mgr.request(URN_1, one, 4000);
        UDPService.instance().send(crOne, InetAddress.getLocalHost(), LISTEN_PORT);
        Thread.sleep(1000); // let the message process.
        assertNotNull(mgr.getResponse(URN_1));
        assertTrue(mgr.getResponse(URN_1).isOK());
        assertTrue(mgr.isVerified(URN_1));
        assertEquals(one.urn, URN_1);
        assertEquals(one.response, mgr.getResponse(URN_1));
    }
    
    
    private static class Observer implements ContentResponseObserver {
        private URN urn;
        private ContentResponseData response;
        
        public void handleResponse(URN urn, ContentResponseData response) {
            this.urn = urn;
            this.response = response;
        }
    }
    
}
