package com.limegroup.gnutella.connection;

import junit.framework.Test;

import com.limegroup.gnutella.ProviderHacks;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.PushRequest;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.routing.PatchTableMessage;
import com.limegroup.gnutella.routing.ResetTableMessage;
import com.limegroup.gnutella.util.LimeTestCase;

/**
 * Tests that the basic queue is just that -- basic.
 */
public class BasicQueueTest extends LimeTestCase {
    
    private BasicQueue QUEUE = new BasicQueue();
	
    private static final byte[] IP = new byte[] { 1, 1, 1, 1 };

	public BasicQueueTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(BasicQueueTest.class);
	}

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
        
    // test buffer doesn't get re-ordered.
    public void testBuffer() throws Exception {
        Message m = null;
        
        QUEUE.add(q("first"));
        QUEUE.add(q("second"));
        QUEUE.add(g(7000));
        QUEUE.add(q("third"));
        QUEUE.add(p(4));
        QUEUE.add(p(3));
        QUEUE.add(q("fourth"));
        QUEUE.add(s(6340));
        QUEUE.add(s(6341));
        QUEUE.add(r(6342));
        QUEUE.add(t());
        QUEUE.add(c(1));
        QUEUE.add(q("fifth"));
        QUEUE.add(c(2));
        
        m = QUEUE.removeNext();
        assertInstanceof(QueryRequest.class, m);
        assertEquals("first", ((QueryRequest)m).getQuery());

        m = QUEUE.removeNext();
        assertInstanceof(QueryRequest.class, m);
        assertEquals("second", ((QueryRequest)m).getQuery());
        
        m = QUEUE.removeNext();
        assertInstanceof(PingReply.class, m);
        assertEquals(7000, ((PingReply)m).getPort());
        
        m = QUEUE.removeNext();
        assertInstanceof(QueryRequest.class, m);
        assertEquals("third", ((QueryRequest)m).getQuery());
        
        m = QUEUE.removeNext();
        assertInstanceof(PingRequest.class, m);
        assertEquals(4, m.getTTL());

        m = QUEUE.removeNext();
        assertInstanceof(PingRequest.class, m);
        assertEquals(3, m.getTTL());
        
        m = QUEUE.removeNext();
        assertInstanceof(QueryRequest.class, m);
        assertEquals("fourth", ((QueryRequest)m).getQuery());
        
        m = QUEUE.removeNext();
        assertInstanceof(PushRequest.class, m);
        assertEquals(6340, ((PushRequest)m).getPort());
        
        m = QUEUE.removeNext();
        assertInstanceof(PushRequest.class, m);
        assertEquals(6341, ((PushRequest)m).getPort());
        
        m = QUEUE.removeNext();
        assertInstanceof(QueryReply.class, m);
        assertEquals(6342, ((QueryReply)m).getPort());
        
        m = QUEUE.removeNext();
        assertInstanceof(ResetTableMessage.class, m);
        
        m = QUEUE.removeNext();
        assertInstanceof(PatchTableMessage.class, m);
        assertEquals(1, ((PatchTableMessage)m).getSequenceNumber());

        m = QUEUE.removeNext();
        assertInstanceof(QueryRequest.class, m);
        assertEquals("fifth", ((QueryRequest)m).getQuery());
        
        m = QUEUE.removeNext();
        assertInstanceof(PatchTableMessage.class, m);
        assertEquals(2, ((PatchTableMessage)m).getSequenceNumber());
        
        assertNull(QUEUE.removeNext());
    }
    
    private QueryRequest q(String query) {
        return ProviderHacks.getQueryRequestFactory().createQuery(query, (byte)5);
    }
    
    private PingReply g(int port) {
        return ProviderHacks.getPingReplyFactory().create(new byte[16], (byte)5, port, IP);
    }
    
    private PingRequest p(int ttl) {
        return new PingRequest((byte)ttl);
    }
    
    private PushRequest s(int port) {
        return new PushRequest(new byte[16], (byte)5, new byte[16], 0, IP, port);
    }
    
    private QueryReply r(int port) {
        return ProviderHacks.getQueryReplyFactory().createQueryReply(new byte[16], (byte)5,
                port, IP, 0, new Response[0], new byte[16], false);
    }
    
    private ResetTableMessage t() {
        return new ResetTableMessage(1024, (byte)2);
    }
    
    private PatchTableMessage c(int sequence) {
        return new PatchTableMessage((short)sequence, (short)sequence, 
                                       PatchTableMessage.COMPRESSOR_NONE,
                                       (byte)8, new byte[10], 5, 9);
    }
}
