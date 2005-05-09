package com.limegroup.gnutella;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.Collection;
import java.util.Set;

import junit.framework.Test;

import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.vendor.HeadPing;
import com.limegroup.gnutella.messages.vendor.HeadPong;
import com.limegroup.gnutella.messages.vendor.VendorMessage;
import com.limegroup.gnutella.routing.QueryRouteTable;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.UltrapeerSettings;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.stubs.FileDescStub;
import com.limegroup.gnutella.stubs.ReplyHandlerStub;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.util.LeafConnection;
import com.limegroup.gnutella.util.NewConnection;
import com.limegroup.gnutella.util.OldConnection;
import com.limegroup.gnutella.util.PrivilegedAccessor;
import com.limegroup.gnutella.util.TestConnection;
import com.limegroup.gnutella.util.TestConnectionManager;
import com.limegroup.gnutella.xml.MetaFileManager;

/**
 * This class tests the <tt>MessageRouter</tt>.
 */
public final class MessageRouterTest extends BaseTestCase {

    /**
     * Handle to the <tt>MessageRouter</tt> for all tests to use
     */
    private static MessageRouter ROUTER;

    private static final int NUM_CONNECTIONS = 20;

    /**
     * Constant array for the keywords that I should have (this node).
     */
    private static final String[] MY_KEYWORDS = {
        "me",
    };

