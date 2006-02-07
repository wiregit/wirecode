package com.limegroup.gnutella.auth;

import junit.framework.Test;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.messages.vendor.ContentResponse;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.util.ManagedThread;
import com.limegroup.gnutella.util.PrivilegedAccessor;
 
public class ContentManagerTest extends BaseTestCase {
    
    private static final String S_URN_1 = "urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB";
    private static final String S_URN_2 = "urn:sha1:PLSTHIPQGSSZTS5FJUPAKOZWUGZQYPFB";
    private static final String S_URN_3 = "urn:sha1:PABCDEFGAPOJTS5FJUPAKOZWUGZQYPFB";
    
    private static URN URN_1;
    private static URN URN_2;
    private static URN URN_3;
    
    private ContentManager mgr;
    private ContentResponse crOne;
    private ContentResponse crTwo;
    private Observer one;
    private Observer two;
    
    
    public ContentManagerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(ContentManagerTest.class);
    }
    
    public static void globalSetUp() throws Exception {
       URN_1 = URN.createSHA1Urn(S_URN_1);
       URN_2 = URN.createSHA1Urn(S_URN_2);
       URN_3 = URN.createSHA1Urn(S_URN_3);
    }
    
    public void setUp() throws Exception {
        PrivilegedAccessor.setValue(ContentManager.class, "ACTIVE", Boolean.TRUE);
        mgr = new ContentManager();
        crOne = new ContentResponse(URN_1, true);
        crTwo = new ContentResponse(URN_2, false);
        one = new Observer();
        two = new Observer();
        assertNull(mgr.getResponse(URN_1));
        assertNull(mgr.getResponse(URN_2));        
        assertNull(mgr.getResponse(URN_3));
        assertNull(one.urn);
        assertNull(two.urn);
        assertNull(one.response);
        assertNull(two.response);        
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
       mgr.handleContentResponse(crOne);
       assertNotNull(mgr.getResponse(URN_1));
       assertNull(mgr.getResponse(URN_2));
       
       Response res = mgr.getResponse(URN_1);
       assertTrue(res.isOK());
       
       mgr.handleContentResponse(crTwo);
       assertNotNull(mgr.getResponse(URN_2));
       res = mgr.getResponse(URN_2);
       assertFalse(res.isOK());
    }
    
    /** Makes sure that handleResponse is called rightly. */
    public void testHandleResponseCalled() throws Exception {
        mgr.request(URN_1, one, 0);
        mgr.request(URN_2, two, 0);
        assertNull(one.urn);
        assertNull(two.urn);        
        assertNull(one.response);
        assertNull(two.response);
        
        mgr.handleContentResponse(crOne);
        assertEquals(URN_1, one.urn);
        assertTrue(one.response.isOK());
        assertNull(two.urn);
        assertNull(two.response);
        
        mgr.handleContentResponse(crTwo);
        assertEquals(URN_2, two.urn);
        assertFalse(two.response.isOK());
    }
    
    /** Tests immediate response. */
    public void testImmediateHandleResponse() throws Exception {
        mgr.handleContentResponse(crOne);
        mgr.handleContentResponse(crTwo);
        
        mgr.request(URN_1, one, 0);
        mgr.request(URN_2, two, 0);
        
        assertEquals(URN_1, one.urn);
        assertTrue(one.response.isOK());
        assertEquals(URN_2, two.urn);
        assertFalse(two.response.isOK());
    }
    
    /** Makes sure that stuff times out. */
    public void testTimeout() throws Exception {
        mgr.initialize(); // must start timeout thread.
        mgr.request(URN_1, one, 1);
        mgr.request(URN_2, two, 1);
        
        Thread.sleep(5000);
        
        assertEquals(URN_1, one.urn);
        assertNull(one.response);
        assertEquals(URN_2, two.urn);
        assertNull(two.response);       
    }
    
    /** Checks blocking requests. */
    public void testBlockingRequest() throws Exception {
        mgr.initialize();
        
        Thread responder = new ManagedThread() {
            public void managedRun() {
                ContentManagerTest.sleep(1000);
                mgr.handleContentResponse(crOne);
                ContentManagerTest.sleep(1000);
                mgr.handleContentResponse(crTwo);
            }
        };
        responder.setDaemon(true);
        responder.start();
        
        long now = System.currentTimeMillis();
        Response rOne =  mgr.request(URN_1, 3000);
        assertNotNull(rOne);
        assertTrue(rOne.isOK());
        assertGreaterThan(600, System.currentTimeMillis() - now);
        
        now = System.currentTimeMillis();
        Response rTwo = mgr.request(URN_2, 3000);
        assertNotNull(rTwo);
        assertFalse(rTwo.isOK());
        assertGreaterThan(600, System.currentTimeMillis() - now);
        
        now = System.currentTimeMillis();
        Response rThree = mgr.request(URN_3, 1000);
        assertNull(rThree);
        assertGreaterThan(900, System.currentTimeMillis() - now);
    }
    
    private static void sleep(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch(InterruptedException ix) {
            throw new RuntimeException(ix);
        }
    }
    
    private static class Observer implements ResponseObserver {
        private URN urn;
        private Response response;
        
        public void handleResponse(URN urn, Response response) {
            this.urn = urn;
            this.response = response;
        }
    }
    
}
