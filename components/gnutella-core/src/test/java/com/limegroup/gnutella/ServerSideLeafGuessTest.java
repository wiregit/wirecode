package com.limegroup.gnutella;

import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.guess.*;
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
public class ServerSideLeafGuessTest 
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
	 * Constant for the size of UDP messages to accept -- dependent upon
	 * IP-layer fragmentation.
	 */
	private final int BUFFER_SIZE = 8192;

    /**
     * Ultrapeer 1 UDP connection.
     */
    private static DatagramSocket UDP_ACCESS;

    public ServerSideLeafGuessTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ServerSideLeafGuessTest.class);
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

        // you also have to set up TCP incoming....
        {
            Socket sock = null;
            OutputStream os = null;
            try {
                sock = Sockets.connect(InetAddress.getLocalHost().getHostAddress(), 
                                       PORT, 12);
                os = sock.getOutputStream();
                os.write("\n\n".getBytes());
            } catch (IOException ignored) {
            } catch (SecurityException ignored) {
            } catch (Throwable t) {
                ErrorService.error(t);
            } finally {
                if(sock != null)
                    try { sock.close(); } catch(IOException ignored) {}
                if(os != null)
                    try { os.close(); } catch(IOException ignored) {}
            }
        }        

        InetAddress localHost = InetAddress.getLocalHost();
        // first send a QueryKey request....
        send(new PingRequest(), localHost, PORT);

        // we should get a QueryKey....
        Message m = null;
        QueryKey qkToUse = null;
        while (true) {
            m = receive();
            if (m instanceof PingReply) {
                PingReply rep = (PingReply) m;
                qkToUse = rep.getQueryKey();
                if (rep.getQueryKey() != null)
                    break;
            }
        }
        assertNotNull(qkToUse);

        // we should be able to send a query
        QueryRequest goodQuery = QueryRequest.createQueryKeyQuery("susheel", 
                                                                  qkToUse);
        byte[] guid = goodQuery.getGUID();
        send(goodQuery, localHost, PORT);
        
        // now we should get an ack
        m = receive();
        assertTrue(m instanceof PingReply);
        PingReply pRep = (PingReply) m;
        assertEquals(new GUID(guid), new GUID(pRep.getGUID()));
        
        // followed by a query hit
        m = receive();
        assertTrue(m instanceof QueryReply);
        QueryReply qRep = (QueryReply) m;
        assertEquals(new GUID(guid), new GUID(qRep.getGUID()));

    }

    public void testGoodURNQuery() throws Exception {
        InetAddress localHost = InetAddress.getLocalHost();
        // first send a QueryKey request....
        send(new PingRequest(), localHost, PORT);

        // we should get a QueryKey....
        Message m = null;
        QueryKey qkToUse = null;
        while (true) {
            m = receive();
            if (m instanceof PingReply) {
                PingReply rep = (PingReply) m;
                qkToUse = rep.getQueryKey();
                if (rep.getQueryKey() != null)
                    break;
            }
        }
        assertNotNull(qkToUse);

        // now send a URN query, make sure that works....
        File berkeley = 
            CommonUtils.getResourceFile("com/limegroup/gnutella/berkeley.txt");
        Iterator iter = FileDesc.calculateAndCacheURN(berkeley).iterator();
        URN berkeleyURN = (URN) iter.next();
        
        // we should be able to send a URN query
        QueryRequest goodQuery = QueryRequest.createQueryKeyQuery(berkeleyURN, 
                                                                  qkToUse);
        byte[] guid = goodQuery.getGUID();
        send(goodQuery, localHost, PORT);
        
        // now we should get an ack
        m = receive();
        assertTrue(m instanceof PingReply);
        PingReply pRep = (PingReply) m;
        assertEquals(new GUID(guid), new GUID(pRep.getGUID()));
        
        // followed by a query hit with a URN
        m = receive();
        assertTrue(m instanceof QueryReply);
        QueryReply qRep = (QueryReply) m;
        assertEquals(new GUID(guid), new GUID(qRep.getGUID()));
        iter = qRep.getResults();
        Response first = (Response) iter.next();
        assertEquals(first.getUrns(),
                     FileDesc.calculateAndCacheURN(berkeley));
    }


    public void testQueryWithNoHit() throws Exception {
        InetAddress localHost = InetAddress.getLocalHost();
        // first send a QueryKey request....
        send(new PingRequest(), localHost, PORT);

        // we should get a QueryKey....
        Message m = null;
        QueryKey qkToUse = null;
        while (true) {
            m = receive();
            if (m instanceof PingReply) {
                PingReply rep = (PingReply) m;
                qkToUse = rep.getQueryKey();
                if (rep.getQueryKey() != null)
                    break;
            }
        }
        assertNotNull(qkToUse);

        // send a query that shouldn't get results....
        QueryRequest goodQuery = QueryRequest.createQueryKeyQuery("anita", 
                                                                  qkToUse);
        byte[] guid = goodQuery.getGUID();
        send(goodQuery, localHost, PORT);
        
        // now we should get an ack
        m = receive();
        assertTrue(m instanceof PingReply);
        PingReply pRep = (PingReply) m;
        assertEquals(new GUID(guid), new GUID(pRep.getGUID()));
        
        // but not a query hit
        try { 
            m = receive();
            assertTrue(false);
        }
        catch (InterruptedIOException expected) {};
    }


    public void testBadQueryKey() throws Exception {
        InetAddress localHost = InetAddress.getLocalHost();
        Message m = null;

        QueryKey qkToUse = QueryKey.getQueryKey(localHost, 0);
        assertNotNull(qkToUse);

        {
            // we shouldn't get any response to our query...
            QueryRequest goodQuery = QueryRequest.createQueryKeyQuery("susheel", 
                                                                      qkToUse);
            byte[] guid = goodQuery.getGUID();
            send(goodQuery, localHost, PORT);
            
            try {
                // now we should NOT get an ack            
                m = receive();
                assertTrue(false);
            }
            catch (InterruptedIOException expected) {}
        }
    }
    


    //////////////////////////////////////////////////////////////////

    private Message receive() throws Exception {
		byte[] datagramBytes = new byte[BUFFER_SIZE];
		DatagramPacket datagram = new DatagramPacket(datagramBytes, 
                                                     BUFFER_SIZE);
        UDP_ACCESS.receive(datagram);
        byte[] data = datagram.getData();
        // construct a message out of it...
        InputStream in = new ByteArrayInputStream(data);
        Message message = Message.read(in);		
        return message;
    }

    private void send(Message msg, InetAddress ip, int port) 
        throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
        msg.write(baos);

		byte[] data = baos.toByteArray();
		DatagramPacket dg = new DatagramPacket(data, data.length, ip, port); 
        UDP_ACCESS.send(dg);
	}

    private void drainAll() throws Exception {
        drainAll(testUPs);
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

