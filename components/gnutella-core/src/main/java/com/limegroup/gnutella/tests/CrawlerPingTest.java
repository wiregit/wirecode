/*
 * CrawlerPingTest.java
 *
 * Created on January 18, 2002, 2:03 PM
 */

package com.limegroup.gnutella.tests;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.handshaking.*;
import com.limegroup.gnutella.routing.*;
import java.util.Properties;
import com.sun.java.util.collections.*;
import java.io.*;

/**
 * Tests the limewire client with a crawler Ping (Hops=0, TTL=2), and checks
 * what is returned
 * @author Anurag Singla
 */
public class CrawlerPingTest
{
    private static final String HOST = "localhost";
    private static final int PORT = 6346;
    
    private static final int LEAF=0;
    private static final int OLD=1;
    private static final int ULTRAPEER=2;
    
    private static final int NUM_LEAF_CONNECTIONS = 5;
    private static final int NUM_ULTRAPEER_CONNECTIONS = 4;
    private static final int NUM_OLD_CONNECTIONS = 2;
    
    /** Prevents connections from being garbage collected */
    private static List /* of Connection */ buffer=new LinkedList();
    
    public static void main(String[] args)
    {
        CrawlerPingTest test = new CrawlerPingTest();
        test.run();
    }
    
    public void run()
    {
        //open some leaf, ultrapeer, and old connections
        openConnections(NUM_LEAF_CONNECTIONS, LEAF);
        openConnections(NUM_ULTRAPEER_CONNECTIONS, ULTRAPEER);
        openConnections(NUM_OLD_CONNECTIONS, OLD);
        
        //open an old connection and send a ping
        Connection connection = connect(HOST, PORT, OLD);
        buffer.add(connection);
        if(connection == null)
            System.out.println("null connection");
        testPong(connection, true);
        
        System.out.println("waiting before quitting");
        try
        {
            Thread.sleep(600000);
        }
        catch(Exception e){}
    }
    
    /** Tests whether c responds or doesn't responds to a ping */
    private static void testPong(Connection c, boolean expectPong) {
        System.out.println("-Testing that host "
                           +(expectPong? "sends" : "does not")
                           +" reply to ping");
       Message m = null;
        try {
            if(c == null)
                System.out.println("null connection");
//            drain(c);
            c.send(new PingRequest((byte)2));
            c.flush();
            while(true)
            {
                try {
                    m=c.receive(10000);
                } catch (InterruptedIOException e) {
                    return;
                }
                System.out.println(m);
            }
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
    
    
    private void openConnections(int numConnections, int type)
    {
        for(int i=0; i < numConnections; i++)
        {
            Connection tmp=connect(HOST, PORT, type);
            if (tmp==null)
                break;
            buffer.add(tmp);
        }
    }
    
    /** 
     * Returns a connection of the given type to host:port, 
     * or null if it failed.
     *   @param type one of t
     */
    private static Connection connect(String host, int port, int type)
    {
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
        
        try
        {
            ret.initialize();
//            ret.send(new PingRequest((byte)7));
//            ret.flush();
//            Message m=ret.receive();
            return ret;
        } catch (IOException e)
        {
            return null;
        } 
//        catch (BadPacketException e)
//        {
//            return null;
//        }
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
    