    public MessageRouterTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(MessageRouterTest.class);
    }    
   
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}  

    public static void globalSetUp() throws Exception {
        RouterService.getAcceptor().setAddress(InetAddress.getLocalHost());
        //TestConnectionManager tcm = new TestConnectionManager(4);
        //PrivilegedAccessor.setValue(RouterService.class, "manager", tcm);
        ROUTER = new StandardMessageRouter();
        ROUTER.initialize();
        Thread.sleep(5000);
    }


    /**
     * Tests the method for forwarding queries to leaves.
     * 
     * @throws Exception if any error in the test occurs
     */
    public void testForwardQueryRequestToLeaves() throws Exception  {
        TestConnectionManager tcm = 
            TestConnectionManager.createManagerWithVariedLeaves();
        PrivilegedAccessor.setValue(RouterService.class, "manager", tcm);
        PrivilegedAccessor.setValue(ROUTER, "_manager", tcm);
        Class[] paramTypes = new Class[] {
            QueryRequest.class,
            ReplyHandler.class,
        };

        Method m = 
            PrivilegedAccessor.getMethod(ROUTER, 
                "forwardQueryRequestToLeaves",
                paramTypes);
        
        QueryRequest qr = 
            QueryRequest.createQuery(LeafConnection.ALT_LEAF_KEYWORD);
        ReplyHandler rh = new ManagedConnection("localhost", 6346);
        Object[] params = new Object[]  {qr, rh};
        m.invoke(ROUTER, params);
        int numQueries = tcm.getNumLeafQueries();
        assertEquals("unexpected number of queries received by leaves", 
            UltrapeerSettings.MAX_LEAVES.getValue()/2, numQueries);

        tcm = TestConnectionManager.createManager();
        PrivilegedAccessor.setValue(RouterService.class, "manager", tcm);
        PrivilegedAccessor.setValue(ROUTER, "_manager", tcm);

        m = PrivilegedAccessor.getMethod(ROUTER, 
                    "forwardQueryRequestToLeaves",
                    paramTypes);
        qr = QueryRequest.createQuery(LeafConnection.LEAF_KEYWORD);
        params[0] = qr;
        m.invoke(ROUTER, params);
        numQueries = tcm.getNumLeafQueries();
        assertEquals("unexpected number of queries received by leaves", 
            UltrapeerSettings.MAX_LEAVES.getValue()/4, numQueries); 
    }

    
    /**
     * Tests the method for creating <tt>QueryReply</tt> instances from
     * <tt>Response</tt> arrays.
     */
    public void testResponsesToQueryReplies() throws Exception {
        ConnectionSettings.LAST_FWT_STATE.setValue(true); 

        Class[] paramTypes = new Class[] {
            Response[].class, 
            QueryRequest.class,
            Integer.TYPE,
        };
        
		Method m = 
            PrivilegedAccessor.getMethod(ROUTER, 
                                         "responsesToQueryReplies",
                                         paramTypes);
        Response[] res = new Response[20];
        Arrays.fill(res, new Response((long)0, (long)10, "test"));
        QueryRequest query = QueryRequest.createQuery("test");
        Object[] params = new Object[] {
            res,
            query,
            new Integer(10),
        };

        Iterator iter = (Iterator)m.invoke(ROUTER, params);
        int size = 0;
        while(iter.hasNext()) {
            iter.next();
            size++;
        }

        assertEquals("responses should have been put in 2 hits", 2, size);
        
        // make sure the query has high hops to test the threshold
        query.hop();
        query.hop();
        query.hop();

        iter = (Iterator)m.invoke(ROUTER, params);
        size = 0;
        while(iter.hasNext()) {
            iter.next();
            size++;
        }

        assertEquals("responses should have been put in 1 hits", 1, size);

        params[2] = new Integer(1);
        iter = (Iterator)m.invoke(ROUTER, params);
        size = 0;
        while(iter.hasNext()) {
            iter.next();
            size++;
        }

        assertEquals("responses should have been put in 20 hits", 20, size);
    }

    /**
     * Test to make sure that the query route tables are forwarded correctly
     * between Ultrapeers.
     */
    public void testIntraUltrapeerForwardQueryRouteTables() throws Exception {
        TestConnectionManager tcm = new TestConnectionManager(NUM_CONNECTIONS);
        FileManager fm = new TestFileManager();
        PrivilegedAccessor.setValue(RouterService.class, "manager", tcm);
        PrivilegedAccessor.setValue(ROUTER, "_manager", tcm);
        PrivilegedAccessor.setValue(MessageRouter.class, "_fileManager", fm);
        Class[] params = new Class[] {};
		Method m = 
            PrivilegedAccessor.getMethod(ROUTER, 
                                         "forwardQueryRouteTables",
                                         params);        
        m.invoke(ROUTER, new Object[]{});
        
        List connections = tcm.getInitializedConnections();
        Iterator iter = connections.iterator();
        while(iter.hasNext()) {
            TestConnection tc = (TestConnection)iter.next();
            QueryRouteTable qrt = tc.getQueryRouteTable();            
            assertTrue("route tables should match",tcm.runQRPMatch(qrt));
        }
    }


    /**
     * Test to make sure that the method to forward query requests
     * to other hosts is working correctly.
     */
    public void testForwardLimitedQueryToUltrapeers() throws Exception {
        TestConnectionManager tcm = new TestConnectionManager(4);
        PrivilegedAccessor.setValue(RouterService.class, "manager", tcm);
        PrivilegedAccessor.setValue(ROUTER, "_manager", tcm);
        Class[] params = new Class[]{QueryRequest.class, ReplyHandler.class};
		Method m = 
            PrivilegedAccessor.getMethod(ROUTER, 
                                         "forwardLimitedQueryToUltrapeers",
                                         params);        

        QueryRequest qr = QueryRequest.createQuery("test");      
        ReplyHandler rh = new OldConnection(10);
        
        m.invoke(ROUTER, new Object[] {qr, rh});
        assertEquals("unexpected number of queries sent", 15, 
                     tcm.getNumUltrapeerQueries());
        assertEquals("unexpected number of queries old sent", 15, 
                     tcm.getNumOldConnectionQueries());
        assertEquals("unexpected number of queries new sent", 0, 
                     tcm.getNumNewConnectionQueries());
    }


    /**
     * Tests the method for adding query routing entries to the
     * QRP table for this node, adding the leaves' QRP tables if
     * we're an Ultrapeer.
     */
    public void testCreateRouteTable() throws Exception {
        TestConnectionManager tcm = 
            new TestConnectionManager(NUM_CONNECTIONS);
        FileManager fm = new TestFileManager();
        PrivilegedAccessor.setValue(RouterService.class, "manager", tcm);
        PrivilegedAccessor.setValue(ROUTER, "_manager", tcm);        
        PrivilegedAccessor.setValue(MessageRouter.class, "_fileManager", fm);
        Class[] params = new Class[] {};
		Method m = 
            PrivilegedAccessor.getMethod(ROUTER, 
                                         "createRouteTable",
                                         params);             
        QueryRouteTable qrt = 
            (QueryRouteTable)m.invoke(ROUTER, new Object[] {});
        tcm.runQRPMatch(qrt);
    }


    /**
     * Test to make sure that queries from old connections are forwarded
     * to new connections if only new connections are available.
     */
    public void testForwardOldQueriesToNewConnections() throws Exception {
        // reset the connection manager
        TestConnectionManager tcm = 
            new TestConnectionManager(NUM_CONNECTIONS);
        PrivilegedAccessor.setValue(RouterService.class, "manager", tcm);
        PrivilegedAccessor.setValue(ROUTER, "_manager", tcm);
        Class[] params = new Class[]{QueryRequest.class, ReplyHandler.class};
		Method m = 
            PrivilegedAccessor.getMethod(ROUTER, 
                                         "forwardLimitedQueryToUltrapeers",
                                         params);        
        QueryRequest qr = QueryRequest.createQuery("test");      
        ReplyHandler rh = new OldConnection(10);
        m.invoke(ROUTER, new Object[] {qr, rh});
        assertEquals("unexpected number of queries sent", 15, 
                     tcm.getNumUltrapeerQueries());
        assertEquals("unexpected number of queries sent", 0, 
                     tcm.getNumOldConnectionQueries());
        assertEquals("unexpected number of queries sent", 15, 
                     tcm.getNumNewConnectionQueries());
    }

    /** 
     * Test to make sure we send a query from an old connection to new 
     * connections if new connections are available, but that we
     * still check the routing tables when the connections are new
     * and the query is on the last hop.
     */
    public void testOldConnectionQueryLastHopForwardingToNewConnection() 
        throws Exception {
        // make sure we send a query from an old connection to new 
        // connections if new connections are available.

        // reset the connection manager
        TestConnectionManager tcm = new TestConnectionManager(NUM_CONNECTIONS);
        PrivilegedAccessor.setValue(RouterService.class, "manager", tcm);
        PrivilegedAccessor.setValue(ROUTER, "_manager", tcm);
        Class[] params = new Class[]{QueryRequest.class, ReplyHandler.class};
		Method m = 
            PrivilegedAccessor.getMethod(ROUTER, 
                                         "forwardLimitedQueryToUltrapeers",
                                         params);        

        QueryRequest qr = QueryRequest.createQuery("test", (byte)1);      
        ReplyHandler rh = new OldConnection(10);
        m.invoke(ROUTER, new Object[] {qr, rh});
        assertEquals("unexpected number of queries sent", 0, tcm.getNumUltrapeerQueries());
        assertEquals("unexpected number of queries sent", 0, 
                     tcm.getNumOldConnectionQueries());
        assertEquals("unexpected number of queries sent", 0, 
                     tcm.getNumNewConnectionQueries());
    }

    /** 
     * Test to make sure we send a query from an old connection to an
     * old connection even when it's the last hop -- that we ignore
     * last-hop QRP on old connections.
     */
    public void testOldConnectionQueriesIgnoreLastHopQRP() 
        throws Exception {
        // make sure we send a query from an old connection to new 
        // connections if new connections are available.

        // reset the connection manager
        TestConnectionManager tcm = new TestConnectionManager(0);
        PrivilegedAccessor.setValue(RouterService.class, "manager", tcm);
        PrivilegedAccessor.setValue(ROUTER, "_manager", tcm);
        Class[] params = new Class[]{QueryRequest.class, ReplyHandler.class};
		Method m = 
            PrivilegedAccessor.getMethod(ROUTER, 
                                         "forwardLimitedQueryToUltrapeers",
                                         params);        

        QueryRequest qr = QueryRequest.createQuery("test", (byte)1);      
        ReplyHandler rh = new OldConnection(10);
        m.invoke(ROUTER, new Object[] {qr, rh});
        // make sure we send a query from an old connection to old 
        // connections even when it's the last hop
        assertEquals("unexpected number of queries sent", 15, 
                     tcm.getNumUltrapeerQueries());
        assertEquals("unexpected number of queries sent", 15, 
                     tcm.getNumOldConnectionQueries());
        assertEquals("unexpected number of queries sent", 0, 
                     tcm.getNumNewConnectionQueries());
    }

    /**
     * Test to make sure that the method to forward query requests
     * from new-style (high out-degree) hosts to others is working
     * correctly when only some of the connections are new.
     */
    public void testForwardQueryToUltrapeers() throws Exception {
        TestConnectionManager tcm = new TestConnectionManager(4);
        PrivilegedAccessor.setValue(RouterService.class, "manager", tcm);
        PrivilegedAccessor.setValue(ROUTER, "_manager", tcm);
        Class[] params = new Class[]{QueryRequest.class, ReplyHandler.class};
		Method m = 
            PrivilegedAccessor.getMethod(ROUTER, 
                                         "forwardQueryToUltrapeers",
                                         params);        

        QueryRequest qr = QueryRequest.createQuery("test");      
        ReplyHandler rh = NewConnection.createConnection(10);
        
        m.invoke(ROUTER, new Object[] {qr, rh});
        assertEquals("unexpected number of queries sent", NUM_CONNECTIONS, 
                     tcm.getNumUltrapeerQueries());
        assertEquals("unexpected number of queries sent", NUM_CONNECTIONS-4, 
                     tcm.getNumOldConnectionQueries());
        assertEquals("unexpected number of queries sent", 4, 
                     tcm.getNumNewConnectionQueries());
    }

    /**
     * Test to make sure that the method to forward query requests
     * from new-style (high out-degree) hosts to others is working
     * correctly when all of the connections are new
     */
    public void testForwardQueryToAllNewUltrapeers() throws Exception {
        // reset the connection manager using all new connections
        TestConnectionManager tcm = new TestConnectionManager(NUM_CONNECTIONS);
        PrivilegedAccessor.setValue(RouterService.class, "manager", tcm);
        PrivilegedAccessor.setValue(ROUTER, "_manager", tcm);
        Class[] params = new Class[]{QueryRequest.class, ReplyHandler.class};
		Method m = 
            PrivilegedAccessor.getMethod(ROUTER, 
                                         "forwardQueryToUltrapeers",
                                         params);        

        QueryRequest qr = QueryRequest.createQuery("test");      
        ReplyHandler rh = NewConnection.createConnection(10);
        m.invoke(ROUTER, new Object[] {qr, rh});
        assertEquals("unexpected number of queries sent", NUM_CONNECTIONS, 
                     tcm.getNumUltrapeerQueries());
        assertEquals("unexpected number of queries sent", 0, 
                     tcm.getNumOldConnectionQueries());
        assertEquals("unexpected number of queries sent", NUM_CONNECTIONS, 
                     tcm.getNumNewConnectionQueries());
    }

    /**
     * Test to make sure that the method to forward query requests
     * from new-style (high out-degree) hosts to others is working
     * correctly when none of the connections are new.
     */
    public void testForwardQueryToNoNewUltrapeers() throws Exception {
        // reset the connection manager using all old connections
        TestConnectionManager tcm = new TestConnectionManager(0);
        PrivilegedAccessor.setValue(RouterService.class, "manager", tcm);
        PrivilegedAccessor.setValue(ROUTER, "_manager", tcm);
        Class[] params = new Class[]{QueryRequest.class, ReplyHandler.class};
		Method m = 
            PrivilegedAccessor.getMethod(ROUTER, 
                                         "forwardQueryToUltrapeers",
                                         params);        

        QueryRequest qr = QueryRequest.createQuery("test");      
        ReplyHandler rh = NewConnection.createConnection(10);
        m.invoke(ROUTER, new Object[] {qr, rh});
        assertEquals("unexpected number of queries sent", NUM_CONNECTIONS, 
                     tcm.getNumUltrapeerQueries());
        assertEquals("unexpected number of queries sent", NUM_CONNECTIONS, 
                     tcm.getNumOldConnectionQueries());
        assertEquals("unexpected number of queries sent", 0, 
                     tcm.getNumNewConnectionQueries());

    }

    /**
     * Test to make sure that the method to forward query requests
     * from new-style (high out-degree) hosts to others is working
     * correctly when the request is on its last hop
     */
    public void testForwardQueryToNewUltrapeersOnLastHop() throws Exception {
        // reset the connection manager using all new connections
        TestConnectionManager tcm = new TestConnectionManager(NUM_CONNECTIONS);
        PrivilegedAccessor.setValue(RouterService.class, "manager", tcm);
        PrivilegedAccessor.setValue(ROUTER, "_manager", tcm);
        Class[] params = new Class[]{QueryRequest.class, ReplyHandler.class};
		Method m = 
            PrivilegedAccessor.getMethod(ROUTER, 
                                         "forwardQueryToUltrapeers",
                                         params);        

        QueryRequest qr = QueryRequest.createQuery("test", (byte)1);      
        ReplyHandler rh = NewConnection.createConnection(10);
        m.invoke(ROUTER, new Object[] {qr, rh});

        // the query should not have been sent along any of the connections,
        // as none of them should have Ultrapeer routing tables.
        assertEquals("unexpected number of queries sent", 0, tcm.getNumUltrapeerQueries());
        assertEquals("unexpected number of queries sent", 0, 
                     tcm.getNumOldConnectionQueries());
        assertEquals("unexpected number of queries sent", 0, 
                     tcm.getNumNewConnectionQueries());

    }

    /**
     * Test to make sure that the method to forward query requests
     * from new-style (high out-degree) hosts to others is working
     * correctly when all of the connections are old and the query
     * is on the last hop.
     */
    public void testForwardQueryToOldUltrapeersOnLastHop() throws Exception {
        // reset the connection manager using all new connections
        TestConnectionManager tcm = new TestConnectionManager(0);
        PrivilegedAccessor.setValue(RouterService.class, "manager", tcm);
        PrivilegedAccessor.setValue(ROUTER, "_manager", tcm);
        Class[] params = new Class[]{QueryRequest.class, ReplyHandler.class};
		Method m = 
            PrivilegedAccessor.getMethod(ROUTER, 
                                         "forwardQueryToUltrapeers",
                                         params);        

        QueryRequest qr = QueryRequest.createQuery("test", (byte)1);      
        ReplyHandler rh = NewConnection.createConnection(10);
        m.invoke(ROUTER, new Object[] {qr, rh});

        // the query should not have been sent along any of the connections,
        // as none of them should have Ultrapeer routing tables.
        assertEquals("unexpected number of queries sent", NUM_CONNECTIONS, 
                     tcm.getNumUltrapeerQueries());
        assertEquals("unexpected number of queries sent", NUM_CONNECTIONS, 
                     tcm.getNumOldConnectionQueries());
        assertEquals("unexpected number of queries sent", 0, 
                     tcm.getNumNewConnectionQueries());
    }

    /**
     * Tests the method for originating queries from leaves.
     */
    public void testOriginateLeafQuery() throws Exception {
        Class[] params = new Class[]{QueryRequest.class};
		Method m = 
            PrivilegedAccessor.getMethod(ROUTER, 
                                         "originateLeafQuery",
                                         params);        

        // make sure that queries from leaves are simply sent to
        // the first three hosts
        TestConnectionManager tcm = new TestConnectionManager(2, false);
        PrivilegedAccessor.setValue(RouterService.class, "manager", tcm);
        PrivilegedAccessor.setValue(ROUTER, "_manager", tcm);
        QueryRequest qr = QueryRequest.createQuery("test");      

        
        m.invoke(ROUTER, new Object[] {qr});
        assertEquals("unexpected number of queries sent", 3, 
                     tcm.getNumUltrapeerQueries());
        assertEquals("unexpected number of queries sent", 1, 
                    tcm.getNumOldConnectionQueries());
        assertEquals("unexpected number of queries sent", 2, 
                     tcm.getNumNewConnectionQueries());        

    }
    
    public void testUDPPingReplies() throws Exception {
        ConnectionManager cm = new ConnectionManager();
        cm.initialize();
        PrivilegedAccessor.setValue(RouterService.class, "manager", cm);
        
        StubRouter stub = new StubRouter();
        // send a PR that doesn't have SCP in it.
        PingRequest pr = new PingRequest((byte)1);
        assertFalse(pr.supportsCachedPongs());
        assertFalse(pr.requestsIP());
        
        InetSocketAddress addr = new InetSocketAddress(InetAddress.getLocalHost(), 1);
        
        stub.respondToUDPPingRequest(pr, addr, null);
        assertEquals(1, stub.sentPongs.size());
        PingReply reply = (PingReply)stub.sentPongs.get(0);
        stub.sentPongs.clear();
        assertEquals(0, reply.getPackedIPPorts().size());
        assertNull(reply.getMyInetAddress());
        assertEquals(0, reply.getMyPort());
        
        // send a PR that does have SCP in it.
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(false);
        UltrapeerSettings.DISABLE_ULTRAPEER_MODE.setValue(true);
        assertFalse(RouterService.isSupernode());
        Collection hosts = RouterService.getPreferencedHosts(false, null,10);
        assertEquals(hosts.toString(), 0, hosts.size());
        pr = PingRequest.createUDPPing();
        assertTrue(pr.supportsCachedPongs());
        assertEquals(0x0, pr.getSupportsCachedPongData()[0] & 0x1);
        assertTrue(pr.requestsIP());
        stub.respondToUDPPingRequest(pr, addr, null);
        assertEquals(1, stub.sentPongs.size());
        reply = (PingReply)stub.sentPongs.get(0);
        stub.sentPongs.clear();
        assertEquals(InetAddress.getLocalHost(), reply.getMyInetAddress());
        assertEquals(1, reply.getMyPort());
        assertEquals(0, reply.getPackedIPPorts().size());
        
        // add some hosts with free leaf slots (just 3), make sure we get'm back.
        addFreeLeafSlotHosts(3);
        hosts = RouterService.getPreferencedHosts(false, null,10);
        assertEquals(hosts.toString(), 3, hosts.size());
        pr = PingRequest.createUDPPing();
        assertTrue(pr.supportsCachedPongs());
        assertEquals(0x0, pr.getSupportsCachedPongData()[0] & 0x1);
        stub.respondToUDPPingRequest(pr, addr, null);
        assertEquals(1, stub.sentPongs.size());
        reply = (PingReply)stub.sentPongs.get(0);
        stub.sentPongs.clear();
        assertEquals(3, reply.getPackedIPPorts().size());
        
        // add 20 more free leaf slots, make sure we only get as many we request.
        int requested = 10;
        addFreeLeafSlotHosts(20);
        hosts = RouterService.getPreferencedHosts(false, null,requested);
        assertEquals(hosts.toString(), requested, hosts.size());
        pr = PingRequest.createUDPPing();
        assertTrue(pr.supportsCachedPongs());
        assertEquals(0x0, pr.getSupportsCachedPongData()[0] & 0x1);
        stub.respondToUDPPingRequest(pr, addr, null);
        assertEquals(1, stub.sentPongs.size());
        reply = (PingReply)stub.sentPongs.get(0);
        stub.sentPongs.clear();
        assertEquals(requested, reply.getPackedIPPorts().size());
        
        clearFreeLeafSlotHosts();
        addFreeLeafSlotHosts(2); // odd number, to make sure it isn't impacting other tests.
        
        // now test if we're an ultrapeer.
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);
        UltrapeerSettings.DISABLE_ULTRAPEER_MODE.setValue(false);
        UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(true);
        UltrapeerSettings.FORCE_ULTRAPEER_MODE.setValue(true);
        assertTrue(RouterService.isSupernode());
        addFreeUltrapeerSlotHosts(4);
        hosts = RouterService.getPreferencedHosts(true, null,10);
        assertEquals(hosts.toString(), 4, hosts.size());
        pr = PingRequest.createUDPPing();
        assertTrue(pr.supportsCachedPongs());
        assertEquals(0x1, pr.getSupportsCachedPongData()[0] & 0x1);
        stub.respondToUDPPingRequest(pr, addr, null);
        assertEquals(1, stub.sentPongs.size());
        reply = (PingReply)stub.sentPongs.get(0);
        stub.sentPongs.clear();
        assertEquals(4, reply.getPackedIPPorts().size());
        
        // and add a lot again, make sure we only get as many we reqeust.
        addFreeUltrapeerSlotHosts(20);
        requested = 15;
        int original = ConnectionSettings.NUM_RETURN_PONGS.getValue();
        ConnectionSettings.NUM_RETURN_PONGS.setValue(requested);
        hosts = RouterService.getPreferencedHosts(true, null,requested);
        assertEquals(hosts.toString(), requested, hosts.size());
        pr = PingRequest.createUDPPing();
        assertTrue(pr.supportsCachedPongs());
        assertEquals(0x1, pr.getSupportsCachedPongData()[0] & 0x1);
        stub.respondToUDPPingRequest(pr, addr, null);
        assertEquals(1, stub.sentPongs.size());
        reply = (PingReply)stub.sentPongs.get(0);
        stub.sentPongs.clear();
        assertEquals(requested, reply.getPackedIPPorts().size());
        ConnectionSettings.NUM_RETURN_PONGS.setValue(original);
        
        // Now try again, without an SCP request, and make sure we got none.
        pr = new PingRequest((byte)1);
        assertFalse(pr.supportsCachedPongs());
        assertFalse(pr.requestsIP());
        stub.respondToUDPPingRequest(pr, addr, null);
        assertEquals(1, stub.sentPongs.size());
        reply = (PingReply)stub.sentPongs.get(0);
        stub.sentPongs.clear();
        assertEquals(0, reply.getPackedIPPorts().size());
        assertNull(reply.getMyInetAddress());
        assertEquals(0, reply.getMyPort());        
    }
    
    public void testHeadPingForwarding() throws Exception {
    	HeadListener pingee = new HeadListener();
    	
    	GUID clientGUID = new GUID(GUID.makeGuid());
    	
    	//make sure our routing table contains an entry for the pingee
    	RouteTable pushRt = (RouteTable) PrivilegedAccessor.getValue(ROUTER,"_pushRouteTable");
    	
    	pushRt.routeReply(clientGUID.bytes(),pingee);
    	
    	// try a HeadPing 
    	URN urn = FileDescStub.DEFAULT_SHA1;
    	HeadPing ping = new HeadPing(new GUID(GUID.makeGuid()),urn, clientGUID, 0xFF);
    	
    	ROUTER.handleUDPMessage(ping, new InetSocketAddress(InetAddress.getLocalHost(), 10));
    	
    	// the pingee should have received it
    	assertNotNull(pingee._lastSent);
    	assertEquals(pingee._lastSent.getGUID(),ping.getGUID());
    	assertTrue(pingee._lastSent instanceof HeadPing);
    	HeadPing ping2 = (HeadPing) pingee._lastSent;
    	
    	// we should have an entry in the routing table
    	RouteTable headRt =(RouteTable) PrivilegedAccessor.getValue(ROUTER,"_headPongRouteTable");
    	
    	ReplyHandler r = headRt.getReplyHandler(ping.getGUID());
    	assertEquals(InetAddress.getLocalHost(),InetAddress.getByName(r.getAddress()));
    	assertEquals(10,r.getPort());
    }
    
    public void testHeadPongForwarding() throws Exception {
    	HeadListener pinger = new HeadListener();
    	
    	//make sure our routing table contains an entry for the pinger
    	RouteTable headRt = (RouteTable) PrivilegedAccessor.getValue(ROUTER,"_headPongRouteTable");
    	
    	
    	//try a headpong
    	URN urn = FileDescStub.DEFAULT_SHA1;
    	HeadPing ping = new HeadPing(new GUID(GUID.makeGuid()),urn, 0xFF);
    	headRt.routeReply(ping.getGUID(),pinger);
    	HeadPong pong = new HeadPong(ping);
    	
    	ROUTER.handleMessage(pong, new ManagedConnectionStub());
    	
    	//the pinger should have gotten the identical object
    	assertNotNull(pinger._lastSent);
    	assertTrue(pinger._lastSent == pong);
    	
    	//the entry in the routing table should be gone
    	ReplyHandler r = headRt.getReplyHandler(ping.getGUID());
    	assertNull(r);
    }
    
    private void addFreeLeafSlotHosts(int num) throws Exception {
        HostCatcher hc = RouterService.getHostCatcher();
        Set set = (Set)PrivilegedAccessor.getValue(hc, "FREE_LEAF_SLOTS_SET");
        for(int i = 0; i < num; i++)
            set.add(new Endpoint("1.2.3." + i, i+1));
    }
    
    private void addFreeUltrapeerSlotHosts(int num) throws Exception {
        HostCatcher hc = RouterService.getHostCatcher();
        Set set = (Set)PrivilegedAccessor.getValue(hc, "FREE_ULTRAPEER_SLOTS_SET");
        for(int i = 0; i < num; i++)
            set.add(new Endpoint("1.2.3." + i, i+1));
    }
    
    private void clearFreeLeafSlotHosts() throws Exception {
        HostCatcher hc = RouterService.getHostCatcher();
        Set set = (Set)PrivilegedAccessor.getValue(hc, "FREE_ULTRAPEER_SLOTS_SET");
        set.clear();
    }        
        
    
    /**
     * Test file manager that returns specialized keywords for QRP testing.
     */
    private static final class TestFileManager extends MetaFileManager {
        
        private final List KEYWORDS = 
            Arrays.asList(MY_KEYWORDS);

        TestFileManager() {}

        public List getKeyWords() {
            return KEYWORDS;
        }

        //added due to changes in FileManager
        protected void buildQRT() {
            super.buildQRT();
            Iterator iter = getKeyWords().iterator();
            while(iter.hasNext()) {
                _queryRouteTable.add((String)iter.next());
            }
        }
    }
    
    /**
     * Stub MessageRouter that catches 'sendPingReply' pings.
     */
    private static final class StubRouter extends StandardMessageRouter {
        private List sentPongs = new LinkedList();
        
        // upp access.
        public void respondToUDPPingRequest(PingRequest req, 
                                            InetSocketAddress addr,
                                            ReplyHandler handler) {
            super.respondToUDPPingRequest(req, addr, handler);
        }
        
        protected void sendPingReply(PingReply pong, ReplyHandler handler) {
            sentPongs.add(pong);
        }
    }
    
    private static class HeadListener extends ReplyHandlerStub {
    	Message _lastSent;
    	public void reply(Message m) {
    		_lastSent = m;
    	}
    }
                                            
}
