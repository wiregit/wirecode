package com.limegroup.gnutella;

import junit.framework.*;
import java.io.*;
import java.util.Properties;
import com.limegroup.gnutella.connection.*;
import com.limegroup.gnutella.handshaking.*;
import com.limegroup.gnutella.routing.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.stubs.*;
import com.sun.java.util.collections.*;

public class ManagedConnectionTest extends TestCase {  
    public ManagedConnectionTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return new TestSuite(ManagedConnectionTest.class);
    }    

    public void setUp() {
        //Restore all the defaults.  Apparently testForwardsGGEP fails if this
        //is in ultrapeer mode and the KEEP_ALIVE is 1.  It seems that WE (the
        //client) send a "503 Service Unavailable" at line 77 of
        //SupernodeHandshakeResponder.
        SettingsManager.instance().loadDefaults();
        SettingsManager.instance().setQuickConnectHosts(new String[0]);
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




    //////////////////////////// Tests ////////////////////////////////////

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

        assertTrue(mc.getNumFiles()==0);
        assertTrue(mc.getNumHosts()==0);
        assertTrue(mc.getTotalFileSize()==0);

        mc.updateHorizonStats(pr1);
        mc.updateHorizonStats(pr1);  //check duplicates
        assertTrue(mc.getNumFiles()==1);
        assertTrue(mc.getNumHosts()==1);
        assertTrue(mc.getTotalFileSize()==10);

        try { Thread.sleep(ManagedConnection.HORIZON_UPDATE_TIME*2); } 
        catch (InterruptedException e) { }
            
        mc.refreshHorizonStats();    
        mc.updateHorizonStats(pr1);  //should be ignored for now
        mc.updateHorizonStats(pr2);
        mc.updateHorizonStats(pr3);
        assertTrue(mc.getNumFiles()==1);
        assertTrue(mc.getNumHosts()==1);
        assertTrue(mc.getTotalFileSize()==10);
        mc.refreshHorizonStats();    //should be ignored
        assertTrue(mc.getNumFiles()==1);
        assertTrue(mc.getNumHosts()==1);
        assertTrue(mc.getTotalFileSize()==10);

        try { Thread.sleep(ManagedConnection.HORIZON_UPDATE_TIME*2); } 
        catch (InterruptedException e) { }            

        mc.refreshHorizonStats();    //update stats
        assertTrue(mc.getNumFiles()==(1+2+3));
        assertTrue(mc.getNumHosts()==3);
        assertTrue(mc.getTotalFileSize()==(10+20+30));

        try { Thread.sleep(ManagedConnection.HORIZON_UPDATE_TIME*2); } 
        catch (InterruptedException e) { }       

        mc.refreshHorizonStats();
        assertTrue(mc.getNumFiles()==0);
        assertTrue(mc.getNumHosts()==0);
        assertTrue(mc.getTotalFileSize()==0);                
    }
    
    public void testIsRouter() {
        assertTrue(! ManagedConnection.isRouter("127.0.0.1"));
        assertTrue(! ManagedConnection.isRouter("18.239.0.1"));
        assertTrue(ManagedConnection.isRouter("64.61.25.171"));
        assertTrue(ManagedConnection.isRouter("64.61.25.139"));
        assertTrue(ManagedConnection.isRouter("64.61.25.143"));
        assertTrue(! ManagedConnection.isRouter("64.61.25.138"));
        assertTrue(! ManagedConnection.isRouter("64.61.25.170"));
        assertTrue(! ManagedConnection.isRouter("www.limewire.com"));
        assertTrue(! ManagedConnection.isRouter("public.bearshare.net"));
        assertTrue(ManagedConnection.isRouter("router.limewire.com"));
        assertTrue(ManagedConnection.isRouter("router4.limewire.com"));
        assertTrue(ManagedConnection.isRouter("router2.limewire.com"));
        assertTrue(ManagedConnection.translateHost("router.limewire.com").
            equals("router4.limewire.com"));
        assertTrue(ManagedConnection.translateHost("router4.limewire.com").
            equals("router4.limewire.com"));
     }

    public void testForwardsGGEP() {
        int TIMEOUT=1000;
        TestConnectionListener listener=new TestConnectionListener();
        try {
            MiniAcceptor acceptor=new MiniAcceptor(
                listener, new GGEPResponder(), 6346);
            ManagedConnection out=new ManagedConnection(
                "localhost", 6346, 
                new MessageRouterStub(), new ConnectionManagerStub());
            out.initialize(new ConnectionListenerStub());
            Connection in=acceptor.accept();
            assertTrue(out.supportsGGEP());

            PingReply reply0=new PingReply(
                new byte[16], (byte)3, 6349, new byte[4],
                13l, 14l, false, 4321);
            assertTrue(!out.write(reply0));
                       
            in.read();
            PingReply reply=(PingReply)listener.message;
            assertTrue(reply!=null);
            assertEquals(reply.getPort(), 6349);
            assertEquals(reply.getDailyUptime(), 4321);
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
        TestConnectionListener listener=new TestConnectionListener();
        try {
            MiniAcceptor acceptor=new MiniAcceptor(
                listener, new EmptyResponder(), 6346);
            ManagedConnection out=new ManagedConnection(
                "localhost", 6346, 
                new MessageRouterStub(), new ConnectionManagerStub());
            out.initialize(new ConnectionListenerStub());
            Connection in=acceptor.accept();
            assertTrue(!out.supportsGGEP());

            PingReply reply0=new PingReply(
                new byte[16], (byte)3, 6349, new byte[4],
                13l, 14l, false, 4321);
            assertTrue(!out.write(reply0));
                       
            in.read();
            PingReply reply=(PingReply)listener.message;
            assertTrue(reply!=null);
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
        }
    }


    public void testForwardsGroupPing() {
        int TIMEOUT=1000;
        TestConnectionListener listener=new TestConnectionListener();
        try {
            MiniAcceptor acceptor=new MiniAcceptor(
                listener, new EmptyResponder(), 6346);
            ManagedConnection out=new ManagedConnection(
                "localhost", 6346, 
                new MessageRouterStub(), new ConnectionManagerStub());
            out.initialize(new ConnectionListenerStub());
            Connection in=acceptor.accept();
            assertTrue(!out.supportsGGEP());

            PingRequest ping1=new GroupPingRequest((byte)3, 6349, new byte[4],
                                                   0l, 0l, "test");
            assertEquals(14+4+1, ping1.getLength());
            out.write(ping1);            
                                   
            in.read();
            PingRequest ping2=(PingRequest)listener.message;
            assertEquals(14+4+1, ping2.getLength());
            assertTrue(Arrays.equals(ping1.getGUID(), ping2.getGUID()));
            in.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
            fail("Mysterious IO problem: "+e);
        }
    }

    
    /** Test that ManagedConnection's buffer multiple message, though doesn't
     *  test the exact buffer policy; CompositeQueueTest covers that. */
    public void testBuffering() {
        int TIMEOUT=1000;
        TestConnectionListener listener=new TestConnectionListener();
        try {
            MiniAcceptor acceptor=new MiniAcceptor(
                listener, new EmptyResponder(), 6346);
            TestManagedConnection out=new TestManagedConnection(
                "localhost", 6346); 

            out.initialize(listener);
            Connection in=acceptor.accept();

            //Try to send first two message, but they fail because this is
            //blocked.
            PingRequest ping1=new PingRequest((byte)3);
            PingRequest ping2=new PingRequest((byte)3);
            PingRequest ping3=new PingRequest((byte)3);
            assertTrue(out.write(ping1));
            assertTrue(out.write(ping2));
            assertEquals(2, out.getNumMessagesSent());
            assertEquals(0, out.getNumSentMessagesDropped());
            in.read();
            in.read();
            assertEquals(null, listener.message);



            //Unblock and send 3rd message.
            out.blocked=false;            
            listener.needsWrite=false;
            assertTrue(!out.write(ping3));
            assertEquals(3, out.getNumMessagesSent());
            assertTrue(listener.normal());

            //And they flow through...though in reverse order.
            in.read();
            in.read();
            in.read();
            assertEquals(3, listener.messages.size());
            assertTrue(Arrays.equals(
                ((PingRequest)listener.messages.get(0)).getGUID(), 
                ping3.getGUID()));
            assertTrue(Arrays.equals(
                ((PingRequest)listener.messages.get(1)).getGUID(), 
                ping2.getGUID()));
            assertTrue(Arrays.equals(
                ((PingRequest)listener.messages.get(2)).getGUID(), 
                ping1.getGUID()));
            in.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
            fail("Mysterious IO problem: "+e);
        }
    }

//      /** Tests that write() does not enter an infinite loop if closed. */
//      public void testWriteToClosed() {
//          int TIMEOUT=1000;
//          TestConnectionListener listener=new TestConnectionListener();
//          try {
//              MiniAcceptor acceptor=new MiniAcceptor(
//                  listener, new EmptyResponder(), 6346);
//              ManagedConnection out=new ManagedConnection(
//                  "localhost", 6346,
//                  new MessageRouterStub(), new ConnectionManagerStub());
//              out.initialize(listener);
//              Connection in=acceptor.accept();
            
//              //Try to write huge message.  Nothing should go through initially.
//              //Except that the line marked below always works on Windows.  Don't
//              //understand why.
//              final int SIZE=10000000;
//              SettingsManager.instance().setMaxLength(SIZE+1);            
//              PingRequest big=new PingRequest(GUID.makeGuid(), 
//                                          (byte)3, (byte)0,
//                                          new byte[SIZE]);
//              assertTrue(out.write(big));   //see above

//              System.out.println("Before");
//              out.close();
//              assertTrue(!out.write());
//              System.out.println("After");
//          } catch (IOException e) {
//              e.printStackTrace();
//              fail("Mysterious IO problem: "+e);
//          }
//      }
}

/** Can simulate blocked connection. */
class TestManagedConnection extends ManagedConnection {
    boolean blocked=true;

    public TestManagedConnection(String host, int port) {
        super(host, port,
              new MessageRouterStub(), new ConnectionManagerStub());
    }
    
    public boolean hasQueued() {
        if (blocked)
            return true;
        else
            return super.hasQueued();
    }

    public boolean write() {
        if (blocked)
            return true;
        else
            return super.write();
    }
}
