package com.limegroup.gnutella.tests;

import com.limegroup.gnutella.*;
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
 * -0.4/leaf/ultrapeer connections eventually rejected 
 *  (through 503 or old way.  Do we insist that leaves are given 503?)
 * -pongs given out if slots for any type of connection
 */
public class AcceptLimitTest {
    /** Assume that no host allows more than this many connections. */
    private static int MAX_CONNECTIONS=100;
    
    static int LEAF=0;
    static int OLD=1;
    static int ULTRAPEER=2;
    
    /** Prevents connections from being garbage collected */
    private static List /* of Connection */ buffer=new LinkedList();

    public static void main(String args[]) {
        String host="localhost";
        int port=6346;

        //Bring up application.
        SettingsManager settings=SettingsManager.instance();
        settings.setPort(6346);
        settings.setQuickConnectHosts(new String[0]);
        settings.setConnectOnStartup(true);
        settings.setConnectOnStartup(false);
        settings.setEverSupernodeCapable(true);
        settings.setDisableSupernodeMode(true);
        settings.setMaxShieldedClientConnections(25);
        RouterService rs=new RouterService(new DebugActivityCallback(),
                                           new MessageRouterStub(),
                                           new FileManagerStub(),
                                           new DummyAuthenticator());
        rs.initialize();
        rs.clearHostCatcher();
        try {
            rs.setKeepAlive(6);
        } catch (BadConnectionSettingException e) { Assert.that(false); }

        //You can reorder these statements (with small changes) in creative ways.
        Connection c=testLimit(host, port, LEAF);        
        testPong(c, true);
        testLimit(host, port, OLD);        
        testPong(c, true);
        testLimit(host, port, ULTRAPEER);        
        testPong(c, false);
    }

    /** 
     * Sees how many connections we can make of the given type, returning the
     * last successful one, or null if none.
     */
    private static Connection testLimit(String host, int port, int type) {
        String description=null;
        if (type==OLD)
            description="old";
        else if (type==LEAF)
            description="leaf";
        else if (type==ULTRAPEER)
            description="ultrapeer";
        else
            Assert.that(false, "Bad type: "+type);

        Connection ret=null;
        int i=0;
        while (i<MAX_CONNECTIONS) {
            Connection tmp=connect(host, port, type);
            if (tmp==null)
                break;
            ret=tmp;
            buffer.add(ret);
            i++;           
        }
        
        //Assert.that(i>0, "Couldn't create any "+description+" connections");
        Assert.that(i<MAX_CONNECTIONS, 
            "There appears to be no limit on "+description+" connections");
        System.out.println("-Established "+i+" "+description
                           +" connections.  Is that right?");
        return ret;
    }

    /** 
     * Returns a connection of the given type to host:port, 
     * or null if it failed.
     *   @param type one of t
     */
    private static Connection connect(String host, int port, int type) {
        Connection ret=null;
        if (type==OLD)
            ret=new Connection(host, port);
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

        try {
            ret.initialize();
            ret.send(new PingRequest((byte)7));
            ret.flush();
            Message m=ret.receive();
            return ret;
        } catch (IOException e) {
            return null;
        } catch (BadPacketException e) {
            return null;
        }   
    }

    /** Tests whether c responds or doesn't responds to a ping */
    private static void testPong(Connection c, boolean expectPong) {
        System.out.println("-Testing that host "
                           +(expectPong? "sends" : "does not")
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


class EmptyResponder implements HandshakeResponder {
    public HandshakeResponse respond(HandshakeResponse response, 
            boolean outgoing) throws IOException {
        return new HandshakeResponse(new Properties());
    }
}

class DebugActivityCallback extends ActivityCallbackStub {
    public void connectionInitializing(Connection c) { 
        //System.out.println("Initializing "+str(c));
    }
    public void connectionInitialized(Connection c) { 
        System.out.println("Initialized "+str(c));
    }
    public void connectionClosed(Connection c) { 
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
