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
		//setStandardSettings();
		
		//ConnectionSettings.KEEP_ALIVE.setValue(2);
		//ConnectionSettings.WATCHDOG_ACTIVE.setValue(true);
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
		ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);

        // required because ManagedConnection delegates
        // to RouterService to get the MessageRouter
        RouterService rs = 
			new RouterService(new ActivityCallbackStub(), 
							  new MessageRouterStub());

		//rs.start();
		//rs.connect();
        PrivilegedAccessor.setValue(RouterService.class, "manager", new ConnectionManagerStub());



		MiniAcceptor acceptor = 
			new MiniAcceptor(new DummyResponder("localhost"), PORT);

		//MiniAcceptor acceptor = 
		//new MiniAcceptor(new HandshakeResponse(new Properties()), PORT);


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

        assertTrue(out.getNumMessagesSent()==0); 
        assertTrue(out.getBytesSent()==0);
        pr=new PingRequest((byte)4);
        out.send(pr);
        start=System.currentTimeMillis();        
        pr=(PingRequest)in.receive();
        elapsed=System.currentTimeMillis()-start;
        assertEquals("unexpected number of sent messages", out.getNumMessagesSent(), 1);
        assertEquals("bytes sent differs from total length", out.getBytesSent(), pr.getTotalLength());
        assertTrue("Unreasonably long send time: "+elapsed, elapsed<500);
        assertEquals("hopped something other than 0", pr.getHops(), 0);
        assertEquals("unexpected ttl", pr.getTTL(), 4);
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
        m=new PingReply(new byte[16], (byte)5, 6343, new byte[4], 0, 0);
        m.hop();  m.hop();  m.hop();
        out.send(m);
        out.send(new ResetTableMessage(1024, (byte)2));
        out.send(new PingReply(new byte[16], (byte)1, 6342, new byte[4], 0, 0));
        m=new PingReply(new byte[16], (byte)3, 6340, new byte[4], 0, 0);
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
        assertTrue("Unexpected message: "+m, m instanceof PingRequest);
        assertTrue("Unexpected message: "+m, m.getHops()==0);  

        m=in.receive(); //push        
        assertTrue("Unexpected message: "+m, m instanceof PushRequest);
        assertEquals("unexpected push request port", ((PushRequest)m).getPort(), 6342);

        m=in.receive(); //push
        assertTrue("Unexpected message: "+m, m instanceof PushRequest);
        assertEquals("unexpected push request port", ((PushRequest)m).getPort(), 6341);

        m=in.receive(); //push
        assertTrue("Unexpected message: "+m, m instanceof PushRequest);
        assertEquals("unexpected push request port", ((PushRequest)m).getPort(), 6340);

        m=in.receive(); //reply/6341 (high priority)
        assertTrue("m not a queryreply", m instanceof QueryReply);
        assertEquals("unexpected queryreply port.  priority: " + m.getPriority(), 
                ((QueryReply)m).getPort(), 6341);

        m=in.receive(); //reply/6342 (medium priority)
        assertTrue("m not a queryreply", m instanceof QueryReply);
        assertEquals("unexpected query reply port", ((QueryReply)m).getPort(), 6342);

        
        m=in.receive(); //query "test2"/0
        assertTrue("m not a queryrequest", m instanceof QueryRequest);
        assertEquals("unexpected query", ((QueryRequest)m).getQuery(), "test2");

        m=in.receive(); //reply 6343
        assertTrue("m not a pingreply", m instanceof PingReply);
        assertEquals("unexpected pingreply port", ((PingReply)m).getPort(), 6343);

        m=in.receive(); //ping
        assertTrue("m not a pingrequest", m instanceof PingRequest);
        assertTrue("unexpected number of hops (>0)", m.getHops()>0);

        m=in.receive(); //QRP reset
        assertTrue("m not a resettablemessage", m instanceof ResetTableMessage);

        

        m=in.receive(); //watchdog pong/6342
        assertTrue("m not a pingreply", m instanceof PingReply);
        assertEquals("unexpected pingreply port", ((PingReply)m).getPort(), 6342);
        assertEquals("unexpected number of hops", m.getHops(), 0);  //watchdog response pong

        m=in.receive(); //reply/6340
        assertTrue("m not a queryreply", m instanceof QueryReply);
        assertEquals("unexpected queryreply port", ((QueryReply)m).getPort(), 6340);

        m=in.receive(); //query "test"/0
        assertTrue("m not a queryrequest", m instanceof QueryRequest);
        assertEquals("unexpected query", ((QueryRequest)m).getQuery(), "test");

        m=in.receive(); //reply 6340
        assertTrue("m not a pingreply", m instanceof PingReply);
        assertEquals("unexpected pingreply port", ((PingReply)m).getPort(), 6340);

        m=in.receive(); //QRP patch1
        assertTrue("m not a patchtablemessage", m instanceof PatchTableMessage);
        assertEquals("unexpedted patchtablemessage sequencenumber", ((PatchTableMessage)m).getSequenceNumber(), 1);


        m=in.receive(); //query "test"/0
        assertTrue("m not a queryrequest", m instanceof QueryRequest);
        assertEquals("unexpected query", ((QueryRequest)m).getQuery(), "test far");

        m=in.receive(); //QRP patch2
        assertTrue("m not a patchtable message", m instanceof PatchTableMessage);
        assertEquals("unexpected patchtablemessage sequencenumber", ((PatchTableMessage)m).getSequenceNumber(), 2);
    }

    public void testBufferTimeout() 
            throws IOException, BadPacketException {
        assertTrue(ManagedConnection.QUEUE_TIME==1000);
        
        //Drop one message
        out.stopOutputRunner();        
        //out.send(new QueryRequest((byte)3, 0, "0", false));   
        out.send(QueryRequest.createQuery("0", (byte)3));   
        sleep(1200);
        //out.send(new QueryRequest((byte)3, 0, "1200", false));        
        out.send(QueryRequest.createQuery("1200", (byte)3)); 
        out.startOutputRunner();
        Message m=(QueryRequest)in.receive(500);
        assertTrue("m not a queryrequest", m instanceof QueryRequest);
        assertEquals("unexpected query", ((QueryRequest)m).getQuery(), "1200");
        try {
            m=in.receive(200);
            fail("buffer didn't timeout in time.  message: " + m.toString());
        } catch (InterruptedIOException e) {
        }
        assertTrue(out.getNumSentMessagesDropped()==1);

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
        assertTrue("m not a queryrequest", m instanceof QueryRequest);
        assertEquals("unexpected query", ((QueryRequest)m).getQuery(), "2000");
        m=in.receive(500);
        assertTrue("m not a queryrequest", m instanceof QueryRequest);
        assertEquals("unexpected query", ((QueryRequest)m).getQuery(), "1100");
        try {
            m=in.receive(200);
            fail("buffer didn't timeout in time.  message: " + m.toString());
        } catch (InterruptedIOException e) {
        }
        assertTrue(out.getNumSentMessagesDropped()==(1+3));
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
        assertTrue("didn't recieve queryrequest", in.receive() instanceof QueryRequest);
        assertTrue("didn't recieve pingrequest", in.receive() instanceof PingRequest);

        //tail...<wrap>...head
        out.stopOutputRunner(); 
        out.send(QueryRequest.createQuery("a", (byte)3));
        out.send(hopped(new PingRequest((byte)5)));
        out.startOutputRunner();
        assertTrue("didn't recieve pingrequest", in.receive() instanceof PingRequest);
        assertTrue("didn't recieve queryrequest", in.receive() instanceof QueryRequest);

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
        assertTrue("Got: "+m, m instanceof QueryRequest);
        m=in.receive();
        assertTrue("GOt: " +m, m instanceof ResetTableMessage);
        m=in.receive();
        assertTrue("Got: " + m, m instanceof PingRequest);
        m=in.receive();
        assertTrue("Got: " + m, m instanceof QueryReply);
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

        int initialDropped=out.getNumSentMessagesDropped();
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
        
        assertTrue("dropped msg cnt > 0", dropped>0);
        assertTrue("drop prct > 0", out.getPercentSentDropped()>0);
        assertTrue("read cnt < total", read<total);
        assertEquals("drop + read == total", total, dropped+read);
    }

	private static class DummyResponder extends AuthenticationHandshakeResponder {
		DummyResponder(String host) {
			super(null, host);
		}

		protected HandshakeResponse 
			respondUnauthenticated(HandshakeResponse response, boolean outgoing) 
			throws IOException {
			return new HandshakeResponse(new Properties());
		}
	}

}
