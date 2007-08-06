package com.limegroup.gnutella.search;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import junit.framework.Test;

import org.limewire.util.PrivilegedAccessor;

import com.limegroup.gnutella.Connection;
import com.limegroup.gnutella.ManagedConnection;
import com.limegroup.gnutella.ProviderHacks;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.LeafConnection;
import com.limegroup.gnutella.util.LimeTestCase;
import com.limegroup.gnutella.util.NewConnection;
import com.limegroup.gnutella.util.TestConnection;
import com.limegroup.gnutella.util.TestConnectionManager;
import com.limegroup.gnutella.util.TestResultCounter;
import com.limegroup.gnutella.util.UltrapeerConnection;


/**
 * Tests the functionality of the <tt>QueryHandlerTest</tt> class.
 */
@SuppressWarnings("unchecked")
public final class QueryHandlerTest extends LimeTestCase {


	public QueryHandlerTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(QueryHandlerTest.class);
	}

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public static void globalSetUp()  {
        new RouterService(new ActivityCallbackStub());
    }

    /**
     * Tests the method for sending an individual query to a 
     * single connections.
     */
    public void testSendQueryToHost() throws Exception {
        ReplyHandler rh = new UltrapeerConnection();        
        QueryRequest query = ProviderHacks.getQueryRequestFactory().createQuery("test", (byte)1);
        QueryHandler qh = 
            ProviderHacks.getQueryHandlerFactory().createHandler(query, rh, new TestResultCounter());
        Class[] paramTypes = new Class[] {
            QueryRequest.class, 
            ManagedConnection.class,
            QueryHandler.class,
        };

		Method m = 
            PrivilegedAccessor.getMethod(QueryHandler.class, 
                                         "sendQueryToHost",
                                         paramTypes);

        ManagedConnection mc = new UltrapeerConnection();
        Object[] params = new Object[] {
            query, mc, qh,
        };

        m.invoke(null, params);
        List hostsQueried = 
            (List)PrivilegedAccessor.getValue(qh, "QUERIED_CONNECTIONS");
        assertEquals("should not have added host", 0, hostsQueried.size());

        // make sure we add the connection to the queries handlers
        // if it doesn't support probe queries and the TTL is 1
        params[1] = LeafConnection.createLeafConnection(false);
        m.invoke(null, params);
        hostsQueried = 
            (List)PrivilegedAccessor.getValue(qh, "QUERIED_CONNECTIONS");
        assertEquals("should have added host", 1, hostsQueried.size());

        // make sure it adds the connection when the ttl is higher
        // than one and the connection supports probe queries
        params[0] = ProviderHacks.getQueryRequestFactory().createQuery("test");
        params[1] = new UltrapeerConnection();

        m.invoke(null, params);
        hostsQueried = 
            (List)PrivilegedAccessor.getValue(qh, "QUERIED_CONNECTIONS");
        assertEquals("should have added host", 2, hostsQueried.size());

        
        
    }    

    /**
     * Tests the method for calculating the next TTL.
     */
    public void testCalculateNewTTL() throws Exception {
        Class[] params = new Class[]{Integer.TYPE, Integer.TYPE, Byte.TYPE};
		Method m = 
            PrivilegedAccessor.getMethod(QueryHandler.class, 
                                         "calculateNewTTL",
                                         params);        
        byte ttl = getTTL(m, 2000, 15, (byte)5);
        assertEquals("unexpected ttl", 3, ttl);

        ttl = getTTL(m, 200, 15, (byte)5);
        assertEquals("unexpected ttl", 2, ttl);


        ttl = getTTL(m, 200000, 15, (byte)4);
        assertEquals("unexpected ttl", 4, ttl);

        ttl = getTTL(m, 20000, 5, (byte)2);
        assertEquals("unexpected ttl", 2, ttl);
    }

    /**
     * Convenience method for getting the TTL from QueryHandler.
     */
    private byte getTTL(Method m, int hosts, int degree, byte maxTTL) 
        throws Exception {
        byte ttl = 
            ((Byte)m.invoke(null, new Object[] {
                new Integer(hosts), new Integer(degree), 
                new Byte(maxTTL)})).byteValue();
        return ttl;
    }

    /**
     * Tests the public sendQuery method to make sure it's working as
     * expected.
     */
    public void testPublicSendQuery() throws Exception {
        assertNotNull("should have a message router", ProviderHacks.getMessageRouter());

        List connections = new LinkedList();
        for(int i=0; i<15; i++) {
            connections.add(NewConnection.createConnection());
        }   

        QueryHandler handler = 
            ProviderHacks.getQueryHandlerFactory().createHandler(ProviderHacks.getQueryRequestFactory().createQuery("test"),
                                       NewConnection.createConnection(),
                                       new TestResultCounter(0));

        TestConnectionManager tcm = new TestConnectionManager();

        assertTrue(tcm.getInitializedConnections().size() > 0);
        PrivilegedAccessor.setValue(QueryHandler.class, "_connectionManager", tcm);
                                           
        handler.sendQuery();
        
        int numQueries = tcm.getNumUltrapeerQueries();
        assertEquals("unexpected number of probe queries sent", 3, numQueries);

        // these calls should not go through, as it's too soon after the probe
        // was sent!
        handler.sendQuery();
        handler.sendQuery();
        handler.sendQuery();

        assertEquals("unexpected number of probe queries sent", 3, 
            tcm.getNumUltrapeerQueries());        
        
        Thread.sleep(8000);

        handler.sendQuery();

        Thread.sleep(1000);

        // after the sleep, it should go through!
        assertEquals("unexpected number of queries sent", 4, tcm.getNumUltrapeerQueries());

        // these calls should not go through, as it's too soon after the last query
        // was sent!
        handler.sendQuery();
        handler.sendQuery();
        handler.sendQuery();

        assertEquals("unexpected number of queries sent", 4, tcm.getNumUltrapeerQueries());        
        
        Thread.sleep(8000);
        handler.sendQuery();

        // after the sleep, it should go through!
        assertEquals("unexpected number of queries sent", 5, tcm.getNumUltrapeerQueries());

        // now, send out a bunch of queries to make sure that, eventually, 
        // new queries don't go out because we've reached too high a theoretical
        // horizon.  These queries aren't returning any results, so the TTLs
        // should be relatively high, causing us to hit the theoretical 
        // horizon limit!

        for(int i=0; i<tcm.getInitializedConnections().size()-2; i++) {
            Thread.sleep(2000);
            handler.sendQuery();
        }        

        assertTrue("too many hosts queried! -- theoretical horizon too high", 
                   (tcm.getNumUltrapeerQueries() <= 12));
    }


    /**
     * Test to make sure that the theoretical horizon we've reached
     * is being calculated correctly.
     */
    public void testCalculateNewHosts() throws Exception {
		Method m = 
            PrivilegedAccessor.getMethod(QueryHandler.class, 
                                         "calculateNewHosts",
                                         new Class[]{Connection.class, 
                                                     Byte.TYPE});
        
        // test for a degree 19, ttl 4 network
        ManagedConnection mc = NewConnection.createConnection(19);
        int horizon = 0;
        for(int i=0; i<19; i++) {
            horizon += 
                ((Integer)m.invoke(null, 
                                  new Object[]{mc, new Byte((byte)4)})).intValue();
        }                
        assertEquals("incorrect horizon", 117325, horizon);

        // test for a degree 30, ttl 3 network
        mc = NewConnection.createConnection(30);
        horizon = 0;
        for(int i=0; i<30; i++) {
            horizon += 
                ((Integer)m.invoke(null, 
                                   new Object[]{mc, new Byte((byte)3)})).intValue();
        }                
        assertEquals("incorrect horizon", 26130, horizon);
    }

    
    /**
     * Tests the private method for sending queries to make sure the
     * basics of query dispatching are working correctly.
     */
    public void testPrivateSendQuery() throws Exception {
        assertNotNull("should have a message router", ProviderHacks.getMessageRouter());
		Method m = 
            PrivilegedAccessor.getMethod(QueryHandler.class, 
                                         "sendQuery",
                                         new Class[]{List.class});

        int numConnections = 15;
        List connections = new ArrayList();
        for(int i=0; i<numConnections; i++) {
            connections.add(NewConnection.createConnection(10));
        }   

        QueryHandler handler = 
            ProviderHacks.getQueryHandlerFactory().createHandler(ProviderHacks.getQueryRequestFactory().createQuery("test"),
                                       NewConnection.createConnection(8),
                                       new TestResultCounter(0));
        
        
        Object[] params = new Object[] {connections};

        // just send queries to all connections
        for(int i=0; i<numConnections; i++) {
            m.invoke(handler, params);
        }


        // just make sure all of the connections received the query
        Iterator iter = connections.iterator();
        while(iter.hasNext()) {
            TestConnection tc = (TestConnection)iter.next();
            assertTrue("should have received the query", tc.receivedQuery());
        }
    }
}
