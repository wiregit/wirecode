package com.limegroup.gnutella;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;

import org.limewire.util.CommonUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.PrivilegedAccessor;

import com.limegroup.gnutella.handshaking.HandshakeResponder;
import com.limegroup.gnutella.handshaking.HandshakeResponse;
import com.limegroup.gnutella.handshaking.HeaderNames;
import com.limegroup.gnutella.handshaking.NoGnutellaOkException;
import com.limegroup.gnutella.handshaking.UltrapeerHeaders;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.settings.SearchSettings;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.settings.UltrapeerSettings;
import com.limegroup.gnutella.spam.SpamManager;

/**
 * Sets up a Test Scenario of a Leaf connected to some Ultrapeers (default of
 * 4, use alternate constructor to specify up to 4)
 * The leaf has the following settings (which you can override by implementing
 * your own doSettings()): runs on SERVER_PORT, is a Leaf, does not connect on
 * startup, GWebCaches and Watchdog are inactive, sharing only txt files, 
 * sharing two txt files (berkeley and susheel), and accepting all search
 * results.
 * You must also implement getActivityCallback() (for custom callbacks) 
 * and numUPs (for the number of Ultrapeers to connect to, must be 1-4), 
 * and main and suite().
 */
@SuppressWarnings("all")
public abstract class ClientSideTestCase 
    extends com.limegroup.gnutella.util.LimeTestCase {
    public static final int SERVER_PORT = 6669;
    protected static int TIMEOUT=500;
    private static final byte[] ultrapeerIP=
        new byte[] {(byte)18, (byte)239, (byte)0, (byte)144};
    private static final byte[] oldIP=
        new byte[] {(byte)111, (byte)22, (byte)33, (byte)44};

    protected static Connection testUP[];
    protected static RouterService rs;

    private static ActivityCallback callback;
    protected static ActivityCallback getCallback() {
        return callback;
    }

    public ClientSideTestCase(String name) {
        super(name);
    }
    
    @SuppressWarnings("unused")
    private static void doSettings() throws Exception {
        String localIP = InetAddress.getLocalHost().getHostAddress();
        FilterSettings.BLACK_LISTED_IP_ADDRESSES.setValue(
            new String[] {"*.*.*.*"});
        FilterSettings.WHITE_LISTED_IP_ADDRESSES.setValue(
            new String[] {"127.*.*.*", "192.168.*.*", "10.254.*.*", localIP});        
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
        FileUtils.copy(berkeley, new File(_sharedDir, "berkeley.txt"));
        FileUtils.copy(susheel, new File(_sharedDir, "susheel.txt"));
        // make sure results get through
        SearchSettings.MINIMUM_SEARCH_QUALITY.setValue(-2);
    }        
    
    public static void globalSetUp(Class callingClass) throws Exception {
        // calls all doSettings() for me and my children
        PrivilegedAccessor.invokeAllStaticMethods(callingClass, "doSettings",
                                                  null);
        callback=
        (ActivityCallback)PrivilegedAccessor.invokeMethod(callingClass,
                                                         "getActivityCallback");
        rs=new RouterService(callback);
        RouterService.preGuiInit();
        assertEquals("unexpected port",
            SERVER_PORT, ConnectionSettings.PORT.getValue());
        rs.start();
        RouterService.clearHostCatcher();
        RouterService.connect();
        Thread.sleep(2000);
        assertEquals("unexpected port",
            SERVER_PORT, ConnectionSettings.PORT.getValue());
        connect();
        Integer numUPs = (Integer)PrivilegedAccessor.invokeMethod(callingClass,
                                                                 "numUPs");
        if ((numUPs.intValue() < 1) || (numUPs.intValue() > 4))
            throw new IllegalArgumentException("Bad value for numUPs!!!");
        testUP = new Connection[numUPs.intValue()];
        for (int i = 0; i < testUP.length; i++) {
            try {
                testUP[i] = connect(6355+i, true, callingClass);
            } catch(NoGnutellaOkException ngoke) {
                fail("couldn't connect ultrapeer: " + (i+1) +
                     ", preferred is: " + 
                     RouterService.getConnectionManager().getPreferredConnectionCount(),
                     ngoke);
            }
        }
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
                                       boolean ultrapeer,
                                       Class callingClass) 
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
         Connection con = new Connection(socket);
         con.initialize(null, responder, 1000);
         Boolean shouldReply = Boolean.TRUE;
         try {
             shouldReply = (Boolean)PrivilegedAccessor.invokeMethod(callingClass,
                                     "shouldRespondToPing");
         } catch(NoSuchMethodException ignored) {}
         
         if(shouldReply.booleanValue())
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

     /**
      * Note that this function will _EAT_ messages until it finds a ping to respond to.
      */  
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

    protected void drainAll() throws Exception {
        drainAll(testUP);
    }

    private static void shutdown() throws IOException {
        //System.out.println("\nShutting down.");
        debug("-Shutting down");
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) { }
    }
    
    /** Marks the Responses of QueryReply as NOT spam */
    protected void markAsNotSpam(QueryReply qr) throws Exception {
        SpamManager.instance().clearFilterData(); // Start from scratch
        
        Response[] resp = qr.getResultsArray();
        RemoteFileDesc rfds[] = new RemoteFileDesc[resp.length];
        for(int i = 0; i < resp.length; i++) {
            rfds[i] = resp[i].toRemoteFileDesc(qr.getHostData());
            //assertTrue(SpamManager.instance().isSpam(rfds[i]));
        }
        
        SpamManager.instance().handleUserMarkedGood(rfds);
        
        // Make sure they're not spam
        for(int i = 0; i < rfds.length; i++) {
            assertFalse(SpamManager.instance().isSpam(rfds[i]));
        }
    }
    
    protected static boolean DEBUG = false;
    protected static void debug(String message) {
        if(DEBUG) 
            System.out.println(message);
    }

    private static class UltrapeerResponder implements HandshakeResponder {
        public HandshakeResponse respond(HandshakeResponse response, 
                boolean outgoing)  {
            Properties props = new UltrapeerHeaders("127.0.0.1"); 
            props.put(HeaderNames.X_DEGREE, "42");           
            return HandshakeResponse.createResponse(props);
        }
        
        public void setLocalePreferencing(boolean b) {}
    }

    private static class OldResponder implements HandshakeResponder {
        public HandshakeResponse respond(HandshakeResponse response, 
                boolean outgoing)  {
            Properties props=new Properties();
            return HandshakeResponse.createResponse(props);
        }
        
        public void setLocalePreferencing(boolean b) {}
    }
}

