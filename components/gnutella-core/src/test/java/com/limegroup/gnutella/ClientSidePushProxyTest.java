package com.limegroup.gnutella;

import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.messages.vendor.*;
import com.limegroup.gnutella.settings.*;
import com.limegroup.gnutella.search.*;
import com.limegroup.gnutella.connection.Connection;
import com.limegroup.gnutella.handshaking.*;
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
public class ClientSidePushProxyTest 
    extends com.limegroup.gnutella.util.BaseTestCase {
    private static final int PORT=6669;
    private static final int TIMEOUT=1000;
    private static final byte[] ultrapeerIP=
        new byte[] {(byte)18, (byte)239, (byte)0, (byte)144};
    private static final byte[] oldIP=
        new byte[] {(byte)111, (byte)22, (byte)33, (byte)44};

    private static Connection testUP;
    private static RouterService rs;

    private static MyActivityCallback callback;

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
        RouterService.clearHostCatcher();
        RouterService.connect();
        Thread.sleep(1000);
        assertEquals("unexpected port",
            PORT, ConnectionSettings.PORT.getValue());
        connect();
    }        
    
    public void setUp() throws Exception  {
        doSettings();
    }
    
    public static void globalTearDown() throws Exception {
        shutdown();
    }

     ////////////////////////// Initialization ////////////////////////

     private static void connect() 
     throws IOException, BadPacketException {
         debug("-Establish connections");
         //Ugh, there is a race condition here from the old days when this was
         //an interactive test.  If rs connects before the listening socket is
         //created, the test will fail.

         //System.out.println("Please establish a connection to localhost:6350\n");
     }
     
     private static Connection connect(int port, 
                                       boolean ultrapeer) 
         throws Exception {
         ServerSocket ss=new ServerSocket(port);
         RouterService.connectToHostAsynchronously("127.0.0.1", port);
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
        c.writer().simpleWrite(reply);
        c.writer().flush();
     }

    ///////////////////////// Actual Tests ////////////////////////////
    
    // THIS TEST SHOULD BE RUN FIRST!!
    public void testPushProxySetup() throws Exception {

        testUP = connect(6355, true);

        // send a MessagesSupportedMessage
        testUP.writer().simpleWrite(MessagesSupportedVendorMessage.instance());
        testUP.writer().flush();

        // we expect to get a PushProxy request
        Message m = null;
        do {
            m = testUP.receive(TIMEOUT);
        } while (!(m instanceof PushProxyRequest)) ;

        // we should answer the push proxy request
        PushProxyAcknowledgement ack = 
        new PushProxyAcknowledgement(InetAddress.getLocalHost(), 
                                     6355, new GUID(m.getGUID()));
        testUP.writer().simpleWrite(ack);
        testUP.writer().flush();

        // client side seems to follow the setup process A-OK
    }

    public void testQueryReplyHasProxiesAndCanGIV() throws Exception {

        drain(testUP);

        // make sure leaf is sharing
        assertEquals(2, RouterService.getFileManager().getNumFiles());

        // send a query that should be answered
        QueryRequest query = new QueryRequest(GUID.makeGuid(), (byte) 1,
                                              "berkeley", null, null, null,
                                              null, false, 0, false);
        testUP.writer().simpleWrite(query);
        testUP.writer().flush();

        // await a response
        Message m = null;
        do {
            m = testUP.receive(TIMEOUT);
        } while (!(m instanceof QueryReply)) ;

        // confirm it has proxy info
        QueryReply reply = (QueryReply) m;
        assertNotNull(reply.getPushProxies());

        // check out PushProxy info
        Set proxies = reply.getPushProxies();
        assertEquals(1, proxies.size());
        Iterator iter = proxies.iterator();
        PushProxyInterface ppi = (PushProxyInterface)iter.next();
        assertEquals(ppi.getPushProxyPort(), 6355);
        assertTrue(ppi.getPushProxyAddress().equals(InetAddress.getLocalHost()));

        // set up a ServerSocket to get give on
        ServerSocket ss = new ServerSocket(9000);
        ss.setSoTimeout(TIMEOUT);

        // test that the client responds to a PushRequest
        PushRequest pr = new PushRequest(GUID.makeGuid(), (byte) 1, 
                                         RouterService.getMessageRouter()._clientGUID,
                                         0, 
                                         InetAddress.getLocalHost().getAddress(),
                                         9000);
        
        // send the PR off
        testUP.writer().simpleWrite(pr);
        testUP.writer().flush();

        // we should get a incoming GIV
        Socket givSock = ss.accept();
        assertNotNull(givSock);

        // start reading and confirming the HTTP request
        String currLine = null;
        BufferedReader reader = 
            new BufferedReader(new
                               InputStreamReader(givSock.getInputStream()));

        // confirm a GIV
        currLine = reader.readLine();
        String givLine = "GIV 0:" + 
        (new GUID(RouterService.getMessageRouter()._clientGUID)).toHexString();
        assertTrue(currLine.startsWith(givLine));

        // everything checks out!
        givSock.close();
        ss.close();
    }

    
    public void testHTTPRequest() throws Exception {
        drain(testUP);
        // some setup
        byte[] clientGUID = GUID.makeGuid();

        // construct and send a query        
        byte[] guid = GUID.makeGuid();
        RouterService.query(guid, "boalt.org");

        // the testUP should get it
        Message m = null;
        do {
            m = testUP.receive(TIMEOUT);
        } while (!(m instanceof QueryRequest)) ;

        // set up a server socket
        ServerSocket ss = new ServerSocket(7000);
        ss.setSoTimeout(4*TIMEOUT);

        // send a reply with some PushProxy info
        //PushProxyInterface[] proxies = new QueryReply.PushProxyContainer[1];
        Set proxies = new HashSet();
        proxies.add(new QueryReply.PushProxyContainer("127.0.0.1", 7000));
        Response[] res = new Response[1];
        res[0] = new Response(10, 10, "boalt.org");
        m = new QueryReply(m.getGUID(), (byte) 1, 6355, new byte[4], 0, res, 
                           clientGUID, new byte[0], false, false, true,
                           true, false, false, proxies);
        testUP.writer().simpleWrite(m);
        testUP.writer().flush();

        // wait a while for Leaf to process result
        Thread.sleep(1000);
        assertTrue(callback.getRFD() != null);

        // tell the leaf to download the file, should result in push proxy
        // request
        RouterService.download((new RemoteFileDesc[] { callback.getRFD() }), true);

        // wait for the incoming HTTP request
        Socket httpSock = ss.accept();
        assertNotNull(httpSock);

        // start reading and confirming the HTTP request
        String currLine = null;
        BufferedReader reader = 
            new BufferedReader(new
                               InputStreamReader(httpSock.getInputStream()));

        // confirm a GET/HEAD pushproxy request
        currLine = reader.readLine();
        assertTrue(currLine.startsWith("GET /gnutella/push-proxy") ||
                   currLine.startsWith("HEAD /gnutella/push-proxy"));
        
        // make sure it sends the correct client GUID
        int beginIndex = currLine.indexOf("ID=") + 3;
        String guidString = currLine.substring(beginIndex, beginIndex+26);
        GUID guidFromBackend = new GUID(clientGUID);
        GUID guidFromNetwork = new GUID(Base32.decode(guidString));
        assertEquals(guidFromNetwork, guidFromBackend);

        // make sure the node sends the correct X-Node
        currLine = reader.readLine();
        assertTrue(currLine.startsWith("X-Node:"));
        StringTokenizer st = new StringTokenizer(currLine, ":");
        assertEquals(st.nextToken(), "X-Node");
        InetAddress addr = InetAddress.getByName(st.nextToken().trim());
        Arrays.equals(addr.getAddress(), RouterService.getAddress());
        assertEquals(Integer.parseInt(st.nextToken()), PORT);

        // send back a 202 and make sure no PushRequest is sent via the normal
        // way
        BufferedWriter writer = 
            new BufferedWriter(new
                               OutputStreamWriter(httpSock.getOutputStream()));
        
        writer.write("HTTP/1.1 202 gobbledygook");
        writer.flush();
        httpSock.close();

        try {
            do {
                m = testUP.receive(TIMEOUT);
                assertTrue(!(m instanceof PushRequest));
            } while (true) ;
        }
        catch (InterruptedIOException expected) {}

        // now make a connection to the leaf to confirm that it will send a
        // correct download request
        Socket push = new Socket(InetAddress.getLocalHost(), PORT);
        writer = 
            new BufferedWriter(new
                               OutputStreamWriter(push.getOutputStream()));
        writer.write("GIV 0:" + new GUID(clientGUID).toHexString() + "/\r\n");
        writer.write("\r\n");
        writer.flush();
    
        reader = 
            new BufferedReader(new
                               InputStreamReader(push.getInputStream()));
        currLine = reader.readLine();
        assertEquals("GET /get/10/boalt.org HTTP/1.1", currLine);

        // awesome - everything checks out!
        push.close();
        ss.close();
    }


    public void testNoProxiesSendsPushNormal() throws Exception {
        drain(testUP);
        // some setup
        byte[] clientGUID = GUID.makeGuid();

        // construct and send a query        
        byte[] guid = GUID.makeGuid();
        RouterService.query(guid, "golf is awesome");

        // the testUP should get it
        Message m = null;
        do {
            m = testUP.receive(TIMEOUT);
        } while (!(m instanceof QueryRequest)) ;

        // send a reply with NO PushProxy info
        Response[] res = new Response[1];
        res[0] = new Response(10, 10, "golf is awesome");
        m = new QueryReply(m.getGUID(), (byte) 1, 6355, new byte[4], 0, res, 
                           clientGUID, new byte[0], false, false, true,
                           true, false, false, null);
        testUP.writer().simpleWrite(m);
        testUP.writer().flush();

        // wait a while for Leaf to process result
        Thread.sleep(1000);
        assertTrue(callback.getRFD() != null);

        // tell the leaf to download the file, should result in normal TCP
        // PushRequest
        RouterService.download((new RemoteFileDesc[] { callback.getRFD() }), 
            true);

        // await a PushRequest
        do {
            m = testUP.receive(4*TIMEOUT);
        } while (!(m instanceof PushRequest)) ;
    }


    public void testCanReactToBadPushProxy() throws Exception {
        drain(testUP);
        // some setup
        byte[] clientGUID = GUID.makeGuid();

        // construct and send a query        
        byte[] guid = GUID.makeGuid();
        RouterService.query(guid, "berkeley.edu");

        // the testUP should get it
        Message m = null;
        do {
            m = testUP.receive(TIMEOUT);
        } while (!(m instanceof QueryRequest)) ;

        // set up a server socket
        ServerSocket ss = new ServerSocket(7000);
        ss.setSoTimeout(4*TIMEOUT);

        // send a reply with some BAD PushProxy info
        //PushProxyInterface[] proxies = new QueryReply.PushProxyContainer[2];
        Set proxies = new HashSet();
        proxies.add(new QueryReply.PushProxyContainer("127.0.0.1", 7000));
        proxies.add(new QueryReply.PushProxyContainer("127.0.0.1", 8000));
        Response[] res = new Response[1];
        res[0] = new Response(10, 10, "berkeley.edu");
        m = new QueryReply(m.getGUID(), (byte) 1, 6355, new byte[4], 0, res, 
                           clientGUID, new byte[0], false, false, true,
                           true, false, false, proxies);
        testUP.writer().simpleWrite(m);
        testUP.writer().flush();

        // wait a while for Leaf to process result
        Thread.sleep(1000);
        assertTrue(callback.getRFD() != null);

        // tell the leaf to download the file, should result in push proxy
        // request
        RouterService.download((new RemoteFileDesc[] { callback.getRFD() }), 
            true);

        // wait for the incoming HTTP request
        Socket httpSock = ss.accept();
        assertNotNull(httpSock);

        // send back a error and make sure the PushRequest is sent via the normal
        // way
        BufferedWriter writer = 
            new BufferedWriter(new
                               OutputStreamWriter(httpSock.getOutputStream()));
        
        writer.write("HTTP/1.1 410 gobbledygook");
        writer.flush();
        // there is something going on with timeouts here....
        if (CommonUtils.isMacOSX() || CommonUtils.isWindows())
            Thread.sleep(300);
        httpSock.close();

        // await a PushRequest
        do {
            m = testUP.receive(TIMEOUT*8);
        } while (!(m instanceof PushRequest)) ;

        // everything checks out
        ss.close();
    }




    //////////////////////////////////////////////////////////////////

    /** Tries to receive any outstanding messages on c 
     *  @return true if this got a message */
    private boolean drain(Connection c) throws IOException {
        boolean ret=false;
        while (true) {
            try {
                c.receive(500);
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

        public void handleQueryResult(RemoteFileDesc rfd, HostData data) {
            this.rfd = rfd;
        }
    }


}

