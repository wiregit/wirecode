package com.limegroup.gnutella.tests;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.routing.*;
import java.util.Properties;
import com.sun.java.util.collections.*;
import java.io.*;

/**
 * Out-of-process test to check whether ultrapeers handle query routing, leaf
 * "TTL boost", routing of marked pongs, etc.
 */
public class UltrapeerTester {
    static final int PORT=6347;
    static final int TIMEOUT=500;
    static Connection leaf;
    static Connection ultrapeer;
    static Connection old;

    public static void main(String args[]) {
        System.out.println(
            "Please make sure you have an ultrapeer with incoming slots, \n"
           +"running on port "+PORT+" of localhost, with no connections\n");
        
        try {
            connect();
            testBroadcastFromLeaf();
            testBroadcastFromOld();
            testBroadcastFromOldToLeaf();
            testPingBroadcast();
            testMisroutedPong();
            testUltrapeerPong();
            shutdown();
        } catch (IOException e) { 
            System.err.println("Mysterious IOException:");
            e.printStackTrace();
        } catch (BadPacketException e) { 
            System.err.println("Mysterious bad packet:");
            e.printStackTrace();
        }
        
        System.out.println("Done");
    }

    private static void connect() throws IOException {
        //1. unrouted 0.4 connection
        old=new Connection("localhost", PORT);
        old.initialize();

        //2. unrouted ultrapeer connection
        ultrapeer=new Connection("localhost", PORT, 
                                            new UltrapeerProperties(),
                                            new EmptyResponder(),
                                            false);
        ultrapeer.initialize();
        
        //3. routed leaf, with route table for "test"
        leaf=new Connection("localhost", PORT, 
                                       new LeafProperties(),
                                       new EmptyResponder(),
                                       false);
        leaf.initialize();
        QueryRouteTable qrt=new QueryRouteTable();
        qrt.add("test");
        for (Iterator iter=qrt.encode(null); iter.hasNext(); ) {
            leaf.send((RouteTableMessage)iter.next());
        }
    }

    private static void testBroadcastFromLeaf() 
             throws IOException, BadPacketException {
        System.out.println("-Testing normal broadcast from leaf, with TTL boost");
        drain(old);
        drain(ultrapeer);

        QueryRequest qr=new QueryRequest((byte)7, 0, "crap");
        leaf.send(qr);
        leaf.flush();
        
        Message m=old.receive(TIMEOUT);
        Assert.that(m instanceof QueryRequest);
        Assert.that(((QueryRequest)m).getQuery().equals("crap"));
        Assert.that(m.getHops()==(byte)0); //not decremented
        Assert.that(m.getTTL()==(byte)7);
      
        m=ultrapeer.receive(TIMEOUT);
        Assert.that(m instanceof QueryRequest);
        Assert.that(((QueryRequest)m).getQuery().equals("crap"));
        Assert.that(m.getHops()==(byte)0); //not decremented
        Assert.that(m.getTTL()==(byte)7);
    }

    private static void testBroadcastFromOld() 
             throws IOException, BadPacketException {
        System.out.println("-Testing normal broadcast from old connnection"
                           +", no forwarding to leaf");
        drain(ultrapeer);
        drain(leaf);

        QueryRequest qr=new QueryRequest((byte)7, 0, "crap");
        old.send(qr);
        old.flush();
              
        Message m=ultrapeer.receive(TIMEOUT);
        Assert.that(m instanceof QueryRequest);
        Assert.that(((QueryRequest)m).getQuery().equals("crap"));
        Assert.that(m.getHops()==(byte)1); 
        Assert.that(m.getTTL()==(byte)6);

        Assert.that(! drain(leaf));
    }

    private static void testBroadcastFromOldToLeaf() 
             throws IOException, BadPacketException {
        System.out.println("-Testing normal broadcast from old connnection"
                           +", with forwarding to leaf");
        drain(ultrapeer);
        drain(leaf);

        QueryRequest qr=new QueryRequest((byte)7, 0, "test");
        old.send(qr);
        old.flush();
              
        Message m=ultrapeer.receive(TIMEOUT);
        Assert.that(m instanceof QueryRequest);
        Assert.that(((QueryRequest)m).getQuery().equals("test"));
        Assert.that(m.getHops()==(byte)1); 
        Assert.that(m.getTTL()==(byte)6);

        m=leaf.receive(TIMEOUT);
        Assert.that(m instanceof QueryRequest);
        Assert.that(((QueryRequest)m).getQuery().equals("test"));
        Assert.that(m.getHops()==(byte)1); 
        Assert.that(m.getTTL()==(byte)6);
    }

    private static void testPingBroadcast() 
             throws IOException, BadPacketException {
        System.out.println("-Testing ping broadcast from old connnection"
                           +", no forwarding to leaf");
        drain(old);
        drain(leaf);

        Message m=new PingRequest((byte)7);
        ultrapeer.send(m);
        ultrapeer.flush();
              
        m=old.receive(TIMEOUT);
        Assert.that(m instanceof PingRequest);
        Assert.that(m.getHops()==(byte)1); 
        Assert.that(m.getTTL()==(byte)6);

        Assert.that(! drain(leaf));
    }


    private static void testMisroutedPong() 
             throws IOException, BadPacketException {
        System.out.println("-Testing misrouted normal pong"
                           +", not forwarded to leaf");
        drain(old);
        drain(leaf);

        Message m=new PingReply(new byte[16], 
                                (byte)7, 6399, new byte[4], 
                                0, 0, false);                                
        ultrapeer.send(m);
        ultrapeer.flush();
              
        Assert.that(! drain(old));
        Assert.that(! drain(leaf));
    }

    private static void testUltrapeerPong() 
             throws IOException, BadPacketException {
        System.out.println("-Testing misrouted ultrapeer pong"
                           +", forwarded to leaf");
        drain(old);
        drain(leaf);

        byte[] guid=new byte[16];
        guid[1]=(byte)7;  //need different GUID to fool duplicate filter
        Message m=new PingReply(guid, 
                                (byte)7, 6399, new byte[4], 
                                0, 0, true);                                
        ultrapeer.send(m);
        ultrapeer.flush();
              
        m=leaf.receive(TIMEOUT);
        Assert.that(m instanceof PingReply);
        Assert.that(((PingReply)m).getPort()==6399);        

        Assert.that(! drain(old));
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

    private static void shutdown() throws IOException {
        System.out.println("\nShutting down.");
        leaf.close();
        ultrapeer.close();
        old.close();
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
