package com.limegroup.gnutella;

import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.settings.*;
import com.limegroup.gnutella.search.*;
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
 * Sets up a Test Scenario of a Leaf connected to some Ultrapeers (default of
 * 4, use alternate constructor to specify up to 4)
 * The leaf has the following settings (which you can override by implementing
 * your own doSettings()): runs on SERVER_PORT, is a Leaf, does not connect on
 * startup, GWebCaches and Watchdog are inactive, sharing only txt files, 
 * sharing two txt files (berkeley and susheel), and accepting all search
 * results.
 * You must also implement getActivityCallback() (for custom callbacks) 
 * and numUPs (for the number of Ultrapeers to connect to, must be 1-4).
 */
public abstract class ClientSideTestCase 
    extends com.limegroup.gnutella.util.BaseTestCase {
    public static final int SERVER_PORT = 6669;
    protected static final int TIMEOUT=500;
    private static final byte[] ultrapeerIP=
        new byte[] {(byte)18, (byte)239, (byte)0, (byte)144};
    private static final byte[] oldIP=
        new byte[] {(byte)111, (byte)22, (byte)33, (byte)44};

    protected static Connection testUP[];
    private static RouterService rs;

    private static ActivityCallback callback;
    protected static ActivityCallback getCallback() {
        return callback;
    }

    public ClientSideTestCase(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ClientSideTestCase.class);
    }    

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    private static void doSettings() {
        //Setup LimeWire backend.  For testing other vendors, you can skip all
        //this and manually configure a client in leaf mode to listen on port
        //6669, with no slots and no connections.  But you need to re-enable
        //the interactive prompts below.
        ConnectionSettings.PORT.setValue(SERVER_PORT);
		ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
		UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(false);
		UltrapeerSettings.DISABLE_ULTRAPEER_MODE.setValue(true);
		UltrapeerSettings.FORCE_ULTRAPEER_MODE.setValue(false);
		ConnectionSettings.NUM_CONNECTIONS.setValue(0);
		ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
		SharingSettings.EXTENSIONS_TO_SHARE.setValue("txt;");
		ConnectionSettings.USE_GWEBCACHE.setValue(false);
		ConnectionSettings.WATCHDOG_ACTIVE.setValue(false);
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
    
    public static void globalSetUp(Class callingClass) throws Exception {
        System.out.println("Calling class is " + callingClass);
        // calls all doSettings() for me and my parents
        PrivilegedAccessor.invokeAllStaticMethods(callingClass, "doSettings",
                                                  null);
        callback=
        (ActivityCallback)PrivilegedAccessor.invokeMethod(callingClass,
                                                         "getActivityCallback",
                                                         null);
        rs=new RouterService(callback);
        assertEquals("unexpected port",
            SERVER_PORT, ConnectionSettings.PORT.getValue());
        rs.start();
        RouterService.clearHostCatcher();
        RouterService.connect();
        Thread.sleep(1000);
        assertEquals("unexpected port",
            SERVER_PORT, ConnectionSettings.PORT.getValue());
        connect();
        Integer numUPs = (Integer)PrivilegedAccessor.invokeMethod(callingClass,
                                                                 "numUPs", null);
        if ((numUPs.intValue() < 1) || (numUPs.intValue() > 4))
            throw new IllegalArgumentException("Bad value for numUPs!!!");
        testUP = new Connection[numUPs.intValue()];
        for (int i = 0; i < testUP.length; i++)
            testUP[i] = connect(6355+i, true);        
    }        
    
    public void setUp() throws Exception  {
        // calls all doSettings() for me and my parents
        PrivilegedAccessor.invokeAllStaticMethods(this.getClass(), "doSettings",
                                                  null);
    }
    
    public static void globalTearDown() throws Exception {
        shutdown();
    }

     ////////////////////////// Initialization ////////////////////////

     private static void connect() 
         throws IOException, BadPacketException {
         debug("-Establish connections");

     }
     
     private static Connection connect(int port, 
                                       boolean ultrapeer) 
         throws IOException, BadPacketException, Exception {
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
        c.send(reply);
        c.flush();
    }

    ///////////////////////// NO Actual Tests ////////////////////////////
    //////////////////////////////////////////////////////////////////

    private static void shutdown() throws IOException {
        //System.out.println("\nShutting down.");
        debug("-Shutting down");
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) { }
    }

    protected static boolean DEBUG = false;
    protected static void debug(String message) {
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
}

