package com.limegroup.gnutella;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.Properties;

import junit.framework.Test;

import com.limegroup.gnutella.handshaking.DefaultHandshakeResponder;
import com.limegroup.gnutella.handshaking.HandshakeResponse;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.PushRequest;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.routing.PatchTableMessage;
import com.limegroup.gnutella.routing.ResetTableMessage;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.util.CompressingOutputStream;
import com.limegroup.gnutella.util.PrivilegedAccessor;
import com.limegroup.gnutella.util.ThrottledOutputStream;
import com.limegroup.gnutella.util.UncompressingInputStream;

/**
 * This class tests <tt>ManagedConnection</tt>'s ability to buffer messages 
 * before sending them.  Normally, buffering occurs when we cannot send
 * messages as fast as they are arriving.  The test artificially creates
 * this situation.
 */
public class ManagedConnectionBufferTest extends BaseTestCase {
	
    public static final int PORT=6666;
    private static final byte[] IP = new byte[] { 1, 1, 1, 1 };
    private ManagedConnection out = null;
    private Connection in = null;
    MiniAcceptor acceptor = null;

	public ManagedConnectionBufferTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(ManagedConnectionBufferTest.class);
	}

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

	public void setUp() throws Exception {
		setStandardSettings();
		
		ConnectionSettings.NUM_CONNECTIONS.setValue(2);
		ConnectionSettings.WATCHDOG_ACTIVE.setValue(true);
		ConnectionSettings.PREFERENCING_ACTIVE.setValue(false);

		ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
		ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);

		// disable connection removal
		ConnectionSettings.REMOVE_ENABLED.setValue(false);
        ConnectionSettings.SOFT_MAX.setValue((byte)4);

        RouterService rs = 
			new RouterService(new ActivityCallbackStub());

		acceptor = new MiniAcceptor(new DummyResponder("localhost"), PORT);

		ManagedConnection.QUEUE_TIME=1000;
		out = new ManagedConnection("localhost", PORT);
    }
    
    public void tearDown() throws Exception {
        in.close();
        out.close();
    }
    
    /**
     * Sets up the out & in for compression
     */    
    private void setupCompressed() throws Exception {
        ConnectionSettings.ACCEPT_DEFLATE.setValue(true);
		ConnectionSettings.ENCODE_DEFLATE.setValue(true);
		
		out.initialize();
		out.buildAndStartQueues();
		in = acceptor.accept();
		assertTrue("out.write should be deflated", out.isWriteDeflated());
		assertTrue("out.read should be deflated", out.isReadDeflated());
		assertTrue("in.write should be deflated", in.isWriteDeflated());
		assertTrue("in.read should be deflated", in.isReadDeflated());
		//checkStreams();
    }
    
    /**
     * Sets up the out & in for not compression
     */
     private void setupNotCompressed() throws Exception {
        ConnectionSettings.ACCEPT_DEFLATE.setValue(false);
		ConnectionSettings.ENCODE_DEFLATE.setValue(false);
		
		out.initialize();
		out.buildAndStartQueues();
		in = acceptor.accept();
		assertTrue("out.write should be !deflated",! out.isWriteDeflated());
		assertTrue("out.read should be !deflated", !out.isReadDeflated());
		assertTrue("in.write should be !deflated", !in.isWriteDeflated());
		assertTrue("in.read should be !deflated", !in.isReadDeflated());
		//checkStreams();
    }
    
    /**
     * Tests that flushing works correctly while compressed.
     */
    public void testSendFlushCompressed() throws Exception {
        setupCompressed();
		tSendFlush();
    }
    
    /**
     * Tests that flushing works correctly while not compressed.
     */
    public void testSendFlushNotCompressed() throws Exception {
        setupNotCompressed();
		tSendFlush();       
    }

    private void tSendFlush() 
		throws IOException, BadPacketException {
        PingRequest pr=null;
        long start=0;
        long elapsed=0;

        assertEquals("unexpected # sent messages", 0, out.getNumMessagesSent()); 
        assertEquals("unexpected # sent bytes", 0, out.getBytesSent());
        pr=new PingRequest((byte)3);
        out.send(pr);
        start=System.currentTimeMillis();        
        pr=(PingRequest)in.receive();
        elapsed=System.currentTimeMillis()-start;
        assertEquals("unexpected number of sent messages", 1, out.getNumMessagesSent());
        assertEquals( pr.getTotalLength(), in.getUncompressedBytesReceived() );
        assertEquals( pr.getTotalLength(), out.getUncompressedBytesSent() );
        assertLessThan("Unreasonably long send time", 500, elapsed);
        assertEquals("hopped something other than 0", 0, pr.getHops());
        assertEquals("unexpected ttl", 3, pr.getTTL());
    }
    
    /**
     * Tests the reordering the buffer works while compressed.
     */
    public void testReorderBufferCompressed() throws Exception {
        setupCompressed();
        tReorderBuffer();
    }
    
    /**
     * Tests that reordering the buffer works while not compressed.
     */
    public void testReorderBufferNotCompressed() throws Exception {
        setupNotCompressed();
        tReorderBuffer();
    }
    
    /**
     * Perhaps the most important test in the suite, this checks to make
     * sure that messages in the outgoing queue are ordered and prioritized
     * correctly.
     * NOTE: It is important to remember that the queues the messages are stored
     * in are BucketQueues -- that means that even if the capacity of the queue is
     * 1, then EACH BUCKET will have 1 item.  So two PingReply's with different
     * hops will BOTH be stored in the queue, because each hop is a different
     * priority, and each priority is a different bucket.
     */
    private void tReorderBuffer() 
		throws IOException, BadPacketException {
        //1. Buffer tons of messages.  By killing the old thread and restarting
        //later, we simulate a stall in the network.
        out.stopOutputRunner();
        Message m=null;

        // send QueryRequest		
		out.send(QueryRequest.createQuery("test", (byte)5));

        // send PingRequest with one hop
        m=new PingRequest((byte)5);
        m.hop();
        out.send(m);

        // send QueryReply with priority 1
        m=new QueryReply(new byte[16], (byte)5, 6340, IP, 0, 
                         new Response[0], new byte[16], false);
        m.setPriority(30000);
        out.send(m);
        
        // send 3 push requests
        out.send(new PushRequest(new byte[16], (byte)5, new byte[16],
                                 0, IP, 6340));
        out.send(new PushRequest(new byte[16], (byte)5, new byte[16],
                                 0, IP, 6341));
        out.send(new PushRequest(new byte[16], (byte)5, new byte[16],
                                 0, IP, 6342));
                                 
        // send QueryReply with priority 1
        m=new QueryReply(new byte[16], (byte)5, 6343, IP, 0, 
                         new Response[0], new byte[16], false);
        m.setPriority(30000);
        out.send(m);
                                 
        // send 4 push requests                                 
        out.send(new PushRequest(new byte[16], (byte)5, new byte[16],
                                 0, IP, 6343));
        out.send(new PushRequest(new byte[16], (byte)5, new byte[16],
                                 0, IP, 6344));
        out.send(new PushRequest(new byte[16], (byte)5, new byte[16],
                                 0, IP, 6345));
        out.send(new PushRequest(new byte[16], (byte)5, new byte[16],
                                 0, IP, 6346));                                                                                                                                    
                                 
        // send QueryReply with priority 7                                 
        m=new QueryReply(new byte[16], (byte)5, 6341, IP, 0, 
                         new Response[0], new byte[16], false);
        out.send(m);
        
        // send PingReply with 3 hops
        m=PingReply.create(new byte[16], (byte)5, 6343, IP);
        m.hop();  m.hop();  m.hop();        
        out.send(m);
        
        // send PingReply with 3 hops
        m=PingReply.create(new byte[16], (byte)5, 6344, IP);
        m.hop();  m.hop();  m.hop();
        out.send(m);        
        
        // send QueryReply with priority 2
        m=new QueryReply(new byte[16], (byte)5, 6344, IP, 0, 
                         new Response[0], new byte[16], false);
        m.setPriority(20000);
        out.send(m);
        
        // send QueryReply with priority 0
        m=new QueryReply(new byte[16], (byte)5, 6345, IP, 0, 
                         new Response[0], new byte[16], false);
        m.setPriority(50000);
        out.send(m);              
        
        // send Reset message
        out.send(new ResetTableMessage(1024, (byte)2));
        
        // send a watchdog pong
        out.send(PingReply.create(new byte[16], (byte)1, 6342, IP));
        
        // send PingReply with 2 hops
        m = PingReply.create(new byte[16], (byte)3, 6340, IP);
        m.hop();
        m.hop();
        out.send(m);
        
        // send QueryReply with priority 4
        m=new QueryReply(new byte[16], (byte)5, 6346, IP, 0, 
                         new Response[0], new byte[16], false);
        m.setPriority(5000);
        out.send(m);            
        
        // send QueryReply with priority 5
        m=new QueryReply(new byte[16], (byte)5, 6342, IP, 0, 
                         new Response[0], new byte[16], false);
        m.setPriority(1000);
        out.send(m);
        
        // send Patch message
        out.send(new PatchTableMessage((short)1, (short)2, 
                                       PatchTableMessage.COMPRESSOR_NONE,
                                       (byte)8, new byte[10], 0, 5));
                                       
        // send a watchdog ping                                       
        out.send(new PingRequest((byte)1));
        
        // send Patch message
        out.send(new PatchTableMessage((short)2, (short)2, 
                                       PatchTableMessage.COMPRESSOR_NONE,
                                       (byte)8, new byte[10], 5, 9));

        // send QueryRequest
		out.send(QueryRequest.createQuery("test2", (byte)5));
				 
        // send QueryRequest with 1 hop
		m = QueryRequest.createQuery("test far", (byte)5);
        m.hop();
        out.send(m);
        
        // send QueryRequest with 2 hop
		m = QueryRequest.createQuery("test farther", (byte)5);
        m.hop(); m.hop();
        out.send(m);        
               
        //2. Now we let the messages pass through, as if the receiver's window
        //became non-zero.  Buffers should look this before emptying:
        // Except for QRP messages, all queues are LIFO when priorities match
        //  WATCHDOG: pong/6342 ping
        //  PUSH: x/6340 x/6341 x/6342 x/6343 x/6344 x/6345 x/6346
        //  QUERY_REPLY: 6345/50k 6340/30k 6343/30k 
        //               6344/20k 6346/5k 6342/1k 6341/0
        //  QUERY: "test farther"/2 "test far"/1, "test"/0, "test2"/0
        //  PING_REPLY: x/6340/2 x/6344/3
        //  PING: x
        //  OTHER: reset patch1 patch2
        out._lastPriority=0;  //cheating to make old tests work
        out.startOutputRunner();

        //3. Read them...now in different order!
        m=in.receive(); //watchdog ping
        assertInstanceof("Unexpected message: "+m, PingRequest.class, m);
        assertEquals("Unexpected # of hops"+m, 0, m.getHops());

        m=in.receive(); //push        
        assertInstanceof("Unexpected message: "+m, PushRequest.class, m);
        assertEquals("unexpected push request port", 6346,
            ((PushRequest)m).getPort());

        m=in.receive(); //push        
        assertInstanceof("Unexpected message: "+m, PushRequest.class, m);
        assertEquals("unexpected push request port", 6345,
            ((PushRequest)m).getPort());

        m=in.receive(); //push
        assertInstanceof("Unexpected message: "+m, PushRequest.class, m);
        assertEquals("unexpected push request port", 6344,
            ((PushRequest)m).getPort());

        m=in.receive(); //push
        assertInstanceof("Unexpected message: "+m, PushRequest.class, m);
        assertEquals("unexpected push request port", 6343,
            ((PushRequest)m).getPort());
            
        m=in.receive(); //push        
        assertInstanceof("Unexpected message: "+m, PushRequest.class, m);
        assertEquals("unexpected push request port", 6342,
            ((PushRequest)m).getPort());

        m=in.receive(); //push        
        assertInstanceof("Unexpected message: "+m, PushRequest.class, m);
        assertEquals("unexpected push request port", 6341,
            ((PushRequest)m).getPort());            

        m=in.receive(); //reply/6341 (high priority)
        assertInstanceof("m not a queryreply", QueryReply.class, m);
        assertEquals("unexpected queryreply port.  priority: "
             + m.getPriority(), 6341, ((QueryReply)m).getPort());

        m=in.receive(); //reply/6342 (medium priority)
        assertInstanceof("m not a queryreply", QueryReply.class, m);
        assertEquals("unexpected query reply port", 6342,
            ((QueryReply)m).getPort());
            
        m=in.receive(); //reply/6346
        assertInstanceof("m not a queryreply", QueryReply.class, m);
        assertEquals("unexpected queryreply port",
            6346, ((QueryReply)m).getPort());

        m=in.receive(); //reply/6344
        assertInstanceof("m not a queryreply", QueryReply.class, m);
        assertEquals("unexpected queryreply port",
            6344, ((QueryReply)m).getPort());            
        
        m=in.receive(); //reply/6343
        assertInstanceof("m not a queryreply", QueryReply.class, m);
        assertEquals("unexpected queryreply port",
            6343, ((QueryReply)m).getPort());

        m=in.receive(); //reply/6340
        assertInstanceof("m not a queryreply", QueryReply.class, m);
        assertEquals("unexpected queryreply port",
            6340, ((QueryReply)m).getPort());

        m=in.receive(); //query "test2"/0
        assertInstanceof("m not a queryrequest", QueryRequest.class, m);
        assertEquals("unexpected query", "test2",
            ((QueryRequest)m).getQuery());
            
        m=in.receive(); //query "test"/0
        assertInstanceof("m not a queryrequest", QueryRequest.class, m);
        assertEquals("unexpected query", "test",
            ((QueryRequest)m).getQuery());            

        m=in.receive(); //query "test far"/1
        assertInstanceof("m not a queryrequest", QueryRequest.class, m);
        assertEquals("unexpected query",
            "test far", ((QueryRequest)m).getQuery());

        m=in.receive(); //reply 6343
        assertInstanceof("m not a pingreply", PingReply.class, m);
        assertEquals("unexpected pingreply port",
            6344, ((PingReply)m).getPort());

        m=in.receive(); //ping 6340
        assertInstanceof("m not a pingrequest", PingRequest.class, m);
        assertGreaterThan("unexpected number of hops (>0)",
            0, m.getHops());

        m=in.receive(); //QRP reset
        assertInstanceof("m not a resettablemessage",
            ResetTableMessage.class, m);

        m=in.receive(); //watchdog pong/6342
        assertInstanceof("m not a pingreply", PingReply.class, m);
        assertEquals("unexpected pingreply port",
            6342, ((PingReply)m).getPort());
        assertEquals("unexpected number of hops",
            0, m.getHops());  //watchdog response pong

        m=in.receive(); //push
        assertInstanceof("Unexpected message: "+m, PushRequest.class, m);
        assertEquals("unexpected push request port", 6340,
            ((PushRequest)m).getPort());            

        m=in.receive(); //reply/6341 (high priority)
        assertInstanceof("m not a queryreply", QueryReply.class, m);
        assertEquals("unexpected queryreply port.  priority: "
             + m.getPriority(),  6345, ((QueryReply)m).getPort());
             
        m=in.receive(); //query "test farther"/2
        assertInstanceof("m not a queryrequest", QueryRequest.class, m);
        assertEquals("unexpected query",
            "test farther", ((QueryRequest)m).getQuery());             

        m=in.receive(); //reply 6340
        assertInstanceof("m not a pingreply", PingReply.class, m);
        assertEquals("unexpected pingreply port",
            6340, ((PingReply)m).getPort());

        m=in.receive(); //QRP patch1
        assertInstanceof("m not a patchtablemessage",
            PatchTableMessage.class, m);
        assertEquals("unexpected patchtablemessage sequencenumber",
            1, ((PatchTableMessage)m).getSequenceNumber());

        m=in.receive(); //QRP patch2
        assertInstanceof("m not a patchtable message",
            PatchTableMessage.class, m);
        assertEquals("unexpected patchtablemessage sequencenumber",
            2, ((PatchTableMessage)m).getSequenceNumber());
    }
    
    /**
     * Tests that the timeout works while compressed.
     */
    public void testBufferTimeoutCompressed() throws Exception {
        setupCompressed();
        tBufferTimeout();
    }
    
    /**
     * Tests that the timeout works while not compressed.
     */
    public void testBufferTimeoutNotCompressed() throws Exception {
        setupNotCompressed();
        tBufferTimeout();
    }

    /**
     * Test to make sure that messages properly timeout in the message
     * queues and are dropped.
     */
    private void tBufferTimeout() 
            throws IOException, BadPacketException {
        assertEquals("unexected queue time",
            1000, ManagedConnection.QUEUE_TIME);
        
        //Drop one message
        out.stopOutputRunner();        
        out.send(QueryRequest.createQuery("0", (byte)3));   
        sleep(1200);
        out.send(QueryRequest.createQuery("1200", (byte)3)); 
        out.startOutputRunner();
        Message m=(QueryRequest)in.receive(500);
        assertInstanceof("m not a queryrequest", QueryRequest.class, m);
        assertEquals("unexpected query", "1200", ((QueryRequest)m).getQuery());
        try {
            m=in.receive(200);
            fail("buffer didn't timeout in time.  message: " + m.toString());
        } catch (InterruptedIOException e) {
        }
        assertEquals("unexpected # of dropped sent messages", 
            1, out.getNumSentMessagesDropped());

        //Drop many messages
        out.stopOutputRunner();        
        out.send(QueryRequest.createQuery("0", (byte)3));   
        sleep(300);
        out.send(QueryRequest.createQuery("300", (byte)3));        
        sleep(300);
        out.send(QueryRequest.createQuery("600", (byte)3));        
        sleep(500);
        out.send(QueryRequest.createQuery("1100", (byte)3));
        sleep(900);
        out.send(QueryRequest.createQuery("2000", (byte)3));
        out.startOutputRunner();
        m=in.receive(500);
        assertInstanceof("m not a queryrequest", QueryRequest.class, m);
        assertEquals("unexpected query", "2000", ((QueryRequest)m).getQuery());
        m=in.receive(500);
        assertInstanceof("m not a queryrequest", QueryRequest.class, m);
        assertEquals("unexpected query", ((QueryRequest)m).getQuery(), "1100");
        try {
            m=in.receive(200);
            fail("buffer didn't timeout in time.  message: " + m.toString());
        } catch (InterruptedIOException e) {
        }
        assertEquals("unexpected # of dropped sent messages",
            1+3, out.getNumSentMessagesDropped());
    }

    /**
     * Tests that the priority hint works with compression
     */
    public void testPriorityHintCompressed() throws Exception {
        setupCompressed();
        tPriorityHint();
    }
    
    /**
     * Tests that the priority hint works without compression
     */
    public void testPriorityHintNotCompressed() throws Exception {
        setupNotCompressed();
        tPriorityHint();
    }
    
    private void tPriorityHint() 
            throws IOException, BadPacketException {
        //Tests wrap-around loop of sendQueuedMessages
        Message m=null;

        // head...tail
        out.stopOutputRunner(); 
        out.send(hopped(new PingRequest((byte)4)));
        out.send(QueryRequest.createQuery("a", (byte)3));
        out.startOutputRunner();
        assertInstanceof("didn't recieve queryrequest", 
            QueryRequest.class, in.receive());
        assertInstanceof("didn't recieve pingrequest", 
            PingRequest.class, in.receive());

        //tail...<wrap>...head
        out.stopOutputRunner(); 
        out.send(QueryRequest.createQuery("a", (byte)3));
        out.send(hopped(new PingRequest((byte)5)));
        out.startOutputRunner();
        assertInstanceof("didn't recieve pingrequest",
            PingRequest.class, in.receive());
        assertInstanceof("didn't recieve queryrequest",
            QueryRequest.class, in.receive());

        //tail...<wrap>...head
        //  WATCHDOG: ping
        //  PUSH:
        //  QUERY_REPLY: reply
        //  QUERY: query
        //  PING_REPLY: 
        //  PING: 
        //  OTHER: reset
        out.stopOutputRunner(); 
        out.send(new PingRequest((byte)1));
        out.send(new QueryReply(new byte[16], (byte)5, 6341, IP, 0, 
                                new Response[0], new byte[16], false));
        out.send(new ResetTableMessage(1024, (byte)2));
        out.send(QueryRequest.createQuery("a", (byte)3));
        out.startOutputRunner();
        m=in.receive();
        assertInstanceof("Got: "+m, QueryRequest.class, m);
        m=in.receive();
        assertInstanceof("GOt: " +m, ResetTableMessage.class, m);
        m=in.receive();
        assertInstanceof("Got: " + m, PingRequest.class, m);
        m=in.receive();
        assertInstanceof("Got: " + m, QueryReply.class, m);
    }

    private static Message hopped(Message m) {
        m.hop();
        return m;
    }

    private static void sleep(long msecs) {
        try { Thread.sleep(msecs); } catch (InterruptedException ignored) { }
    }
    
    /**
     * Tests that the buffer drops when compressed.
     */
    public void testDropBufferCompressed() throws Exception {
        setupCompressed();
        tDropBuffer();
    }
    
    /**
     * Tests that the buffer drops when not compressed.
     */
    public void testDropBufferNotCompressed() throws Exception {
        setupNotCompressed();
        tDropBuffer();
    }
    

    private void tDropBuffer() 
		throws IOException, BadPacketException {
        //Send tons of messages...but don't read them
        int total=500;

        int initialDropped = out.getNumSentMessagesDropped();
        int initialSent = out.getNumMessagesSent();
        long initialBytes = out.getBytesSent();
        
        for (int i=0; i<total; i++) {
            out.send(QueryRequest.createQuery(
                "Some reaaaaaalllllly big query", (byte)3));
        }

        int read=0;
        int bytesRead=0;
        while (true) {
            try {
                Message m=in.receive(1000);
                read++;
                bytesRead+=m.getTotalLength();
            } catch (InterruptedIOException e) {
                break;
            }
        }
        
        int dropped=out.getNumSentMessagesDropped()-initialDropped;
        int sent = out.getNumMessagesSent() - initialSent;
        long bytes = out.getBytesSent() - initialBytes;
        //System.out.println("Sent messages/bytes: " + sent + "/" + bytes);
        //System.out.println("Dropped messages: "+dropped);
        //System.out.println("Read messages/bytes: "+read+"/"+bytesRead);
        
        assertGreaterThan("dropped msg cnt > 0", 0, dropped);
        assertGreaterThan("drop prct > 0", 0, out.getPercentSentDropped());
        assertLessThan("read cnt < total", total, read);
        assertEquals("drop + read == total", total, dropped+read);
    }

	private static class DummyResponder extends DefaultHandshakeResponder {
		DummyResponder(String host) {
			super(host);
		}

		public HandshakeResponse respond(HandshakeResponse response, boolean outgoing) throws IOException {
			Properties props = new Properties();
			props.put("X-Ultrapeer", "True");
			if (ConnectionSettings.ACCEPT_DEFLATE.getValue())
				props.put("Accept-Encoding", "deflate");
			if (response.isDeflateAccepted())
				props.put("Content-Encoding", "deflate");
			return HandshakeResponse.createResponse(props);
		}
		
		protected HandshakeResponse respondToIncoming(HandshakeResponse response) {
			return null;
		}

		protected HandshakeResponse respondToOutgoing(HandshakeResponse response) {
			return null;
		}
	}

}
