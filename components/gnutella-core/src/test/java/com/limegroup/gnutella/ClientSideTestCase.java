package com.limegroup.gnutella;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;

import org.limewire.net.SocketsManager.ConnectType;
import org.limewire.util.CommonUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.PrivilegedAccessor;

import com.limegroup.gnutella.connection.BlockingConnection;
import com.limegroup.gnutella.connection.BlockingConnectionFactory;
import com.google.inject.Injector;
import com.limegroup.gnutella.handshaking.HandshakeResponder;
import com.limegroup.gnutella.handshaking.HandshakeResponse;
import com.limegroup.gnutella.handshaking.HeaderNames;
import com.limegroup.gnutella.handshaking.HeadersFactory;
import com.limegroup.gnutella.handshaking.NoGnutellaOkException;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingReplyFactory;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.settings.SearchSettings;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.settings.UltrapeerSettings;
import com.limegroup.gnutella.spam.SpamManager;
import com.limegroup.gnutella.util.LimeTestCase;
import com.limegroup.gnutella.xml.LimeXMLDocumentFactory;

/**
 * Sets up a Test Scenario of a Leaf connected to some Ultrapeers (default of
 * 4, use {@link #getNumberOfPeers()} to override that number).
 * 
 * Subclasses need to call {@link #setUp(Injector)} and pass in their configured
 * injector.
 * 
 * The leaf has the following settings (which you can override by implementing
 * your own setSettings()): runs on SERVER_PORT, is a Leaf, does not connect on
 * startup, Watchdog are inactive, sharing only txt files, 
 * sharing two txt files (berkeley and susheel), and accepting all search
 * results.
 * and {@link #getNumberOfPeers()} (for the number of Ultrapeers to connect to, must be 1-4), 
 * and main and suite().
 */
@SuppressWarnings("all")
public abstract class ClientSideTestCase extends LimeTestCase {
    
    public final int SERVER_PORT = 6669;
    protected int TIMEOUT=500;
    private final byte[] ultrapeerIP=
        new byte[] {(byte)18, (byte)239, (byte)0, (byte)144};
    private final byte[] oldIP=
        new byte[] {(byte)111, (byte)22, (byte)33, (byte)44};


    protected BlockingConnection testUP[];

    private ActivityCallback callback;
    private LifecycleManager lifecycleManager;
    private ConnectionServices connectionServices;
    private PingReplyFactory pingReplyFactory;
    private BlockingConnectionFactory connectionFactory;
    private SpamManager spamManager;
    private HeadersFactory headersFactory;
    private LimeXMLDocumentFactory instance;
    
    public ClientSideTestCase(String name) {
        super(name);
    }
    
    @SuppressWarnings("unused")
    public final void doSettings() throws Exception {
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
        
        setSettings();
    }        
    
    /**
     * Can be overridden by subclasses and is called from {@link #setUp(Injector)}.
     * @throws Exception 
     */
    public void setSettings() throws Exception {
    }
    
    /**
     * Can be overriden in subclasses that need a different number of ultrapeers.
     * 
     * Must be >= 1 && <= 4.
     * 
     * @return the number ultrapeers that should be instantiated, by default 4
     */
    public int getNumberOfPeers() {
        return 4;
    }
    
    /**
     * Can be overridden. Returns <code>true</code> by default.
     */
    protected boolean shouldRespondToPing() {
        return true;
    }
    
    @Override
    protected void setUp() throws Exception {
        this.setUp(LimeTestUtils.createInjector());
    }
    
    public void setUp(Injector injector) throws Exception {
        doSettings();
        
        assertEquals("unexpected port", SERVER_PORT, ConnectionSettings.PORT.getValue());
        
        lifecycleManager = injector.getInstance(LifecycleManager.class);
        connectionServices = injector.getInstance(ConnectionServices.class);
        ConnectionManager connectionManager = injector.getInstance(ConnectionManager.class);
        pingReplyFactory = injector.getInstance(PingReplyFactory.class);
        connectionFactory = injector.getInstance(BlockingConnectionFactory.class);
        spamManager = injector.getInstance(SpamManager.class);
        headersFactory = injector.getInstance(HeadersFactory.class);
        instance = injector.getInstance(LimeXMLDocumentFactory.class);

        lifecycleManager.start();
        connectionServices.connect();

        assertEquals("unexpected port", SERVER_PORT, ConnectionSettings.PORT.getValue());
        int numUPs = getNumberOfPeers();
        
        if (numUPs < 1 || numUPs > 4)
            throw new IllegalArgumentException("Bad value for numUPs!!!");
        testUP = new BlockingConnection[numUPs];
        for (int i = 0; i < testUP.length; i++) {
            try {
                testUP[i] = connect(6355+i, true);
            } catch(NoGnutellaOkException ngoke) {
                fail("couldn't connect ultrapeer: " + (i+1) +
                     ", preferred is: " + 
                     connectionManager.getPreferredConnectionCount(),
                     ngoke);
            }
        }
    }        
    
    @Override
    protected void tearDown() throws Exception {
        if (lifecycleManager != null) {
            lifecycleManager.shutdown();
        }
        if (connectionServices != null) {
            connectionServices.disconnect();
        }
    }
    
     ////////////////////////// Initialization ////////////////////////


     private BlockingConnection connect(int port, boolean ultrapeer)
         throws IOException, BadPacketException, Exception {
         ServerSocket ss=new ServerSocket(port);
         connectionServices.connectToHostAsynchronously("127.0.0.1", port, ConnectType.PLAIN);
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
    BlockingConnection con = connectionFactory.createConnection(socket);
         con.initialize(null, responder, 1000);
         if (shouldRespondToPing()) {
             replyToPing(con, ultrapeer);
         }
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
    private void replyToPing(BlockingConnection c, boolean ultrapeer)
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
        
        Socket socket = c.getSocket();
        PingReply reply = pingReplyFactory.createExternal(guid, (byte)7,
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

    /** Marks the Responses of QueryReply as NOT spam */
    protected void markAsNotSpam(QueryReply qr) throws Exception {
        spamManager.clearFilterData(); // Start from scratch
        
        Response[] resp = qr.getResultsArray();
        RemoteFileDesc rfds[] = new RemoteFileDesc[resp.length];
        for(int i = 0; i < resp.length; i++) {
            rfds[i] = resp[i].toRemoteFileDesc(qr.getHostData());
            //assertTrue(SpamManager.instance().isSpam(rfds[i]));
        }
        
        spamManager.handleUserMarkedGood(rfds);
        
        // Make sure they're not spam
        for(int i = 0; i < rfds.length; i++) {
            assertFalse(spamManager.isSpam(rfds[i]));
        }
    }
    
    private class UltrapeerResponder implements HandshakeResponder {
        public HandshakeResponse respond(HandshakeResponse response, 
                boolean outgoing)  {
            Properties props = headersFactory.createUltrapeerHeaders("127.0.0.1"); 
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

