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
 * Tests that What is new support is fully functional.  We use a leaf here - we
 * assume that an Ultrapeer will be equally functional.
 */
public class ServerSideWhatIsNewTest 
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

    private static File berkeley = null;
    private static File susheel = null;


    public ServerSideWhatIsNewTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ServerSideWhatIsNewTest.class);
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
        berkeley = 
            CommonUtils.getResourceFile("com/limegroup/gnutella/berkeley.txt");
        susheel = 
            CommonUtils.getResourceFile("com/limegroup/gnutella/susheel.txt");
        // now move them to the share dir
        CommonUtils.copy(berkeley, new File(_sharedDir, "berkeley.txt"));
        CommonUtils.copy(susheel, new File(_sharedDir, "susheel.txt"));
        berkeley = new File(_sharedDir, "berkeley.txt");
        susheel = new File(_sharedDir, "susheel.txt");
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
         //         replyToPing(con, ultrapeer);
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
    
    // THIS TEST SHOULD BE RUN FIRST!!
    public void testSendsCapabilitiesMessage() throws Exception {

        testUP = connect(rs, 6355, true);

        // send a MessagesSupportedMessage and capabilities VM
        testUP.send(MessagesSupportedVendorMessage.instance());
        testUP.send(CapabilitiesVM.instance());
        testUP.flush();

        // we expect to get 
        Message m = null;
        do {
            m = testUP.receive(TIMEOUT);
        } while (!(m instanceof CapabilitiesVM)) ;
        assertTrue(((CapabilitiesVM)m).supportsWhatIsNew());

        // client side seems to follow the setup process A-OK
    }


    public void testCreationTimeCacheInitialState() throws Exception {
        // wait for fm to finish
        int i = 0;
        for (; (i < 15) && (rs.getNumSharedFiles() < 2); i++)
            Thread.sleep(1000);
        if (i == 15) assertTrue(false);
        

        CreationTimeCache ctCache = CreationTimeCache.instance();
        FileManager fm = rs.getFileManager();
        URN berkeleyURN = fm.getURNForFile(berkeley);
        URN susheelURN = fm.getURNForFile(susheel);

        {
            Map urnToLong = 
                (Map)PrivilegedAccessor.getValue(ctCache, "URN_TO_TIME_MAP");
            assertEquals(2, urnToLong.size());
            assertNotNull(""+urnToLong, urnToLong.get(berkeleyURN));
            assertNotNull(""+urnToLong, urnToLong.get(susheelURN));
        }

        {
            Map longToUrns =
                (Map)PrivilegedAccessor.getValue(ctCache, "TIME_TO_URNSET_MAP");
            if (longToUrns.size() == 1) {
                Iterator iter = longToUrns.entrySet().iterator();
                Set urnSet = (Set)((Map.Entry)iter.next()).getValue();
                assertTrue(urnSet.contains(berkeleyURN));
                assertTrue(urnSet.contains(susheelURN));
                assertEquals(2, urnSet.size());
            }
            else if (longToUrns.size() == 2) {
                Iterator iter = longToUrns.entrySet().iterator();
                Set urnSet = (Set)((Map.Entry)iter.next()).getValue();
                assertTrue(
                           ( urnSet.contains(berkeleyURN) && 
                             !urnSet.contains(susheelURN) )
                           ||
                           ( !urnSet.contains(berkeleyURN) && 
                             urnSet.contains(susheelURN) )
                           );
                assertEquals(1, urnSet.size());
                urnSet = (Set)((Map.Entry)iter.next()).getValue();
                assertTrue(
                           ( urnSet.contains(berkeleyURN) && 
                             !urnSet.contains(susheelURN) )
                           ||
                           ( !urnSet.contains(berkeleyURN) && 
                             urnSet.contains(susheelURN) )
                           );
                assertEquals(1, urnSet.size());
            }
            else assertTrue("Bad Cache!", false);
        }
    }


    public void testWhatIsNewQuery() throws Exception {
        drain(testUP);

        QueryRequest whatIsNewQuery = 
            new QueryRequest(GUID.makeGuid(), (byte)2, 
                             QueryRequest.WHAT_IS_NEW_QUERY_STRING, "", null, 
                             null, null, false, Message.N_UNKNOWN, false, true);
        whatIsNewQuery.hop();
        testUP.send(whatIsNewQuery);
        testUP.flush();

        // give time to process
        Thread.sleep(1000);

        QueryReply reply = 
            (QueryReply) getFirstInstanceOfMessageType(testUP,
                                                       QueryReply.class);
        assertNotNull(reply);
        assertEquals(2, reply.getResultCount());
        Iterator iter = reply.getResults();
        Response currResp = (Response) iter.next();
        assertTrue(currResp.getName().equals("berkeley.txt") ||
                   currResp.getName().equals("susheel.txt"));
        currResp = (Response) iter.next();
        assertTrue(currResp.getName().equals("berkeley.txt") ||
                   currResp.getName().equals("susheel.txt"));
        assertFalse(iter.hasNext());
    }


    public void testAddSharedFiles() throws Exception {
        CreationTimeCache ctCache = CreationTimeCache.instance();
        FileManager fm = rs.getFileManager();
        URN berkeleyURN = fm.getURNForFile(berkeley);
        URN susheelURN = fm.getURNForFile(susheel);

        File tempFile1 = new File("tempFile1.txt");
        File tempFile2 = new File("tempFile2.txt");
        tempFile1.deleteOnExit(); tempFile2.deleteOnExit();

        FileWriter writer = null;
        {
            writer = new FileWriter(tempFile1);
            writer.write(tempFile1.getName(), 0, tempFile1.getName().length());
            writer.flush();
            writer.close();
        }
        {
            writer = new FileWriter(tempFile2);
            writer.write(tempFile2.getName(), 0, tempFile2.getName().length());
            writer.flush();
            writer.close();
        }
        
        // now move them to the share dir
        CommonUtils.copy(tempFile1, new File(_sharedDir, "tempFile1.txt"));
        CommonUtils.copy(tempFile2, new File(_sharedDir, "tempFile2.txt"));
        tempFile1 = new File(_sharedDir, "tempFile1.txt");
        tempFile2 = new File(_sharedDir, "tempFile2.txt");
        assertTrue(tempFile1.exists());
        assertTrue(tempFile2.exists());
        
        rs.getFileManager().loadSettings(false);
        int i = 0;
        for (; (i < 15) && (rs.getNumSharedFiles() < 4); i++)
            Thread.sleep(1000);
        if (i == 15) assertTrue("num shared files? " + rs.getNumSharedFiles(),
                                false);

        URN tempFile1URN = fm.getURNForFile(tempFile1);
        URN tempFile2URN = fm.getURNForFile(tempFile2);
        {
            Map urnToLong = 
                (Map)PrivilegedAccessor.getValue(ctCache, "URN_TO_TIME_MAP");
            assertEquals(4, urnToLong.size());
            assertNotNull(""+urnToLong, urnToLong.get(berkeleyURN));
            assertNotNull(""+urnToLong, urnToLong.get(susheelURN));
            assertNotNull(""+urnToLong, urnToLong.get(tempFile1URN));
            assertNotNull(""+urnToLong, urnToLong.get(tempFile2URN));
        }
        {
            Map longToUrns =
                (Map)PrivilegedAccessor.getValue(ctCache, "TIME_TO_URNSET_MAP");
            assertTrue((longToUrns.size() == 2) || (longToUrns.size() == 3) ||
                       (longToUrns.size() == 4));
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
        return new byte[] { (byte)192, (byte)168, 0, 1 };
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

