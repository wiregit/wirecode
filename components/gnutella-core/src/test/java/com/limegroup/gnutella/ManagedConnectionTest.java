package com.limegroup.gnutella;

import junit.framework.*;
import java.io.*;
import java.util.Properties;
import com.limegroup.gnutella.handshaking.*;
import com.limegroup.gnutella.routing.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.tests.*;
import com.limegroup.gnutella.tests.stubs.*;
import com.sun.java.util.collections.*;

public class ManagedConnectionTest extends TestCase {  
    public ManagedConnectionTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return new TestSuite(ManagedConnectionTest.class);
    }    
   
    /*
    public static Test suite() {
        //return new TestSuite(ManagedConnectionTest.class);
        TestSuite ret=new TestSuite("Simplified ManagedConnection tests");
        ret.addTest(new ManagedConnectionTest("testBuffering"));
        ret.addTest(new ManagedConnectionTest("testClose"));
        ret.addTest(new ManagedConnectionTest("testHorizonStatistics"));
        ret.addTest(new ManagedConnectionTest("testIsRouter"));
        ret.addTest(new ManagedConnectionTest("testForwardsGGEP"));
        ret.addTest(new ManagedConnectionTest("testStripsGGEP"));
        ret.addTest(new ManagedConnectionTest("testForwardsGroupPing"));
        return ret;
    }
    */

    public void setUp() {
        //Restore all the defaults.  Apparently testForwardsGGEP fails if this
        //is in ultrapeer mode and the KEEP_ALIVE is 1.  It seems that WE (the
        //client) send a "503 Service Unavailable" at line 77 of
        //SupernodeHandshakeResponder.
        SettingsManager.instance().loadDefaults();
        SettingsManager.instance().setQuickConnectHosts(new String[0]);
    }

    /** 
     * Tests buffering, dropping, and reordering of messages.  This is a
     * colossal test, delegating to lots of static helper methods.  At some
     * point, those static methods should be made standard JUnit testX methods.
     */
    public void testBuffering() {
        try {
            //Create loopback connection.  Uncomment the MiniAcceptor class in
            //Connection to get this to work.
            com.limegroup.gnutella.tests.MiniAcceptor acceptor=
                new com.limegroup.gnutella.tests.MiniAcceptor(null);
            ManagedConnection.QUEUE_TIME=1000;
            ManagedConnection out=newConnection("localhost", 6346);
            out.initialize();
            Connection in=acceptor.accept();      
            testSendFlush(out, in);
            testReorderBuffer(out, in);
            testBufferTimeout(out, in);
            testDropBuffer(out, in);
            testPriorityHint(out, in);
            in.close();
            out.close();

        } catch (IOException e) {
            e.printStackTrace();
            Assert.that(false, "Unexpected IO problem");
        } catch (BadPacketException e) {
            e.printStackTrace();
            Assert.that(false, "Unexpected bad packet");
        }
    }

    private static void testSendFlush(ManagedConnection out, Connection in) 
            throws IOException, BadPacketException {
        PingRequest pr=null;
        long start=0;
        long elapsed=0;

        Assert.that(out.getNumMessagesSent()==0); 
        Assert.that(out.getBytesSent()==0);
        pr=new PingRequest((byte)4);
        out.send(pr);
        start=System.currentTimeMillis();        
        pr=(PingRequest)in.receive();
        elapsed=System.currentTimeMillis()-start;
        Assert.that(out.getNumMessagesSent()==1);
        Assert.that(out.getBytesSent()==pr.getTotalLength());
        Assert.that(elapsed<300, "Unreasonably long send time: "+elapsed);
        Assert.that(pr.getHops()==0);
        Assert.that(pr.getTTL()==4);
    }

    private static void testReorderBuffer(ManagedConnection out, Connection in) 
            throws IOException, BadPacketException {
        //This is the most important test in our suite. TODO: simplify this by
        //breaking it into subparts, e.g., to test that twice as many replies as
        //queries are sent, that replies are ordered by GUID volume, that
        //queries with the same hops are LIFO, etc.

        //1. Buffer tons of messages.  By killing the old thread and restarting
        //later, we simulate a stall in the network.
        out.stopOutputRunner();
        Message m=null;
        out.send(new QueryRequest((byte)5, 0, "test"));
        m=new PingRequest((byte)5);
        m.hop();
        out.send(m);
        m=new QueryReply(new byte[16], (byte)5, 6340, new byte[4], 0, 
                         new Response[0], new byte[16]);
        m.setPriority(30000);
        out.send(m);
        out.send(new PushRequest(new byte[16], (byte)5, new byte[16],
                                 0, new byte[4], 6340));
        out.send(new PushRequest(new byte[16], (byte)5, new byte[16],
                                 0, new byte[4], 6341));
        out.send(new PushRequest(new byte[16], (byte)5, new byte[16],
                                 0, new byte[4], 6342));
        m=new QueryReply(new byte[16], (byte)5, 6341, new byte[4], 0, 
                         new Response[0], new byte[16]);
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
                         new Response[0], new byte[16]);
        m.setPriority(1000);
        out.send(m);
        out.send(new PatchTableMessage((short)1, (short)2, 
                                       PatchTableMessage.COMPRESSOR_NONE,
                                       (byte)8, new byte[10], 0, 5));
        out.send(new PingRequest((byte)2));
        out.send(new PatchTableMessage((short)2, (short)2, 
                                       PatchTableMessage.COMPRESSOR_NONE,
                                       (byte)8, new byte[10], 5, 9));
        out.send(new QueryRequest((byte)5, 0, "test2"));
        m=new QueryRequest((byte)5, 0, "test far");
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
        Assert.that(m instanceof PingRequest, "Unexpected message: "+m);
        Assert.that(m.getHops()==0, "Unexpected message: "+m);  

        m=in.receive(); //push        
        Assert.that(m instanceof PushRequest, "Unexpected message: "+m);
        Assert.that(((PushRequest)m).getPort()==6342);

        m=in.receive(); //push
        Assert.that(m instanceof PushRequest, "Unexpected message: "+m);
        Assert.that(((PushRequest)m).getPort()==6341);

        m=in.receive(); //push
        Assert.that(m instanceof PushRequest, "Unexpected message: "+m);
        Assert.that(((PushRequest)m).getPort()==6340);

        m=in.receive(); //reply/6341 (high priority)
        Assert.that(m instanceof QueryReply);
        Assert.that(((QueryReply)m).getPort()==6341, 
                    ((QueryReply)m).getPort()+" "+m.getPriority());

        m=in.receive(); //reply/6342 (medium priority)
        Assert.that(m instanceof QueryReply);
        Assert.that(((QueryReply)m).getPort()==6342);

        
        m=in.receive(); //query "test2"/0
        Assert.that(m instanceof QueryRequest);
        Assert.that(((QueryRequest)m).getQuery().equals("test2"), "Got: "+m);

        m=in.receive(); //reply 6343
        Assert.that(m instanceof PingReply);
        Assert.that(((PingReply)m).getPort()==6343);

        m=in.receive(); //ping
        Assert.that(m instanceof PingRequest);
        Assert.that(m.getHops()>0);

        m=in.receive(); //QRP reset
        Assert.that(m instanceof ResetTableMessage);

        

        m=in.receive(); //watchdog pong/6342
        Assert.that(m instanceof PingReply);
        Assert.that(((PingReply)m).getPort()==6342);
        Assert.that(m.getHops()==0);  //watchdog response pong

        m=in.receive(); //reply/6340
        Assert.that(m instanceof QueryReply);
        Assert.that(((QueryReply)m).getPort()==6340);

        m=in.receive(); //query "test"/0
        Assert.that(m instanceof QueryRequest);
        Assert.that(((QueryRequest)m).getQuery().equals("test"));

        m=in.receive(); //reply 6340
        Assert.that(m instanceof PingReply);
        Assert.that(((PingReply)m).getPort()==6340, m.toString());

        m=in.receive(); //QRP patch1
        Assert.that(m instanceof PatchTableMessage);
        Assert.that(((PatchTableMessage)m).getSequenceNumber()==1);


        m=in.receive(); //query "test"/0
        Assert.that(m instanceof QueryRequest);
        Assert.that(((QueryRequest)m).getQuery().equals("test far"));

        m=in.receive(); //QRP patch2
        Assert.that(m instanceof PatchTableMessage);
        Assert.that(((PatchTableMessage)m).getSequenceNumber()==2);
    }

    private static void testBufferTimeout(ManagedConnection out, Connection in) 
            throws IOException, BadPacketException {
        Assert.that(ManagedConnection.QUEUE_TIME==1000);
        
        //Drop one message
        out.stopOutputRunner();        
        out.send(new QueryRequest((byte)3, 0, "0"));   
        sleep(1200);
        out.send(new QueryRequest((byte)3, 0, "1200"));        
        out.startOutputRunner();
        Message m=(QueryRequest)in.receive(500);
        Assert.that(m instanceof QueryRequest);
        Assert.that(((QueryRequest)m).getQuery().equals("1200"));
        try {
            m=in.receive(200);
            Assert.that(false, m.toString());
        } catch (InterruptedIOException e) {
        }
        Assert.that(out.getNumSentMessagesDropped()==1);

        //Drop many messages
        out.stopOutputRunner();        
        out.send(new QueryRequest((byte)3, 0, "0"));   
        sleep(300);
        out.send(new QueryRequest((byte)3, 0, "300"));        
        sleep(300);
        out.send(new QueryRequest((byte)3, 0, "600"));        
        sleep(500);
        out.send(new QueryRequest((byte)3, 0, "1100"));
        sleep(900);
        out.send(new QueryRequest((byte)3, 0, "2000"));
        out.startOutputRunner();
        m=in.receive(500);
        Assert.that(m instanceof QueryRequest);
        Assert.that(((QueryRequest)m).getQuery().equals("2000"));
        m=in.receive(500);
        Assert.that(m instanceof QueryRequest);
        Assert.that(((QueryRequest)m).getQuery().equals("1100"));
        try {
            m=in.receive(200);
            Assert.that(false, m.toString());
        } catch (InterruptedIOException e) {
        }
        Assert.that(out.getNumSentMessagesDropped()==(1+3));
    }


    private static void testPriorityHint(ManagedConnection out, Connection in) 
            throws IOException, BadPacketException {
        //Tests wrap-around loop of sendQueuedMessages
        Message m=null;

        // head...tail
        out.stopOutputRunner(); 
        out.send(hopped(new PingRequest((byte)4)));
        out.send(new QueryRequest((byte)3, 0, "a"));
        out.startOutputRunner();
        Assert.that(in.receive() instanceof QueryRequest);
        Assert.that(in.receive() instanceof PingRequest);

        //tail...<wrap>...head
        out.stopOutputRunner(); 
        out.send(new QueryRequest((byte)3, 0, "a"));
        out.send(hopped(new PingRequest((byte)5)));
        out.startOutputRunner();
        Assert.that(in.receive() instanceof PingRequest);
        Assert.that(in.receive() instanceof QueryRequest);

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
                                new Response[0], new byte[16]));
        out.send(new ResetTableMessage(1024, (byte)2));
        out.send(new QueryRequest((byte)3, 0, "a"));
        out.startOutputRunner();
        m=in.receive();
        Assert.that(m instanceof QueryRequest, "Got: "+m);
        m=in.receive();
        Assert.that(m instanceof ResetTableMessage, "Got: "+m);
        m=in.receive();
        Assert.that(m instanceof PingRequest, "Got: "+m);
        m=in.receive();
        Assert.that(m instanceof QueryReply, "Got: "+m);
    }

    private static Message hopped(Message m) {
        m.hop();
        return m;
    }

    private static void sleep(long msecs) {
        try { Thread.sleep(msecs); } catch (InterruptedException ignored) { }
    }

    private static void testDropBuffer(ManagedConnection out, Connection in) 
            throws IOException, BadPacketException {
        //Send tons of messages...but don't read them
        int total=20000;

        int initialDropped=out.getNumSentMessagesDropped();
        for (int i=0; i<total; i++) {
            out.send(new QueryRequest((byte)4, i, 
                                      "Some reaaaaaalllllly big query"));
        }
        int dropped=out.getNumSentMessagesDropped()-initialDropped;
        //System.out.println("Dropped messages: "+dropped);
        Assert.that(dropped>0);
        Assert.that(out.getPercentSentDropped()>0);

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
        //System.out.println("Read messages/bytes: "+read+"/"+bytesRead);
        Assert.that(read<total);
        Assert.that(dropped+read==total);
    }

    ///////////////////////// End testBuffer ////////////////////////

    public static void testClose() {
        try {
            ManagedConnection out=null;
            Connection in=null;
            com.limegroup.gnutella.tests.MiniAcceptor acceptor=null;                
            //When receive() or sendQueued() gets IOException, it calls
            //ConnectionManager.remove().  This in turn calls
            //ManagedConnection.close().  Our stub does this.
            ConnectionManager manager=new ConnectionManagerStub(true);

            //1. Locally closed
            acceptor=new com.limegroup.gnutella.tests.MiniAcceptor(null);
            out=newConnection("localhost", 6346, manager);
            out.initialize();            
            in=acceptor.accept(); 
            Assert.that(out.isOpen());
            Assert.that(! out.runnerDied());
            out.close();
            sleep(100);
            Assert.that(! out.isOpen());
            Assert.that(out.runnerDied());
            try { Thread.sleep(1000); } catch (InterruptedException e) { }
            in.close(); //needed to ensure connect below works

            //2. Remote close: discovered on read
            acceptor=new com.limegroup.gnutella.tests.MiniAcceptor(null);
            out=newConnection("localhost", 6346, manager);
            out.initialize();            
            in=acceptor.accept(); 
            Assert.that(out.isOpen());
            Assert.that(! out.runnerDied());
            in.close();
            try {
                out.receive();
                Assert.that(false);
            } catch (BadPacketException e) {
                Assert.that(false);
            } catch (IOException e) { }            
            sleep(100);
            Assert.that(! out.isOpen());
            Assert.that(out.runnerDied());

            //3. Remote close: discovered on write.  Because of TCP's half-close
            //semantics, we need TWO writes to discover this.  (See unit tests
            //for Connection.)
            acceptor=new com.limegroup.gnutella.tests.MiniAcceptor(null);
            out=newConnection("localhost", 6346, manager);
            out.initialize();            
            in=acceptor.accept(); 
            Assert.that(out.isOpen());
            Assert.that(! out.runnerDied());
            in.close();
            out.send(new PingRequest((byte)3));
            out.send(new PingRequest((byte)3));
            sleep(100);
            Assert.that(! out.isOpen());
            Assert.that(out.runnerDied());

        } catch (IOException e) {
            fail("Unexpected IO problem");
            e.printStackTrace();
        }
    }

 

    public void testHorizonStatistics() {
        ManagedConnection mc=newConnection();
        //For testing.  You may need to ensure that HORIZON_UPDATE_TIME is
        //non-final to compile.
        mc.HORIZON_UPDATE_TIME=1*200;   
        PingReply pr1=new PingReply(
            GUID.makeGuid(), (byte)3, 6346,
            new byte[] {(byte)127, (byte)0, (byte)0, (byte)1},
            1, 10);
        PingReply pr2=new PingReply(
            GUID.makeGuid(), (byte)3, 6347,
            new byte[] {(byte)127, (byte)0, (byte)0, (byte)1},
            2, 20);
        PingReply pr3=new PingReply(
            GUID.makeGuid(), (byte)3, 6346,
            new byte[] {(byte)127, (byte)0, (byte)0, (byte)2},
            3, 30);

        Assert.that(mc.getNumFiles()==0);
        Assert.that(mc.getNumHosts()==0);
        Assert.that(mc.getTotalFileSize()==0);

        mc.updateHorizonStats(pr1);
        mc.updateHorizonStats(pr1);  //check duplicates
        Assert.that(mc.getNumFiles()==1);
        Assert.that(mc.getNumHosts()==1);
        Assert.that(mc.getTotalFileSize()==10);

        try { Thread.sleep(ManagedConnection.HORIZON_UPDATE_TIME*2); } 
        catch (InterruptedException e) { }
            
        mc.refreshHorizonStats();    
        mc.updateHorizonStats(pr1);  //should be ignored for now
        mc.updateHorizonStats(pr2);
        mc.updateHorizonStats(pr3);
        Assert.that(mc.getNumFiles()==1);
        Assert.that(mc.getNumHosts()==1);
        Assert.that(mc.getTotalFileSize()==10);
        mc.refreshHorizonStats();    //should be ignored
        Assert.that(mc.getNumFiles()==1);
        Assert.that(mc.getNumHosts()==1);
        Assert.that(mc.getTotalFileSize()==10);

        try { Thread.sleep(ManagedConnection.HORIZON_UPDATE_TIME*2); } 
        catch (InterruptedException e) { }            

        mc.refreshHorizonStats();    //update stats
        Assert.that(mc.getNumFiles()==(1+2+3));
        Assert.that(mc.getNumHosts()==3);
        Assert.that(mc.getTotalFileSize()==(10+20+30));

        try { Thread.sleep(ManagedConnection.HORIZON_UPDATE_TIME*2); } 
        catch (InterruptedException e) { }       

        mc.refreshHorizonStats();
        Assert.that(mc.getNumFiles()==0);
        Assert.that(mc.getNumHosts()==0);
        Assert.that(mc.getTotalFileSize()==0);                
    }
    
    public void testIsRouter() {
        Assert.that(! ManagedConnection.isRouter("127.0.0.1"));
        Assert.that(! ManagedConnection.isRouter("18.239.0.1"));
        Assert.that(ManagedConnection.isRouter("64.61.25.171"));
        Assert.that(ManagedConnection.isRouter("64.61.25.139"));
        Assert.that(ManagedConnection.isRouter("64.61.25.143"));
        Assert.that(! ManagedConnection.isRouter("64.61.25.138"));
        Assert.that(! ManagedConnection.isRouter("64.61.25.170"));
        Assert.that(! ManagedConnection.isRouter("www.limewire.com"));
        Assert.that(! ManagedConnection.isRouter("public.bearshare.net"));
        Assert.that(ManagedConnection.isRouter("router.limewire.com"));
        Assert.that(ManagedConnection.isRouter("router4.limewire.com"));
        Assert.that(ManagedConnection.isRouter("router2.limewire.com"));
        Assert.that(ManagedConnection.translateHost("router.limewire.com").
            equals("router4.limewire.com"));
        Assert.that(ManagedConnection.translateHost("router4.limewire.com").
            equals("router4.limewire.com"));
     }

    public void testForwardsGGEP() {
        int TIMEOUT=1000;
        MiniAcceptor acceptor=new MiniAcceptor(new GGEPResponder(), 6346);
        ManagedConnection out=new ManagedConnection("localhost", 6346,
                                                   new MessageRouterStub(),
                                                   new ConnectionManagerStub());
        try {
            out.initialize();
            Connection in=acceptor.accept();
            assertTrue(out.supportsGGEP());

            out.send(new PingReply(new byte[16], (byte)3, 6349, new byte[4],
                                   13l, 14l, false, 4321));
            PingReply reply=(PingReply)in.receive(TIMEOUT);
            assertEquals(reply.getPort(), 6349);
            try {
                assertEquals(reply.getDailyUptime(), 4321);
            } catch (BadPacketException e) {
                fail("Couldn't extract uptime");
            }
            in.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
            fail("Mysterious IO problem: "+e);
        } catch (BadPacketException e) {
            fail("Bad packet: "+e);
        }
    }

   public void testStripsGGEP() {
        int TIMEOUT=1000;
        MiniAcceptor acceptor=new MiniAcceptor(new EmptyResponder(), 6346);
        ManagedConnection out=new ManagedConnection("localhost", 6346,
                                                   new MessageRouterStub(),
                                                   new ConnectionManagerStub());
        try {
            out.initialize();
            Connection in=acceptor.accept();
            assertTrue(! out.supportsGGEP());

            out.send(new PingReply(new byte[16], (byte)3, 6349, new byte[4],
                                   13l, 14l, false, 4321));
            PingReply reply=(PingReply)in.receive(TIMEOUT);
            assertEquals(14, reply.getLength());
            assertEquals(reply.getPort(), 6349);
            try {
                reply.getDailyUptime();
                fail("Payload wasn't stripped");
            } catch (BadPacketException e) { }
            in.close();
            out.close();
        } catch (IOException e) {
            fail("Mysterious IO problem: "+e);
        } catch (BadPacketException e) {
            fail("Bad packet: "+e);
        }
    }

    public void testForwardsGroupPing() {
        int TIMEOUT=1000;
        MiniAcceptor acceptor=new MiniAcceptor(new EmptyResponder(), 6346);
        //Router connection
        ManagedConnection out=new ManagedConnection("localhost", 6346,
                                                   new MessageRouterStub(),
                                                   new ConnectionManagerStub());
        try {
            out.initialize();
            Connection in=acceptor.accept();
            assertTrue(! out.supportsGGEP());

            PingRequest ping1=new GroupPingRequest((byte)3, 6349, new byte[4],
                                                   0l, 0l, "test");
            assertEquals(14+4+1, ping1.getLength());
            out.send(ping1);            
            
            //Note that Message won't create a GroupPingRequest unless
            //PARSE_GROUP_PINGS is true.
            PingRequest ping2=(PingRequest)in.receive(TIMEOUT);
            assertEquals(14+4+1, ping2.getLength());
            assertTrue(Arrays.equals(ping1.getGUID(), ping2.getGUID()));
            in.close();
            out.close();
        } catch (IOException e) {
            fail("Mysterious IO problem: "+e);
        } catch (BadPacketException e) {
            fail("Bad packet: "+e);
        }
    }
    

    class GGEPResponder implements HandshakeResponder {
        public HandshakeResponse respond(HandshakeResponse response,
                                         boolean outgoing) throws IOException {
            Properties props=new Properties();
            props.put("GGEP", "0.6"); 
            return new HandshakeResponse(props);
        }
    }

    class EmptyResponder implements HandshakeResponder {
        public HandshakeResponse respond(HandshakeResponse response,
                                         boolean outgoing) throws IOException {
            Properties props=new Properties();
            return new HandshakeResponse(props);
        }
    }

    private static ManagedConnection newConnection() {
        return newConnection("", 0);
    }

    private static ManagedConnection newConnection(String host, int port) {
        return new ManagedConnection(host, port, new MessageRouterStub(),
                                     new ConnectionManagerStub());
    }

    private static ManagedConnection newConnection(String host, int port,
                                                   ConnectionManager cm) {
        return new ManagedConnection(host, port, new MessageRouterStub(), cm);
    }

    public static void main(String argv[]) {
        junit.textui.TestRunner.run(suite());
    }
}
