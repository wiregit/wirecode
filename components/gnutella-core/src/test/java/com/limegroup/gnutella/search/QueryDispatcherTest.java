package com.limegroup.gnutella.search;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.stubs.*;
import com.limegroup.gnutella.messages.*;
import com.sun.java.util.collections.*;
import java.lang.reflect.*;
import junit.framework.*;

/**
 * This class tests the dispatching of dynamic queries for ourselves 
 * (as Ultrapeer) and on behalf of our leaves.
 */
public class QueryDispatcherTest extends BaseTestCase {

	public QueryDispatcherTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(QueryDispatcherTest.class);
	}

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }    

    /**
     * Test to make sure that <tt>ReplyHandler</tt>s are removed correctly
     * from the query dispatcher.
     */
    public void testRemoveReplyHandler() throws Exception {
        QueryDispatcher qd = QueryDispatcher.instance();
        qd.start();
        
        List queries = (List)PrivilegedAccessor.getValue(qd, "QUERIES");

        assertEquals("should not be any queries", 0, queries.size());

        QueryRequest qr = QueryRequest.createQuery("test");
        ReplyHandler rh = new TestReplyHandler();
        QueryHandler handler = 
            QueryHandler.createHandlerForNewLeaf(qr, rh, 
                                                 new TestResultCounter());


        qd.addQuery(handler);
        qd.addQuery(handler);
        Thread.sleep(3000);
        
        assertEquals("unexpected queries size", 2, queries.size());
        qd.removeReplyHandler(rh);
        assertEquals("should not be any queries", 0, queries.size());
        
    }

    private static final class TestResultCounter implements ResultCounter {
        
        public int getNumResults() {
            return 0;
        }
    }

    private static final class TestReplyHandler extends ReplyHandlerStub {
        
        public boolean isSupernodeClientConnection() {
            return true;
        }
    }
}
