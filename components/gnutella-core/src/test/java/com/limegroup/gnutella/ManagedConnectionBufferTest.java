package com.limegroup.gnutella;

import junit.framework.*;
import java.io.*;
import java.net.*;
import java.util.Properties;
import com.limegroup.gnutella.handshaking.*;
import com.limegroup.gnutella.routing.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.stubs.*;
import com.limegroup.gnutella.settings.*;
import com.limegroup.gnutella.util.*;
import com.sun.java.util.collections.*;

/**
 * This class tests <tt>ManagedConnection</tt>'s ability to buffer messages 
 * before sending them.  Normally, buffering occurs when we cannot send
 * messages as fast as they are arriving.  The test artificially creates
 * this situation.
 */
public class ManagedConnectionBufferTest extends BaseTestCase {
	
    public static final int PORT=6666;
    private ManagedConnection out = null;
    private Connection in = null;

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
		
		ConnectionSettings.KEEP_ALIVE.setValue(2);
		ConnectionSettings.WATCHDOG_ACTIVE.setValue(true);
		ConnectionSettings.PREFERENCING_ACTIVE.setValue(false);

		ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
		ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);

		// disable connection removal
		ConnectionSettings.REMOVE_ENABLED.setValue(false);

        RouterService rs = 
			new RouterService(new ActivityCallbackStub());

		MiniAcceptor acceptor = 
			new MiniAcceptor(new DummyResponder("localhost"), PORT);

		ManagedConnection.QUEUE_TIME=1000;
		out = new ManagedConnection("localhost", PORT);

		out.initialize();
		in = acceptor.accept();
    }
    
    public void tearDown() throws Exception {
		in.close();
        out.close();
    }

    public void testSendFlush() 
		throws IOException, BadPacketException {
        PingRequest pr=null;
        long start=0;
        long elapsed=0;

        assertEquals("unexpected # sent messages", 0, out.getNumMessagesSent()); 
        assertEquals("unexpected # sent bytes", 0, out.getBytesSent());
        pr=new PingRequest((byte)4);
        out.send(pr);
        start=System.currentTimeMillis();        
        pr=(PingRequest)in.receive();
        elapsed=System.currentTimeMillis()-start;
        assertEquals("unexpected number of sent messages", 1, out.getNumMessagesSent());
        assertEquals("bytes sent differs from total length",
					 out.getBytesSent(), pr.getTotalLength());
        assertLessThan("Unreasonably long send time", 500, elapsed);
        assertEquals("hopped something other than 0", 0, pr.getHops());
        assertEquals("unexpected ttl", 4, pr.getTTL());
    }

    public void testReorderBuffer() 
		throws IOException, BadPacketException {
        //This is the most important test in our suite. TODO: simplify this by
        //breaking it into subparts, e.g., to test that twice as many replies as
        //queries are sent, that replies are ordered by GUID volume, that
        //queries with the same hops are LIFO, etc.

        //1. Buffer tons of messages.  By killing the old thread and restarting
        //later, we simulate a stall in the network.
        out.stopOutputRunner();
        Message m=null;
        //out.send(new QueryRequest((byte)5, 0, "test", false));
		
		out.send(QueryRequest.createQuery("test", (byte)5));
        m=new PingRequest((byte)5);
        m.hop();
        out.send(m);
        m=new QueryReply(new byte[16], (byte)5, 6340, new byte[4], 0, 
                         new Response[0], new byte[16], false);
        m.setPriority(30000);
        out.send(m);
        out.send(new PushRequest(new byte[16], (byte)5, new byte[16],
                                 0, new byte[4], 6340));
        out.send(new PushRequest(new byte[16], (byte)5, new byte[16],
                                 0, new byte[4], 6341));
        out.send(new PushRequest(new byte[16], (byte)5, new byte[16],
                                 0, new byte[4], 6342));
        m=new QueryReply(new byte[16], (byte)5, 6341, new byte[4], 0, 
                         new Response[0], new byte[16], false);
        out.send(m);
        m=PingReply.create(new byte[16], (byte)5, 6343, new byte[4]);
        m.hop();  m.hop();  m.hop();
        out.send(m);
        out.send(new ResetTableMessage(1024, (byte)2));
        out.send(PingReply.create(new byte[16], (byte)1, 6342, new byte[4]));
        m = PingReply.create(new byte[16], (byte)3, 6340, new byte[4]);
        m.hop();
        m.hop();
        out.send(m);
        m=new QueryReply(new byte[16], (byte)5, 6342, new byte[4], 0, 
                         new Response[0], new byte[16], false);
        m.setPriority(1000);
        out.send(m);
        out.send(new PatchTableMessage((short)1, (short)2, 
                                       PatchTableMessage.COMPRESSOR_NONE,
                                       (byte)8, new byte[10], 0, 5));
        out.send(new PingRequest((byte)2));
        out.send(new PatchTableMessage((short)2, (short)2, 
                                       PatchTableMessage.COMPRESSOR_NONE,
                                       (byte)8, new byte[10], 5, 9));

		out.send(QueryRequest.createQuery("test2", (byte)5));
				 
		m = QueryRequest.createQuery("test far", (byte)5);
        m.hop();
        out.send(m);
               
        //2. Now we let the messages pass through, as if the receiver's window
        //became non-zero.  Buffers look this before emptying:
        //  WATCHDOG: pong/6342 ping
        //  PUSH: x/6340 x/6341 x/6342
        //  QUERY_REPLY: 6340/3 6342/1000 6341/0 (highest priority)
        //  QUERY: "test far"/1, "test"/0, "test2"/0
        //  PING_REPLY: x/6340 x/6343
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
        assertEquals("unexpected push request port", 6342,
            ((PushRequest)m).getPort());

        m=in.receive(); //push
        assertInstanceof("Unexpected message: "+m, PushRequest.class, m);
        assertEquals("unexpected push request port", 6341,
            ((PushRequest)m).getPort());

        m=in.receive(); //push
        assertInstanceof("Unexpected message: "+m, PushRequest.class, m);
        assertEquals("unexpected push request port", 6340,
            ((PushRequest)m).getPort());

        m=in.receive(); //reply/6341 (high priority)
        assertInstanceof("m not a queryreply", QueryReply.class, m);
        assertEquals("unexpected queryreply port.  priority: "
             + m.getPriority(), ((QueryReply)m).getPort(), 6341);

        m=in.receive(); //reply/6342 (medium priority)
        assertInstanceof("m not a queryreply", QueryReply.class, m);
        assertEquals("unexpected query reply port", 6342,
            ((QueryReply)m).getPort());

        
        m=in.receive(); //query "test2"/0
        assertInstanceof("m not a queryrequest", QueryRequest.class, m);
        assertEquals("unexpected query", "test2",
            ((QueryRequest)m).getQuery());

        m=in.receive(); //reply 6343
        assertInstanceof("m not a pingreply", PingReply.class, m);
        assertEquals("unexpected pingreply port",
            6343, ((PingReply)m).getPort());

        m=in.receive(); //ping
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

        m=in.receive(); //reply/6340
        assertInstanceof("m not a queryreply", QueryReply.class, m);
        assertEquals("unexpected queryreply port",
            6340, ((QueryReply)m).getPort());

        m=in.receive(); //query "test"/0
        assertInstanceof("m not a queryrequest", QueryRequest.class, m);
        assertEquals("unexpected query", "test",
            ((QueryRequest)m).getQuery());

        m=in.receive(); //reply 6340
        assertInstanceof("m not a pingreply", PingReply.class, m);
        assertEquals("unexpected pingreply port",
            6340, ((PingReply)m).getPort());

        m=in.receive(); //QRP patch1
        assertInstanceof("m not a patchtablemessage",
            PatchTableMessage.class, m);
        assertEquals("unexpected patchtablemessage sequencenumber",
            1, ((PatchTableMessage)m).getSequenceNumber());

        m=in.receive(); //query "test"/0
        assertInstanceof("m not a queryrequest", QueryRequest.class, m);
        assertEquals("unexpected query",
            "test far", ((QueryRequest)m).getQuery());

        m=in.receive(); //QRP patch2
        assertInstanceof("m not a patchtable message",
            PatchTableMessage.class, m);
        assertEquals("unexpected patchtablemessage sequencenumber",
            2, ((PatchTableMessage)m).getSequenceNumber());
    }

    /**
     * Test to make sure that messages properly timeout in the message
     * queues and are dropped.
     */
    public void testBufferTimeout() 
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


    public void testPriorityHint() 
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
        out.send(new QueryReply(new byte[16], (byte)5, 6341, new byte[4], 0, 
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

    public void testDropBuffer() 
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

	private static class DummyResponder
	  extends AuthenticationHandshakeResponder {
		DummyResponder(String host) {
			super(null, host);
		}

		protected HandshakeResponse 
			respondUnauthenticated(HandshakeResponse response,
			                       boolean outgoing) 
			throws IOException {
			return new HandshakeResponse(new Properties());
		}
	}

}
