package com.limegroup.gnutella.tests;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.xml.*;
import com.limegroup.gnutella.handshaking.*;
import com.limegroup.gnutella.security.*;
import com.limegroup.gnutella.routing.*;
import com.limegroup.gnutella.tests.stubs.*;
import com.limegroup.gnutella.gui.*;

import java.util.Properties;
import com.sun.java.util.collections.*;
import java.io.*;


/**
 * Test the following properties of ultrapeers:
 *
 * <ul>
 * <li>0.4/leaf/ultrapeer connections eventually rejected 
 *  through 503 or old way.
 * <li>pongs given out if slots for any type of connection
 * <li>ultrapeers are preferenced properly (including when fetching)
 * </ul>
 */
public class AcceptLimitTest {
    public static boolean DEBUG=false;

    /** The number of leaf connections */
    private static int LEAF_CONNECTIONS=15;
    /** The number of normal connections */
    private static int KEEP_ALIVE=6;

    static int LEAF=0;
    static int OLD_04=1;
    static int OLD_06=2;
    static int ULTRAPEER=3;

    static int REJECT_503=0;
    static int REJECT_SILENT=1;
    
    /** Prevents connections from being garbage collected */
    private static List /* of Connection */ buffer=new LinkedList();

    public static void main(String args[]) {
        String host="localhost";
        int port=6346;
        System.out.println("If this test doesn't pass, try disabling ConnectionWatchdog");

        //Bring up application.
        SettingsManager settings=SettingsManager.instance();
        settings.setPort(6346);
        settings.setUseQuickConnect(false);
        settings.setQuickConnectHosts(new String[0]);
        settings.setConnectOnStartup(false);
        settings.setEverSupernodeCapable(true);
        settings.setDisableSupernodeMode(false);
        settings.setMaxShieldedClientConnections(LEAF_CONNECTIONS);
        ActivityCallback callback=new ActivityCallbackStub();
        FileManager files=new FileManagerStub();
        TestMessageRouter router=new TestMessageRouter(callback, files);
        RouterService rs=new RouterService(callback,
                                           router,
                                           files,
                                           new DummyAuthenticator());
        rs.initialize();
        rs.clearHostCatcher();
        try {
            rs.setKeepAlive(6);
        } catch (BadConnectionSettingException e) { Assert.that(false); }
        
        testFetch(rs, router);
        cleanup(rs);
        testAcceptI(host, port);
        cleanup(rs);
        testAcceptII(host, port);
    }

    private static void testFetch(RouterService rs, TestMessageRouter router) {
        System.out.println("\nTesting fetching (may fail from race conditions):");
        try {
            MiniAcceptor acceptor=null;
            Connection in=null;
            final byte[] LOCALHOST={(byte)127, (byte)0, (byte)0, (byte)1};
            rs.setKeepAlive(0);
            
            //Fetch an 0.6 connection.  Note there is a race condition here: 
            //we must start listening on the socket before the fetcher starts fetcheing
            System.out.println("-Testing that first fetched 0.6 is accepted");
            acceptor=new MiniAcceptor(new EmptyResponder(), 6347);
            router.addHost(LOCALHOST, 6347);
            rs.setKeepAlive(1);
            in=acceptor.accept();
            Assert.that(in!=null);
            Assert.that(acceptor.getError()==null);

            //Fetch another 0.6
            System.out.println("-Testing that second fetched 0.6 is accepted");
            acceptor=new MiniAcceptor(new EmptyResponder(), 6348);
            router.addHost(LOCALHOST, 6348);
            rs.setKeepAlive(2);
            in=acceptor.accept();
            Assert.that(in!=null);
            Assert.that(acceptor.getError()==null);

            //Fetch a third...should be rejected!
            System.out.println("-Testing that third 0.6 is rejected");
            acceptor=new MiniAcceptor(new EmptyResponder(), 6349);
            router.addHost(LOCALHOST, 6349);
            Thread.yield();
            rs.setKeepAlive(3);
            in=acceptor.accept();
            Assert.that(in==null);
            IOException e=acceptor.getError();
            Assert.that(e instanceof NoGnutellaOkException);
            Assert.that(((NoGnutellaOkException)e).getCode()==503);

            //Fetch a fourth ultrapeer.  Should be allowed
            System.out.println("-Testing that fourth ultrapeer allowed");
            acceptor=new MiniAcceptor(new UltrapeerResponder(), 6350);
            router.addHost(LOCALHOST, 6350);
            Thread.yield();
            rs.setKeepAlive(3);
            in=acceptor.accept();
            Assert.that(in!=null);
            Assert.that(acceptor.getError()==null);

            rs.disconnect();
            rs.clearHostCatcher();
            rs.setKeepAlive(SettingsManager.instance().getKeepAlive());
        } catch (BadConnectionSettingException e) {
            Assert.that(false);
        }
    }

