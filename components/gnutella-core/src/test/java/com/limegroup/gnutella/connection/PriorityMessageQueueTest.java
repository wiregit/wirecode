package com.limegroup.gnutella.connection;

import junit.framework.*;
import com.limegroup.gnutella.util.PrivilegedAccessor;
import com.limegroup.gnutella.util.BaseTestCase;
import com.sun.java.util.collections.*;

import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.Response;

/**
 * Unit tests for PriorityMessageQueue
 */
public class PriorityMessageQueueTest extends BaseTestCase {
    
	public PriorityMessageQueueTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(PriorityMessageQueueTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}

    public void testLegacy() {
        Message m=null;

        //By guid volume
        PriorityMessageQueue q=new PriorityMessageQueue(
            1000, Integer.MAX_VALUE, 100);
        m=new QueryReply(new byte[16], (byte)5, 6341, new byte[4], 0, 
                         new Response[0], new byte[16], 
                         false, false, false, false, false, false);
        m.setPriority(1000);
        q.add(m);
        m=new QueryReply(new byte[16], (byte)5, 6331, new byte[4], 0, 
                         new Response[0], new byte[16],
                         false, false, false, false, false, false);
        m.setPriority(1000);
        q.add(m);
        m=new QueryReply(new byte[16], (byte)5, 6349, new byte[4], 0, 
                         new Response[0], new byte[16],
                         false, false, false, false, false, false);
        m.setPriority(9000);
        q.add(m);
        m=new QueryReply(new byte[16], (byte)5, 6340, new byte[4], 0, 
                         new Response[0], new byte[16],
                         false, false, false, false, false, false);
        m.setPriority(0);
        q.add(m);
        m=q.removeNextInternal();
        assertEquals(6340, ((QueryReply)m).getPort());
        m=q.removeNextInternal();
        assertEquals(6331, ((QueryReply)m).getPort());
        m=q.removeNextInternal();
        assertEquals(6341, ((QueryReply)m).getPort());
        m=q.removeNextInternal();
        assertEquals(6349, ((QueryReply)m).getPort());
        assertNull(q.removeNext());
        m=null;

        //By hops
        q=new PriorityMessageQueue(1000, Integer.MAX_VALUE, 100);
        QueryRequest query = QueryRequest.createQuery("low hops", (byte)5);
        q.add(query);
        query=QueryRequest.createQuery("high hops",(byte)5);
        for (int i=0; i<8; i++)
            query.hop(); 
        q.add(query);
        query=QueryRequest.createQuery("medium hops", (byte)5);
        query.hop();
        q.add(query);
        query=QueryRequest.createQuery("medium hops2", (byte)5);
        query.hop();
        q.add(query);

        query=(QueryRequest)q.removeNextInternal();
        assertEquals("low hops", query.getQuery());
        query=(QueryRequest)q.removeNextInternal();
        assertEquals("medium hops2", query.getQuery());
        query=(QueryRequest)q.removeNextInternal();
        assertEquals("medium hops", query.getQuery());
        query=(QueryRequest)q.removeNextInternal();
        assertEquals("high hops", query.getQuery());
        assertNull(q.removeNextInternal());
        query=null;

        //By negative hops
        q=new PriorityMessageQueue(1000, Integer.MAX_VALUE, 100);
        PingReply pong=PingReply.create(
            new byte[16], (byte)5, 6340, new byte[4], 0, 0);
        q.add(pong);
        pong=PingReply.create(new byte[16], (byte)5, 6330, new byte[4], 0, 0);
        q.add(pong);
        pong=PingReply.create(new byte[16], (byte)5, 6342, new byte[4], 0, 0);
        pong.hop();
        pong.hop();
        q.add(pong);
        pong=PingReply.create(new byte[16], (byte)5, 6341, new byte[4], 0, 0);
        pong.hop();
        q.add(pong);
        pong=(PingReply)q.removeNextInternal();
        assertEquals(6342, pong.getPort());
        pong=(PingReply)q.removeNextInternal();
        assertEquals(6341, pong.getPort());
        pong=(PingReply)q.removeNextInternal();
        assertEquals(6330, pong.getPort());
        pong=(PingReply)q.removeNextInternal();
        assertEquals(6340, pong.getPort());
        assertNull(q.removeNextInternal());
    }
    
}
