package com.limegroup.gnutella;

import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.messages.vendor.*;
import com.limegroup.gnutella.settings.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.handshaking.*;
import com.limegroup.gnutella.routing.*;
import com.limegroup.gnutella.security.*;
import com.limegroup.gnutella.settings.*;
import com.limegroup.gnutella.stubs.*;
import com.limegroup.gnutella.util.*;

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
public class LeafQRPTest extends BaseTestCase {
    private static final int PORT=6669;
    private static final int TIMEOUT=500;
    private static final byte[] ultrapeerIP=
        new byte[] {(byte)18, (byte)239, (byte)0, (byte)144};
    private static final byte[] oldIP=
        new byte[] {(byte)111, (byte)22, (byte)33, (byte)44};

    private static Connection ultrapeer1;
    private static RouterService rs;

    public LeafQRPTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(LeafQRPTest.class);
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
    }        
    
    public static void globalSetUp() throws Exception {
        doSettings();

        ActivityCallback callback=new ActivityCallbackStub();
        rs=new RouterService(callback);
        assertEquals("unexpected port",
            PORT, ConnectionSettings.PORT.getValue());
        rs.start();
        rs.clearHostCatcher();
        assertEquals("unexpected port",
            PORT, ConnectionSettings.PORT.getValue());
    }        
    
    public void setUp() throws Exception  {
        doSettings();
    }
    
    public static void globalTearDown() throws Exception {
        shutdown();
    }

     ////////////////////////// Initialization ////////////////////////

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
            responder = new EmptyResponder();
        }
        
        Connection con = new Connection(socket, responder);
        con.initialize();
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
    
    // the only test - make sure the QRP table sent by the leaf is send and is
    // valid.
    public void testQRPExchange() throws Exception {
        // set up the connection
        ultrapeer1 = connect(rs, 6350, true);
        QueryRouteTable qrt = new QueryRouteTable();
        BitSet retSet = (BitSet) PrivilegedAccessor.getValue(qrt,"bitTable");
        assertEquals(0, retSet.cardinality());
        Thread.sleep(15000);
        try {
            Message m = null;
            while (true) {
                m = ultrapeer1.receive(500);
                if (m instanceof ResetTableMessage)
                    qrt.reset((ResetTableMessage) m);
                else if (m instanceof PatchTableMessage)
                    qrt.patch((PatchTableMessage) m);
            }
        }
        catch (InterruptedIOException bad) {
            // we are waiting for all messages to be processed
        }

        // get the URNS for the files
        File berkeley = 
            CommonUtils.getResourceFile("com/limegroup/gnutella/berkeley.txt");
        File susheel = 
            CommonUtils.getResourceFile("com/limegroup/gnutella/susheel.txt");
        Iterator iter = FileDesc.calculateAndCacheURN(berkeley).iterator();
        URN berkeleyURN = (URN) iter.next();
        iter = FileDesc.calculateAndCacheURN(susheel).iterator();
        URN susheelURN = (URN) iter.next();

        // send a query that should hit in the qrt
        QueryRequest query = QueryRequest.createQuery("berkeley");
        QueryRequest query2 = QueryRequest.createQuery("susheel");
        QueryRequest queryURN = QueryRequest.createQuery(berkeleyURN);
        QueryRequest queryURN2 = QueryRequest.createQuery(susheelURN);

        assertTrue(qrt.contains(query));
        assertTrue(qrt.contains(query2));
        assertTrue(qrt.contains(queryURN));
        assertTrue(qrt.contains(queryURN2));

        /* //TODO: investigate why this isn't working....
        retSet = (BitSet) PrivilegedAccessor.getValue(qrt,"bitTable");
        assertEquals(4, retSet.cardinality());
        */
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
        ultrapeer1.close();
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
}
