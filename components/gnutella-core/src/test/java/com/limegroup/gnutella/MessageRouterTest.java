package com.limegroup.gnutella;

import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.xml.*;
import com.limegroup.gnutella.stubs.*;
import com.limegroup.gnutella.security.*;
import com.limegroup.gnutella.messages.*;
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
     * Constant for the number of connections that the test 
     * connection manager should maintain.
     */
    private static final int NUM_CONNECTIONS = 20;

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

        // make sure we send a query from an old connection to new 
        // connections if new connections are available.

        // reset the connection manager
        tcm = new TestConnectionManager(NUM_CONNECTIONS);
        PrivilegedAccessor.setValue(RouterService.class, "manager", tcm);
        PrivilegedAccessor.setValue(ROUTER, "_manager", tcm);
        qr = QueryRequest.createQuery("test");      
        rh = new OldConnection(10);
        m.invoke(ROUTER, new Object[] {qr, rh});
        assertEquals("unexpected number of queries sent", 5, tcm.getNumQueries());
        assertEquals("unexpected number of queries sent", 0, 
                     tcm.getNumOldConnectionQueries());
        assertEquals("unexpected number of queries sent", 5, 
                     tcm.getNumNewConnectionQueries());


        // make sure we send a query from an old connection to new 
        // connections if new connections are available, but that we
        // still check the routing tables when the connections are new
        // and the query is on the last hop.

        // reset the connection manager
        tcm = new TestConnectionManager(NUM_CONNECTIONS);
        PrivilegedAccessor.setValue(RouterService.class, "manager", tcm);
        PrivilegedAccessor.setValue(ROUTER, "_manager", tcm);
        qr = QueryRequest.createQuery("test", (byte)1);      
        rh = new OldConnection(10);
        m.invoke(ROUTER, new Object[] {qr, rh});
        assertEquals("unexpected number of queries sent", 0, tcm.getNumQueries());
        assertEquals("unexpected number of queries sent", 0, 
                     tcm.getNumOldConnectionQueries());
        assertEquals("unexpected number of queries sent", 0, 
                     tcm.getNumNewConnectionQueries());

        // make sure we send a query from an old connection to old 
        // connections even when it's the last hop

        // reset the connection manager to use all old connections
        tcm = new TestConnectionManager(0);
        PrivilegedAccessor.setValue(RouterService.class, "manager", tcm);
        PrivilegedAccessor.setValue(ROUTER, "_manager", tcm);
        qr = QueryRequest.createQuery("test", (byte)1);      
        rh = new OldConnection(10);
        m.invoke(ROUTER, new Object[] {qr, rh});
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
     * correctly
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

        // test that we send the query to everyone when all of the 
        // connections are new

        // reset the connection manager using all new connections
        tcm = new TestConnectionManager(NUM_CONNECTIONS);
        PrivilegedAccessor.setValue(RouterService.class, "manager", tcm);
        PrivilegedAccessor.setValue(ROUTER, "_manager", tcm);
        qr = QueryRequest.createQuery("test");      
        rh = new NewConnection(10);
        m.invoke(ROUTER, new Object[] {qr, rh});
        assertEquals("unexpected number of queries sent", NUM_CONNECTIONS, 
                     tcm.getNumQueries());
        assertEquals("unexpected number of queries sent", 0, 
                     tcm.getNumOldConnectionQueries());
        assertEquals("unexpected number of queries sent", NUM_CONNECTIONS, 
                     tcm.getNumNewConnectionQueries());

        // test that we send the query to everyone when all of the 
        // connections are old

        // reset the connection manager using all old connections
        tcm = new TestConnectionManager(0);
        PrivilegedAccessor.setValue(RouterService.class, "manager", tcm);
        PrivilegedAccessor.setValue(ROUTER, "_manager", tcm);
        qr = QueryRequest.createQuery("test");      
        rh = new NewConnection(10);
        m.invoke(ROUTER, new Object[] {qr, rh});
        assertEquals("unexpected number of queries sent", NUM_CONNECTIONS, 
                     tcm.getNumQueries());
        assertEquals("unexpected number of queries sent", NUM_CONNECTIONS, 
                     tcm.getNumOldConnectionQueries());
        assertEquals("unexpected number of queries sent", 0, 
                     tcm.getNumNewConnectionQueries());

        // test that queries are correctly processed when they're the 
        // last hop, and they're being sent to new nodes that use
        // ultrapeer query routing

        // reset the connection manager using all new connections
        tcm = new TestConnectionManager(NUM_CONNECTIONS);
        PrivilegedAccessor.setValue(RouterService.class, "manager", tcm);
        PrivilegedAccessor.setValue(ROUTER, "_manager", tcm);
        qr = QueryRequest.createQuery("test", (byte)1);      
        rh = new NewConnection(10);
        m.invoke(ROUTER, new Object[] {qr, rh});

        // the query should not have been sent along any of the connections,
        // as none of them should have Ultrapeer routing tables.
        assertEquals("unexpected number of queries sent", 0, tcm.getNumQueries());
        assertEquals("unexpected number of queries sent", 0, 
                     tcm.getNumOldConnectionQueries());
        assertEquals("unexpected number of queries sent", 0, 
                     tcm.getNumNewConnectionQueries());

        // test that queries are correctly processed when they're the 
        // last hop, and they're being sent to old nodes that don't use
        // ultrapeer query routing

        // reset the connection manager using all new connections
        tcm = new TestConnectionManager(0);
        PrivilegedAccessor.setValue(RouterService.class, "manager", tcm);
        PrivilegedAccessor.setValue(ROUTER, "_manager", tcm);
        qr = QueryRequest.createQuery("test", (byte)1);      
        rh = new NewConnection(10);
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


        // make sure that queries from leaves are simply sent to
        // only three hosts even when we have more connections hosts
        tcm = new TestConnectionManager(5, false);
        PrivilegedAccessor.setValue(RouterService.class, "manager", tcm);
        PrivilegedAccessor.setValue(ROUTER, "_manager", tcm);
        qr = QueryRequest.createQuery("test");      

        
        rh = new OldConnection(10);
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
         * The list of <tt>Connection</tt> instances
         */
        private final List CONNECTIONS;

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
            CONNECTIONS = new LinkedList();
            for(int i=0; i<NUM_CONNECTIONS; i++) {
                if(i < numNewConnections) {
                    CONNECTIONS.add(new NewConnection(15));
                } else {
                    CONNECTIONS.add(new OldConnection(15));                    
                }
            }
            ULTRAPEER = ultrapeer;
        }

        /**
         * Accessor for the custom list of connections.
         */
        public List getInitializedConnections2() {
            return CONNECTIONS;
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

        public void send(Message msg) {
            if(!(msg instanceof QueryRequest)) return;

            _receivedQuery = true;
            _numQueries++;
            QueryRequest qr = (QueryRequest)msg;
            int ttl = qr.getTTL();

            //if(ttl > headers().getMaxTTL()) {
                // the TTL is higher than we specified
            //  throw new IllegalArgumentException("ttl too high: "+ttl);
            //}

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
    private static final class NewConnection extends TestConnection {        

        NewConnection(int connections) {
            super(connections);
        }

        public boolean isGoodConnection() {
            return true;
        }

        public boolean isUltrapeerQueryRoutingConnection() {
            return true;
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
}
