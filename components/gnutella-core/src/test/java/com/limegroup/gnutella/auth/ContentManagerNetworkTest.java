package com.limegroup.gnutella.auth;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import junit.framework.Test;

import com.limegroup.gnutella.Acceptor;
import com.limegroup.gnutella.FileDetails;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.StandardMessageRouter;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.auth.ContentResponseData.Authorization;
import com.limegroup.gnutella.messages.vendor.ContentRequest;
import com.limegroup.gnutella.messages.vendor.ContentResponse;
import com.limegroup.gnutella.settings.ContentSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.gnutella.util.IpPortImpl;
import com.limegroup.gnutella.xml.LimeXMLDocument;
 
public class ContentManagerNetworkTest extends BaseTestCase {

	private static final Log LOG = LogFactory.getLog(ContentManagerNetworkTest.class);
	
    private static final String S_URN_1 = "urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB";
    
    private static URN URN_1;
    private static FileDetails details_1;
    
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
        details_1 = new URNFileDetails(URN_1);
    }
    
    @Override
    public void setUp() throws Exception {
        ContentSettings.CONTENT_MANAGEMENT_ACTIVE.setValue(true);
        ContentSettings.USER_WANTS_MANAGEMENTS.setValue(true);
        ContentSettings.ONLY_SECURE_CONTENT_RESPONSES.setValue(false);
        mgr = new ContentManager();
        crOne = new ContentResponse(URN_1, Authorization.AUTHORIZED, "True");
        one = new Observer();
        assertNull(mgr.getResponse(URN_1));
        assertNull(one.urn);
        assertNull(one.response);
    }
    
    @Override
    public void tearDown() throws Exception {
        mgr.shutdown();
    }
    
    public void testMessageSent() throws Exception {
        InetSocketAddress addr = new InetSocketAddress(SEND_PORT);
        DatagramSocket socket = new DatagramSocket(addr);
        socket.setReuseAddress(true);
        socket.setSoTimeout(5000);
        
        mgr.setContentAuthorities(new ContentAuthority[] { 
        		new IpPortContentAuthority(new IpPortImpl("127.0.0.1", socket.getLocalPort()), true)
        });
        mgr.initialize();
        mgr.request(details_1, one);
        
        DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
        socket.receive(packet);
        byte[] read = packet.getData();
        
        ContentRequest expectSentMsg = new ContentRequest(details_1);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        expectSentMsg.write(out);
        byte[] expectSentBytes = out.toByteArray();
        
        assertEquals(expectSentBytes.length, packet.getLength());
        
        // start at 16, because less than that is the GUID which is random.
        for(int i = 16; i < expectSentBytes.length; i++)
            assertEquals("byte[" + i + "] wrong. ", expectSentBytes[i], read[i]);
        
        socket.close();
    }
    
    // TODO fberger
    public void testDelayedRequestSent() throws Exception {
        InetSocketAddress addr = new InetSocketAddress(SEND_PORT);
        final DatagramSocket socket = new DatagramSocket(addr);
        socket.setReuseAddress(true);
        socket.setSoTimeout(5000);
        final IpPort authority = new IpPortImpl("127.0.0.1", socket.getLocalPort());
        
        mgr.shutdown();
        mgr = new ContentManager() {
            protected ContentAuthority[] getDefaultContentAuthorities() {
                return new ContentAuthority[] { new IpPortContentAuthority(authority, true) };
            }
        };
        mgr.request(details_1, one);
        mgr.initialize();
        
        DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
        socket.receive(packet);
        byte[] read = packet.getData();
        

        ContentRequest expectSentMsg = new ContentRequest(details_1);

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
    	LOG.debug("testResponseReceived");
        mgr.shutdown();
        
        mgr = RouterService.getContentManager();
        mgr.setContentAuthorities(new IpPortContentAuthority(new IpPortImpl("127.0.0.1", 5555), true));
        mgr.initialize();
        
//        Thread.yield();
        Thread.sleep(1000);
        synchronized (one) {
        	mgr.request(details_1, one);
        	UDPService.instance().send(crOne, InetAddress.getLocalHost(), LISTEN_PORT);
        	one.wait();
        }
        
        assertNotNull(mgr.getResponse(URN_1));
        assertEquals(Authorization.AUTHORIZED, mgr.getResponse(URN_1).getAuthorization());
        assertTrue(mgr.isVerified(URN_1));
        assertEquals(one.urn, URN_1);
        assertEquals(one.response, mgr.getResponse(URN_1));
    }
    
    private static class Observer implements ContentResponseObserver {
        private URN urn;
        private ContentResponseData response;
        
        public synchronized void handleResponse(URN urn, ContentResponseData response) {
        	LOG.debug(this + "handelResponse " + urn + " " + response);        	
            this.urn = urn;
            this.response = response;
            notify();
        }
    }
    
    public static class URNFileDetails implements FileDetails {

    	private final URN urn;
    	
    	public URNFileDetails(URN urn) {
    		this.urn = urn;
    	}
    	
		public File getFile() {
			return null;
		}

		public String getFileName() {
			return "";
		}

		public long getFileSize() {
			// TODO Auto-generated method stub
			return 0;
		}

		public URN getSHA1Urn() {
			return urn;
		}

		public InetSocketAddress getSocketAddress() {
			// TODO Auto-generated method stub
			return null;
		}

		public Set<URN> getUrns() {
			return new TreeSet<URN>(Arrays.asList(urn)); 
		}

		public LimeXMLDocument getXMLDocument() {
			// TODO Auto-generated method stub
			return null;
		}

		public boolean isFirewalled() {
			// TODO Auto-generated method stub
			return false;
		}
    	
    }
}
