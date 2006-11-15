package com.limegroup.gnutella.auth;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import junit.framework.Test;

import com.limegroup.gnutella.FileDetails;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.StandardMessageRouter;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.messages.vendor.ContentResponse;
import com.limegroup.gnutella.settings.ContentSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.gnutella.util.IpPortImpl;
import com.limegroup.gnutella.util.ManagedThread;
 
public class ContentManagerTest extends BaseTestCase {
    
    private static final String S_URN_1 = "urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB";
    private static final String S_URN_2 = "urn:sha1:PLSTHIPQGSSZTS5FJUPAKOZWUGZQYPFB";
    private static final String S_URN_3 = "urn:sha1:PABCDEFGAPOJTS5FJUPAKOZWUGZQYPFB";
    
    private static URN URN_1;
    private static FileDetails details_1;
    private static URN URN_2;
    private static FileDetails details_2;
    private static URN URN_3;
    private static FileDetails details_3;
    
    private ContentManager mgr;
    private ContentResponse crOne;
    private ContentResponse crTwo;
    private Observer one;
    private Observer two;
    private Observer three;
    
    private InetSocketAddress addr;
    
    public ContentManagerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(ContentManagerTest.class);
    }
    
    /**
	 * Runs this test individually.
	 */
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
    
    public static void globalSetUp() throws Exception {
    	URN_1 = URN.createSHA1Urn(S_URN_1);
    	details_1 = new ContentManagerNetworkTest.URNFileDetails(URN_1);
    	URN_2 = URN.createSHA1Urn(S_URN_2);
    	details_2 = new ContentManagerNetworkTest.URNFileDetails(URN_2);
    	URN_3 = URN.createSHA1Urn(S_URN_3);
    	details_3 = new ContentManagerNetworkTest.URNFileDetails(URN_3);
    }
    
    public void setUp() throws Exception {
    	
    	
        ContentSettings.CONTENT_MANAGEMENT_ACTIVE.setValue(true);
        ContentSettings.USER_WANTS_MANAGEMENTS.setValue(true);        
        mgr = new ContentManager() {
        	@Override
        	protected ContentAuthority getDefaultContentAuthority() {
        		try {
					return new IpPortContentAuthority(new IpPortImpl("127.0.0.1", 9999), true);
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return null;
        	}
        };
        crOne = new ContentResponse(URN_1, true, "True");
        crTwo = new ContentResponse(URN_2, false, "False");

        one = new Observer();
        two = new Observer();
        three = new Observer();
        assertNull(mgr.getResponse(URN_1));
        assertNull(mgr.getResponse(URN_2));        
        assertNull(mgr.getResponse(URN_3));
        assertNull(one.urn);
        assertNull(two.urn);
        assertNull(one.response);
        assertNull(two.response);
        
        new RouterService(new ActivityCallbackStub(), new StandardMessageRouter());
        mgr.initialize();
        
        InetAddress address = InetAddress.getByName("64.61.25.171");
    	addr = new InetSocketAddress(address, 10000);
    }
    
    public void teardown() throws Exception {
        mgr.shutdown();
    }
    

    /**
     * Makes sure that content messages add responses.
     * 
     * @throws Exception
     */
    public void testResponseStored() throws Exception {
    	
        mgr.request(details_1, one, 0);
        RouterService.getMessageRouter().handleUDPMessage(crOne, addr);
        assertNotNull(mgr.getResponse(URN_1));
        assertNull(mgr.getResponse(URN_2));

        ContentResponseData res = mgr.getResponse(URN_1);
        assertTrue(res.isOK());

        mgr.request(details_2, two, 0);
        RouterService.getMessageRouter().handleUDPMessage(crTwo, addr);
        assertNotNull(mgr.getResponse(URN_2));
        res = mgr.getResponse(URN_2);
        assertFalse(res.isOK());
    }
    
    /** Makes sure that handleResponse is called rightly. */
    public void testHandleResponseCalled() throws Exception {
        mgr.request(details_1, one, 0);
        mgr.request(details_2, two, 0);
        assertNull(one.urn);
        assertNull(two.urn);
        assertNull(one.response);
        assertNull(two.response);

        RouterService.getMessageRouter().handleUDPMessage(crOne, addr);
        assertEquals(URN_1, one.urn);
        assertTrue(one.response.isOK());
        assertNull(two.urn);
        assertNull(two.response);

        RouterService.getMessageRouter().handleUDPMessage(crTwo, addr);
        assertEquals(URN_2, two.urn);
        assertFalse(two.response.isOK());
    }
    
    /** Tests immediate response. */
    public void testImmediateHandleResponse() throws Exception {
        mgr.request(details_1, one, 0);
        mgr.request(details_2, two, 0);
        RouterService.getMessageRouter().handleUDPMessage(crOne, addr);
        RouterService.getMessageRouter().handleUDPMessage(crTwo, addr);

        one = new Observer();
        two = new Observer();
        mgr.request(details_1, one, 0);
        mgr.request(details_2, two, 0);

        assertEquals(URN_1, one.urn);
        assertTrue(one.response.isOK());
        assertEquals(URN_2, two.urn);
        assertFalse(two.response.isOK());
    }
    
    /** Makes sure that stuff times out. */
    public void testTimeout() throws Exception {
        mgr.initialize(); // must start timeout thread.
        mgr.request(details_1, one, 1);
        mgr.request(details_2, two, 1);
        
        Thread.sleep(5000);
        
        assertEquals(URN_1, one.urn);
        assertNull(one.response);
        assertEquals(URN_2, two.urn);
        assertNull(two.response);       
    }
    
    /** Makes sure that responses without requests aren't processed. */
    public void testResponseWithoutRequest() throws Exception {
    	RouterService.getMessageRouter().handleUDPMessage(crOne, addr);
        assertNull(mgr.getResponse(URN_1));
    }
    
    /** Makes sure that isVerified works */
    public void testIsVerified() throws Exception {
        mgr.initialize();
        assertFalse(mgr.isVerified(URN_1));
        mgr.request(details_1, one, 2000);
        assertFalse(mgr.isVerified(URN_1));
        Thread.sleep(5000);
        assertTrue(mgr.isVerified(URN_1)); // verified by timeout (no response).
        
        assertFalse(mgr.isVerified(URN_2));
        mgr.request(details_2, two, 2000);
        assertFalse(mgr.isVerified(URN_2));
        RouterService.getMessageRouter().handleUDPMessage(new ContentResponse(URN_2, true, "True"), addr);
        assertTrue(mgr.isVerified(URN_2)); // verified by true response
        
        assertFalse(mgr.isVerified(URN_3));
        mgr.request(details_3, one, 2000);
        assertFalse(mgr.isVerified(URN_3));        

        RouterService.getMessageRouter().handleUDPMessage(new ContentResponse(URN_3, false, "False"), addr);
        assertTrue(mgr.isVerified(URN_3)); // verified by false response.
    }
    
    /** Checks blocking requests. */
    public void testBlockingRequest() throws Exception {
        mgr.initialize();
        
        Thread responder = new ManagedThread() {
            public void managedRun() {
                ContentManagerTest.sleep(1000);
                RouterService.getMessageRouter().handleUDPMessage(crOne, addr);
                ContentManagerTest.sleep(1000);
                RouterService.getMessageRouter().handleUDPMessage(crTwo, addr);
            }
        };
        responder.setDaemon(true);
        responder.start();
        
        long now = System.currentTimeMillis();
        ContentResponseData rOne =  mgr.request(details_1, 3000);
        assertNotNull(rOne);
        assertTrue(rOne.isOK());
        assertGreaterThan(600, System.currentTimeMillis() - now);
        
        now = System.currentTimeMillis();
        ContentResponseData rTwo = mgr.request(details_2, 3000);
        assertNotNull(rTwo);
        assertFalse(rTwo.isOK());
        assertGreaterThan(600, System.currentTimeMillis() - now);
        
        now = System.currentTimeMillis();
        ContentResponseData rThree = mgr.request(details_3, 1000);
        assertNull(rThree);
        assertGreaterThan(900, System.currentTimeMillis() - now);
    }
    
    public void testVaryingTimeouts() throws Exception {
        mgr.initialize();
        mgr.request(details_1, one, 6000);
        mgr.request(details_2, two, 2000);
        mgr.request(details_3, three, 10000);
        
        assertNull(one.urn);
        assertNull(two.urn);
        assertNull(three.urn);
        
        Thread.sleep(4000);
        assertEquals(URN_2, two.urn);
        assertNull(two.response);
        assertNull(one.urn);
        assertNull(three.urn);
        
        Thread.sleep(4000);
        assertEquals(URN_1, one.urn);
        assertNull(one.response);
        assertNull(three.urn);
        
        Thread.sleep(4000);
        assertEquals(URN_3, three.urn);
        assertNull(three.response);
    }
    
    private static void sleep(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch(InterruptedException ix) {
            throw new RuntimeException(ix);
        }
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
