package com.limegroup.gnutella;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import junit.framework.Test;

import org.limewire.util.PrivilegedAccessor;

import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.search.HostData;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.UltrapeerSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.LimeTestCase;
import com.limegroup.gnutella.util.SocketsManager.ConnectType;


/**
 * Test to make sure that query routing tables are correctly exchanged between
 * Ultrapeers.
 *
 * ULTRAPEER_1  ----  ULTRAPEER_2
 */
@SuppressWarnings("unchecked")
public final class UltrapeerQueryRouteTableTest extends LimeTestCase {


    @SuppressWarnings("unused") //DPINJ - testfix
    private static ActivityCallback CALLBACK;
    @SuppressWarnings("unused") //DPINJ - testfix
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
		ConnectionSettings.WATCHDOG_ACTIVE.setValue(false);
        ConnectionSettings.ALLOW_WHILE_DISCONNECTED.setValue(true);
        ConnectionSettings.PORT.setValue(6332);
        
        UltrapeerSettings.NEED_MIN_CONNECT_TIME.setValue(false);
    }
    
    public static void globalSetUp() throws Exception {
        setSettings();
        
        launchBackend();

        CALLBACK = new TestCallback();
        MESSAGE_ROUTER = new TestMessageRouter();
     //   ROUTER_SERVICE = new RouterService(CALLBACK, MESSAGE_ROUTER);
        ProviderHacks.getLifecycleManager().start();
        
        RouterService.connectToHostAsynchronously("localhost", 
            Backend.BACKEND_PORT, ConnectType.PLAIN);    
        
        // Wait for awhile after the connection to make sure the hosts have 
        // time to exchange QRP tables.
        Thread.sleep(10 * 1000);
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
        QueryRequest qr = ProviderHacks.getQueryRequestFactory().createQuery(noMatch, (byte)1);
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
     * Test to make sure that dynamic querying sends a query with TTL=1 and 
     * other properties when a neighboring Ultrapeer has a hit in its QRP
     * table for that query.
     */
    public void testDynamicQueryingWithQRPHit() throws Exception {
        assertTrue("should be connected", RouterService.isConnected());
                
        QueryRequest qr = ProviderHacks.getQueryRequestFactory().createQuery(
            "FileManagerTest.class." + Backend.SHARED_EXTENSION, (byte)1);
        sendQuery(qr);
        Thread.sleep(4000);
        assertTrue("should have sent query", !SENT.isEmpty());
        assertTrue("should have received replies", !REPLIES.isEmpty());
        
        QueryRequest qSent = (QueryRequest)SENT.get(0);
        
        // The TTL on the sent query should be 1 because the other Ultrapeer
        // should have a "hit" in its QRP table.  When there's a hit, we 
        // send with TTL 1 simply because it's likely that it's popular.
        assertEquals("wrong ttl", 1, qSent.getTTL());
        assertEquals("wrong hops", 0, qSent.getHops());
        assertEquals("wrong query", qr.getQuery(), qSent.getQuery());
        assertEquals("wrong guid", qr.getGUID(), qSent.getGUID());        
    }


    /**
     * The actual QueryRequest sent will not be the same (==) as this,
     * because QueryHandler creates new queries with appropriate TTLs.
     */
    private static void sendQuery(QueryRequest qr) throws Exception {
        ResponseVerifier VERIFIER = (ResponseVerifier)PrivilegedAccessor.getValue(ROUTER_SERVICE, "VERIFIER");
        VERIFIER.record(qr);
        
        MessageRouter mr = ProviderHacks.getMessageRouter();
        mr.sendDynamicQuery(qr);
        //mr.broadcastQueryRequest(qr);
    }
    
    private static class TestMessageRouter extends HackMessageRouter {
        
        public boolean originateQuery(QueryRequest r, ManagedConnection c) {
            SENT.add(r);
            super.sendQueryRequest(r, c);
            return true;
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
