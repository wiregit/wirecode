package com.limegroup.gnutella;


import junit.framework.*;
import java.io.*;
import java.net.*;
import java.util.Properties;
import com.limegroup.gnutella.handshaking.*;
import com.limegroup.gnutella.routing.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.stubs.*;
import com.sun.java.util.collections.*;

public class ManagedConnectionTest extends TestCase {  
    public static final int PORT=6666;

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
		SettingsManager.instance().setLocalIsPrivate(false);
    }
 
    private static void sleep(long msecs) {
        try { Thread.sleep(msecs); } catch (InterruptedException ignored) { }
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
    
	/*
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
	*/

    public void testForwardsGGEP() {
        int TIMEOUT=1000;
        MiniAcceptor acceptor=new MiniAcceptor(new GGEPResponder(), PORT);
        ManagedConnection out=new ManagedConnection("localhost", PORT,
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
                assertEquals("incorrect daily uptime!",reply.getDailyUptime(), 4321);
            } catch (BadPacketException e) {
                fail("Couldn't extract uptime: "+e);
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
        MiniAcceptor acceptor=new MiniAcceptor(new EmptyResponder(), PORT);
        ManagedConnection out = 
			new ManagedConnection("localhost", PORT,
								  new MessageRouterStub(),
								  new ConnectionManagerStub());
        try {
            out.initialize();
            Connection in = acceptor.accept();
            assertTrue("Connection should not support GGEP", !out.supportsGGEP());

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
			e.printStackTrace();
            fail("Mysterious IO problem: "+e);
        } catch (BadPacketException e) {
			e.printStackTrace();
            fail("Bad packet: "+e);
        }
    }
    

    public void testClose() {
        try {
            ManagedConnection out=null;
            Connection in=null;
            com.limegroup.gnutella.MiniAcceptor acceptor=null;                
            //When receive() or sendQueued() gets IOException, it calls
            //ConnectionManager.remove().  This in turn calls
            //ManagedConnection.close().  Our stub does this.
            ConnectionManager manager=new ConnectionManagerStub(true);

            //1. Locally closed
            acceptor=new com.limegroup.gnutella.MiniAcceptor(null, PORT);
            out=newConnection("localhost", PORT, manager);
            out.initialize();            
            in=acceptor.accept(); 
			assertTrue("connection should be open", out.isOpen());
			assertTrue("runner should not be dead", !out.runnerDied());
            out.close();
            sleep(100);
            assertTrue("connection should not be open", !out.isOpen());
            assertTrue("", out.runnerDied());
            try { Thread.sleep(1000); } catch (InterruptedException e) { }
            in.close(); //needed to ensure connect below works

            //2. Remote close: discovered on read
            acceptor = new com.limegroup.gnutella.MiniAcceptor(null, PORT);
            out = newConnection("localhost", PORT, manager);
            out.initialize();            
            in=acceptor.accept(); 
            assertTrue("connection should be open", out.isOpen());
            assertTrue("runner should not be dead", !out.runnerDied());
            in.close();
            try {
                out.receive();
                Assert.that(false);
            } catch (BadPacketException e) {
                Assert.that(false);
            } catch (IOException e) { }            
            sleep(100);
            assertTrue("connection should not be open", !out.isOpen());
            assertTrue("runner should not be dead", out.runnerDied());

            //3. Remote close: discovered on write.  Because of TCP's half-close
            //semantics, we need TWO writes to discover this.  (See unit tests
            //for Connection.)
            acceptor=new com.limegroup.gnutella.MiniAcceptor(null, PORT);
            out=newConnection("localhost", PORT, manager);
            out.initialize();            
            in=acceptor.accept(); 
            assertTrue("connection should be open", out.isOpen());
            assertTrue("runner should not be dead", !out.runnerDied());
            in.close();
            out.send(new PingRequest((byte)3));
            out.send(new PingRequest((byte)3));
            sleep(100);
            assertTrue(! out.isOpen());
            assertTrue(out.runnerDied());
			sleep(2000);

        } catch (IOException e) {
            fail("Unexpected IO problem");
            e.printStackTrace();
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
            Properties props = new Properties();
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
