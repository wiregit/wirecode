package com.limegroup.gnutella;

import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.settings.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.handshaking.*;
import com.limegroup.gnutella.routing.*;
import com.limegroup.gnutella.security.*;
import com.limegroup.gnutella.stubs.*;
import com.limegroup.gnutella.util.*;

import junit.framework.*;
import java.util.Properties;
import java.util.StringTokenizer;
import com.sun.java.util.collections.*;
import java.io.*;
import java.net.*;

/**
 * Checks whether (multi)leaves avoid forwarding messages to ultrapeers, do
 * redirects properly, etc.  The test includes a leaf attached to 3 
 * Ultrapeers.
 */
public class LeafRoutingTest extends com.limegroup.gnutella.util.BaseTestCase {
    private static final int PORT=6669;
    private static final int TIMEOUT=500;
    private static final byte[] ultrapeerIP=
        new byte[] {(byte)18, (byte)239, (byte)0, (byte)144};
    private static final byte[] oldIP=
        new byte[] {(byte)111, (byte)22, (byte)33, (byte)44};

    static Connection ultrapeer1;
    static Connection ultrapeer2;
    static Connection old1;
    static Connection old2;

    public LeafRoutingTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return new TestSuite(LeafRoutingTest.class);
    }    

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }


    public void testLegacy() throws Exception {
        //Setup LimeWire backend.  For testing other vendors, you can skip all
        //this and manually configure a client in leaf mode to listen on port
        //6669, with no slots and no connections.  But you need to re-enable
        //the interactive prompts below.
        SettingsManager settings=SettingsManager.instance();
        settings.setPort(PORT);
        settings.setDirectories(new File[0]);
		ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
		UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(false);
		UltrapeerSettings.DISABLE_ULTRAPEER_MODE.setValue(true);
		UltrapeerSettings.FORCE_ULTRAPEER_MODE.setValue(false);
		ConnectionSettings.KEEP_ALIVE.setValue(0);
		ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);

        ActivityCallback callback=new ActivityCallbackStub();
        //FileManager files=new FileManagerStub();
        //MessageRouter router=new MessageRouterStub();
        RouterService rs=new RouterService(callback);
        assertEquals("unexpected port", PORT, settings.getPort());
        rs.start();
        rs.clearHostCatcher();
        assertEquals("unexpected port", PORT, settings.getPort());

        //Run tests
        connect(rs);
        doRedirect();
        doLeafBroadcast(rs);
        doBroadcastFromUltrapeer();
        doConnectionToOldDisallowed();
        //doNoBroadcastFromOld();
        shutdown();
     }

     ////////////////////////// Initialization ////////////////////////

     private static void connect(RouterService rs) 
             throws IOException, BadPacketException {
         //Ugh, there is a race condition here from the old days when this was
         //an interactive test.  If rs connects before the listening socket is
         //created, the test will fail.

         //System.out.println("Please establish a connection to localhost:6350\n");
         rs.connectToHostAsynchronously("127.0.0.1", 6350);
         ultrapeer1=new Connection(accept(6350), new UltrapeerResponder());
         ultrapeer1.initialize();
         replyToPing(ultrapeer1, true);

         //System.out.println("Please establish a connection to localhost:6351\n");
         rs.connectToHostAsynchronously("127.0.0.1", 6351);
         ultrapeer2=new Connection(accept(6351), new UltrapeerResponder());
         ultrapeer2.initialize();
         replyToPing(ultrapeer2, true);

         //System.out.println("Please establish a connection to localhost:6352\n");
         rs.connectToHostAsynchronously("127.0.0.1", 6352);
         //old1=new Connection(accept(6352), new OldResponder());
         old1=new Connection(accept(6352), new UltrapeerResponder());
         old1.initialize();
         //replyToPing(old1, false);
         replyToPing(old1, true);

         //System.out.println("Please establish a connection to localhost:6353\n");
         rs.connectToHostAsynchronously("127.0.0.1", 6353);
         //old2=new Connection(accept(6353), new OldResponder());
         old2=new Connection(accept(6353), new UltrapeerResponder());
         old2.initialize();
         //replyToPing(old2, false);
         replyToPing(old2, true);
     }

     private static Socket accept(int port) throws IOException { 
         ServerSocket ss=new ServerSocket(port);
         Socket s=ss.accept();
         InputStream in=s.getInputStream();
         String word=readWord(in);
         if (! word.equals("GNUTELLA"))
             throw new IOException("Bad word: "+word);
         return s;
     }

     /**
      * Acceptor.readWord
      *
      * @modifies sock
      * @effects Returns the first word (i.e., no whitespace) of less
      *  than 8 characters read from sock, or throws IOException if none
      *  found.
      */
     private static String readWord(InputStream sock) throws IOException {
         final int N=9;  //number of characters to look at
         char[] buf=new char[N];
         for (int i=0 ; i<N ; i++) {
             int got=sock.read();
             if (got==-1)  //EOF
                 throw new IOException();
             if ((char)got==' ') { //got word.  Exclude space.
                 return new String(buf,0,i);
             }
             buf[i]=(char)got;
         }
         throw new IOException();
     }

     private static void replyToPing(Connection c, boolean ultrapeer) 
             throws IOException, BadPacketException {
         Message m=c.receive(1000);
         assertTrue(m instanceof PingRequest);
         PingRequest pr=(PingRequest)m;
         byte[] localhost=new byte[] {(byte)127, (byte)0, (byte)0, (byte)1};
         PingReply reply=new PingReply(pr.getGUID(), (byte)7,
                                       c.getLocalPort(), 
                                       ultrapeer ? ultrapeerIP : oldIP,
                                       0, 0, ultrapeer);
         reply.hop();
         c.send(reply);
         c.flush();
     }

     ///////////////////////// Actual Tests ////////////////////////////

    private static void doLeafBroadcast(RouterService rs) 
            throws IOException, BadPacketException {
        //System.out.println("Please send a query for \"crap\" from your leaf\n");
        //try {
        //    Thread.sleep(6000);
        //} catch (InterruptedException e) { }
        byte[] guid=rs.newQueryGUID();
        rs.query(guid, "crap", 0);
        //System.out.println("-Testing broadcast from leaf");

        while (true) {
            Message m=ultrapeer1.receive(2000);
            if (m instanceof QueryRequest) {
                assertEquals("unexpected query name", "crap", 
                             ((QueryRequest)m).getQuery());
                break;
            }
        }       
        while (true) {
            Message m=ultrapeer2.receive(2000);
            if (m instanceof QueryRequest) {
                assertEquals("unexpected query name", "crap", 
                             ((QueryRequest)m).getQuery());
                break;
            }
        }
        while (true) {
            Message m=old1.receive(2000);
            if (m instanceof QueryRequest) {
                assertEquals("unexpected query name", "crap", 
                             ((QueryRequest)m).getQuery());
                break;
            }
        }
//          while (true) {
//              Message m=old2.receive(2000);
//              if (m instanceof QueryRequest) {
//                  assertEquals("unexpected query name", "crap", 
//                               ((QueryRequest)m).getQuery());
//                  break;
//              }
//          }
    }

    /**
     * Tests that the X-Try and X-Try-Ultrapeer headers are correctly
     * being transferred in connection headers.
     */
    private static void doRedirect() {
        Connection c=new Connection("127.0.0.1", PORT,
                                    new Properties(),
                                    new OldResponder()
                                    );

        try {
            c.initialize();
            fail("handshake should not have succeeded");
        } catch (IOException e) {
            String hosts=c.getProperty(ConnectionHandshakeHeaders.X_TRY);
            //System.out.println("X-Try: "+hosts);
            assertNotNull("hosts should not be null", hosts);
            Set s=list2set(hosts);

            // size should be 0 since the test contains no non-Ultrapeer nodes
            assertEquals("unexpected size of X-Try hosts list", 0, s.size());

            hosts=c.getProperty(
                                ConnectionHandshakeHeaders.X_TRY_SUPERNODES);
            //System.out.println("X-Try-Ultrapeers: "+hosts);
            assertNotNull("unexpected null value", hosts);
            s=list2set(hosts);
            assertEquals("unexpected size of X-Try-Ultrapeers list hosts: "+hosts, 
                         8, s.size());
            byte[] localhost=new byte[] {(byte)127, (byte)0, (byte)0, (byte)1};
            assertTrue("expected Ultrapeer not present in list",
                       s.contains(new Endpoint(ultrapeerIP, 6350)));
            assertTrue("expected Ultrapeer not present in list",
                       s.contains(new Endpoint(ultrapeerIP, 6351)));
            assertTrue("expected Ultrapeer not present in list",
                       s.contains(new Endpoint(ultrapeerIP, 6352)));
            assertTrue("expected Ultrapeer not present in list",
                       s.contains(new Endpoint(ultrapeerIP, 6353)));

            assertTrue("expected Ultrapeer not present in list",
                       s.contains(new Endpoint(localhost, 6350))); 
            assertTrue("expected Ultrapeer not present in list",
                       s.contains(new Endpoint(localhost, 6351)));
            assertTrue("expected Ultrapeer not present in list",
                       s.contains(new Endpoint(localhost, 6352))); 
            assertTrue("expected Ultrapeer not present in list",
                       s.contains(new Endpoint(localhost, 6353)));

        }
    }

    private static void doBroadcastFromUltrapeer() throws IOException {
        //System.out.println("-Test query from ultrapeer not broadcasted");
        drain(ultrapeer2);
        drain(old1);
        drain(old2);

        QueryRequest qr=new QueryRequest((byte)7, 0, "crap", false);
        ultrapeer1.send(qr);
        ultrapeer1.flush();

        assertTrue("drain should have returned false", !drain(ultrapeer2));
        //We don't care whether this is forwarded to the old connections
    }

    /*
    private static void doNoBroadcastFromOld() 
        throws IOException, BadPacketException {
        //System.out.println("-Test query from old not broadcasted");
        drain(ultrapeer1);
        drain(ultrapeer2);
        drain(old2);

        QueryRequest qr=new QueryRequest((byte)7, 0, "crap", false);
        old1.send(qr);
        old1.flush();

        assertTrue(! drain(ultrapeer1));
        assertTrue(! drain(ultrapeer2));
        Message m=old2.receive(500);
        assertTrue(((QueryRequest)m).getQuery().equals("crap"));
        assertEquals("unexpected hops", (byte)1, m.getHops()); 
        // we adjust all TTLs down to 6....
		assertEquals("unexpected TTL", (byte)5, m.getTTL());        
    }
    */

    /**
     * Tests to make sure that connections to old hosts are not allowed
     */
    private static void doConnectionToOldDisallowed() {
        Connection c=new Connection("127.0.0.1", PORT,
                                    new Properties(),
                                    new OldResponder()
                                    );
        try {
            c.initialize();
            fail("handshake should not have succeeded");
        } catch (IOException e) {
        }        
    }


    /** Converts the given X-Try[-Ultrapeer] header value to
     *  a Set of Endpoints. */
    private static Set /* of Endpoint */ list2set(String addresses) {
        Set ret=new HashSet();
        StringTokenizer st = new StringTokenizer(addresses,
            Constants.ENTRY_SEPARATOR);
        while(st.hasMoreTokens()){
            //get an address
            String address = ((String)st.nextToken()).trim();
            Endpoint e;
            try {
                e = new Endpoint(address);
                ret.add(e);
            } catch(IllegalArgumentException iae) {
                assertTrue("Bad endpoint", false);
            }
        }
        return ret;
    }

    //////////////////////////////////////////////////////////////////

    /** Tries to receive any outstanding messages on c 
     *  @return true if this got a message */
    private static boolean drain(Connection c) throws IOException {
        boolean ret=false;
        while (true) {
            try {
                Message m=c.receive(500);
                ret=true;
                //System.out.println("Draining "+m+" from "+c);
            } catch (InterruptedIOException e) {
                return ret;
            } catch (BadPacketException e) {
            }
        }
    }

    private static void shutdown() throws IOException {
        //System.out.println("\nShutting down.");
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) { }
        ultrapeer1.close();
        ultrapeer2.close();
        old1.close();
        old2.close();
    }
}

class UltrapeerResponder implements HandshakeResponder {
    public HandshakeResponse respond(HandshakeResponse response, 
            boolean outgoing) throws IOException {
        Properties props=new Properties();
        props.put(ConnectionHandshakeHeaders.USER_AGENT, 
                  CommonUtils.getHttpServer());
        props.put(ConnectionHandshakeHeaders.X_QUERY_ROUTING, "0.1");
        props.put(ConnectionHandshakeHeaders.X_SUPERNODE, "True");
        return new HandshakeResponse(props);
    }
}


class OldResponder implements HandshakeResponder {
    public HandshakeResponse respond(HandshakeResponse response, 
            boolean outgoing) throws IOException {
        Properties props=new Properties();
        return new HandshakeResponse(props);
    }
}
