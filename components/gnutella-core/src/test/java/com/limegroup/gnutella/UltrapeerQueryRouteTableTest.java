package com.limegroup.gnutella;

import junit.framework.Test;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.search.HostData;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.xml.MetaFileManager;
import com.sun.java.util.collections.LinkedList;
import com.sun.java.util.collections.List;
import com.sun.java.util.collections.Set;


/**
 * Test to make sure that query routing tables are correctly exchanged between
 * Ultrapeers.
 *
 * ULTRAPEER_1  ----  ULTRAPEER_2
 */
public final class UltrapeerQueryRouteTableTest extends BaseTestCase {

    private static ActivityCallback CALLBACK;        
    private static MetaFileManager FMAN;    
    private static TestMessageRouter MESSAGE_ROUTER;        
    private static RouterService ROUTER_SERVICE;
            
	/**
     * A filename that won't match.
     */
    private static final String noMatch = "junkie junk";

	/**
	 * The central Ultrapeer used in the test.
	 */
	//private static final RouterService ULTRAPEER_2 = 
    //new RouterService(new TestCallback());

    //private static final ReplyHandler REPLY_HANDLER =
    //  new TestReplyHandler();

    private static List REPLIES = new LinkedList();
    
    private static List SENT = new LinkedList();

    public UltrapeerQueryRouteTableTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(UltrapeerQueryRouteTableTest.class);
    }    
   
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
    
    private static void setSettings() throws Exception {
        setStandardSettings();
        //SearchSettings.PROBE_TTL.setValue((byte)1);
		ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);	
		ConnectionSettings.USE_GWEBCACHE.setValue(false);
		ConnectionSettings.WATCHDOG_ACTIVE.setValue(false);
        ConnectionSettings.IGNORE_KEEP_ALIVE.setValue(true);
        ConnectionSettings.PORT.setValue(6332);
    }
    
    public static void globalSetUp() throws Exception {
        setSettings();
        
        launchBackend();

        CALLBACK = new TestCallback();
        FMAN = new MetaFileManager();
        MESSAGE_ROUTER = new TestMessageRouter();
        ROUTER_SERVICE = new RouterService(
            CALLBACK, MESSAGE_ROUTER, FMAN);
        ROUTER_SERVICE.start();
        
        RouterService.connectToHostAsynchronously("localhost", Backend.BACKEND_PORT);    
        Thread.sleep(3000);
        assertTrue("should be connected", RouterService.isConnected());
    }

	public void setUp() throws Exception {
        setSettings();
        REPLIES.clear();
        SENT.clear();
	}

	public void tearDown() throws Exception {
	}
    
    /**
     * Test to make sure we will never send with a TTL of 1 to a 
     * Ultrapeer that doesn't have a hit.
     */
    public void testSentQueryIsNotTTL1() throws Exception {
        assertTrue("should be connected", RouterService.isConnected());
        QueryRequest qr = QueryRequest.createQuery(noMatch, (byte)1);
        sendQuery(qr);        
        Thread.sleep(2000);
        // we will send the query, but with a TTL of 2, not 1, because
        // the ultrapeer doesn't have this query in its qrp table.
        assertTrue("should have sent query", !SENT.isEmpty());
        assertEquals("should not have received any replies", 0, REPLIES.size());
        QueryRequest qSent = (QueryRequest)SENT.get(0);
        assertEquals("wrong ttl", 2, qSent.getTTL());
        assertEquals("wrong hops", 0, qSent.getHops());
        assertEquals("wrong query", qr.getQuery(), qSent.getQuery());
        assertEquals("wrong guid", qr.getGUID(), qSent.getGUID());
	}
    
    /**
     * Test to make sure that queries with one more hop to go are
     * properly routed when the last hop is an Ultrapeer.
     */
    public void testLastHopQueryRouting() throws Exception {
        assertTrue("should be connected", RouterService.isConnected());
                
        QueryRequest qr = QueryRequest.createQuery(
            "FileManager.class." + Backend.SHARED_EXTENSION, (byte)1);
        sendQuery(qr);
        Thread.sleep(4000);
        assertTrue("should have sent query", !SENT.isEmpty());
        assertTrue("should have received replies", !REPLIES.isEmpty());
        
        QueryRequest qSent = (QueryRequest)SENT.get(0);
        assertEquals("wrong ttl", 1, qSent.getTTL());
        assertEquals("wrong hops", 0, qSent.getHops());
        assertEquals("wrong query", qr.getQuery(), qSent.getQuery());
        assertEquals("wrong guid", qr.getGUID(), qSent.getGUID());        
    }


    /**
     * The actual QueryRequest sent will not be the same (==) as this,
     * because QueryHandler creates new queries with appropriate TTLs.
     */
    private static void sendQuery(QueryRequest qr) {
        MessageRouter mr = RouterService.getMessageRouter();
        mr.sendDynamicQuery(qr);
        //mr.broadcastQueryRequest(qr, REPLY_HANDLER);
    }
    
    private static class TestMessageRouter extends StandardMessageRouter {
        public TestMessageRouter() {
            super();
        }
        
        public void originateQuery(QueryRequest r, ManagedConnection c) {
            SENT.add(r);
            super.sendQueryRequest(r, c);
        }
	}

    private static class TestCallback extends ActivityCallbackStub {
        public void handleQueryResult(RemoteFileDesc rfd,
                                      HostData hd,
                                      Set locs) {
            REPLIES.add(new Object());
        }
    }
}
