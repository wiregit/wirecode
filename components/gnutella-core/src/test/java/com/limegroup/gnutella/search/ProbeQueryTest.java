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
public final class ProbeQueryTest extends BaseTestCase {

    /**
     * Cached method for creating the probe lists.
     */
    private static Method CREATE_PROBE_LISTS;

	public ProbeQueryTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(ProbeQueryTest.class);
	}

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public static void globalSetUp() throws Exception {
        Class[] params = new Class[]{List.class, QueryRequest.class};
		CREATE_PROBE_LISTS = 
            PrivilegedAccessor.getMethod(ProbeQuery.class, 
                                         "createProbeLists",
                                         params);        
    }


    /**
     * Tests the method for sending a probe query to make sure it is sent to
     * the number of nodes we think it's sent to and to make sure it's sent
     * with the proper TTL.
     */
    public void testSendProbe() throws Exception {
        RouterService rs = new RouterService(new ActivityCallbackStub());
        assertNotNull("should have a message router", rs.getMessageRouter());
		Method m = 
            PrivilegedAccessor.getMethod(ProbeQuery.class, 
                                         "sendProbe",
                                         new Class[]{});
        List connections = new LinkedList();
        for(int i=0; i<15; i++) {
            connections.add(NewConnection.createConnection(10));
        }

        QueryHandler handler = 
            QueryHandler.createHandler(QueryRequest.createQuery("test"),
                                       NewConnection.createConnection(8),
                                       new TestResultCounter(0));
        
        ProbeQuery probe = new ProbeQuery(connections, handler);
        m.invoke(probe, new Object[]{});

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

        System.out.println("TTL=1: "+queryLists[0].size()); 
        System.out.println("TTL=2: "+queryLists[1].size()); 
        assertTrue("should not be any ttl=2 queries", queryLists[1].isEmpty());
        assertTrue("should not be too many ttl=1", queryLists[0].size() < 15);
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
}
