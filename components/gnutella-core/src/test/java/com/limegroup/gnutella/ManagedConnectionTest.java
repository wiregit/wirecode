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

public class ManagedConnectionTest extends com.limegroup.gnutella.util.BaseTestCase {  
    public static final int PORT=6666;


    private static final RouterService ROUTER_SERVICE =
        new RouterService(new ActivityCallbackStub());

    public ManagedConnectionTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return new TestSuite(ManagedConnectionTest.class);
    }    

    public void setUp() {
        SettingsManager.instance().setPort(6444);
		ConnectionSettings.KEEP_ALIVE.setValue(1);
		ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
		ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);
		ConnectionSettings.USE_GWEBCACHE.setValue(false);
        UltrapeerSettings.FORCE_ULTRAPEER_MODE.setValue(true);
        UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(true);
        
        
        if(ROUTER_SERVICE.isStarted()) return;

        ROUTER_SERVICE.start();
		ROUTER_SERVICE.connect();
        RouterService.clearHostCatcher();
    }

	public void tearDown() {
		
	}

	public void testServerRunning() {
		try {
			ManagedConnection mc = 
				new ManagedConnection("localhost", Backend.DEFAULT_PORT);
			mc.initialize();		
		} catch(IOException e) {
			e.printStackTrace();
			failWithServerMessage();
		}
	}

    private void failWithServerMessage() {
        fail("You must run this test with servers running --\n"+
             "use the test6300 ant target to run LimeWire servers "+
             "on ports 6300 and 6300.\n\n"+
             "Type ant -D\"class=ConnectionManagerTest\" test6300\n\n");        
    }
 
    private static void sleep(long msecs) {
        try { Thread.sleep(msecs); } catch (InterruptedException ignored) { }
    }


	/**
	 * Tests the method for checking whether or not the connection is a high-
	 * degree connection that maintains high numbers of intra-Ultrapeer 
	 * connections.
	 */
	public void testIsHighDegreeConnection() throws IOException {
		ManagedConnection mc = new ManagedConnection("localhost", 6300);
		mc.initialize();
		assertTrue("connection should be high degree", 
				   mc.isHighDegreeConnection());
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
        assertEquals("unexpected number of files", 0,mc.getNumFiles());
        assertEquals("unexpected number of hosts", 0,mc.getNumHosts());
        assertEquals("unexpected total file size", 0,mc.getTotalFileSize());                
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

	/**
	 * Test to make sure that GGEP extensions are correctly returned in pongs.
	 */
    public void testForwardsGGEP() {
        ManagedConnection out = newConnection("localhost", Backend.DEFAULT_PORT);
        try {
            out.initialize();

			// receive initial ping
			out.receive();
			out.receive();
			out.receive();

            assertTrue("connection should support GGEP", out.supportsGGEP());
			out.send(new PingRequest((byte)2));
			
			Message m = out.receive();
			System.out.println(m); 
			assertTrue("should be a pong", m instanceof PingReply);
			PingReply pr = (PingReply)m;

			assertTrue("should not support unicast", !pr.supportsUnicast());

			assertTrue("incorrect daily uptime!", pr.getDailyUptime() > 0);
			assertTrue("unexpected vendor", pr.getVendor().equals("LIME"));
			assertTrue("pong should have GGEP", pr.hasGGEPExtension());
			out.close();
        } catch (IOException e) {
            e.printStackTrace();
            fail("Mysterious IO problem: "+e);
        } catch (BadPacketException e) {
			e.printStackTrace();
			fail("Bad packet: "+e);
        }
    }
	
	
	/**
	 * Tests to make sure that LimeWire correctly strips any GGEP extensions
	 * in pongs if it thinks the connection on the other end does not 
	 * support GGEP.
	 */
	public void testStripsGGEP() {
        ManagedConnection out = 
			ManagedConnection.createTestConnection("localhost", 
												   Backend.DEFAULT_PORT,
												   new NoGGEPProperties(),
												   new EmptyResponder());
        try {
			out.initialize();

			// receive initial ping
			out.receive();
			out.receive()
;			out.receive();

            //Connection in = acceptor.accept();
			//assertNotNull("connection should not be null", in);
            assertTrue("Connection should support GGEP", out.supportsGGEP());
			out.send(new PingRequest((byte)2));
			
			Message m = out.receive();
			assertTrue("should be a pong", m instanceof PingReply);
			PingReply pr = (PingReply)m;
            try {
                pr.getDailyUptime();
                fail("Payload wasn't stripped");
            } catch (BadPacketException e) { }			

            try {
                pr.getDailyUptime();
                fail("GGEP payload wasn't stripped");
            } catch (BadPacketException e) { }			
            try {
                pr.supportsUnicast();
                fail("GGEP payload wasn't stripped");
            } catch (BadPacketException e) { }			

			assertTrue("pong should not have GGEP", !pr.hasGGEPExtension());

            out.close();
        } catch (IOException e) {
			e.printStackTrace();
            fail("Mysterious IO problem: "+e);
        } catch (BadPacketException e) {
			e.printStackTrace();
            fail("Bad packet: "+e);
        }
    }
    
	/**
	 * Tests to make sure that connections are closed correctly from the
	 * client side.
	 */
    public void testClientSideClose() {
        try {
            ManagedConnection out=null;
            Connection in=null;
            //com.limegroup.gnutella.MiniAcceptor acceptor=null;                
            //When receive() or sendQueued() gets IOException, it calls
            //ConnectionManager.remove().  This in turn calls
            //ManagedConnection.close().  Our stub does this.
            ConnectionManager manager=new ConnectionManagerStub(true);

            //1. Locally closed
            //acceptor=new com.limegroup.gnutella.MiniAcceptor(null, PORT);
			out = new ManagedConnection("localhost", Backend.DEFAULT_PORT);
            //out=newConnection("localhost", PORT, manager);
            out.initialize();            

            //in=acceptor.accept(); 
			assertTrue("connection should be open", out.isOpen());
			assertTrue("runner should not be dead", !out.runnerDied());
            out.close();
            sleep(100);
            assertTrue("connection should not be open", !out.isOpen());
            assertTrue("runner should have died", out.runnerDied());

            try { Thread.sleep(1000); } catch (InterruptedException e) { }
            //in.close(); //needed to ensure connect below works
		} catch (IOException e) {
            e.printStackTrace();
            fail("Unexpected IO problem");
        }
	}

	/**
	 * Tests to make sure that connections are closed correctly from the
	 * server side.
	 */
    public void testServerSideClose() {
		try {
			MiniAcceptor acceptor = new MiniAcceptor(PORT);
			
			//2. Remote close: discovered on read
			ManagedConnection out = new ManagedConnection("localhost", PORT);
			//out = newConnection("localhost", PORT, manager);
			out.initialize();            
            Connection in = acceptor.accept(); 
            assertTrue("connection should be open", out.isOpen());
            assertTrue("runner should not be dead", !out.runnerDied());


            in.close();
            try {
				out.receive();
				fail("should not have received message");
            } catch (BadPacketException e) {
				e.printStackTrace();
				fail("should not have received bad packet");
            } catch (IOException e) { }            

            sleep(100);
            assertTrue("connection should not be open", !out.isOpen());
            assertTrue("runner should be dead", out.runnerDied());

            //3. Remote close: discovered on write.  Because of TCP's half-close
            //semantics, we need TWO writes to discover this.  (See unit tests
            //for Connection.)
            acceptor = new com.limegroup.gnutella.MiniAcceptor(PORT);
            out = new ManagedConnection("localhost", PORT);
            out.initialize();            
            in = acceptor.accept(); 
            assertTrue("connection should be open", out.isOpen());
            assertTrue("runner should not be dead", !out.runnerDied());
            in.close();
            out.send(new PingRequest((byte)3));
            out.send(new PingRequest((byte)3));
            sleep(100);

            assertTrue("connection should not be open", !out.isOpen());
            assertTrue("runner should be dead", out.runnerDied());
			sleep(2000);

        } catch (IOException e) {
            e.printStackTrace();
            fail("Unexpected IO problem");
        }
    }

    /** Tries to receive any outstanding messages on c 
     *  @return true if this got a message */
    private static boolean drain(Connection c) throws IOException {
        boolean ret=false;
        while (true) {
            try {
                Message m = c.receive(500);
                ret=true;
            } catch (InterruptedIOException e) {
                return ret;
            } catch (BadPacketException e) {
            }
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

	private static class NoGGEPProperties extends SupernodeProperties {
		public NoGGEPProperties() {
			super("localhost");
			remove(ConnectionHandshakeHeaders.GGEP);
		}
	}

    private static ManagedConnection newConnection() {
        return newConnection("", 0);
    }

    private static ManagedConnection newConnection(String host, int port) {
        ManagedConnection mc = new ManagedConnection(host, port);
		setStubs(mc, new ConnectionManagerStub());
		return mc;
    }

    private static ManagedConnection newConnection(String host, int port,
                                                   ConnectionManager cm) {
        ManagedConnection mc = new ManagedConnection(host, port);
		setStubs(mc, cm);
		return mc;
    }

	private static void setStubs(ManagedConnection mc, ConnectionManager cm) {
        try {
            PrivilegedAccessor.setValue(mc, "_router", new MessageRouterStub());
            PrivilegedAccessor.setValue(mc, "_manager", cm);
        } catch(Exception e) {
            e.printStackTrace();
            fail("could not initialize test");
        }		
	}

    public static void main(String argv[]) {
        junit.textui.TestRunner.run(suite());
    }
}
