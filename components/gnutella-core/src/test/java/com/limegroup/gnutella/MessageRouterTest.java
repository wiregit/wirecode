package com.limegroup.gnutella;

import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.xml.*;
import com.limegroup.gnutella.stubs.*;
import com.limegroup.gnutella.security.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.routing.*;
import junit.framework.*;
import com.sun.java.util.collections.*;
import java.lang.reflect.*;

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
        //TestConnectionManager tcm = new TestConnectionManager(4);
        //PrivilegedAccessor.setValue(RouterService.class, "manager", tcm);
        ROUTER = new MetaEnabledMessageRouter(new ActivityCallbackStub(), 
                                              new FileManagerStub());
        ROUTER.initialize();
    }

    //public void tearDown() {
    //  LeafConnection.resetCounter();
    //  UltrapeerConnection.resetCounter();
    //}

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
        assertEquals("unexpected number of queries sent", 5, 
                     tcm.getNumQueries());
        assertEquals("unexpected number of queries old sent", 5, 
                     tcm.getNumOldConnectionQueries());
        assertEquals("unexpected number of queries new sent", 0, 
                     tcm.getNumNewConnectionQueries());
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
        
        List connections = tcm.getInitializedConnections2();
        Iterator iter = connections.iterator();
        while(iter.hasNext()) {
            TestConnection tc = (TestConnection)iter.next();
            QueryRouteTable qrt = tc.getQueryRouteTable();
            assertTrue("route tables should match",tcm.runQRPMatch(qrt));
        }
    }



    /**
     * Tests the method for adding query routing entries to the
     * QRP table for this node, adding the leaves' QRP tables if
     * we're an Ultrapeer.
     */
    public void testAddQueryRoutingEntries() throws Exception {
        TestConnectionManager tcm = 
            new TestConnectionManager(NUM_CONNECTIONS);
        FileManager fm = new TestFileManager();
        PrivilegedAccessor.setValue(RouterService.class, "manager", tcm);
        PrivilegedAccessor.setValue(ROUTER, "_manager", tcm);        
        PrivilegedAccessor.setValue(MessageRouter.class, "_fileManager", fm);
        Class[] params = new Class[] {QueryRouteTable.class};
		Method m = 
            PrivilegedAccessor.getMethod(ROUTER, 
                                         "addQueryRoutingEntries",
                                         params);             
        QueryRouteTable qrt = new QueryRouteTable();
        m.invoke(ROUTER, new Object[] {qrt});
        tcm.runQRPMatch(qrt);

        /*
        for(int i=0; i<MY_KEYWORDS.length; i++) {
            QueryRequest qr = QueryRequest.createQuery(MY_KEYWORDS[i]);
            assertTrue("should contain the given keyword: "+qr, 
                       qrt.contains(qr));
        }

        for(int i=0; i<NUM_LEAF_CONNECTIONS; i++) {
            QueryRequest qr = QueryRequest.createQuery(LEAF_KEYWORDS[i]);
            assertTrue("should contain the given keyword: "+qr, 
                       qrt.contains(qr));
        }
        */
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
        assertEquals("unexpected number of queries sent", 5, tcm.getNumQueries());
        assertEquals("unexpected number of queries sent", 0, 
                     tcm.getNumOldConnectionQueries());
        assertEquals("unexpected number of queries sent", 5, 
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
        assertEquals("unexpected number of queries sent", 0, tcm.getNumQueries());
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
        assertEquals("unexpected number of queries sent", 5, 
                     tcm.getNumQueries());
        assertEquals("unexpected number of queries sent", 5, 
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
        ReplyHandler rh = new NewConnection(10);
        
        m.invoke(ROUTER, new Object[] {qr, rh});
        assertEquals("unexpected number of queries sent", NUM_CONNECTIONS, 
                     tcm.getNumQueries());
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
        ReplyHandler rh = new NewConnection(10);
        m.invoke(ROUTER, new Object[] {qr, rh});
        assertEquals("unexpected number of queries sent", NUM_CONNECTIONS, 
                     tcm.getNumQueries());
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
        ReplyHandler rh = new NewConnection(10);
        m.invoke(ROUTER, new Object[] {qr, rh});
        assertEquals("unexpected number of queries sent", NUM_CONNECTIONS, 
                     tcm.getNumQueries());
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
        ReplyHandler rh = new NewConnection(10);
        m.invoke(ROUTER, new Object[] {qr, rh});

        // the query should not have been sent along any of the connections,
        // as none of them should have Ultrapeer routing tables.
        assertEquals("unexpected number of queries sent", 0, tcm.getNumQueries());
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
        ReplyHandler rh = new NewConnection(10);
        m.invoke(ROUTER, new Object[] {qr, rh});

        // the query should not have been sent along any of the connections,
        // as none of them should have Ultrapeer routing tables.
        assertEquals("unexpected number of queries sent", NUM_CONNECTIONS, 
                     tcm.getNumQueries());
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
        TestConnectionManager tcm = new TestConnectionManager(3, false);
        PrivilegedAccessor.setValue(RouterService.class, "manager", tcm);
        PrivilegedAccessor.setValue(ROUTER, "_manager", tcm);
        QueryRequest qr = QueryRequest.createQuery("test");      

        
        ReplyHandler rh = new OldConnection(10);
        m.invoke(ROUTER, new Object[] {qr});
        assertEquals("unexpected number of queries sent", 3, tcm.getNumQueries());
        assertEquals("unexpected number of queries sent", 0, 
                     tcm.getNumOldConnectionQueries());
        assertEquals("unexpected number of queries sent", 3, 
                     tcm.getNumNewConnectionQueries());        

    }
    
    /**
     * Tests the method for originating queries from leaves to make sure
     * that we only send the query to 3 hosts even when we have more.
     */
    public void testOriginateLeafQueryLimit() throws Exception {  
        Class[] params = new Class[]{QueryRequest.class};
		Method m = 
            PrivilegedAccessor.getMethod(ROUTER, 
                                         "originateLeafQuery",
                                         params);          
        TestConnectionManager tcm = new TestConnectionManager(5, false);
        PrivilegedAccessor.setValue(RouterService.class, "manager", tcm);
        PrivilegedAccessor.setValue(ROUTER, "_manager", tcm);
        QueryRequest qr = QueryRequest.createQuery("test");      
        ReplyHandler rh = new OldConnection(10);
        m.invoke(ROUTER, new Object[] {qr});
        assertEquals("unexpected number of queries sent", 3, tcm.getNumQueries());
        assertEquals("unexpected number of queries sent", 0, 
                     tcm.getNumOldConnectionQueries());
        assertEquals("unexpected number of queries sent", 3, 
                     tcm.getNumNewConnectionQueries());        
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
    }
}