    private static void testAcceptI(String host, int port) {
        System.out.println("\nTesting accept I:");
        Connection c=testLimit(host, port, LEAF, LEAF_CONNECTIONS, REJECT_503);        
        testPong(c, true);        
        testLimit(host, port, OLD_06, 
                  ConnectionManager.DESIRED_OLD_CONNECTIONS, 
                  REJECT_503);  //save slots for ultrapers
        testPong(c, true);
        testLimit(host, port, ULTRAPEER, 
                  KEEP_ALIVE-ConnectionManager.DESIRED_OLD_CONNECTIONS, 
                  REJECT_503);       
        testPong(c, false);
    }        

    private static void testAcceptII(String host, int port) {
        System.out.println("\nTesting accept II:");
        //ignores guidance
        Connection c=testLimit(host, port, ULTRAPEER, KEEP_ALIVE, REJECT_SILENT); 
        testPong(c, true);
        testLimit(host, port, OLD_06, 0, REJECT_503);  
        testPong(c, true);
        testLimit(host, port, OLD_04, 0, REJECT_SILENT);         //no handshaking  
        testPong(c, true); //Still have leaf slots
        c=testLimit(host, port, LEAF, LEAF_CONNECTIONS, REJECT_503);        
        testPong(c, false);
    }

    /** 
     * Checks that HOST:PORT will accept LIMIT connections of the given type,
     * returning the last successful one, or null if none.
     *
     * @param host the host to connect to
     * @param port the port to conenct to
     * @param type the type of connection to establish: LEAF, OLD_04, OLD_06,
     *  or ULTRAPEER
     * @param limit the expected number of connections
     * @param rejectType what sort of rejection to expect when over the limit:
     *  REJECT_503 (during handshaking) or REJECT_SILENT (during messaging)
     */
    private static Connection testLimit(String host, int port, int type, 
                                        int limit, int rejectType) {
        String description=null;
        if (type==OLD_04)
            description="old/0.4";
        else if (type==OLD_06)
            description="old/0.6";
        else if (type==LEAF)
            description="leaf";
        else if (type==ULTRAPEER)
            description="ultrapeer";
        else
            Assert.that(false, "Bad type: "+type);
        System.out.println("-Testing limit of "+limit+" "+description+" connections");

        //Try to establish LIMIT connections
        Connection ret=null;
        for (int i=0; i<limit; i++) {
            try {
                ret=connect(host, port, type);
            } catch (IOException e) {
                Assert.that(false, "Connection "+i+" disallowed");
            }
            buffer.add(ret);
        }

        //Check that next connection return 503.
        try {            
            Connection tmp=connect(host, port, type);
            Assert.that(false, "Extra connection allowed");
        } catch (NoGnutellaOkException e) {
            Assert.that(rejectType==REJECT_503 
                        && e.getCode()==HandshakeResponse.SLOTS_FULL, 
                        "Unexpected code: "+e.getCode());
        } catch (IOException e) {
            Assert.that(rejectType==REJECT_SILENT, "Mysterious IO exception: "+e);
        }
        
        return ret;
    }

    /** 
     * Returns a connection of the given type to host:port, 
     * or null if it failed.
     *   @param type one of t
     */
    private static Connection connect(String host, int port, int type) 
            throws IOException {
        Connection ret=null;
        if (type==OLD_04)
            ret=new Connection(host, port);
        else if (type==OLD_06)
            ret=new Connection(host, port, 
                               new Properties(),
                               new EmptyResponder(),
                               false);
        else if (type==LEAF)
            ret=new Connection(host, port, 
                               new LeafProperties(),
                               new EmptyResponder(),
                               false);
        else if (type==ULTRAPEER)
            ret=new Connection(host, port, 
                               new UltrapeerProperties(),
                               new EmptyResponder(),
                               false);
        else
            Assert.that(false, "Bad type: "+type);
        
        ret.initialize();
        try {
            ret.send(new PingRequest((byte)7));
            ret.flush();
            Message m=ret.receive();
        } catch (BadPacketException e) {
            throw new IOException("Couldn't read pong");
        }
        return ret;
    }

