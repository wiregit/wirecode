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
        RouterService rs = new RouterService(new ActivityCallbackStub());
        QueryDispatcher qd = QueryDispatcher.instance();
        qd.start();
        
        Map queries = (Map)PrivilegedAccessor.getValue(qd, "QUERIES");

        assertEquals("should not be any queries", 0, queries.size());

        QueryRequest qr = QueryRequest.createQuery("test");
        ReplyHandler rh = new TestReplyHandler();
        QueryHandler handler = 
            QueryHandler.createHandlerForNewLeaf(qr, rh, 
                                                 new TestResultCounter(0));


        qd.addQuery(handler);
        qd.addQuery(handler);
        Thread.sleep(3000);
        
        assertEquals("unexpected queries size", 1, queries.size());
        qd.removeReplyHandler(rh);
        assertEquals("should not be any queries", 0, queries.size());
        
        // now add two different queries and make sure everything is koser
        QueryRequest qrAlt = QueryRequest.createQuery("test 2");
        QueryHandler handlerAlt = 
        QueryHandler.createHandlerForNewLeaf(qrAlt, rh, 
                                             new TestResultCounter(0));
        
        qd.addQuery(handler);
        qd.addQuery(handlerAlt);
        Thread.sleep(3000);
        
        assertEquals("unexpected queries size", 2, queries.size());
        qd.removeReplyHandler(rh);
        assertEquals("should not be any queries", 0, queries.size());

        // one more test - make sure different RHs don't effect each other
        QueryRequest qrOther = QueryRequest.createQuery("test other");
        ReplyHandler rhOther = new TestReplyHandler();
        QueryHandler handlerOther = 
        QueryHandler.createHandlerForNewLeaf(qrOther, rhOther, 
                                             new TestResultCounter(0));
        
        qd.addQuery(handler);
        qd.addQuery(handlerAlt);
        qd.addQuery(handlerOther);
        Thread.sleep(3000);
        
        assertEquals("unexpected queries size", 3, queries.size());
        qd.removeReplyHandler(rh);
        assertEquals("should only be Other queries", 1, queries.size());
        qd.removeReplyHandler(rhOther);
        assertEquals("should not be any queries", 0, queries.size());
    }


    private static final class TestReplyHandler extends ReplyHandlerStub {
        
        public boolean isSupernodeClientConnection() {
            return true;
        }
    }
}
