package com.limegroup.gnutella;

import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.messages.vendor.*;
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
public class ClientSidePushProxyTest 
    extends com.limegroup.gnutella.util.BaseTestCase {
    private static final int PORT=6669;
    private static final int TIMEOUT=500;
    private static final byte[] ultrapeerIP=
        new byte[] {(byte)18, (byte)239, (byte)0, (byte)144};
    private static final byte[] oldIP=
        new byte[] {(byte)111, (byte)22, (byte)33, (byte)44};

    private static Connection ultrapeer1;
    private static Connection ultrapeer2;
    private static Connection old1;
    private static Connection old2;
    private static Connection testUP;
    private static RouterService rs;

    public ClientSidePushProxyTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ClientSidePushProxyTest.class);
    }    

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    private static void doSettings() {
        //Setup LimeWire backend.  For testing other vendors, you can skip all
        //this and manually configure a client in leaf mode to listen on port
        //6669, with no slots and no connections.  But you need to re-enable
        //the interactive prompts below.
        SettingsManager settings=SettingsManager.instance();
        settings.setPort(PORT);
		ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
		UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(false);
		UltrapeerSettings.DISABLE_ULTRAPEER_MODE.setValue(true);
		UltrapeerSettings.FORCE_ULTRAPEER_MODE.setValue(false);
		ConnectionSettings.NUM_CONNECTIONS.setValue(0);
		ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        settings.setExtensions("txt;");
        // get the resource file for com/limegroup/gnutella
        File berkeley = 
            CommonUtils.getResourceFile("com/limegroup/gnutella/berkeley.txt");
        File susheel = 
            CommonUtils.getResourceFile("com/limegroup/gnutella/susheel.txt");
        // now move them to the share dir
        CommonUtils.copy(berkeley, new File(_sharedDir, "berkeley.txt"));
        CommonUtils.copy(susheel, new File(_sharedDir, "susheel.txt"));
    }        
    
    public static void globalSetUp() throws Exception {
        doSettings();

        SettingsManager settings=SettingsManager.instance();
        ActivityCallback callback=new ActivityCallbackStub();
        rs=new RouterService(callback);
        assertEquals("unexpected port", PORT, settings.getPort());
        rs.start();
        rs.clearHostCatcher();
        rs.connect();
        Thread.sleep(1000);
        assertEquals("unexpected port", PORT, settings.getPort());
        connect(rs);
    }        
    
    public void setUp() throws Exception  {
        doSettings();
    }
    
    public static void globalTearDown() throws Exception {
        shutdown();
    }

     ////////////////////////// Initialization ////////////////////////

     private static void connect(RouterService rs) 
     throws IOException, BadPacketException {
         debug("-Establish connections");
         //Ugh, there is a race condition here from the old days when this was
         //an interactive test.  If rs connects before the listening socket is
         //created, the test will fail.

         //System.out.println("Please establish a connection to localhost:6350\n");
         ultrapeer1 = connect(rs, 6350, true);
         ultrapeer2 = connect(rs, 6351, true);
         old1 = connect(rs, 6352, true);
         old2 = connect(rs, 6353, true);
     }
     
     private static Connection connect(RouterService rs, int port, 
                                       boolean ultrapeer) 
         throws IOException, BadPacketException {
         ServerSocket ss=new ServerSocket(port);
         rs.connectToHostAsynchronously("127.0.0.1", port);
         Socket socket = ss.accept();
         ss.close();
         
         socket.setSoTimeout(3000);
         InputStream in=socket.getInputStream();
         String word=readWord(in);
         if (! word.equals("GNUTELLA"))
             throw new IOException("Bad word: "+word);
         
         HandshakeResponder responder;
         if (ultrapeer) {
             responder = new UltrapeerResponder();
         } else {
             responder = new OldResponder();
         }
         Connection con = new Connection(socket, responder);
         con.initialize();
         replyToPing(con, ultrapeer);
         return con;
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
         Message m=c.receive(5000);
         assertTrue(m instanceof PingRequest);
         PingRequest pr=(PingRequest)m;
         byte[] localhost=new byte[] {(byte)127, (byte)0, (byte)0, (byte)1};
         PingReply reply = 
             PingReply.createExternal(pr.getGUID(), (byte)7,
                                       c.getLocalPort(), 
                                       ultrapeer ? ultrapeerIP : oldIP,
                                       ultrapeer);
         reply.hop();
         c.send(reply);
         c.flush();
     }

    ///////////////////////// Actual Tests ////////////////////////////
    
    // THIS TEST SHOULD BE RUN FIRST!!
    public void testPushProxySetup() throws Exception {

        testUP = connect(rs, 6355, true);

        // send a MessagesSupportedMessage
        testUP.send(MessagesSupportedVendorMessage.instance());
        testUP.flush();

        // we expect to get a PushProxy request
        Message m = null;
        do {
            m = testUP.receive(TIMEOUT);
        } while (!(m instanceof PushProxyRequest)) ;

        // we should answer the push proxy request
        PushProxyAcknowledgement ack = 
            new PushProxyAcknowledgement(6355, new GUID(m.getGUID()));
        testUP.send(ack);
        testUP.flush();

        // client side seems to follow the setup process A-OK
    }

    public void testQueryReplyHasProxies() throws Exception {

        drain(testUP);

        // make sure leaf is sharing
        assertEquals(2, rs.getFileManager().getNumFiles());

        // send a query that should be answered
        QueryRequest query = new QueryRequest(GUID.makeGuid(), (byte) 1,
                                              "berkeley", null, null, null,
                                              null, false, 0, false);
        testUP.send(query);
        testUP.flush();

        // await a response
        Message m = null;
        do {
            m = testUP.receive(TIMEOUT);
        } while (!(m instanceof QueryReply)) ;

        // confirm it has proxy info
        QueryReply reply = (QueryReply) m;
        assertNotNull(reply.getPushProxies());

        // check out PushProxy info
        PushProxyInterface[] proxies = reply.getPushProxies();
        assertEquals(proxies.length, 1);
        assertEquals(proxies[0].getPushProxyPort(), 6355);
        assertTrue(proxies[0].getPushProxyAddress().getHostAddress().startsWith("127"));

        // everything checks out!
    }


    //////////////////////////////////////////////////////////////////

    /** Tries to receive any outstanding messages on c 
     *  @return true if this got a message */
    private boolean drain(Connection c) throws IOException {
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
        debug("-Shutting down");
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) { }
        ultrapeer1.close();
        ultrapeer2.close();
        old1.close();
        old2.close();
    }

    private static final boolean DEBUG = false;
    
    static void debug(String message) {
        if(DEBUG) 
            System.out.println(message);
    }

    private static class UltrapeerResponder implements HandshakeResponder {
        public HandshakeResponse respond(HandshakeResponse response, 
                boolean outgoing) throws IOException {
            Properties props=new Properties();
            props.put(HeaderNames.USER_AGENT, 
                      CommonUtils.getHttpServer());
            props.put(HeaderNames.X_QUERY_ROUTING, "0.1");
            props.put(HeaderNames.X_ULTRAPEER, "True");
            return HandshakeResponse.createResponse(props);
        }
    }


    private static class OldResponder implements HandshakeResponder {
        public HandshakeResponse respond(HandshakeResponse response, 
                boolean outgoing) throws IOException {
            Properties props=new Properties();
            return HandshakeResponse.createResponse(props);
        }
    }
}
