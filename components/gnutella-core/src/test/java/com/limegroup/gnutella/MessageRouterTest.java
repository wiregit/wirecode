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

    /**
     * Constant for the number of Ultrapeer connections that the 
     * test connection manager should maintain.
     */
    private static final int NUM_CONNECTIONS = 20;

    /**
     * Constant for the number of leaf connections that the test 
     * connection manager should maintain.
     */
    private static final int NUM_LEAF_CONNECTIONS = 30;

    /**
     * Constant array for the keywords that I should have (this node).
     */
    private static final String[] MY_KEYWORDS = {
        "me", 
    };

    /**
     * Constant array for the keywords for Ultrapeers to use.
     */
    private static final String[] ULTRAPEER_KEYWORDS = {
        "qwe", "wer", "ert", "rty", "tyu", 
        "yui", "uio", "iop", "opa ", "pas", 
        "asd", "sdf", "dfg", "fgh", "ghj", 
        "hjk", "jkl", "klz", "lzx", "zxc", 
        "xcv", "cvb", "vbn", "bnm", "qwer",
        "wert", "erty", "rtyu", "tyui", "yuio",        
    };

    /**
     * Constant array for the keywords for leaves to use.
     */
    private static final String[] LEAF_KEYWORDS = {
        "this", "is", "a", "test", "for", 
        "query", "routing", "in", "all", "its", 
        "forms", "including", "both", "leaves", "and", 
        "Ultrapeers", "which", "should", "both", "work", 
        "like", "we", "expect", "them", "to", 
        "at", "least", "in", "theory", "and", 
        "hopefully", "in", "fact", "as", "well", 
        "but", "it's", "hard", "to", "know", 
    };

    /**
     * Array of keywords that should not match anything in the routing
     * tables.
     */
    private static final String[] UNMATCHING_KEYWORDS = {
        "miss", "NCWEPHCE", "IEYWHFDSNC", "UIYRIEH", "dfjaivuih",
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

    public void tearDown() {
        LeafConnection.resetCounter();
        UltrapeerConnection.resetCounter();
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
            runQRPMatch(qrt);
        }
    }

    /**
     * Test to make sure that the given <tt>QueryRouteTable</tt> has matches
     * for all of the expected keywords and that it doesn't have matches
     * for any of the unexpected keywords.
     *
     * @param qrt the <tt>QueryRouteTable</tt> instance to test
     */
    private static void runQRPMatch(QueryRouteTable qrt) {
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

        for(int i=0; i<UNMATCHING_KEYWORDS.length; i++) {
            QueryRequest qr = 
                QueryRequest.createQuery(UNMATCHING_KEYWORDS[i]);
            assertTrue("should not contain the given keyword: "+qr, 
                       !qrt.contains(qr));
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
        runQRPMatch(qrt);

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
        assertEquals("unexpected number of queries sent", 5, tcm.getNumQueries());
        assertEquals("unexpected number of queries sent", 5, 
                     tcm.getNumOldConnectionQueries());
        assertEquals("unexpected number of queries sent", 0, 
                     tcm.getNumNewConnectionQueries());
    }

    /**
     * Test to make sure that queries from old connections are forwarded
     * to new connections if only new connections are available.
     */
    public void testForwardOldQueriesToNewConnections() throws Exception {
        // reset the connection manager
        TestConnectionManager tcm = new TestConnectionManager(NUM_CONNECTIONS);
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
     * Helper class that supplies the list of connections for searching.
     */
    private static final class TestConnectionManager extends ConnectionManager {

        /**
         * The list of ultrapeer <tt>Connection</tt> instances
         */
        private final List CONNECTIONS = new LinkedList();

        /**
         * The list of leaf <tt>Connection</tt> instances
         */
        private final List LEAF_CONNECTIONS = new LinkedList();

        /**
         * Constant for whether or not this should be considered an
         * Ultrapeer.
         */
        private final boolean ULTRAPEER;
       
        /**
         * Creates a new <tt>ConnectionManager</tt> with a list of 
         * <tt>TestConnection</tt>s for testing.
         *
         * @param numNewConnections the number of new connections to 
         *  include in the set of connections
         */
        TestConnectionManager(int numNewConnections) {
            this(numNewConnections, true);
        }

        /**
         * Creates a new <tt>ConnectionManager</tt> with a list of 
         * <tt>TestConnection</tt>s for testing.
         *
         * @param numNewConnections the number of new connections to 
         *  include in the set of connections
         * @param ultrapeer whether or not this should be considered
         *  an ultrapeer
         */
        TestConnectionManager(int numNewConnections, boolean ultrapeer) {
            super(new DummyAuthenticator());
            for(int i=0; i<NUM_CONNECTIONS; i++) {
                if(i < numNewConnections) {
                    CONNECTIONS.add(new UltrapeerConnection());
                } else {
                    CONNECTIONS.add(new OldConnection(15));                    
                }
            }

            // now, give ourselves 30 leaves
            for(int i=0; i<NUM_LEAF_CONNECTIONS; i++) {
                LEAF_CONNECTIONS.add(new LeafConnection());
            }
            ULTRAPEER = ultrapeer;
        }

        /**
         * Accessor for the custom list of connections.
         */
        public List getInitializedConnections2() {
            return CONNECTIONS;
        }

        public List getInitializedClientConnections2() {
            return LEAF_CONNECTIONS;
        }

        public boolean isSupernode() {
            return ULTRAPEER;
        }

        /**
         * Returns the total number of queries received over all connections.
         */
        public int getNumQueries() {
            int numQueries = 0;
            Iterator iter = CONNECTIONS.iterator();
            while(iter.hasNext()) {
                TestConnection tc = (TestConnection)iter.next();
                numQueries += tc.getNumQueries();
            }
            return numQueries;
        }


        /**
         * Returns the total number of queries received over all old connections.
         */
        public int getNumOldConnectionQueries() {
            int numQueries = 0;
            Iterator iter = CONNECTIONS.iterator();
            while(iter.hasNext()) {
                TestConnection tc = (TestConnection)iter.next();
                if(tc instanceof OldConnection) {
                    numQueries += tc.getNumQueries();
                }
            }
            return numQueries;
        }

        /**
         * Returns the total number of queries received over all old connections.
         */
        public int getNumNewConnectionQueries() {
            int numQueries = 0;
            Iterator iter = CONNECTIONS.iterator();
            while(iter.hasNext()) {
                TestConnection tc = (TestConnection)iter.next();
                if(tc instanceof NewConnection) {
                    numQueries += tc.getNumQueries();
                }
            }
            return numQueries;
        }    

        public boolean isClientSupernodeConnection() {
            return false;
        }
    }

    /**
     * Helper class that overrides getNumIntraUltrapeerConnections for
     * testing the horizon calculation and testing the new search
     * architecture.
     */
    private static abstract class TestConnection extends ManagedConnection {
        
        private final int CONNECTIONS;

        private int _numQueries = 0;

        private int _totalTTL = 0;

        private boolean _receivedQuery;

        /**
         * Constant for the query route table for this connection.
         */
        private final QueryRouteTable QRT = new QueryRouteTable();

        TestConnection(int connections) {
            super("60.76.5.3", 4444);
            CONNECTIONS = connections;
        }

        public int getNumIntraUltrapeerConnections() {
            return CONNECTIONS;
        }

        public boolean isUltrapeerQueryRoutingConnection() {
            return false;
        }

        /**
         * Override the stability check method -- assume we're always stable.
         */
        public boolean isStable(long time) {
            return true;
        }

        /**
         * Accessor for the <tt>QueryRouteTable</tt> instance.
         */
        public QueryRouteTable getQueryRouteTable() {
            return QRT;
        }

        public void send(Message msg) {            
            if(msg instanceof RouteTableMessage) {
                try {
                    QRT.update((RouteTableMessage)msg);
                } catch (BadPacketException e) {
                    fail("should not have received a bad packet: "+msg);
                }
            }
            if(!(msg instanceof QueryRequest)) return;

            _receivedQuery = true;
            _numQueries++;
            QueryRequest qr = (QueryRequest)msg;
            int ttl = qr.getTTL();
            _totalTTL += ttl;
        }

        int getNumQueries() {
            return _numQueries;
        }

        int getTotalTTL() {
            return _totalTTL;
        }

        boolean receivedQuery() {
            return _receivedQuery;
        }
    }

    /**
     * Helper class that overrides getNumIntraUltrapeerConnections for
     * testing the horizon calculation and testing the new search
     * architecture.
     */
    private static class NewConnection extends TestConnection {    
       
        private static int counter = 0;

        private final ManagedConnectionQueryInfo QUERY_INFO =
            new ManagedConnectionQueryInfo();
        
        protected final QueryRouteTable QRT = new QueryRouteTable();

        NewConnection(int connections) {
            super(connections);
            QUERY_INFO.lastReceived = QRT;
            counter++;
        }

        public boolean isGoodConnection() {
            return true;
        }

        public boolean isUltrapeerQueryRoutingConnection() {
            return true;
        }

        public ManagedConnectionQueryInfo getQueryRouteState() {
            return QUERY_INFO;
        }
    }

    /**
     * Specialized class that uses special keywords for Ultrapeer routing
     * tables.
     */
    private static final class UltrapeerConnection extends NewConnection {

        private static int counter = 0;

        UltrapeerConnection() {
            super(15);
            QRT.add(ULTRAPEER_KEYWORDS[counter]);
            counter++;
        }        

        private static void resetCounter() {
            counter = 0;
        }
    }

    /**
     * Specialized class that uses special keywords for leaf routing
     * tables.
     */
    private static final class LeafConnection extends NewConnection {

        private static int counter = 0;

        LeafConnection() {
            super(15);
            QRT.add(LEAF_KEYWORDS[counter]);
            counter++;
        }        

        private static void resetCounter() {
            counter = 0;
        }
    }

    /**
     * Helper class that overrides getNumIntraUltrapeerConnections for
     * testing the horizon calculation and testing the new search
     * architecture.
     */
    private static final class OldConnection extends TestConnection {

        OldConnection(int connections) {
            super(connections);
        }

        public boolean isGoodConnection() {
            return false;
        }
    }

    /**
     * Test file manager that returns specialized keywords for QRP testing.
     */
    private static final class TestFileManager extends MetaFileManager {
        
        private final List KEYWORDS = Arrays.asList(MY_KEYWORDS);

        TestFileManager() {}

        public List getKeyWords() {
            return KEYWORDS;
        }
    }
}
