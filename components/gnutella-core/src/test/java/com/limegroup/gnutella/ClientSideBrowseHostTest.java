package com.limegroup.gnutella;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.messages.vendor.*;
import com.limegroup.gnutella.settings.*;
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
public class ClientSideBrowseHostTest 
    extends com.limegroup.gnutella.util.BaseTestCase {
    private static final int PORT=6669;
    private static final int TIMEOUT=500;
    private static final byte[] ultrapeerIP=
        new byte[] {(byte)18, (byte)239, (byte)0, (byte)144};
    private static final byte[] oldIP=
        new byte[] {(byte)111, (byte)22, (byte)33, (byte)44};

    private static Connection testUP;
    private static RouterService rs;

    private static MyActivityCallback callback;

    public ClientSideBrowseHostTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ClientSideBrowseHostTest.class);
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
        // make sure results get through
        settings.setMinimumSearchQuality(-2);
    }        
    
    public static void globalSetUp() throws Exception {
        doSettings();

        SettingsManager settings=SettingsManager.instance();
        callback=new MyActivityCallback();
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
    
    // Tests the following behaviors:
    // ------------------------------
    // 1. that the client makes a correct direct connection if possible
    // 2. that the client makes a correct push proxy connection if necessary
    // 3. if all else fails the client sends a PushRequest

    public void testHTTPRequest() throws Exception {
        testUP = connect(rs, 6355, true);

        drain(testUP);
        // some setup
        final byte[] clientGUID = GUID.makeGuid();

        // construct and send a query        
        byte[] guid = GUID.makeGuid();
        rs.query(guid, "boalt.org");

        // the testUP should get it
        Message m = null;
        do {
            m = testUP.receive(TIMEOUT);
        } while (!(m instanceof QueryRequest)) ;

        // set up a server socket
        ServerSocket ss = new ServerSocket(7000);
        ss.setSoTimeout(TIMEOUT);

        // send a reply with some PushProxy info
        PushProxyInterface[] proxies = new QueryReply.PushProxyContainer[1];
        proxies[0] = new QueryReply.PushProxyContainer("127.0.0.1", 7000);
        Response[] res = new Response[1];
        res[0] = new Response(10, 10, "boalt.org");
        m = new QueryReply(m.getGUID(), (byte) 1, 7000, 
                           InetAddress.getLocalHost().getAddress(), 0, res, 
                           clientGUID, new byte[0], false, false, true,
                           true, false, false, null);
        testUP.send(m);
        testUP.flush();

        // wait a while for Leaf to process result
        Thread.sleep(1000);
        assertTrue(callback.getRFD() != null);

        // tell the leaf to browse host the file, should result in direct HTTP
        // request
        Thread browseThread = new Thread() {
            public void run() {
                rs.doBrowseHost(callback.getRFD().getHost(),
                                callback.getRFD().getPort(),
                                new GUID(GUID.makeGuid()), new GUID(clientGUID),
                                null);
            }
        };
        browseThread.start();

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
        assertTrue(currLine.startsWith("GET / HTTP/1.1"));
        
        // make sure the node sends the correct Host val
        currLine = reader.readLine();
        assertTrue(currLine.startsWith("Host:"));
        StringTokenizer st = new StringTokenizer(currLine, ":");
        assertEquals(st.nextToken(), "Host");
        InetAddress addr = InetAddress.getByName(st.nextToken().trim());
        Arrays.equals(addr.getAddress(), rs.getAddress());
        assertEquals(Integer.parseInt(st.nextToken()), PORT);

        // let the other side do its thing
        Thread.sleep(500);

        // send back a 200 and make sure no PushRequest is sent via the normal
        // way
        BufferedWriter writer = 
            new BufferedWriter(new
                               OutputStreamWriter(httpSock.getOutputStream()));

        writer.write("HTTP/1.1 200 OK\r\n");
        writer.flush();
        writer.write("\r\n");
        writer.flush();
        //TODO: should i send some Query Hits?  Might be a good test.
        httpSock.close();

        try {
            do {
                m = testUP.receive(TIMEOUT);
                assertTrue(!(m instanceof PushRequest));
            } while (true) ;
        }
        catch (InterruptedIOException expected) {}

        // awesome - everything checks out!
        ss.close();
    }


    public void testPushProxyRequest() throws Exception {
        drain(testUP);
        // some setup
        final byte[] clientGUID = GUID.makeGuid();

        // construct and send a query        
        byte[] guid = GUID.makeGuid();
        rs.query(guid, "nyu.edu");

        // the testUP should get it
        Message m = null;
        do {
            m = testUP.receive(TIMEOUT);
        } while (!(m instanceof QueryRequest)) ;

        // set up a server socket to wait for proxy request
        ServerSocket ss = new ServerSocket(7000);
        ss.setSoTimeout(TIMEOUT);

        // send a reply with some PushProxy info
        final PushProxyInterface[] proxies = 
            new QueryReply.PushProxyContainer[1];
        proxies[0] = new QueryReply.PushProxyContainer("127.0.0.1", 7000);
        Response[] res = new Response[1];
        res[0] = new Response(10, 10, "nyu.edu");
        m = new QueryReply(m.getGUID(), (byte) 1, 6999, 
                           InetAddress.getLocalHost().getAddress(), 0, res, 
                           clientGUID, new byte[0], false, false, true,
                           true, false, false, proxies);
        testUP.send(m);
        testUP.flush();

        // wait a while for Leaf to process result
        Thread.sleep(1000);
        assertTrue(callback.getRFD() != null);

        // tell the leaf to browse host the file, should result in PushProxy
        // request
        Thread browseThread = new Thread() {
            public void run() {
                rs.doBrowseHost(callback.getRFD().getHost(),
                                callback.getRFD().getPort(),
                                new GUID(GUID.makeGuid()), new GUID(clientGUID),
                                proxies);
            }
        };
        browseThread.start();

        // wait for the incoming PushProxy request
        Socket httpSock = ss.accept();
        assertNotNull(httpSock);

        // start reading and confirming the HTTP request
        String currLine = null;
        BufferedReader reader = 
            new BufferedReader(new
                               InputStreamReader(httpSock.getInputStream()));

        // confirm a GET/HEAD pushproxy request
        currLine = reader.readLine();
        assertTrue(currLine.startsWith("GET /gnutella/pushproxy") ||
                   currLine.startsWith("HEAD /gnutella/pushproxy"));
        
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
        Arrays.equals(addr.getAddress(), rs.getAddress());
        assertEquals(Integer.parseInt(st.nextToken()), PORT);

        // now we need to GIV
        Socket push = new Socket(InetAddress.getLocalHost(), PORT);
        BufferedWriter writer = 
            new BufferedWriter(new
                               OutputStreamWriter(push.getOutputStream()));
        writer.write("GIV 0:" + new GUID(clientGUID).toHexString() + "/\r\n");
        writer.write("\r\n");
        writer.flush();

        // confirm a BrowseHost request
        reader = 
            new BufferedReader(new
                               InputStreamReader(push.getInputStream()));
        currLine = reader.readLine();
        assertTrue(currLine.startsWith("GET / HTTP/1.1"));
        
        // make sure the node sends the correct Host val
        currLine = reader.readLine();
        assertTrue(currLine.startsWith("Host:"));
        st = new StringTokenizer(currLine, ":");
        assertEquals(st.nextToken(), "Host");
        addr = InetAddress.getByName(st.nextToken().trim());
        Arrays.equals(addr.getAddress(), rs.getAddress());
        assertEquals(Integer.parseInt(st.nextToken()), PORT);

        // let the other side do its thing
        Thread.sleep(500);

        // send back a 200 and make sure no PushRequest is sent via the normal
        // way
        writer = 
            new BufferedWriter(new
                               OutputStreamWriter(push.getOutputStream()));

        writer.write("HTTP/1.1 200 OK\r\n");
        writer.flush();
        writer.write("\r\n");
        writer.flush();
        httpSock.close();

        try {
            do {
                m = testUP.receive(TIMEOUT);
                assertTrue(!(m instanceof PushRequest));
            } while (true) ;
        }
        catch (InterruptedIOException expected) {}

        // awesome - everything checks out!
        ss.close();
    }


    public void testSendsPushRequest() throws Exception {
        drain(testUP);
        // some setup
        final byte[] clientGUID = GUID.makeGuid();

        // construct and send a query        
        byte[] guid = GUID.makeGuid();
        rs.query(guid, "anita");

        // the testUP should get it
        Message m = null;
        do {
            m = testUP.receive(TIMEOUT);
        } while (!(m instanceof QueryRequest)) ;

        // send a reply with some BAD PushProxy info
        final PushProxyInterface[] proxies = 
            new QueryReply.PushProxyContainer[1];
        proxies[0] = new QueryReply.PushProxyContainer("127.0.0.1", 7001);
        Response[] res = new Response[1];
        res[0] = new Response(10, 10, "anita");
        m = new QueryReply(m.getGUID(), (byte) 1, 7000, 
                           InetAddress.getLocalHost().getAddress(), 0, res, 
                           clientGUID, new byte[0], false, false, true,
                           true, false, false, proxies);
        testUP.send(m);
        testUP.flush();

        // wait a while for Leaf to process result
        Thread.sleep(1000);
        assertTrue(callback.getRFD() != null);

        // tell the leaf to browse host the file,
        Thread browseThread = new Thread() {
            public void run() {
                rs.doBrowseHost(callback.getRFD().getHost(),
                                callback.getRFD().getPort(),
                                new GUID(GUID.makeGuid()), new GUID(clientGUID),
                                proxies);
            }
        };
        browseThread.start();

        // nothing works for the guy, we should get a PushRequest
        do {
            m = testUP.receive(TIMEOUT*2);
        } while (!(m instanceof PushRequest)) ;

        // awesome - everything checks out!
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

    public static class MyActivityCallback extends ActivityCallbackStub {
        private RemoteFileDesc rfd = null;
        public RemoteFileDesc getRFD() {
            return rfd;
        }

        public void handleQueryResult(HostData data, 
                                                   Response response, 
                                                   List docs) {
            rfd = new RemoteFileDesc(data.getIP(), data.getPort(),
                                     response.getIndex(), 
                                     response.getName(),
                                     (int) response.getSize(), 
                                     data.getClientGUID(),
                                     0, data.isChatEnabled(), 3, false,
                                     null, null, false, 
                                     data.getPushProxies());
        }
    }


}

