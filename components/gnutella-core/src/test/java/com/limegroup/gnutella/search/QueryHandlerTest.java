package com.limegroup.gnutella.search;

import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.stubs.*;
import com.limegroup.gnutella.xml.*;
import com.limegroup.gnutella.search.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.security.*;
import com.limegroup.gnutella.*;
import java.util.Properties;
import java.util.StringTokenizer;
import com.sun.java.util.collections.*;
import java.lang.reflect.*;
import junit.framework.*;


/**
 * Tests the functionality of the <tt>QueryHandlerTest</tt> class.
 */
public final class QueryHandlerTest extends BaseTestCase {

    private static Method CREATE_PROBE_LISTS;

	public QueryHandlerTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(QueryHandlerTest.class);
	}

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public static void globalSetUp() throws Exception {
        Class[] params = new Class[]{List.class, QueryRequest.class};
		CREATE_PROBE_LISTS = 
            PrivilegedAccessor.getMethod(QueryHandler.class, 
                                         "createProbeLists",
                                         params);        
    }

    public void testAddToList() throws Exception {
        Class[] paramTypes = 
            new Class[]{List.class, List.class, List.class, Integer.TYPE};
		Method m = 
            PrivilegedAccessor.getMethod(QueryHandler.class, 
                                         "addToList",
                                         paramTypes);                

        List listToAddTo = new LinkedList();
        List list1 = new LinkedList();
        List list2 = new LinkedList();
        Integer numElements = new Integer(3);

        Object[] params = 
            new Object[] {listToAddTo, list1, list2, numElements};
        
        Integer one   = new Integer(1);
        Integer two   = new Integer(2);
        Integer three = new Integer(3);
        Integer four  = new Integer(4);
        Integer five  = new Integer(5);
        Integer six   = new Integer(6);
        Integer seven = new Integer(7);

        List testList = new LinkedList();
        testList.add(one);
        testList.add(two);
        testList.add(three);        

        list1.add(one);
        list1.add(two);
        list1.add(three);

        m.invoke(null, params);
        assertEquals("lists should be equal", testList, listToAddTo);

        list1.clear();
        list2.clear();
        listToAddTo.clear();

        
        list2.add(one);
        list2.add(two);
        list2.add(three);

        m.invoke(null, params);
        assertEquals("lists should be equal", testList, listToAddTo);

        list1.clear();
        list2.clear();
        listToAddTo.clear();
        
        list1.add(one);
        list2.add(two);
        list2.add(three);

        m.invoke(null, params);
        assertEquals("lists should be equal", testList, listToAddTo);

        list1.clear();
        list2.clear();
        listToAddTo.clear();
        
        list1.add(one);
        list1.add(two);
        list2.add(three);
        list2.add(four);

        m.invoke(null, params);
        assertEquals("lists should be equal", testList, listToAddTo);
    }

    /**
     * Test to make sure that the utility method for creating
     * our probe query lists is working as we expect it to when
     * the content we're searching for is very popular.
     */
    public void testCreateProbeListsForPopularContent() throws Exception {
        List connections = new LinkedList();
        for(int i=0; i<15; i++) {
            connections.add(NewConnection.createHitConnection());
        }           
        
        for(int i=0; i<15; i++) {
            connections.add(new OldConnection(5));
        }                   

        QueryRequest query = QueryRequest.createQuery("test");
        List[] queryLists = 
            (List[])CREATE_PROBE_LISTS.invoke(null, 
                                              new Object[]{connections, query}); 

        assertTrue("should not be any ttl=2 queries", queryLists[1].isEmpty());
        assertEquals("should not be only 1 ttl=1 query", queryLists[0].size(), 1);
    }

    /**
     * Test to make sure that the utility method for creating
     * our probe query lists is working as we expect it to.
     */
    public void testCreateProbeListsForSomewhatPopular() throws Exception {
        List connections = new LinkedList();
        connections.clear();
        for(int i=0; i<20; i++) {
            connections.add(NewConnection.createHitConnection());
        }           

        for(int i=0; i<5; i++) {
            connections.add(NewConnection.createConnection());
        }           
        
        for(int i=0; i<5; i++) {
            connections.add(new OldConnection(5));
        }                   

        QueryRequest query = QueryRequest.createQuery("test");
        List[] queryLists = 
            (List[])CREATE_PROBE_LISTS.invoke(null, 
                                              new Object[]{connections, query}); 

        //assertTrue("should not be any ttl=2 queries", queryLists[1].isEmpty());
        //assertEquals("should not be only 1 ttl=1 query", queryLists[0].size(), 1);

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
        assertEquals("unexpected ttl", 4, ttl);

        ttl = getTTL(m, 200, 15, (byte)5);
        assertEquals("unexpected ttl", 3, ttl);


        ttl = getTTL(m, 200000, 15, (byte)4);
        assertEquals("unexpected ttl", 4, ttl);
    }

    /**
     * Convenience method for getting the TTL from QueryHandler/
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
        RouterService rs = new RouterService(new ActivityCallbackStub());
        assertNotNull("should have a message router", rs.getMessageRouter());
		Method m = 
            PrivilegedAccessor.getMethod(QueryHandler.class, 
                                         "sendQuery",
                                         new Class[]{});

        List connections = new LinkedList();
        for(int i=0; i<15; i++) {
            connections.add(NewConnection.createConnection());
        }   

        QueryHandler handler = 
            QueryHandler.createHandler(QueryRequest.createQuery("test"),
                                       NewConnection.createConnection(),
                                       new TestResultCounter(0));

        TestConnectionManager tcm = new TestConnectionManager();

        PrivilegedAccessor.setValue(QueryHandler.class, "_connectionManager", tcm);
                                           
        handler.sendQuery();
        
        int numQueries = tcm.getNumQueries();
        assertEquals("unexpected number of probe queries sent", 3, numQueries);

        // these calls should not go through, as it's too soon after the probe
        // was sent!
        handler.sendQuery();
        handler.sendQuery();
        handler.sendQuery();

        assertEquals("unexpected number of probe queries sent", 3, tcm.getNumQueries());        
        
        Thread.sleep(8000);

        handler.sendQuery();

        Thread.sleep(1000);

        // after the sleep, it should go through!
        assertEquals("unexpected number of queries sent", 4, tcm.getNumQueries());

        // these calls should not go through, as it's too soon after the last query
        // was sent!
        handler.sendQuery();
        handler.sendQuery();
        handler.sendQuery();

        assertEquals("unexpected number of queries sent", 4, tcm.getNumQueries());        
        
        Thread.sleep(2000);
        handler.sendQuery();

        // after the sleep, it should go through!
        assertEquals("unexpected number of queries sent", 5, tcm.getNumQueries());

        // now, send out a bunch of queries to make sure that, eventually, 
        // new queries don't go out because we've reached too high a theoretical
        // horizon.  These queries aren't returning any results, so the TTLs
        // should be relatively high, causing us to hit the theoretical 
        // horizon limit!

        for(int i=0; i<tcm.getInitializedConnections2().size()-2; i++) {
            Thread.sleep(2000);
            handler.sendQuery();
        }        

        int horizon = 
            ((Integer)PrivilegedAccessor.getValue(handler, 
                                                  "_theoreticalHostsQueried")).intValue();
        assertTrue("too many hosts queried! -- theoretical horizon too high", 
                   (tcm.getNumQueries() <= 12));
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
     * Tests the method for sending a probe query to make sure it is sent to
     * the number of nodes we think it's sent to and to make sure it's sent
     * with the proper TTL.
     */
    public void testProbeQuery() throws Exception {
        RouterService rs = new RouterService(new ActivityCallbackStub());
        assertNotNull("should have a message router", rs.getMessageRouter());
		Method m = 
            PrivilegedAccessor.getMethod(QueryHandler.class, 
                                         "sendProbeQuery",
                                         new Class[]{QueryHandler.class, 
                                                     List.class});
        List connections = new LinkedList();
        for(int i=0; i<15; i++) {
            connections.add(NewConnection.createConnection(10));
        }

        QueryHandler handler = 
            QueryHandler.createHandler(QueryRequest.createQuery("test"),
                                       NewConnection.createConnection(8),
                                       new TestResultCounter(0));
        
        m.invoke(null, new Object[]{handler, connections});

        int queriesSent = 0;
        int totalTTL = 0;
        Iterator iter = connections.iterator();
        while(iter.hasNext()) {
            TestConnection tc = (TestConnection)iter.next();
            queriesSent += tc.getNumQueries();
            totalTTL += tc.getTotalTTL();
        }

        assertEquals("should have only sent two queries", 3, queriesSent);
        assertEquals("should have sent 2 queries with TTL=2", 6, totalTTL);
    }

    
    /**
     * Tests the private method for sending queries to make sure the
     * basics of query dispatching are working correctly.
     */
    public void testPrivateSendQuery() throws Exception {
        RouterService rs = new RouterService(new ActivityCallbackStub());
        assertNotNull("should have a message router", rs.getMessageRouter());
		Method m = 
            PrivilegedAccessor.getMethod(QueryHandler.class, 
                                         "sendQuery",
                                         new Class[]{QueryHandler.class, 
                                                     List.class});

        List connections = new LinkedList();
        for(int i=0; i<15; i++) {
            connections.add(NewConnection.createConnection(10));
        }   

        QueryHandler handler = 
            QueryHandler.createHandler(QueryRequest.createQuery("test"),
                                       NewConnection.createConnection(8),
                                       new TestResultCounter(0));
        
        
        Iterator iter = connections.iterator();
        //int results = 0;
        while(iter.hasNext()) {
            TestConnection tc = (TestConnection)iter.next();
            m.invoke(null, new Object[]{handler, connections}); 
        }


        // just make sure all of the connections received the query
        iter = connections.iterator();
        while(iter.hasNext()) {
            TestConnection tc = (TestConnection)iter.next();
            assertTrue("should have received the query", tc.receivedQuery());
        }

        
        //m.invoke(null, new Object[]{handler, connections});        
    }


    /**
     * Helper result counter that we can use to artificially produce 
     * results.
     */
    private static final class TestResultCounter implements ResultCounter {
        private final int RESULTS;
        TestResultCounter(int results) {
            RESULTS = results;
        }
        public int getNumResults() {
            return RESULTS;
        }
    }
   
}
