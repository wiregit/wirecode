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
 * <li>Ultrapeer guidance handled properly.
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
        System.out.println(
            "If this test doesn't pass, try disabling ConnectionWatchdog.\n"
           +"Also, beware of race conditions in the fetch code.\n");

        //Bring up application.
        SettingsManager settings=SettingsManager.instance();
        settings.setPort(6346);
        settings.setDirectories(new File[0]);
        settings.setUseQuickConnect(false);
        settings.setQuickConnectHosts(new String[0]);
        settings.setConnectOnStartup(false);
        settings.setEverSupernodeCapable(true);
        settings.setDisableSupernodeMode(false);
        settings.setForceSupernodeMode(false);
        settings.setMaxShieldedClientConnections(LEAF_CONNECTIONS);
        settings.setKeepAlive(KEEP_ALIVE);
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
        } catch (BadConnectionSettingException e) { 
            e.printStackTrace();
            Assert.that(false); 
        }

        System.out.println("\nTesting ultrapeer guidance:");
        testGuidanceI(rs);
        cleanup(rs);
        testGuidanceII(rs);
        cleanup(rs);
        testGuidanceIII(rs);
        cleanup(rs);

        testFetchI(rs, router);
        cleanup(rs);
        testFetchII(rs, router);
        cleanup(rs);
        testAcceptI(rs, router, host, port);
        cleanup(rs);
        testAcceptII(rs, router, host, port);
        cleanup(rs);
        testAcceptIII(rs, router, host, port);
    }

    private static void testFetchI(RouterService rs, TestMessageRouter router) {
        System.out.println("\nTesting fetching with no ultrapeer pongs:");
        try {
            MiniAcceptor acceptor=null;
            Connection in=null;
            final String LOCALHOST="127.0.0.1";
            rs.setKeepAlive(6);
            
            //Fetch an 0.6 connection.  Note there is a race condition here: 
            //we must start listening on the socket before the fetcher starts fetcheing
            System.out.println("-Testing that first fetched 0.6 is accepted");
            testOneFetch(rs, router, LOCALHOST, 6347, new EmptyResponder(), 200);

            System.out.println("-Testing that second fetched 0.6 is accepted");
            testOneFetch(rs, router, LOCALHOST, 6348, new EmptyResponder(), 200);

            System.out.println("-Testing that third fetched 0.6 is accepted");
            testOneFetch(rs, router, LOCALHOST, 6349, new EmptyResponder(), 200);

            System.out.println("-Testing that fourth fetched 0.6 is accepted");
            testOneFetch(rs, router, LOCALHOST, 6350, new EmptyResponder(), 200);

            rs.disconnect();
            rs.clearHostCatcher();
            rs.setKeepAlive(SettingsManager.instance().getKeepAlive());
        } catch (BadConnectionSettingException e) {
            Assert.that(false);
        }
    }


    private static void testFetchII(RouterService rs, TestMessageRouter router) {
        System.out.println("\nTesting fetching with ultrapeer pongs:");
        try {
            MiniAcceptor acceptor=null;
            Connection in=null;
            final String LOCALHOST="127.0.0.1";
            rs.setKeepAlive(6);
            
            //Fill HostCatcher with bogus pongs.
            for (int i=0; i<100; i++) 
                router.addHost("1.1.1."+i, 6340, true);

            //Fetch an 0.6 connection.  Note there is a race condition here: 
            //we must start listening on the socket before the fetcher starts fetcheing
            System.out.println("-Testing that first fetched 0.6 is accepted");
            testOneFetch(rs, router, LOCALHOST, 6351, new EmptyResponder(), 200);

            System.out.println("-Testing that second fetched 0.6 is accepted");
            testOneFetch(rs, router, LOCALHOST, 6352, new EmptyResponder(), 200);

            System.out.println("-Testing that third fetched 0.6 is rejected");
            testOneFetch(rs, router, LOCALHOST, 6353, new EmptyResponder(), 503);

            System.out.println("-Testing that fetched ultrapeer is accepted");
            testOneFetch(rs, router, LOCALHOST, 6354, new UltrapeerResponder(), 200);

            rs.disconnect();
            rs.clearHostCatcher();
            rs.setKeepAlive(SettingsManager.instance().getKeepAlive());
        } catch (BadConnectionSettingException e) {
            Assert.that(false);
        }
    }
    
    /**
     * Tests that router (un)successfully fetches a connection to host:port.
     *
     * @param rs the facade of the backend to give the pong to
     * @param router the actual message router of the backend
     * @param host the 4-byte address of the host to connect to
     * @param port the port of the host to connect to
     * @param responder how the server should respond: 503 for reject 
     *  or 200 for OK.
     */
    private static void testOneFetch(RouterService rs,
                                     TestMessageRouter router,
                                     String host, int port, 
                                     HandshakeResponder responder,
                                     int code) {
        MiniAcceptor acceptor=new MiniAcceptor(responder, port);
        Thread.yield();
        rs.connectToHostAsynchronously(host, port);
        Connection in=acceptor.accept();
        if (code==200) {
            Assert.that(in!=null);
            Assert.that(acceptor.getError()==null);
        } else if (code==503) {
            Assert.that(in==null);
            IOException e=acceptor.getError();
            Assert.that(e instanceof NoGnutellaOkException);
            Assert.that(((NoGnutellaOkException)e).getCode()==code);
        }
    }


    ///////////////////////////////////////////////////////////////////////////

    private static void testAcceptI(RouterService rs,
                                    TestMessageRouter router, 
                                    String host, int port) {
        System.out.println("\nTesting accept I:");
        //Fill HostCatcher with bogus pongs.
        for (int i=0; i<100; i++) 
            router.addHost("1.1.1."+i, 6340, true);

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

    private static void testAcceptII(RouterService rs, 
                                     TestMessageRouter router, 
                                     String host, int port) {
        System.out.println("\nTesting accept II:");
        //Fill HostCatcher with bogus pongs.
        for (int i=0; i<100; i++) 
            router.addHost("1.1.1."+i, 6340, true);

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

    private static void testAcceptIII(RouterService rs, 
                                      TestMessageRouter router, 
                                      String host, int port) {
        System.out.println("\nTesting accept III, no ultrapeer pongs:");
        rs.clearHostCatcher();
        //ignores guidance
        Connection c=testLimit(host, port, OLD_06, KEEP_ALIVE, REJECT_503);  
        testPong(c, true);
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
                e.printStackTrace();
                if (e instanceof NoGnutellaOkException) {
                    System.out.println("Code: "+((NoGnutellaOkException)e).getCode());
                }
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

    private static void testGuidanceI(RouterService rs) {
        System.out.println("-Testing normal leaf guidance");
        Assert.that(rs.isSupernode());
        Assert.that(! rs.hasClientSupernodeConnection());
        MiniAcceptor acceptor=new MiniAcceptor(
            new GuidingUltrapeerResponder(), 6340);
        Thread.yield();
        rs.connectToHostAsynchronously("localhost", 6340);
        Connection in=acceptor.accept();
        Assert.that(! rs.isSupernode());
        Assert.that(rs.hasClientSupernodeConnection()); 
        in.close();
    }

    private static void testGuidanceII(RouterService rs) {
        System.out.println("-Testing ignored leaf guidance (because of leaf)");
        //Connect one leaf
        Assert.that(rs.getNumConnections()==0);
        Assert.that(rs.isSupernode());
        Assert.that(! rs.hasClientSupernodeConnection());
        MiniAcceptor acceptor=new MiniAcceptor(
            new LeafResponder(), 6341);
        Thread.yield();
        rs.connectToHostAsynchronously("localhost", 6341);
        Connection in=acceptor.accept();
        Assert.that(in!=null);
        Assert.that(rs.isSupernode());
        Assert.that(rs.getNumConnections()==1);
        //Assert.that(rs.hasSupernodeClientConnection());


        //Now connect to ultrapeer, ignoring guidance
        acceptor=new MiniAcceptor(
            new GuidingUltrapeerResponder(), 6342);
        Thread.yield();
        rs.connectToHostAsynchronously("localhost", 6342);
        Connection in2=acceptor.accept();
        Assert.that(in2!=null);
        Assert.that(rs.isSupernode());
        //Assert.that(rs.hasSupernodeClientConnection());
        Assert.that(rs.getNumConnections()==2);

        in.close();
        in2.close();
    }


    private static void testGuidanceIII(RouterService rs) {
        System.out.println("-Testing ignored leaf guidance (because of ultrapeer)");
        //Connect one ultrapeer
        Assert.that(rs.getNumConnections()==0);
        Assert.that(rs.isSupernode());
        Assert.that(! rs.hasClientSupernodeConnection());
        MiniAcceptor acceptor=new MiniAcceptor(
            new UltrapeerResponder(), 6343);
        Thread.yield();
        rs.connectToHostAsynchronously("localhost", 6343);
        Connection in=acceptor.accept();
        Assert.that(in!=null);
        Assert.that(rs.isSupernode());
        Assert.that(rs.getNumConnections()==1);

        //Now connect to ultrapeer, ignoring guidance
        acceptor=new MiniAcceptor(
            new GuidingUltrapeerResponder(), 6344);
        Thread.yield();
        rs.connectToHostAsynchronously("localhost", 6344);
        Connection in2=acceptor.accept();
        Assert.that(in2!=null);
        Assert.that(rs.isSupernode());
        Assert.that(rs.getNumConnections()==2);

        in.close();
        in2.close();
    }


    /////////////////////////////////////////////////////////////////////

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

class GuidingUltrapeerResponder implements HandshakeResponder {
    public HandshakeResponse respond(HandshakeResponse response, 
            boolean outgoing) throws IOException {
        Properties props=new UltrapeerProperties();
        props.put(ConnectionHandshakeHeaders.X_SUPERNODE_NEEDED, "false");
        return new HandshakeResponse(props);
    }
}

class LeafResponder implements HandshakeResponder {
    public HandshakeResponse respond(HandshakeResponse response, 
            boolean outgoing) throws IOException {
        return new HandshakeResponse(new LeafProperties());
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

    public void addHost(String host, int port, boolean isUltrapeer) {
        //fake up a pong
        //PingReply pong=new PingReply(new byte[16], (byte)5,
        //                             port, host,
        //                             0l, 0l);                                     
        Endpoint e=new Endpoint(host, port);
        this._catcher.add(e, isUltrapeer);
    }
}
