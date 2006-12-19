package com.limegroup.gnutella.auth;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import junit.framework.Test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.FileDetails;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.StandardMessageRouter;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.auth.ContentResponseData.Authorization;
import com.limegroup.gnutella.messages.vendor.ContentResponse;
import com.limegroup.gnutella.settings.ContentSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.util.ManagedThread;
 
public class ContentManagerTest extends BaseTestCase {

	private static final Log LOG = LogFactory.getLog(ContentManagerTest.class); 
	
	private static final int DELTA = 30;
	
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
    	
    }
    
    @Override
    public void setUp() throws Exception {
    	URN_1 = URN.createSHA1Urn(S_URN_1);
    	details_1 = new ContentManagerNetworkTest.URNFileDetails(URN_1);
    	URN_2 = URN.createSHA1Urn(S_URN_2);
    	details_2 = new ContentManagerNetworkTest.URNFileDetails(URN_2);
    	URN_3 = URN.createSHA1Urn(S_URN_3);
    	details_3 = new ContentManagerNetworkTest.URNFileDetails(URN_3);
    	
        ContentSettings.CONTENT_MANAGEMENT_ACTIVE.setValue(true);
        ContentSettings.USER_WANTS_MANAGEMENTS.setValue(true);
        ContentSettings.ONLY_SECURE_CONTENT_RESPONSES.setValue(false);
        
        mgr = createManager(new IpPortContentAuthority("127.0.0.1", 9999, true));

        crOne = new ContentResponse(URN_1, Authorization.AUTHORIZED, "True");
        crTwo = new ContentResponse(URN_2, Authorization.UNAUTHORIZED, "False");

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
    
    @Override
    public void tearDown() throws Exception {
        mgr.shutdown();
    }
    
    /**
     * Makes sure that content messages add responses.
     * 
     * @throws Exception
     */
    public void testResponseStored() throws Exception {

//    	synchronized (one) {
    		mgr.request(details_1, one);
    		RouterService.getMessageRouter().handleUDPMessage(crOne, addr);
//    		while (one.response == null) {
//    			one.wait();
//    		}
    		assertNotNull(mgr.getResponse(URN_1));
    		assertNull(mgr.getResponse(URN_2));
//    	}    		

//    	synchronized (two) {
    		mgr.request(details_2, two);
    		RouterService.getMessageRouter().handleUDPMessage(crTwo, addr);
//    		while (two.response == null) {
//    			two.wait();
//    		}
//    	}
    	assertNotNull(mgr.getResponse(URN_2));
        ContentResponseData res = mgr.getResponse(URN_2);
        assertEquals(Authorization.UNAUTHORIZED, res.getAuthorization());
    }
    
    /** Makes sure that handleResponse is called rightly. */
    public void testHandleResponseCalled() throws Exception {
    	LOG.debug("responseCalled");
        mgr.request(details_1, one);
        mgr.request(details_2, two);
        assertNull(one.urn);
        assertNull(two.urn);
        assertNull(one.response);
        assertNull(two.response);

        RouterService.getMessageRouter().handleUDPMessage(crOne, addr);
        assertEquals(URN_1, one.urn);
        assertEquals(Authorization.AUTHORIZED, one.response.getAuthorization());
        assertNull(two.urn);
        assertNull(two.response);

        RouterService.getMessageRouter().handleUDPMessage(crTwo, addr);
        assertEquals(URN_2, two.urn);
        assertEquals(Authorization.UNAUTHORIZED, two.response.getAuthorization());
    }
    
    /** Tests immediate response. */
    public void testImmediateHandleResponse() throws Exception {
    	LOG.debug("testImmediateHandleResponse");
        mgr.request(details_1, one);
        mgr.request(details_2, two);
        RouterService.getMessageRouter().handleUDPMessage(crOne, addr);
        RouterService.getMessageRouter().handleUDPMessage(crTwo, addr);

        one = new Observer();
        two = new Observer();
        mgr.request(details_1, one);
        mgr.request(details_2, two);

        assertEquals(URN_1, one.urn);
        assertEquals(Authorization.AUTHORIZED, one.response.getAuthorization());
        assertEquals(URN_2, two.urn);
        assertEquals(Authorization.UNAUTHORIZED, two.response.getAuthorization());
    }
    

    /** Makes sure that responses without requests aren't processed. */
    public void testResponseWithoutRequest() throws Exception {
    	RouterService.getMessageRouter().handleUDPMessage(crOne, addr);
        assertNull(mgr.getResponse(URN_1));
    }
    
    /** Checks blocking requests. */
    public void testBlockingRequest() throws Exception {
        
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
        ContentResponseData rOne =  mgr.request(details_1);
        assertNotNull(rOne);
        assertEquals(Authorization.AUTHORIZED, rOne.getAuthorization());
        assertGreaterThan(600, System.currentTimeMillis() - now);
        
        now = System.currentTimeMillis();
        ContentResponseData rTwo = mgr.request(details_2);
        assertNotNull(rTwo);
        assertEquals(Authorization.UNAUTHORIZED, rTwo.getAuthorization());
        assertGreaterThan(600, System.currentTimeMillis() - now);
        
        now = System.currentTimeMillis();
        ContentResponseData rThree = mgr.request(details_3);
        assertEquals(Authorization.UNKNOWN, rThree.getAuthorization());
        assertGreaterThan(900, System.currentTimeMillis() - now);
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
        
        public synchronized void handleResponse(URN urn, ContentResponseData response) {
            this.urn = urn;
            this.response = response;
            notify();
        }
    }
    
    public void testSetContentAuthorities() {
    	ContentManager manager = new ContentManager();
    	ContentAuthority[] auths = new ContentAuthority[] {
    			new StubContentAuthority(),
    			new StubContentAuthority()
    	};
    	manager.setContentAuthorities(auths);
    	
    	try {
    		manager.setContentAuthorities(auths);
    	}
    	catch (IllegalStateException ise) {
    		return;
    	}
    	fail("IllegalStateException expected");
    }
    
    public void testRequest() {
    	Map<URN, Authorization> map = new HashMap<URN, Authorization>();
    	map.put(URN_1, Authorization.AUTHORIZED);
    	map.put(URN_2, Authorization.UNAUTHORIZED);
    	
    	ContentManager manager = createManager(map);
    	manager.initialize();
    	ContentResponseData response = manager.request(details_1);
    	assertEquals(Authorization.AUTHORIZED, response.getAuthorization());
    	
    	response = manager.request(details_2);
    	assertEquals(Authorization.UNAUTHORIZED, response.getAuthorization());
    	
    	response = manager.request(details_3);
    	assertEquals(Authorization.UNKNOWN, response.getAuthorization());
    }
    
    public void testFirstRequestReturnsUnknown() {
    	Map<URN, Authorization> map1 = new HashMap<URN, Authorization>();
    	Map<URN, Authorization> map2 = new HashMap<URN, Authorization>();
    	map2.put(URN_1, Authorization.AUTHORIZED);
    	map2.put(URN_2, Authorization.UNAUTHORIZED);
    	
    	ContentManager manager = createManager(map1, map2);
    	manager.initialize();
    	ContentResponseData response = manager.request(details_1);
    	assertEquals(Authorization.AUTHORIZED, response.getAuthorization());
    	
    	response = manager.request(details_2);
    	assertEquals(Authorization.UNAUTHORIZED, response.getAuthorization());
    	
    	response = manager.request(details_3);
    	assertEquals(Authorization.UNKNOWN, response.getAuthorization());
    }
    
    public void testFirstRequestTimesOut() {
    	Map<URN, Authorization> map2 = new HashMap<URN, Authorization>();
    	map2.put(URN_1, Authorization.AUTHORIZED);
    	map2.put(URN_2, Authorization.UNAUTHORIZED);
    	
    	ContentManager manager = createManager(new NotRespondingAuth(),
    					new ContentAuth(map2));
    	manager.initialize();
    	ContentResponseData response = manager.request(details_1);
    	assertEquals(Authorization.AUTHORIZED, response.getAuthorization());
    	
    	response = manager.request(details_2);
    	assertEquals(Authorization.UNAUTHORIZED, response.getAuthorization());
    	
    	response = manager.request(details_3);
    	assertEquals(Authorization.UNKNOWN, response.getAuthorization());
    	manager.shutdown();
    }
    
    public void testUnknownAndTimeouts() { 
    	Map<URN, Authorization> map1 = new HashMap<URN, Authorization>();
    	Map<URN, Authorization> map2 = new HashMap<URN, Authorization>();
    	map2.put(URN_1, Authorization.AUTHORIZED);
    	map2.put(URN_2, Authorization.UNAUTHORIZED);
    	
    	ContentManager manager = createManager(new ContentAuth(map1), new NotRespondingAuth(),
    			new ContentAuth(map2));
    	manager.initialize();
    	ContentResponseData response = manager.request(details_1);
    	assertEquals(Authorization.AUTHORIZED, response.getAuthorization());
    	
    	response = manager.request(details_2);
    	assertEquals(Authorization.UNAUTHORIZED, response.getAuthorization());
    	
    	response = manager.request(details_3);
    	assertEquals(Authorization.UNKNOWN, response.getAuthorization());
    	manager.shutdown();
    }
    
    public void testLastRequestTimesOut() {
    	ContentManager manager = createManager(new NotRespondingAuth());
    	manager.initialize();
    	ContentResponseData response = manager.request(details_1);
    	assertEquals(Authorization.UNKNOWN, response.getAuthorization());
    	
    	response = manager.request(details_2);
    	assertEquals(Authorization.UNKNOWN, response.getAuthorization());
    	
    	response = manager.request(details_3);
    	assertEquals(Authorization.UNKNOWN, response.getAuthorization());
    	manager.shutdown();
    }
    
    public void testSingleAuthorityTimeout() {
    	ContentManager manager = createManager(new NotRespondingAuth(100));
    	manager.initialize();
    	long start = System.currentTimeMillis();
    	ContentResponseData response = manager.request(details_1);
    	long end = System.currentTimeMillis();
    	assertEquals(Authorization.UNKNOWN, response.getAuthorization());
    	assertGreaterThanOrEquals(100, end - start);
    	assertLessThan(100 + DELTA, end - start);
    	manager.shutdown();
    
    	manager = createManager(new NotRespondingAuth(70));
    	manager.initialize();
    	start = System.currentTimeMillis();
    	response = manager.request(details_2);
    	end = System.currentTimeMillis();
    	assertEquals(Authorization.UNKNOWN, response.getAuthorization());
    	assertGreaterThanOrEquals(70, end - start);
    	assertLessThan(70 + DELTA, end - start);
    	manager.shutdown();
    }
    
    public void testAccumlatedTimeouts() {
    	ContentManager manager = createManager(new NotRespondingAuth(100), new NotRespondingAuth(100));
    	manager.initialize();
    	long start = System.currentTimeMillis();
    	ContentResponseData response = manager.request(details_1);
    	long end = System.currentTimeMillis();
    	assertEquals(Authorization.UNKNOWN, response.getAuthorization());
    	assertGreaterThanOrEquals(100, end - start);
    	assertLessThan(200 + DELTA, end - start);
    	manager.shutdown();
    }
    
    /**
     * The first authority answers after its timeout into the second one's
     * timeout period with the repsonse "UNKNOWN". The expected result is
     * the second one's repsonse "UNAUTHORIZED".
     */
    public void testFirstAnswersAfterTimeout() {
    	ContentManager manager = createManagerWithTimeouts();
    	manager.initialize();
    	
    	ContentResponseData response = manager.request(details_2);
    	assertEquals(Authorization.UNAUTHORIZED, response.getAuthorization());
    	manager.shutdown();
    }
    
    
    public void testFirstAnswerAfterTimeoutUnitialized() throws InterruptedException {
    	ContentManager manager = createManagerWithTimeouts();
    	synchronized (one) {
    		manager.request(details_2, one);
    		manager.initialize();
    		one.wait();
    	}
    	assertEquals(Authorization.UNAUTHORIZED, one.response.getAuthorization());
    	manager.shutdown();
    }

    public void testSameRequestTwice() {
    	ContentManager manager = createManagerWithTimeouts();
    	manager.initialize();
    	manager.request(details_1, one);
    	ContentResponseData response = manager.request(details_1);
    	assertEquals(Authorization.AUTHORIZED, one.response.getAuthorization());
    	assertEquals(Authorization.AUTHORIZED, response.getAuthorization());
    	manager.shutdown();
    }
    
    public void testSameRequestTwiceUninitialized() throws InterruptedException {
    	ContentManager manager = createManagerWithTimeouts();
    	manager.request(details_2, one);
    	synchronized (two) {
    		manager.request(details_2, two);
    		manager.initialize();
    		two.wait();
    	}
    	assertEquals(Authorization.UNAUTHORIZED, one.response.getAuthorization());
    	assertEquals(Authorization.UNAUTHORIZED, two.response.getAuthorization());
    	manager.shutdown();
    }
    
    public void testSameRequestTwiceHalfInitialized() {
    	ContentManager manager = createManagerWithTimeouts();
    	manager.request(details_1, one);
    	manager.initialize();
    	ContentResponseData response = manager.request(details_1);
    	assertEquals(Authorization.AUTHORIZED, one.response.getAuthorization());
    	assertEquals(Authorization.AUTHORIZED, response.getAuthorization());
    	manager.shutdown();
    }
    
    private ContentManager createManagerWithTimeouts() {
    	Map<URN, Authorization> map1 = new HashMap<URN, Authorization>();
    	Map<URN, Authorization> map2 = new HashMap<URN, Authorization>();
    	map2.put(URN_1, Authorization.AUTHORIZED);
    	map2.put(URN_2, Authorization.UNAUTHORIZED);
    	return createManager(new RespondingAfterTimeoutAuth(10, 100, map1),
    			new RespondingAfterTimeoutAuth(500, 200, map2));
    }
    
    public void testInitializeShutdown() {
    	ContentManager manager = new ContentManager();
    	manager.initialize();
    	manager.shutdown();
    	manager.initialize();
    	manager.shutdown();
    }
    
    /** Makes sure that isVerified works */
    public void testIsVerified() throws Exception {
        assertFalse(mgr.isVerified(URN_1));
        synchronized (one) {
        	mgr.request(details_1, one);
        	assertFalse(mgr.isVerified(URN_1));
        	while (one.response == null) {
        		one.wait();
        	}
        }
        assertTrue(mgr.isVerified(URN_1)); // verified by timeout (no response).
        
        assertFalse(mgr.isVerified(URN_2));
        mgr.request(details_2, two);
        assertFalse(mgr.isVerified(URN_2));
        RouterService.getMessageRouter().handleUDPMessage(new ContentResponse(URN_2, Authorization.AUTHORIZED, "True"), addr);
        assertTrue(mgr.isVerified(URN_2)); // verified by true response
        
        assertFalse(mgr.isVerified(URN_3));
        mgr.request(details_3, one);
        assertFalse(mgr.isVerified(URN_3));        

        RouterService.getMessageRouter().handleUDPMessage(new ContentResponse(URN_3, Authorization.UNAUTHORIZED, "False"), addr);
        assertTrue(mgr.isVerified(URN_3)); // verified by false response.
    }
    
    /** Makes sure that stuff times out. */
    public void testTimeout() throws Exception {
        
    	synchronized (two) {
    		mgr.request(details_1, one);
    		mgr.request(details_2, two);
    		while (two.response == null) {
    			two.wait();
    		}
    	}
        assertEquals(URN_1, one.urn);
        assertEquals(Authorization.UNKNOWN, one.response.getAuthorization());
        assertEquals(URN_2, two.urn);
        assertEquals(Authorization.UNKNOWN, two.response.getAuthorization());       
    }
    
    private ContentManager createManager(final Map<URN, Authorization>... maps) {
    	List<ContentAuthority> auths = new ArrayList<ContentAuthority>(maps.length);
    	for (Map<URN, Authorization> map : maps) {
    		auths.add(new ContentAuth(map));
    	}
    	return createManager(auths.toArray(new ContentAuthority[0]));
    }
    
    private ContentManager createManager(final ContentAuthority... authorities) {
    	return new ContentManager() {
    		@Override
    		protected ContentAuthority[] getDefaultContentAuthorities() {
    			return authorities;
    		} 
    	};
    }
    
    private class ContentAuth extends AbstractContentAuthority {

    	Map<URN, Authorization> authForURN;
    	
    	ContentAuthorityResponseObserver observer;
    	
    	public ContentAuth(long timeout, Map<URN, Authorization> map) {
    		super(timeout);
    		authForURN = map;
    	}
    	
    	public ContentAuth(Map<URN, Authorization> map) {
    		this(100, map);
    	}
    	

		public void sendAuthorizationRequest(FileDetails details) {
			URN urn = details.getSHA1Urn();
			Authorization auth = authForURN.get(urn);
			if (auth == null) {
				auth = Authorization.UNKNOWN;
			}
			observer.handleResponse(this, urn, new ContentResponseData(System.currentTimeMillis(), auth, "message"));
		}

		public void setContentResponseObserver(ContentAuthorityResponseObserver observer) {
			this.observer = observer;
		}

    }
    
    private class NotRespondingAuth extends AbstractContentAuthority {

    	public NotRespondingAuth() {
    		this(5);
    	}
    	
    	public NotRespondingAuth(long timeout) {
    		super(timeout);
		}
    	
		public void sendAuthorizationRequest(FileDetails details) {
		}

		public void setContentResponseObserver(ContentAuthorityResponseObserver observer) {

		}

    }
    
    /**
     * Replies after a delay to requests.
     */
    private class RespondingAfterTimeoutAuth extends ContentAuth {
    	
    	private Timer timer = new Timer("RespondingAfterTimeoutAuth Timer", true);
    	
    	private final long replyAfter;
    	
		public RespondingAfterTimeoutAuth(long timeout, long replyAfter, Map<URN, Authorization> map) { 
			super(timeout, map);
			this.replyAfter = replyAfter;
		}

		public void sendAuthorizationRequest(final FileDetails details) {
			timer.schedule(new TimerTask() { 
				public void run() {
					RespondingAfterTimeoutAuth.super.sendAuthorizationRequest(details);
				}
			}, replyAfter);
		}
		
		@Override
		public String toString() {
			return "RespondingAfterTimeoutAuth: " + replyAfter;
		} 
    }
}
