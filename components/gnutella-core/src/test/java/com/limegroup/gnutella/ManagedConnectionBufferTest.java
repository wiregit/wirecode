package com.limegroup.gnutella;

import junit.framework.*;
import java.io.*;
import java.lang.reflect.Method;
import java.util.Properties;

import com.limegroup.gnutella.connection.BIOMessageWriter;
import com.limegroup.gnutella.connection.CompositeQueue;
import com.limegroup.gnutella.handshaking.*;
import com.limegroup.gnutella.routing.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.stubs.*;
import com.limegroup.gnutella.settings.*;
import com.limegroup.gnutella.util.*;

/**
 * This class tests <tt>ManagedConnection</tt>'s ability to buffer messages 
 * before sending them.  Normally, buffering occurs when we cannot send
 * messages as fast as they are arriving.  The test artificially creates
 * this situation.
 */
public class ManagedConnectionBufferTest extends BaseTestCase {
	
    public static final int BUFFER_PORT = 6666;
    private Connection out = null;
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

    public static void globalSetup() throws Exception {
        // make sure we don't use NIO
        ConnectionSettings.USE_NIO.setValue(false);    
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

        new RouterService(new ActivityCallbackStub());

		acceptor = new MiniAcceptor(new DummyResponder("localhost"), 
            BUFFER_PORT);

        CompositeQueue.QUEUE_TIME=1000;
		out = new Connection("localhost", BUFFER_PORT);
    }
    
    public void tearDown() throws Exception {
        assertNotNull("in should not be null", in);
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
		in = acceptor.accept();
        assertNotNull("in should not be null", in);
		assertTrue("out.write should be deflated", out.isWriteDeflated());
		assertTrue("out.read should be deflated", out.isReadDeflated());
		assertTrue("in.write should be deflated", in.isWriteDeflated());
		assertTrue("in.read should be deflated", in.isReadDeflated());
		checkStreams();
    }
    
    /**
     * Sets up the out & in for not compression
     */
     private void setupNotCompressed() throws Exception {
        ConnectionSettings.ACCEPT_DEFLATE.setValue(false);
		ConnectionSettings.ENCODE_DEFLATE.setValue(false);
		
		out.initialize();
		in = acceptor.accept();
        assertNotNull("in should not be null", in);
		assertTrue("out.write should be !deflated",! out.isWriteDeflated());
		assertTrue("out.read should be !deflated", !out.isReadDeflated());
		assertTrue("in.write should be !deflated", !in.isWriteDeflated());
		assertTrue("in.read should be !deflated", !in.isReadDeflated());
		checkStreams();
    }
    
    /**
     * Checks the streams (in/out) of the connection to make sure they're
     * using the correct streams.  If compressed, they will all be
     * UncompressingOutputStream / CompressingInputStream.  If not,
     * outOut will be ThrottledOutputStream, out's in will be
     * BufferedInputStream, and in's will be BufferedXStreams.
     */
    private void checkStreams() throws Exception {
        InputStream outIn, inIn;
        OutputStream outOut, inOut;
        outIn = (InputStream)PrivilegedAccessor.getValue(out, "_in");
        outOut = (OutputStream)PrivilegedAccessor.getValue(out, "_out");
        inIn = (InputStream)PrivilegedAccessor.getValue(in, "_in");
        inOut = (OutputStream)PrivilegedAccessor.getValue(in, "_out");
        
        if( out.isReadDeflated() )
            assertInstanceof(UncompressingInputStream.class, outIn);
        else
            assertInstanceof(BufferedInputStream.class, outIn);
        
        if( out.isWriteDeflated() )
            assertInstanceof(CompressingOutputStream.class, outOut);
        else
            assertInstanceof(ThrottledOutputStream.class, outOut);
        
        if( in.isReadDeflated() )
            assertInstanceof(UncompressingInputStream.class, inIn);
        else
            assertInstanceof(BufferedInputStream.class, inIn);
        
        if( in.isWriteDeflated() )
            assertInstanceof(CompressingOutputStream.class, inOut);
        else
            assertInstanceof(ThrottledOutputStream.class, inOut);
    }        

