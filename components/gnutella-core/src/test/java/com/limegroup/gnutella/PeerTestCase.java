package com.limegroup.gnutella;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;

import org.limewire.util.PrivilegedAccessor;

import com.limegroup.gnutella.handshaking.HandshakeResponder;
import com.limegroup.gnutella.handshaking.HandshakeResponse;
import com.limegroup.gnutella.handshaking.HeaderNames;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.settings.SearchSettings;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.settings.UltrapeerSettings;
import com.limegroup.gnutella.util.LimeTestCase;
import com.limegroup.gnutella.util.SocketsManager.ConnectType;

/**
 * Allows a testcase to easily interact with a fully running LimeWire.
 */
public abstract class PeerTestCase extends LimeTestCase {
    
    public static final int SERVER_PORT = 6669;
    
    protected static int TIMEOUT=500;
    
    private static final byte[] ultrapeerIP=
        new byte[] {(byte)18, (byte)239, (byte)0, (byte)144};
    
    private static final byte[] oldIP=
        new byte[] {(byte)111, (byte)22, (byte)33, (byte)44};

    protected static RouterService rs;

    private static ActivityCallback callback;
    protected static ActivityCallback getCallback() {
        return callback;
    }

    public PeerTestCase(String name) {
        super(name);
    }
    
    @SuppressWarnings("unused")
    private static void doSettings() throws Exception {
        String localIP = InetAddress.getLocalHost().getHostAddress();
        FilterSettings.BLACK_LISTED_IP_ADDRESSES.setValue(
            new String[] {"*.*.*.*"});
        FilterSettings.WHITE_LISTED_IP_ADDRESSES.setValue(
            new String[] {"127.*.*.*", "192.168.*.*", "10.254.*.*", localIP});        
        ConnectionSettings.PORT.setValue(SERVER_PORT);
        ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
        UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(false);
        UltrapeerSettings.DISABLE_ULTRAPEER_MODE.setValue(true);
        UltrapeerSettings.FORCE_ULTRAPEER_MODE.setValue(false);
        ConnectionSettings.NUM_CONNECTIONS.setValue(0);
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        SharingSettings.EXTENSIONS_TO_SHARE.setValue("txt;");
        ConnectionSettings.WATCHDOG_ACTIVE.setValue(false);
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
    }        
    
    public void setUp() throws Exception  {
        // calls all doSettings() for me and my parents
        PrivilegedAccessor.invokeAllStaticMethods(this.getClass(), "doSettings",
                                                  null);
    }

    protected static Connection connect(boolean ultrapeer) throws Exception {
         ServerSocket ss=new ServerSocket();
         ss.setReuseAddress(true);
         ss.bind(new InetSocketAddress(0));
         RouterService.connectToHostAsynchronously("127.0.0.1", ss.getLocalPort(), ConnectType.PLAIN);
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
        ProviderHacks.getPingReplyFactory().createExternal(guid, (byte)7,
                                 socket.getLocalPort(), 
                                 ultrapeer ? ultrapeerIP : oldIP,
                                 ultrapeer);
        reply.hop();
        c.send(reply);
        c.flush();
    }

    private static class UltrapeerResponder implements HandshakeResponder {
        public HandshakeResponse respond(HandshakeResponse response, 
                boolean outgoing)  {
            Properties props = ProviderHacks.getHeadersFactory().createUltrapeerHeaders("127.0.0.1"); 
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

