package com.limegroup.gnutella.connection;

import junit.framework.Test;

import com.google.inject.Injector;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingReplyFactory;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryReplyFactory;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.util.LimeTestCase;

/**
 * Unit tests for PriorityMessageQueue
 */
public class PriorityMessageQueueTest extends LimeTestCase {
    
    /**
     * A non-blank IP.
     */
    private static final byte[] IP = new byte[] { 1, 1, 1, 1 };
    
    private QueryReplyFactory queryReplyFactory;
    private QueryRequestFactory queryRequestFactory;
    private PingReplyFactory pingReplyFactory;
    
	public PriorityMessageQueueTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(PriorityMessageQueueTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}

   @Override
    protected void setUp() throws Exception {
        Injector injector = LimeTestUtils.createInjector();
        queryReplyFactory = injector.getInstance(QueryReplyFactory.class);
        queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
        pingReplyFactory = injector.getInstance(PingReplyFactory.class);
    }
            

    public void testLegacy() {
        Message m=null;

        //By guid volume
        PriorityMessageQueue q=new PriorityMessageQueue(
            1000, Integer.MAX_VALUE, 100);
        m=queryReplyFactory.createQueryReply(new byte[16], (byte)5, 6341,
                IP, 0, new Response[0], new byte[16], false, false, false, false,
                false, false);
        m.setPriority(1000);
        q.add(m);
        m=queryReplyFactory.createQueryReply(new byte[16], (byte)5, 6331,
                IP, 0, new Response[0], new byte[16], false, false, false, false,
                false, false);
        m.setPriority(1000);
        q.add(m);
        m=queryReplyFactory.createQueryReply(new byte[16], (byte)5, 6349,
                IP, 0, new Response[0], new byte[16], false, false, false, false,
                false, false);
        m.setPriority(9000);
        q.add(m);
        m=queryReplyFactory.createQueryReply(new byte[16], (byte)5, 6340,
                IP, 0, new Response[0], new byte[16], false, false, false, false,
                false, false);
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
        QueryRequest query = queryRequestFactory.createQuery("low hops", (byte)5);
        q.add(query);
        query=queryRequestFactory.createQuery("high hops",(byte)5);
        for (int i=0; i<8; i++)
            query.hop(); 
        q.add(query);
        query=queryRequestFactory.createQuery("medium hops", (byte)5);
        query.hop();
        q.add(query);
        query=queryRequestFactory.createQuery("medium hops2", (byte)5);
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
        PingReply pong=pingReplyFactory.create(
            new byte[16], (byte)5, 6340, IP, 0, 0);
        q.add(pong);
        pong=pingReplyFactory.create(new byte[16], (byte)5, 6330, IP, 0, 0);
        q.add(pong);
        pong=pingReplyFactory.create(new byte[16], (byte)5, 6342, IP, 0, 0);
        pong.hop();
        pong.hop();
        q.add(pong);
        pong=pingReplyFactory.create(new byte[16], (byte)5, 6341, IP, 0, 0);
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