    /**
     * Tests that the buffer drops when compressed.
     */
    public void testDropBufferCompressed() throws Exception {
        setupCompressed();
        tDropBuffer();
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
		throws IOException, BadPacketException, InterruptedException {
        PingRequest pr=null;
        long start=0;
        long elapsed=0;

        assertEquals("unexpected # sent messages", 0, 
            out.stats().getNumMessagesSent()); 
        assertEquals("unexpected # sent bytes", 0, out.stats().getBytesSent());
        pr=new PingRequest((byte)3);
        out.send(pr);
        Thread.sleep(400);
        start=System.currentTimeMillis();        
        pr=(PingRequest)in.receive();
        elapsed=System.currentTimeMillis()-start;
        assertEquals("unexpected number of sent messages", 1, 
            out.stats().getNumMessagesSent());
        assertEquals( pr.getTotalLength(), 
            in.stats().getUncompressedBytesReceived() );
        assertEquals( pr.getTotalLength(), 
            out.stats().getUncompressedBytesSent() );
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
		throws Exception {
        //1. Buffer tons of messages.  By killing the old thread and restarting
        //later, we simulate a stall in the network.
        stopOutputRunner(out);
        //out.stopOutputRunner();
        Message m=null;

        // send QueryRequest		
		out.send(QueryRequest.createQuery("test", (byte)5));

        // send PingRequest with one hop
        m=new PingRequest((byte)5);
        m.hop();
        out.send(m);

        // send QueryReply with priority 1
        m=new QueryReply(new byte[16], (byte)5, 6340, new byte[4], 0, 
                         new Response[0], new byte[16], false);
        m.setPriority(30000);
        out.send(m);
        
        // send 3 push requests
        out.send(new PushRequest(new byte[16], (byte)5, new byte[16],
                                 0, new byte[4], 6340));
        out.send(new PushRequest(new byte[16], (byte)5, new byte[16],
                                 0, new byte[4], 6341));
        out.send(new PushRequest(new byte[16], (byte)5, new byte[16],
                                 0, new byte[4], 6342));
                                 
        // send QueryReply with priority 1
        m=new QueryReply(new byte[16], (byte)5, 6343, new byte[4], 0, 
                         new Response[0], new byte[16], false);
        m.setPriority(30000);
        out.send(m);
                                 
        // send 4 push requests                                 
        out.send(new PushRequest(new byte[16], (byte)5, new byte[16],
                                 0, new byte[4], 6343));
        out.send(new PushRequest(new byte[16], (byte)5, new byte[16],
                                 0, new byte[4], 6344));
        out.send(new PushRequest(new byte[16], (byte)5, new byte[16],
                                 0, new byte[4], 6345));
        out.send(new PushRequest(new byte[16], (byte)5, new byte[16],
                                 0, new byte[4], 6346));                                                                                                                                    
                                 
        // send QueryReply with priority 7                                 
        m=new QueryReply(new byte[16], (byte)5, 6341, new byte[4], 0, 
                         new Response[0], new byte[16], false);
        out.send(m);
        
        // send PingReply with 3 hops
        m=PingReply.create(new byte[16], (byte)5, 6343, new byte[4]);
        m.hop();  m.hop();  m.hop();        
        out.send(m);
        
        // send PingReply with 3 hops
        m=PingReply.create(new byte[16], (byte)5, 6344, new byte[4]);
        m.hop();  m.hop();  m.hop();
        out.send(m);        
        
        // send QueryReply with priority 2
        m=new QueryReply(new byte[16], (byte)5, 6344, new byte[4], 0, 
                         new Response[0], new byte[16], false);
        m.setPriority(20000);
        out.send(m);
        
        // send QueryReply with priority 0
        m=new QueryReply(new byte[16], (byte)5, 6345, new byte[4], 0, 
                         new Response[0], new byte[16], false);
        m.setPriority(50000);
        out.send(m);              
        
        // send Reset message
        out.send(new ResetTableMessage(1024, (byte)2));
        
        // send a watchdog pong
        out.send(PingReply.create(new byte[16], (byte)1, 6342, new byte[4]));
        
        // send PingReply with 2 hops
        m = PingReply.create(new byte[16], (byte)3, 6340, new byte[4]);
        m.hop();
        m.hop();
        out.send(m);
        
        // send QueryReply with priority 4
        m=new QueryReply(new byte[16], (byte)5, 6346, new byte[4], 0, 
                         new Response[0], new byte[16], false);
        m.setPriority(5000);
        out.send(m);            
        
        // send QueryReply with priority 5
        m=new QueryReply(new byte[16], (byte)5, 6342, new byte[4], 0, 
                         new Response[0], new byte[16], false);
        m.setPriority(1000);
        out.send(m);
        
        // send Patch message
        out.send(new PatchTableMessage((short)1, (short)2, 
                                       PatchTableMessage.COMPRESSOR_NONE,
                                       (byte)8, new byte[10], 0, 5));
                                       
        // send a watchdog ping                                       
        out.send(new PingRequest((byte)2));
        
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
        //out._lastPriority=0;  //cheating to make old tests work
        //out.resetPriority();
        
        resetPriority(out);
        startOutputRunner(out);
        

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
            throws Exception {
        assertEquals("unexected queue time",
            1000, CompositeQueue.QUEUE_TIME);
        
        //Drop one message
        stopOutputRunner(out);
        //out.stopOutputRunner();        
        out.send(QueryRequest.createQuery("0", (byte)3));   
        sleep(1200);
        out.send(QueryRequest.createQuery("1200", (byte)3)); 
        startOutputRunner(out);
        //out.startOutputRunner();
        Message m=(QueryRequest)in.receive(500);
        assertInstanceof("m not a queryrequest", QueryRequest.class, m);
        assertEquals("unexpected query", "1200", ((QueryRequest)m).getQuery());
        try {
            m=in.receive(200);
            fail("buffer didn't timeout in time.  message: " + m);
        } catch (InterruptedIOException e) {
        }
        assertEquals("unexpected # of dropped sent messages", 
            1, out.stats().getNumSentMessagesDropped());

        //Drop many messages
        stopOutputRunner(out);
        //out.stopOutputRunner();        
        out.send(QueryRequest.createQuery("0", (byte)3));   
        sleep(300);
        out.send(QueryRequest.createQuery("300", (byte)3));        
        sleep(300);
        out.send(QueryRequest.createQuery("600", (byte)3));        
        sleep(500);
        out.send(QueryRequest.createQuery("1100", (byte)3));
        sleep(900);
        out.send(QueryRequest.createQuery("2000", (byte)3));
        startOutputRunner(out);
        //out.startOutputRunner();
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
            1+3, out.stats().getNumSentMessagesDropped());
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
            throws Exception {
        //Tests wrap-around loop of sendQueuedMessages
        Message m=null;

        // head...tail
        stopOutputRunner(out);
        //out.stopOutputRunner(); 
        out.send(hopped(new PingRequest((byte)4)));
        out.send(QueryRequest.createQuery("a", (byte)3));
        startOutputRunner(out);
        //out.startOutputRunner();
        assertInstanceof("didn't recieve queryrequest", 
            QueryRequest.class, in.receive());
        assertInstanceof("didn't recieve pingrequest", 
            PingRequest.class, in.receive());

        //tail...<wrap>...head
        stopOutputRunner(out);
        //out.stopOutputRunner(); 
        out.send(QueryRequest.createQuery("a", (byte)3));
        out.send(hopped(new PingRequest((byte)5)));
        startOutputRunner(out);
        //out.startOutputRunner();
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
        stopOutputRunner(out);
        //out.stopOutputRunner(); 
        out.send(new PingRequest((byte)1));
        out.send(new QueryReply(new byte[16], (byte)5, 6341, new byte[4], 0, 
                                new Response[0], new byte[16], false));
        out.send(new ResetTableMessage(1024, (byte)2));
        out.send(QueryRequest.createQuery("a", (byte)3));
        startOutputRunner(out);
        //out.startOutputRunner();
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

        int initialDropped = out.stats().getNumSentMessagesDropped();
        
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
        
        int dropped=out.stats().getNumSentMessagesDropped()-initialDropped;
        assertGreaterThan("dropped msg cnt > 0", 0, dropped);
        assertGreaterThan("drop prct > 0", 0, out.stats().getPercentSentDropped());
        assertLessThan("read cnt < total", total, read);
        assertEquals("drop + read == total", total, dropped+read);
    }

	private static class DummyResponder
	  extends AuthenticationHandshakeResponder {
		DummyResponder(String host) {
			super(null, host);
		}

		protected HandshakeResponse 
			respondUnauthenticated(HandshakeResponse response,
			                       boolean outgoing) 
			throws IOException {
			    Properties props = new Properties();
			    if(ConnectionSettings.ACCEPT_DEFLATE.getValue())
			        props.put("Accept-Encoding", "deflate");
                if(response.isDeflateAccepted())
                    props.put("Content-Encoding", "deflate");
			return HandshakeResponse.createResponse(props);
		}
	}

    /**
     * Utility method that uses reflection to stop the sending thread of the
     * given connection.
     * 
     * @param mc the connection to stop
     * @throws Exception for a whole bunch of possible reasons
     */
    private static void stopOutputRunner(Connection mc) 
        throws Exception {
        Object obj = PrivilegedAccessor.getValue(mc, "_messageWriter");
        BIOMessageWriter writer = 
            (BIOMessageWriter)PrivilegedAccessor.getValue(obj, "DELEGATE"); 
        Object writeLock = 
            PrivilegedAccessor.getValue(writer, "QUEUE_LOCK");
        Method close = 
            PrivilegedAccessor.getMethod(writer, "close", new Class[0]);
        synchronized(writeLock) {
            
            PrivilegedAccessor.setValue(mc, "_closed", Boolean.TRUE);
            close.invoke(writer, new Object[0]);
        }
        while(!mc.runnerDied()) {
            Thread.yield();
        }
        PrivilegedAccessor.setValue(mc, "_runnerDied", Boolean.FALSE);
        PrivilegedAccessor.setValue(mc, "_closed", Boolean.FALSE);
        //mc.stopOutputRunner();
    }
    
    private static void startOutputRunner(Connection mc)
        throws Exception {
        Object obj = PrivilegedAccessor.getValue(mc, "_messageWriter");
        BIOMessageWriter writer = 
            (BIOMessageWriter)PrivilegedAccessor.getValue(obj, "DELEGATE"); 
        Method start = 
            PrivilegedAccessor.getMethod(writer, "start", new Class[0]);        
        start.invoke(writer, new Object[0]);   
    }
    
    private static void resetPriority(Connection mc)
        throws Exception {
            Object obj = PrivilegedAccessor.getValue(mc, "_messageWriter");
            BIOMessageWriter writer = 
                (BIOMessageWriter)PrivilegedAccessor.getValue(obj, "DELEGATE"); 
            Method resetPriority = 
                PrivilegedAccessor.getMethod(writer, "resetPriority", new Class[0]);        
            resetPriority.invoke(writer, new Object[0]);                
    }
}
