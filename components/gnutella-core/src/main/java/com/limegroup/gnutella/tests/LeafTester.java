package com.limegroup.gnutella.tests;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.connection.*;
import com.limegroup.gnutella.handshaking.*;
import com.limegroup.gnutella.routing.*;
import java.util.Properties;
import java.util.StringTokenizer;
import com.sun.java.util.collections.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.net.*;

/**
 * Out-of-process test to check whether (multi)leaves avoid forwarding messages
 * to ultrapeers, do redirects properly, etc.  This test is interactive; you
 * must establish connections and initiate a query on your leaf.
 */
public class LeafTester {
    static final int PORT=6347;
    static final int TIMEOUT=500;
    static final byte[] ultrapeerIP=
        new byte[] {(byte)18, (byte)239, (byte)0, (byte)144};
    static final byte[] oldIP=
        new byte[] {(byte)111, (byte)22, (byte)33, (byte)44};

    static SimpleConnection ultrapeer1;
    static SimpleConnection ultrapeer2;
    static SimpleConnection old1;
    static SimpleConnection old2;

    public static void main(String args[]) {
        System.out.println(
            "Please make sure you have a leaf running with no connections,\n"
            +"listening on port "+PORT+", without any connection fetchers,\n"
            +"an empty host cache, and connection watchdogs disabled\n");
        
        try {
            connect();
            testRedirect();
            testLeafBroadcast();
            testBroadcastFromUltrapeer();
            testNoBroadcastFromOld();
            shutdown();
        } catch (IOException e) { 
            System.err.println("Mysterious IOException:");
            e.printStackTrace();
        } catch (BadPacketException e) { 
            System.err.println("Mysterious bad packet:");
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        System.out.println("Done");
     }

     ////////////////////////// Initialization ////////////////////////

     private static void connect() throws IOException, BadPacketException {
         System.out.println("Please establish a connection to localhost:6350\n");
         ultrapeer1=new SimpleConnection(accept(6350), new UltrapeerResponder());
         ultrapeer1.initialize();
         replyToPing(ultrapeer1, true);

         System.out.println("Please establish a connection to localhost:6351\n");
         ultrapeer2=new SimpleConnection(accept(6351), new UltrapeerResponder());
         ultrapeer2.initialize();
         replyToPing(ultrapeer2, true);

         System.out.println("Please establish a connection to localhost:6352\n");
         old1=new SimpleConnection(accept(6352), new OldResponder());
         old1.initialize();
         replyToPing(old1, false);

         System.out.println("Please establish a connection to localhost:6353\n");
         old2=new SimpleConnection(accept(6353), new OldResponder());
         old2.initialize();
         replyToPing(old2, false);
     }

     private static Socket accept(int port) throws IOException { 
         ServerSocketChannel channel=ServerSocketChannel.open();
         channel.socket().bind(new InetSocketAddress(port));
         Socket s=channel.accept().socket();
         InputStream in=s.getInputStream();
         String word=readWord(in);
         if (! word.equals("GNUTELLA"))
             throw new IOException("Bad word: "+word);
         return s;
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

     private static void replyToPing(SimpleConnection c, boolean ultrapeer) 
             throws IOException, BadPacketException {
         Message m=c.receive(500);
         Assert.that(m instanceof PingRequest);
         PingRequest pr=(PingRequest)m;
         byte[] localhost=new byte[] {(byte)127, (byte)0, (byte)0, (byte)1};
         PingReply reply=new PingReply(pr.getGUID(), (byte)7,
                                       c.getLocalPort(), 
                                       ultrapeer ? ultrapeerIP : oldIP,
                                       0, 0, ultrapeer);
         reply.hop();
         c.send(reply);
         c.flush();
     }

     ///////////////////////// Actual Tests ////////////////////////////

    private static void testLeafBroadcast() 
            throws IOException, BadPacketException {
        System.out.println("Please send a query for \"crap\" from your leaf\n");
        try {
            Thread.sleep(6000);
        } catch (InterruptedException e) { }
        System.out.println("-Testing broadcast from leaf");

        while (true) {
            Message m=ultrapeer1.receive(2000);
            if (m instanceof QueryRequest) {
                Assert.that(((QueryRequest)m).getQuery().equals("crap"));
                break;
            }
        }       
        while (true) {
            Message m=ultrapeer2.receive(2000);
            if (m instanceof QueryRequest) {
                Assert.that(((QueryRequest)m).getQuery().equals("crap"));
                break;
            }
        }
        while (true) {
            Message m=old1.receive(2000);
            if (m instanceof QueryRequest) {
                Assert.that(((QueryRequest)m).getQuery().equals("crap"));
                break;
            }
        }
        while (true) {
            Message m=old2.receive(2000);
            if (m instanceof QueryRequest) {
                Assert.that(((QueryRequest)m).getQuery().equals("crap"));
                break;
            }
        }
    }

    private static void testRedirect() {
        System.out.println("-Test X-Try/X-Try-Ultrapeer headers");
        SimpleConnection c=new SimpleConnection("127.0.0.1", PORT,
                                    new Properties(),
                                    new OldResponder(),
                                    false);
        try {
            c.initialize();
            Assert.that(false, "Handshake succeeded!");
        } catch (IOException e) {
            String hosts=c.getProperty(ConnectionHandshakeHeaders.X_TRY);
            //System.out.println("X-Try: "+hosts);
            Assert.that(hosts!=null, "No hosts");
            Set s=list2set(hosts);
            Assert.that(s.size()==2);
            Assert.that(s.contains(new Endpoint(oldIP, 6352)));
            Assert.that(s.contains(new Endpoint(oldIP, 6353)));

            hosts=c.getProperty(
                                ConnectionHandshakeHeaders.X_TRY_SUPERNODES);
            //System.out.println("X-Try-Ultrapeers: "+hosts);
            Assert.that(hosts!=null);
            s=list2set(hosts);
            Assert.that(s.size()==4);
            byte[] localhost=new byte[] {(byte)127, (byte)0, (byte)0, (byte)1};
            Assert.that(s.contains(new Endpoint(ultrapeerIP, 6350)));
            Assert.that(s.contains(new Endpoint(ultrapeerIP, 6351)));
            Assert.that(s.contains(new Endpoint(localhost, 6350))); 
            Assert.that(s.contains(new Endpoint(localhost, 6351)));
        }
    }

    private static void testBroadcastFromUltrapeer() throws IOException {
        System.out.println("-Test query from ultrapeer not broadcasted");
        drain(ultrapeer2);
        drain(old1);
        drain(old2);

        QueryRequest qr=new QueryRequest((byte)7, 0, "crap");
        ultrapeer1.send(qr);
        ultrapeer1.flush();

        Assert.that(! drain(ultrapeer2));
        //We don't care whether this is forwarded to the old connections
    }


    private static void testNoBroadcastFromOld() 
        throws IOException, BadPacketException {
        System.out.println("-Test query from old not broadcasted");
        drain(ultrapeer1);
        drain(ultrapeer2);
        drain(old2);

        QueryRequest qr=new QueryRequest((byte)7, 0, "crap");
        old1.send(qr);
        old1.flush();

        Assert.that(! drain(ultrapeer1));
        Assert.that(! drain(ultrapeer2));
        Message m=old2.receive(500);
        Assert.that(((QueryRequest)m).getQuery().equals("crap"));
        Assert.that(m.getHops()==(byte)1);
        Assert.that(m.getTTL()==(byte)6);         
    }

    /** Converts the given X-Try[-Ultrapeer] header value to
     *  a Set of Endpoints. */
    private static Set /* of Endpoint */ list2set(String addresses) {
        Set ret=new HashSet();
        StringTokenizer st = new StringTokenizer(addresses,
            Constants.ENTRY_SEPARATOR);
        while(st.hasMoreTokens()){
            //get an address
            String address = ((String)st.nextToken()).trim();
            Endpoint e;
            try {
                e = new Endpoint(address);
                ret.add(e);
            } catch(IllegalArgumentException iae) {
                Assert.that(false, "Bad endpoint");
            }
        }
        return ret;
    }

    //////////////////////////////////////////////////////////////////

    /** Tries to receive any outstanding messages on c 
     *  @return true if this got a message */
    private static boolean drain(SimpleConnection c) throws IOException {
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
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) { }
        ultrapeer1.close();
        ultrapeer2.close();
        old1.close();
        old2.close();
    }
}

class UltrapeerResponder implements HandshakeResponder {
    public HandshakeResponse respond(HandshakeResponse response, 
            boolean outgoing) throws IOException {
        Properties props=new Properties();
        props.put(ConnectionHandshakeHeaders.X_QUERY_ROUTING, "0.1");
        props.put(ConnectionHandshakeHeaders.X_SUPERNODE, "True");
        return new HandshakeResponse(props);
    }
}


class OldResponder implements HandshakeResponder {
    public HandshakeResponse respond(HandshakeResponse response, 
            boolean outgoing) throws IOException {
        Properties props=new Properties();
        return new HandshakeResponse(props);
    }
}
