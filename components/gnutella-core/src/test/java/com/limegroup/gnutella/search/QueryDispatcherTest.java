package com.limegroup.gnutella.search;

import junit.framework.Test;

import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.stubs.ReplyHandlerStub;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.util.PrivilegedAccessor;
import com.limegroup.gnutella.util.TestResultCounter;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

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
        new RouterService(new ActivityCallbackStub());
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

    /**
     * tests that user stopped queries via _toRemove set are stopped and 
     * removed correctly
     */
    public void testRemoveStoppedQuery() throws Exception {
        new RouterService(new ActivityCallbackStub());
        QueryDispatcher qd = QueryDispatcher.instance();
        qd.start();
        
        Set toR = (Set)PrivilegedAccessor.getValue(qd, "TO_REMOVE");
        Map queries = (Map)PrivilegedAccessor.getValue(qd, "QUERIES");
        
        assertEquals("TO_REMOVE should be empty", 0, toR.size());
        
        QueryRequest qr = QueryRequest.createQuery("test");
        ReplyHandler rh = new TestReplyHandler();
        QueryHandler qhand = 
            QueryHandler.createHandlerForNewLeaf(qr, rh,
                                                 new TestResultCounter(0));

        QueryRequest qr2 = QueryRequest.createQuery("test2");
        QueryHandler qhand2 = 
            QueryHandler.createHandlerForNewLeaf(qr2, rh,
                                                 new TestResultCounter(0));
        
        qd.addQuery(qhand);
        qd.addQuery(qhand2);
        Thread.sleep(3000);
        assertEquals("there should be 2 queries", 2, queries.size());
        
        qd.addToRemove(qhand.getGUID());
        Thread.sleep(1000);
        
        assertEquals("there should now be only 1 query", 1, queries.size());
        //now make sure the right query got deleted
        Iterator iter = queries.values().iterator();
        assertEquals("the wrong query got removed", 
                     qhand2.getGUID(),
                     ((QueryHandler)iter.next()).getGUID());
        //also make sure the removed GUID was deleted from the
        //TO_REMOVE set
        assertEquals("TO_REMOVE should be empty", 0, toR.size());
    }

    private static final class TestReplyHandler extends ReplyHandlerStub {
        
        public boolean isSupernodeClientConnection() {
            return true;
        }
    }
}