    /** Tests whether c responds or doesn't responds to a ping */
    private static void testPong(Connection c, boolean expectPong) {
        System.out.println("-Testing that host "
                           +(expectPong? "sends" : "does NOT")
                           +" reply to ping");
//          //Avoid being a victim of the duplicate filter
//          try {
//              Thread.sleep(4000);
//          } catch (InterruptedException e) {
//          }

        try {
            drain(c);
            c.send(new PingRequest((byte)7));
            c.flush();
            Message m=null;
            try {
                m=c.receive(500);
            } catch (InterruptedIOException e) {
                Assert.that(! expectPong);
                return;
            }
            Assert.that(m instanceof PingReply, 
                        "Got message of wrong type: "+m);
            Assert.that(expectPong, "Wasn't expecting a pong");
        } catch (IOException e) {
            e.printStackTrace();
            Assert.that(false, "Unexpected IOException");
        } catch (BadPacketException e) {
            Assert.that(false, "Unexpected BadPacketException");
        }
    }

    /** Tries to receive any outstanding messages on c 
     *  @return true if this got a message */
    private static boolean drain(Connection c) throws IOException {
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

    /** Closes all connections. */
    private static void cleanup(RouterService rs) {
        //Close on client
        for (Iterator iter=buffer.iterator(); iter.hasNext(); ) {
            Connection c=(Connection)iter.next();
            c.close();
        }
        try {
            //Give server time to cleanup connections.
            Thread.sleep(200);
        } catch (InterruptedException e) { }
    }
}

    

class LeafProperties extends Properties {
    public LeafProperties() {
        put(ConnectionHandshakeHeaders.X_QUERY_ROUTING, "0.1");
        put(ConnectionHandshakeHeaders.X_SUPERNODE, "False");
    }
}

class UltrapeerProperties extends Properties {
    public UltrapeerProperties() {
        put(ConnectionHandshakeHeaders.X_QUERY_ROUTING, "0.1");
        put(ConnectionHandshakeHeaders.X_SUPERNODE, "true");
    }
}

class UltrapeerResponder implements HandshakeResponder {
    public HandshakeResponse respond(HandshakeResponse response, 
            boolean outgoing) throws IOException {
        return new HandshakeResponse(new UltrapeerProperties());
    }
}

class EmptyResponder implements HandshakeResponder {
    public HandshakeResponse respond(HandshakeResponse response, 
            boolean outgoing) throws IOException {
        return new HandshakeResponse(new Properties());
    }
}

class DebugActivityCallback extends ActivityCallbackStub {
    public void connectionInitializing(Connection c) { 
        if (AcceptLimitTest.DEBUG)
            System.out.println("Initializing "+str(c));
    }
    public void connectionInitialized(Connection c) { 
        //System.out.println("Initialized "+str(c));
    }
    public void connectionClosed(Connection c) { 
        if (AcceptLimitTest.DEBUG)
            System.out.println("CLOSED "+str(c));
    }
    private static String str(Connection c) {
        String ultrapeer="old";
        String ultrapeerProp=c.getProperty(ConnectionHandshakeHeaders.X_SUPERNODE);
        if (ultrapeerProp!=null) {
            if (ultrapeerProp.toLowerCase().equals("true")) 
                ultrapeer="ultrapeer";
            else if (ultrapeerProp.toLowerCase().equals("false")) 
                ultrapeer="leaf";
        }
        return "["+c.getInetAddress()+", "+ultrapeer+"]";
    }
}

/**
 * Provides HostCatcher access.
 */ 
class TestMessageRouter extends MetaEnabledMessageRouter {
    public TestMessageRouter(ActivityCallback callback, FileManager files) {
        super(callback, files);
    }

    public void addHost(byte[] host, int port) {
        //fake up a pong
        //PingReply pong=new PingReply(new byte[16], (byte)5,
        //                             port, host,
        //                             0l, 0l);                                     
        Endpoint e=new Endpoint(host, port);
        this._catcher.add(e, false);
    }
}
