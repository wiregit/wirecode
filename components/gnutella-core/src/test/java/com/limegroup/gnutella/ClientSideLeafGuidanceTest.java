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
public class ClientSideLeafGuidanceTest 
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

    private final int REPORT_INTERVAL = SearchResultHandler.REPORT_INTERVAL;

    public ClientSideLeafGuidanceTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ClientSideLeafGuidanceTest.class);
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
    
    // THIS TEST SHOULD BE RUN FIRST!!
    public void testBasicGuidance() throws Exception {
        
        for (int i = 0; i < testUPs.length; i++) {
            testUPs[i] = connect(rs, 6355+i, true);

            // send a MessagesSupportedMessage
            testUPs[i].send(MessagesSupportedVendorMessage.instance());
            testUPs[i].flush();
            drain(testUPs[i]);
        }

        // spawn a query and make sure all UPs get it
        GUID queryGuid = new GUID(rs.newQueryGUID());
        rs.query(queryGuid.bytes(), "susheel");
        Thread.sleep(250);

        for (int i = 0; i < testUPs.length; i++) {
            QueryRequest qr = getFirstQueryRequest(testUPs[i]);
            assertNotNull(qr);
            assertEquals(new GUID(qr.getGUID()), queryGuid);
        }

        // now send back results and make sure that we get a QueryStatus
        // from the leaf
        Message m = null;
        assertGreaterThan(REPORT_INTERVAL, 2*testUPs.length);
        for (int i = 0; i < testUPs.length; i++) {
            Response[] res = new Response[2];
            res[0] = new Response(10, 10, "susheel"+i);
            res[1] = new Response(10, 10, "susheel smells good"+i);
            m = new QueryReply(queryGuid.bytes(), (byte) 1, 6355, myIP(), 0, res,
                               GUID.makeGuid(), new byte[0], false, false, true,
                               true, false, false, null);

            testUPs[i].send(m);
            testUPs[i].flush();
        }
        
        // all UPs should get a QueryStatusResponse
        for (int i = 0; i < testUPs.length; i++) {
            QueryStatusResponse stat = getFirstQueryStatus(testUPs[i]);
            assertNotNull(stat);
            assertEquals(new GUID(stat.getGUID()), queryGuid);
            assertEquals(1, stat.getNumResults());
        }

        // shut off the query....
        rs.stopQuery(queryGuid);

        // all UPs should get a QueryStatusResponse with 65535
        for (int i = 0; i < testUPs.length; i++) {
            QueryStatusResponse stat = getFirstQueryStatus(testUPs[i]);
            assertNotNull(stat);
            assertEquals(new GUID(stat.getGUID()), queryGuid);
            assertEquals(65535, stat.getNumResults());
        }
    }


    public void testAdvancedGuidance1() throws Exception {

        for (int i = 0; i < testUPs.length; i++)
            drain(testUPs[i]);
        
        // spawn a query and make sure all UPs get it
        GUID queryGuid = new GUID(rs.newQueryGUID());
        rs.query(queryGuid.bytes(), "susheel daswanu");

        for (int i = 0; i < testUPs.length; i++) {
            QueryRequest qr = getFirstQueryRequest(testUPs[i]);
            assertNotNull(qr);
            assertEquals(new GUID(qr.getGUID()), queryGuid);
        }

        // now send back results and make sure that we get a QueryStatus
        // from the leaf
        Message m = null;
        for (int i = 0; i < testUPs.length; i++) {
            Response[] res = new Response[REPORT_INTERVAL*4];
            for (int j = 0; j < res.length; j++)
                res[j] = new Response(10, 10, "susheel good"+i+j);

            m = new QueryReply(queryGuid.bytes(), (byte) 1, 6355, myIP(), 0, res,
                               GUID.makeGuid(), new byte[0], false, false, true,
                               true, false, false, null);

            testUPs[i].send(m);
            testUPs[i].flush();
        }
        
        // all UPs should get a QueryStatusResponse
        for (int i = 0; i < testUPs.length; i++) {
            for (int j = 0; j < testUPs.length; j++) {
                QueryStatusResponse stat = getFirstQueryStatus(testUPs[j]);
                assertNotNull(stat);
                assertEquals(new GUID(stat.getGUID()), queryGuid);
                assertEquals(5*(i+1), stat.getNumResults());
            }
        }
    }


    public void testAdvancedGuidance2() throws Exception {

        Message m = null;

        for (int i = 0; i < testUPs.length; i++)
            drain(testUPs[i]);
        
        // spawn a query and make sure all UPs get it
        GUID queryGuid = new GUID(rs.newQueryGUID());
        rs.query(queryGuid.bytes(), "anita kesavan");

        for (int i = 0; i < testUPs.length; i++) {
            QueryRequest qr = getFirstQueryRequest(testUPs[i]);
            assertNotNull(qr);
            assertEquals(new GUID(qr.getGUID()), queryGuid);
        }

        // now send back results and make sure that we get a QueryStatus
        // from the leaf
        Response[] res = new Response[REPORT_INTERVAL*4];
        for (int j = 0; j < res.length; j++)
            res[j] = new Response(10, 10, "anita is pretty"+j);

        m = new QueryReply(queryGuid.bytes(), (byte) 1, 6355, myIP(), 0, res,
                           GUID.makeGuid(), new byte[0], false, false, true,
                           true, false, false, null);
        
        testUPs[0].send(m);
        testUPs[0].flush();

        // all UPs should get a QueryStatusResponse
        for (int i = 0; i < testUPs.length; i++) {
            QueryStatusResponse stat = getFirstQueryStatus(testUPs[i]);
            assertNotNull(stat);
            assertEquals(new GUID(stat.getGUID()), queryGuid);
            assertEquals(REPORT_INTERVAL, stat.getNumResults());
        }


        // now send just a few responses - less than the number of
        // REPORT_INTERVAL - and confirm we don't get messages
        res = new Response[REPORT_INTERVAL-1];
        for (int j = 0; j < res.length; j++)
            res[j] = new Response(10, 10, "anita is sweet"+j);

        m = new QueryReply(queryGuid.bytes(), (byte) 1, 6355, myIP(), 0, res,
                           GUID.makeGuid(), new byte[0], false, false, true,
                           true, false, false, null);
        
        testUPs[2].send(m);
        testUPs[2].flush();

        // no UPs should get a QueryStatusResponse
        for (int i = 0; i < testUPs.length; i++) {
            QueryStatusResponse stat = getFirstQueryStatus(testUPs[i]);
            assertNull(stat);
        }

        // simply send 2 more responses....
        res = new Response[2];
        for (int j = 0; j < res.length; j++)
            res[j] = new Response(10, 10, "anita is young"+j);

        m = new QueryReply(queryGuid.bytes(), (byte) 1, 6355, myIP(), 0, res,
                           GUID.makeGuid(), new byte[0], false, false, true,
                           true, false, false, null);
        
        testUPs[1].send(m);
        testUPs[1].flush();

        // and all UPs should get a QueryStatusResponse
        for (int i = 0; i < testUPs.length; i++) {
            QueryStatusResponse stat = getFirstQueryStatus(testUPs[i]);
            assertNotNull(stat);
            assertEquals(new GUID(stat.getGUID()), queryGuid);
            assertEquals(5+((REPORT_INTERVAL+1)/4), stat.getNumResults());
        }

        // shut off the query....
        rs.stopQuery(queryGuid);

        // all UPs should get a QueryStatusResponse with 65535
        for (int i = 0; i < testUPs.length; i++) {
            QueryStatusResponse stat = getFirstQueryStatus(testUPs[i]);
            assertNotNull(stat);
            assertEquals(new GUID(stat.getGUID()), queryGuid);
            assertEquals(65535, stat.getNumResults());
        }

        // more results should not result in more status messages...
        res = new Response[REPORT_INTERVAL*2];
        for (int j = 0; j < res.length; j++)
            res[j] = new Response(10, 10, "anita is pretty"+j);

        m = new QueryReply(queryGuid.bytes(), (byte) 1, 6355, myIP(), 0, res,
                           GUID.makeGuid(), new byte[0], false, false, true,
                           true, false, false, null);
        
        testUPs[0].send(m);
        testUPs[0].flush();

        // no UPs should get a QueryStatusResponse
        for (int i = 0; i < testUPs.length; i++) {
            final int index = i;
            Thread newThread = new Thread() {
                    public void run() {
                        try {
                            QueryStatusResponse stat = 
                                getFirstQueryStatus(testUPs[index]);
                            assertNull(stat);
                        }
                        catch (Exception e) {
                            assertNull(e);
                        }
                    }
                };
            newThread.start();
        }
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

