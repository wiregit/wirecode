package com.limegroup.gnutella;

import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.messages.vendor.*;
import com.limegroup.gnutella.settings.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.search.*;
import com.limegroup.gnutella.handshaking.*;
import com.limegroup.gnutella.routing.*;
import com.limegroup.gnutella.security.*;
import com.limegroup.gnutella.stubs.*;
import com.limegroup.gnutella.util.*;
import com.bitzi.util.*;

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
public class ClientSideOutOfBandReplyTest 
    extends com.limegroup.gnutella.util.BaseTestCase {
    private static final int PORT=6669;
    private static final int TIMEOUT=3000;
    private static final byte[] ultrapeerIP=
        new byte[] {(byte)18, (byte)239, (byte)0, (byte)144};
    private static final byte[] oldIP=
        new byte[] {(byte)111, (byte)22, (byte)33, (byte)44};

    private static Connection[] testUPs = new Connection[4];
    private static RouterService rs;

    private static MyActivityCallback callback;

    /**
     * Ultrapeer 1 UDP connection.
     */
    private static DatagramSocket UDP_ACCESS;

    public ClientSideOutOfBandReplyTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ClientSideOutOfBandReplyTest.class);
    }    

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    private static void doSettings() {
        //Setup LimeWire backend.  For testing other vendors, you can skip all
        //this and manually configure a client in leaf mode to listen on port
        //6669, with no slots and no connections.  But you need to re-enable
        //the interactive prompts below.
        ConnectionSettings.PORT.setValue(PORT);
		ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
		UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(false);
		UltrapeerSettings.DISABLE_ULTRAPEER_MODE.setValue(true);
		UltrapeerSettings.FORCE_ULTRAPEER_MODE.setValue(false);
		ConnectionSettings.NUM_CONNECTIONS.setValue(0);
		ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
		SharingSettings.EXTENSIONS_TO_SHARE.setValue("txt;");
        // get the resource file for com/limegroup/gnutella
        File berkeley = 
            CommonUtils.getResourceFile("com/limegroup/gnutella/berkeley.txt");
        File susheel = 
            CommonUtils.getResourceFile("com/limegroup/gnutella/susheel.txt");
        // now move them to the share dir
        CommonUtils.copy(berkeley, new File(_sharedDir, "berkeley.txt"));
        CommonUtils.copy(susheel, new File(_sharedDir, "susheel.txt"));
        // make sure results get through
        SearchSettings.MINIMUM_SEARCH_QUALITY.setValue(-2);
    }        
    
    public static void globalSetUp() throws Exception {
        doSettings();

        callback=new MyActivityCallback();
        rs=new RouterService(callback);
        assertEquals("unexpected port",
            PORT, ConnectionSettings.PORT.getValue());
        rs.start();
        rs.clearHostCatcher();
        rs.connect();
        Thread.sleep(1000);
        assertEquals("unexpected port",
            PORT, ConnectionSettings.PORT.getValue());
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
     }
     
     private static Connection connect(RouterService rs, int port, 
                                       boolean ultrapeer) 
         throws Exception {
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
             throws Exception {
        // respond to a ping iff one is given.
        Message m = null;
        byte[] guid;
        try {
            while (!(m instanceof PingRequest)) {
                m = c.receive(500);
            }
            guid = ((PingRequest)m).getGUID();            
        } catch(InterruptedIOException iioe) {
            //nothing's coming, send a fake pong anyway.
            guid = new GUID().bytes();
        }
        
        Socket socket = (Socket)PrivilegedAccessor.getValue(c, "_socket");
        PingReply reply = 
        PingReply.createExternal(guid, (byte)7,
                                 socket.getLocalPort(), 
                                 ultrapeer ? ultrapeerIP : oldIP,
                                 ultrapeer);
        reply.hop();
        c.send(reply);
        c.flush();
     }


    /** @return The first QueyrRequest received from this connection.  If null
     *  is returned then it was never recieved (in a timely fashion).
     */
    private static QueryRequest getFirstQueryRequest(Connection c)
        throws BadPacketException, IOException {
        while (true) {
            try {
                Message m=c.receive(TIMEOUT);
                if (m instanceof QueryRequest) 
                    return (QueryRequest)m;
            }
            catch (InterruptedIOException ie) {
                return null;
            }
        }
    }

    /** @return The first QueyrRequest received from this connection.  If null
     *  is returned then it was never recieved (in a timely fashion).
     */
    private static QueryStatusResponse getFirstQueryStatus(Connection c) 
        throws BadPacketException, IOException {
        while (true) {
            try {
                Message m=c.receive(TIMEOUT);
                if (m instanceof QueryStatusResponse) 
                    return (QueryStatusResponse)m;
            }
            catch (InterruptedIOException ie) {
                return null;
            }
        }
    }

    ///////////////////////// Actual Tests ////////////////////////////

    // MUST RUN THIS TEST FIRST
    public void testBasicProtocol() throws Exception {
        DatagramPacket pack = null;
        UDP_ACCESS = new DatagramSocket();

        for (int i = 0; i < testUPs.length; i++) {
            testUPs[i] = connect(rs, 6355+i, true);
            drain(testUPs[i]);
            // OOB client side needs server side leaf guidance
            testUPs[i].send(MessagesSupportedVendorMessage.instance());
            testUPs[i].flush();
        }

        // first we need to set up GUESS capability
        // ----------------------------------------
        // set up solicited UDP support
        {
            drainAll();
            PingReply pong = 
                PingReply.create(GUID.makeGuid(), (byte) 4,
                                 UDP_ACCESS.getLocalPort(), 
                                 InetAddress.getLocalHost().getAddress(), 
                                 10, 10, true, 900, true);
            testUPs[0].send(pong);
            testUPs[0].flush();

            // wait for the ping request from the test UP
            UDP_ACCESS.setSoTimeout(2000);
            pack = new DatagramPacket(new byte[1000], 1000);
            try {
                UDP_ACCESS.receive(pack);
            }
            catch (IOException bad) {
               fail("Did not get ping", bad);
            }
            InputStream in = new ByteArrayInputStream(pack.getData());
            // as long as we don't get a ClassCastException we are good to go
            PingRequest ping = (PingRequest) Message.read(in);
            
            // send the pong in response to the ping
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            pong = PingReply.create(ping.getGUID(), (byte) 4,
                                    UDP_ACCESS.getLocalPort(), 
                                    InetAddress.getLocalHost().getAddress(), 
                                    10, 10, true, 900, true);
            pong.write(baos);
            pack = new DatagramPacket(baos.toByteArray(), 
                                      baos.toByteArray().length,
                                      pack.getAddress(), pack.getPort());
            UDP_ACCESS.send(pack);
        }

        // set up unsolicited UDP support
        {
            // resend this to start exchange
            testUPs[0].send(MessagesSupportedVendorMessage.instance());
            testUPs[0].flush();

            byte[] cbGuid = null;
            int cbPort = -1;
            while (cbGuid == null) {
                try {
                    Message m = testUPs[0].receive(TIMEOUT);
                    if (m instanceof UDPConnectBackVendorMessage) {
                        UDPConnectBackVendorMessage udp = 
                            (UDPConnectBackVendorMessage) m;
                        cbGuid = udp.getConnectBackGUID().bytes();
                        cbPort = udp.getConnectBackPort();
                    }
                }
                catch (Exception ie) {
                    fail("did not get the UDP CB message!", ie);
                }
            }

            // ok, now just do a connect back to the up so unsolicited support
            // is all set up
            PingRequest pr = new PingRequest(cbGuid, (byte) 1, (byte) 0);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            pr.write(baos);
            pack = new DatagramPacket(baos.toByteArray(), 
                                      baos.toByteArray().length,
                                      testUPs[0].getInetAddress(), cbPort);
            UDP_ACCESS.send(pack);
        }
        // ----------------------------------------

        Thread.sleep(250);
        // we should now be guess capable
        assertTrue(rs.isGUESSCapable());

        // first of all, we should confirm that we are sending out a OOB query.
        GUID queryGuid = new GUID(rs.newQueryGUID());
        assertTrue(GUID.addressesMatch(queryGuid.bytes(), rs.getAddress(), 
                                       rs.getPort()));
        rs.query(queryGuid.bytes(), "susheel");
        Thread.sleep(250);

        // all connected UPs should get a OOB query
        for (int i = 0; i < testUPs.length; i++) {
            QueryRequest qr = getFirstQueryRequest(testUPs[i]);
            assertNotNull(qr);
            assertEquals(new GUID(qr.getGUID()), queryGuid);
            assertTrue(qr.desiresOutOfBandReplies());
        }

        // now confirm that we follow the OOB protocol
        ReplyNumberVendorMessage vm = 
           new ReplyNumberVendorMessage(queryGuid, 10);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        vm.write(baos);
        pack = new DatagramPacket(baos.toByteArray(), 
                                  baos.toByteArray().length,
                                  testUPs[0].getInetAddress(), rs.getPort());
        UDP_ACCESS.send(pack);

        // we should get a LimeACK in response
        LimeACKVendorMessage ack = null;
        while (ack == null) {
            pack = new DatagramPacket(new byte[1000], 1000);
            try {
                UDP_ACCESS.receive(pack);
            }
            catch (IOException bad) {
                fail("Did not get ack", bad);
            }
            InputStream in = new ByteArrayInputStream(pack.getData());
            Message m = Message.read(in);
            if (m instanceof LimeACKVendorMessage)
                ack = (LimeACKVendorMessage) m;
        }
        assertEquals(queryGuid, new GUID(ack.getGUID()));
        assertEquals(10, ack.getNumResults());
    }


    public void testRemovedQuerySemantics() throws Exception {
        DatagramPacket pack = null;
        // send a query and make sure that after it is removed (i.e. stopped by
        // the user) we don't request OOB replies for it

        // first of all, we should confirm that we are sending out a OOB query.
        GUID queryGuid = new GUID(rs.newQueryGUID());
        assertTrue(GUID.addressesMatch(queryGuid.bytes(), rs.getAddress(), 
                                       rs.getPort()));
        rs.query(queryGuid.bytes(), "susheel");
        Thread.sleep(250);

        // all connected UPs should get a OOB query
        for (int i = 0; i < testUPs.length; i++) {
            QueryRequest qr = getFirstQueryRequest(testUPs[i]);
            assertNotNull(qr);
            assertEquals(new GUID(qr.getGUID()), queryGuid);
            assertTrue(qr.desiresOutOfBandReplies());
        }

        // now confirm that we follow the OOB protocol
        ReplyNumberVendorMessage vm = 
           new ReplyNumberVendorMessage(queryGuid, 10);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        vm.write(baos);
        pack = new DatagramPacket(baos.toByteArray(), 
                                  baos.toByteArray().length,
                                  testUPs[0].getInetAddress(), rs.getPort());
        UDP_ACCESS.send(pack);

        // we should get a LimeACK in response
        LimeACKVendorMessage ack = null;
        while (ack == null) {
            pack = new DatagramPacket(new byte[1000], 1000);
            try {
                UDP_ACCESS.receive(pack);
            }
            catch (IOException bad) {
                fail("Did not get ack", bad);
            }
            InputStream in = new ByteArrayInputStream(pack.getData());
            Message m = Message.read(in);
            if (m instanceof LimeACKVendorMessage)
                ack = (LimeACKVendorMessage) m;
        }
        assertEquals(queryGuid, new GUID(ack.getGUID()));
        assertEquals(10, ack.getNumResults());

        // now stop the query
        rs.stopQuery(queryGuid);
        Thread.sleep(200);

        // send another ReplyNumber
        vm = new ReplyNumberVendorMessage(queryGuid, 5);
        baos = new ByteArrayOutputStream();
        vm.write(baos);
        pack = new DatagramPacket(baos.toByteArray(), 
                                  baos.toByteArray().length,
                                  testUPs[0].getInetAddress(), rs.getPort());
        UDP_ACCESS.send(pack);

        // we should NOT get a LimeACK in response
        while (true) {
            pack = new DatagramPacket(new byte[1000], 1000);
            try {
                UDP_ACCESS.receive(pack);
            }
            catch (InterruptedIOException expected) {
                break;
            }
            catch (IOException bad) {
                bad.printStackTrace();
            }
            InputStream in = new ByteArrayInputStream(pack.getData());
            Message m = Message.read(in);
            if (m instanceof LimeACKVendorMessage)
                assertTrue("we got an ack, weren't supposed to!!", false);
        }

    }


    public void testExpiredQuerySemantics() throws Exception {
        DatagramPacket pack = null;
        // send a query and make sure that after it is expired (i.e. enough
        // results are recieved) we don't request OOB replies for it

        // first of all, we should confirm that we are sending out a OOB query.
        GUID queryGuid = new GUID(rs.newQueryGUID());
        assertTrue(GUID.addressesMatch(queryGuid.bytes(), rs.getAddress(), 
                                       rs.getPort()));
        rs.query(queryGuid.bytes(), "susheel");
        Thread.sleep(250);

        // all connected UPs should get a OOB query
        for (int i = 0; i < testUPs.length; i++) {
            QueryRequest qr = getFirstQueryRequest(testUPs[i]);
            assertNotNull(qr);
            assertEquals(new GUID(qr.getGUID()), queryGuid);
            assertTrue(qr.desiresOutOfBandReplies());
        }

        // now confirm that we follow the OOB protocol
        ReplyNumberVendorMessage vm = 
           new ReplyNumberVendorMessage(queryGuid, 10);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        vm.write(baos);
        pack = new DatagramPacket(baos.toByteArray(), 
                                  baos.toByteArray().length,
                                  testUPs[0].getInetAddress(), rs.getPort());
        UDP_ACCESS.send(pack);

        // we should get a LimeACK in response
        LimeACKVendorMessage ack = null;
        while (ack == null) {
            pack = new DatagramPacket(new byte[1000], 1000);
            try {
                UDP_ACCESS.receive(pack);
            }
            catch (IOException bad) {
                fail("Did not get ack", bad);
            }
            InputStream in = new ByteArrayInputStream(pack.getData());
            Message m = Message.read(in);
            if (m instanceof LimeACKVendorMessage)
                ack = (LimeACKVendorMessage) m;
        }
        assertEquals(queryGuid, new GUID(ack.getGUID()));
        assertEquals(10, ack.getNumResults());

        // now expire the query by routing hundreds of replies back
        int respsPerUP = QueryHandler.ULTRAPEER_RESULTS/testUPs.length + 5;
        for (int i = 0; i < testUPs.length; i++) {
            Response[] res = new Response[respsPerUP];
            for (int j = 0; j < res.length; j++)
                res[j] = new Response(10, 10, "susheel"+i+j);
            Message m = 
                new QueryReply(queryGuid.bytes(), (byte) 1, 6355, myIP(), 0, res,
                               GUID.makeGuid(), new byte[0], false, false, true,
                               true, false, false, null);

            testUPs[i].send(m);
            testUPs[i].flush();
        }
        Thread.sleep(2000); // lets process these results...

        // send another ReplyNumber
        vm = new ReplyNumberVendorMessage(queryGuid, 5);
        baos = new ByteArrayOutputStream();
        vm.write(baos);
        pack = new DatagramPacket(baos.toByteArray(), 
                                  baos.toByteArray().length,
                                  testUPs[0].getInetAddress(), rs.getPort());
        UDP_ACCESS.send(pack);

        // we should NOT get a LimeACK in response
        while (true) {
            pack = new DatagramPacket(new byte[1000], 1000);
            try {
                UDP_ACCESS.receive(pack);
            }
            catch (InterruptedIOException expected) {
                break;
            }
            catch (IOException bad) {
                bad.printStackTrace();
            }
            InputStream in = new ByteArrayInputStream(pack.getData());
            Message m = Message.read(in);
            if (m instanceof LimeACKVendorMessage)
                assertTrue("we got an ack, weren't supposed to!!", false);
        }

    }

    
    //////////////////////////////////////////////////////////////////

    private void drainAll() throws Exception {
        for (int i = 0; i < testUPs.length; i++)
            drain(testUPs[i]);
    }

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
    }
    
    private static byte[] myIP() {
        return new byte[] { (byte)127, (byte)0, 0, 1 };
    }

    private static final boolean DEBUG = false;
    
    static void debug(String message) {
        if(DEBUG) 
            System.out.println(message);
    }

    private static class UltrapeerResponder implements HandshakeResponder {
        public HandshakeResponse respond(HandshakeResponse response, 
                boolean outgoing) throws IOException {
            Properties props = new UltrapeerHeaders("127.0.0.1"); 
            props.put(HeaderNames.X_DEGREE, "42");           
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

    public static class MyActivityCallback extends ActivityCallbackStub {
        private RemoteFileDesc rfd = null;
        public RemoteFileDesc getRFD() {
            return rfd;
        }

        public void handleQueryResult(RemoteFileDesc rfd,
                                      HostData data,
                                      Set locs) {
            this.rfd = rfd;
        }
    }


}

